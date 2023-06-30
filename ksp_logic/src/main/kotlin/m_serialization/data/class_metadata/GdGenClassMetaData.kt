package m_serialization.data.class_metadata

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import m_serialization.data.prop_meta_data.*
import m_serialization.data.prop_meta_data.PrimitiveType.*
import m_serialization.utils.KSClassDecUtils.getAllActualChild
import java.io.OutputStream
import java.util.*

class GdStream(private val stream: OutputStream, private val indent: Int = 0) {
    fun line(line: String) {
        stream.write(("\t".repeat(indent) + line + "\n").toByteArray())
    }

    fun withTab(f: GdStream.() -> Unit) {
        f(GdStream(stream, indent + 1))
    }

    fun <T> forWithTab(iterable: Iterable<T>, f: GdStream.(T) -> Unit) {
        for (value in iterable) {
            f(GdStream(stream, indent + 1), value)
        }
    }
}

class GdGenClassMetaData : ClassMetaData() {
    private fun isClassAbstract(classDec: KSClassDeclaration): Boolean {
        return classDec.modifiers.contains(Modifier.SEALED)
    }

    override fun doGenCode(codeGenerator: CodeGenerator) {
        val bufferClass = "ZfooByteBuffer"
        val isAbstract = isClassAbstract(classDec)
        val props = constructorProps + otherProps
        val classSig = getTypeSig(classDec)
        GdStream(
            codeGenerator.createNewFile(
                Dependencies(false),
                classDec.packageName.asString(),
                classDec.simpleName.asString(),
                "gd"
            )
        ).apply {
            props.mapNotNull { prop ->
                when (prop) {
                    is ListObjectPropMetaData -> prop.elementClass
                    is ListPrimitivePropMetaData -> null
                    is MapObjectValueMetaData -> prop.valueClassDec
                    is MapPrimitiveValueMetaData -> null
                    is ObjectPropMetaData -> prop.classDec
                    is PrimitivePropMetaData -> null
                }
            }.toSet().forEach { classDec ->
                val importName = getTypeSig(classDec)
                line(
                    "const ${importName} = preload('res://m/${
                        classDec.packageName.asString().replace(".", "/")
                    }/${importName}.gd').$importName"
                )
            }
            if (isAbstract) {
                classDec.getAllActualChild().forEach { kclass ->
                    val importName = getTypeSig(kclass)
                    line(
                        "const ${importName} = preload('res://m/${
                            kclass.packageName.asString().replace(".", "/")
                        }/${importName}.gd').$importName"
                    )
                }
            }
            val parent = run {
                val parent =
                    classDec.superTypes.find { (it.resolve().declaration as KSClassDeclaration).classKind == ClassKind.CLASS }
                if (parent != null && globalUniqueTag.contains(parent.resolve().declaration)) {
                    parent
                } else null
            }
            if (parent != null) {
                val parentDec = parent.resolve().declaration as KSClassDeclaration
                val importName = getTypeSig(parentDec)
                line(
                    "const ${importName} = preload('res://m/${
                        parentDec.packageName.asString().replace(".", "/")
                    }/${importName}.gd').$importName"
                )
            }


            line("class $classSig:")
            withTab {
                if (parent != null) {
                    val parentDec = parent.resolve().declaration as KSClassDeclaration
                    line("extends ${getTypeSig(parentDec)}")
                }
                val tag = protocolUniqueId
                // declaration
                props.mapNotNull { if (it.propDec.findOverridee() == null) it else null }.forEach { prop ->
                    line("var ${prop.name}: ${getTypeSig(prop)}")
                }
                // constructor
                if (!isAbstract) {
                    line("")
                    val params = constructorProps.map { prop ->
                        "${prop.name}_: ${getTypeSig(prop)}"
                    }.joinToString(", ")
                    line("func _init($params):")
                    forWithTab(constructorProps) { prop ->
                        line("${prop.name} = ${prop.name}_")
                    }
                }
                // serialize
                if (!isAbstract) {
                    line("")
                    line("func write_to(buffer: $bufferClass):")
                    withTab {
                        for (prop in props) {
                            val varName = prop.name
                            when (prop) {
                                is ListObjectPropMetaData -> {
                                    line("# ListObjectPropMetaData")
                                    line("buffer.writeInt($varName.size())")
                                    line("for e in $varName:")
                                    withTab {
                                        if (isClassAbstract(prop.elementClass)) {
                                            line("e.write_with_tag(buffer)")
                                        } else {
                                            line("e.write_to(buffer)")
                                        }
                                    }
                                }

                                is ListPrimitivePropMetaData -> {
                                    line("# ListPrimitivePropMetaData")
                                    line("buffer.writeInt($varName.size())")
                                    line("for e in $varName:")
                                    withTab {
                                        bufferWritePrimitive(prop.type, "e")
                                    }
                                }

                                is MapObjectValueMetaData -> {
                                    line("# MapObjectValueMetaData")
                                    line("buffer.writeInt($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        if (isClassAbstract(prop.valueClassDec)) {
                                            line("$varName[key].write_with_tag(buffer)")
                                        } else {
                                            line("$varName[key].write_to(buffer)")
                                        }
                                    }
                                }

                                is MapPrimitiveValueMetaData -> {
                                    line("# MapPrimitiveValueMetaData")
                                    line("buffer.writeInt($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        bufferWritePrimitive(prop.keyType, "$varName[key]")
                                    }
                                }

                                is ObjectPropMetaData ->
                                    if (isClassAbstract(prop.classDec)) {
                                        line("$varName.write_with_tag(buffer)")
                                    } else {
                                        line("$varName.write_to(buffer)")
                                    }

                                is PrimitivePropMetaData ->
                                    bufferWritePrimitive(prop.type, varName)
                            }
                        }
                    }
                    line("func write_with_tag(buffer: $bufferClass):")
                    withTab {
                        line("buffer.writeShort($tag)")
                        line("write_to(buffer)")
                    }
                }
                // deserialize
                line("")
                line("static func read_${classSig}_from(buffer: $bufferClass) -> $classSig:")
                if (!isAbstract) withTab {
                    props.forEach { prop ->
                        val varName = prop.name
                        val typeSig = getTypeSig(prop)
                        when (prop) {
                            is ListObjectPropMetaData -> {
                                line("# ListObjectPropMetaData")
                                line("var $varName: $typeSig = []")
                                line("$varName.resize(buffer.readInt())")
                                line("for i in $varName.size():")
                                withTab {
                                    val elementSig = getTypeSig(prop.elementClass)
                                    line("var val = $elementSig.read_${elementSig}_from(buffer)")
                                    line("$varName[i] = val")
                                }
                            }

                            is ListPrimitivePropMetaData -> {
                                line("# ListPrimitivePropMetaData")
                                line("var $varName: $typeSig = []")
                                line("$varName.resize(buffer.readInt())")
                                line("for i in $varName.size():")
                                withTab {
                                    bufferReadPrimitive(prop.type, "val")
                                    line("$varName[i] = val")
                                }
                            }

                            is MapObjectValueMetaData -> {
                                line("# MapObjectValueMetaData")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.readInt():")
                                withTab {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    val valueSig = getTypeSig(prop.valueClassDec)
                                    line("var val = $valueSig.read_${valueSig}_from(buffer)")
                                    line("$varName[key] = val")
                                }
                            }

                            is MapPrimitiveValueMetaData -> {
                                line("# MapPrimitiveValueMetaData")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.readInt():")
                                withTab {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    bufferReadPrimitive(prop.keyType, "val")
                                    line("$varName[key] = val")
                                }
                            }

                            is ObjectPropMetaData -> {
                                val classSig = getTypeSig(prop.classDec)
                                line("var $varName := $classSig.read_${classSig}_from(buffer)")
                            }

                            is PrimitivePropMetaData ->
                                bufferReadPrimitive(prop.type, varName)
                        }
                    }
                    // constructor
                    val params = constructorProps.map { prop -> prop.name }.joinToString(", ")
                    line("var ret := $classSig.new($params)")
                    for (prop in otherProps) {
                        val varName = prop.name
                        line("ret.$varName = $varName")
                    }
                    line("return ret")
                }
                else withTab {
                    line("match buffer.readShort():")
                    forWithTab(classDec.getAllActualChild()) { kclass ->
                        val tag = globalUniqueTag.getValue(kclass)
                        line("$tag:")
                        val typeSig = getTypeSig(kclass)
                        withTab {
                            line("return $typeSig.read_${typeSig}_from(buffer)")
                        }
                    }
                    line("breakpoint")
                    line("return null")
                }
                // utils
                run {
                    line("")
                    line("func _to_string_with_tab(tab: int) -> String:")
                    val params = props.map { prop ->
                        val varName = prop.name
                        "'\\n' + t1 + '$varName: ' + ${getStr(varName, prop)}"
                    }.joinToString(" + ")
                    withTab {
                        line("var t0 := '  '.repeat(tab)")
                        line("var t1 := t0 + '  '")
                        line("var t2 := t1 + '  '")
                        line("return '$classSig {' + $params + '\\n' + t0 + '}'")
                    }
                    line("func _to_string():")
                    withTab {
                        line("return _to_string_with_tab(0)")
                    }
                }
            }
        }
    }

    private fun getStr(varName: String, prop: AbstractPropMetadata) = when (prop) {
        is ListObjectPropMetaData -> "'Array<${getTypeSig(prop.elementClass)}>(%s) [\\n%s%s\\n%s]' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.map(func(v): return v._to_string_with_tab(tab + 2)))), t1]"
        is ListPrimitivePropMetaData -> "'Array<${getTypeSig(prop.type)}>(%s) [\\n%s%s\\n%s]' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.map(func(v): return str(v)))), t1]"
        is MapObjectValueMetaData -> "'Map<${getTypeSig(prop.keyType)}, ${getTypeSig(prop.valueClassDec)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, $varName[key]._to_string_with_tab(tab + 2)]))), t1]"
        is MapPrimitiveValueMetaData -> "'Map<${getTypeSig(prop.keyType)}, ${getTypeSig(prop.valueType)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, str($varName[key])]))), t1]"
        is ObjectPropMetaData -> "$varName._to_string_with_tab(tab + 1)"
        is PrimitivePropMetaData -> "str($varName)"
    }

    private fun getTypeSig(prop: AbstractPropMetadata) = when (prop) {
        is ListObjectPropMetaData -> "Array[${getTypeSig(prop.elementClass)}]"
        is ListPrimitivePropMetaData -> "Array[${getTypeSig(prop.type)}]"
        is MapObjectValueMetaData -> "Dictionary"
        is MapPrimitiveValueMetaData -> "Dictionary"
        is ObjectPropMetaData -> getTypeSig(prop.classDec)
        is PrimitivePropMetaData -> getTypeSig(prop.type)
    }

    private fun getTypeSig(kclass: KSClassDeclaration) = kclass.simpleName.asString()
    private fun getTypeSig(primitive: PrimitiveType) = when (primitive) {
        INT -> "int"
        SHORT -> "int"
        DOUBLE -> "float"
        BYTE -> "int"
        BOOL -> "bool"
        FLOAT -> "float"
        LONG -> "int"
        STRING -> "String"
    }

    private fun GdStream.bufferReadPrimitive(
        varType: PrimitiveType,
        varName: String,
    ) {
        when (varType) {
            INT -> line("var $varName := buffer.readInt()")
            SHORT -> line("var $varName := buffer.readShort()")
            DOUBLE -> line("var $varName := buffer.readDouble()")
            BYTE -> line("var $varName := buffer.readByte()")
            BOOL -> line("var $varName := buffer.readBool()")
            FLOAT -> line("var $varName := buffer.readFloat()")
            LONG -> line("var $varName := buffer.readLong()")
            STRING -> line("var $varName := buffer.readString()")
        }
    }

    private fun GdStream.bufferWritePrimitive(
        varType: PrimitiveType,
        varName: String,
    ) {
        when (varType) {
            INT -> line("buffer.writeInt($varName)")
            SHORT -> line("buffer.writeShort($varName)")
            DOUBLE -> line("buffer.writeDouble($varName)")
            BYTE -> line("buffer.writeByte($varName)")
            BOOL -> line("buffer.writeBool($varName)")
            FLOAT -> line("buffer.writeFloat($varName)")
            LONG -> line("buffer.writeLong($varName)")
            STRING -> line("buffer.writeString($varName)")
        }
    }


    // String extensions
    private fun String.camelToSnakeCase(): String {
        val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
        return camelRegex.replace(this) {
            "_${it.value}"
        }.lowercase(Locale.getDefault())
    }

    private fun String.snakeToLowerCamelCase(): String {
        val snakeRegex = "_[a-zA-Z]".toRegex()
        return snakeRegex.replace(this) {
            it.value.replace("_", "")
                .uppercase(Locale.getDefault())
        }
    }

    private fun String.snakeToUpperCamelCase(): String {
        return this.snakeToLowerCamelCase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}