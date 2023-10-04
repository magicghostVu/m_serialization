package m_serialization.data.class_metadata

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import m_serialization.data.gen_protocol_version.IGenFileProtocolVersion
import m_serialization.data.prop_meta_data.*
import m_serialization.data.prop_meta_data.PrimitiveType.*
import m_serialization.utils.KSClassDecUtils.getAllActualChild
import m_serialization.utils.KSClassDecUtils.getAllEnumEntrySimpleName
import java.io.OutputStream
import java.util.*

class GdStream(private val stream: OutputStream, private val indent: Int = 0) {
    fun line(line: String) {
        if (line.isEmpty()) {
            stream.write("\n".toByteArray())
        } else {
            stream.write(("\t".repeat(indent) + line + "\n").toByteArray())
        }
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

class GdGenClassMetaData(val rootFolderGen: String) : ClassMetaData() {
    val root = if (rootFolderGen == "") {
        "m"
    } else {
        rootFolderGen
    }

    private fun isClassAbstract(classDec: KSClassDeclaration): Boolean {
        return classDec.modifiers.contains(Modifier.SEALED)
    }

    override fun doGenCode(codeGenerator: CodeGenerator) {
        if (classDec.modifiers.contains(Modifier.ENUM)) {
            genEnum(codeGenerator)
        } else {
            genClass(codeGenerator)
        }
    }

    private fun genClass(codeGenerator: CodeGenerator) {
        val bufferClass = "StreamPeer"
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
            props.flatMap { prop ->
                when (prop) {
                    is ListObjectPropMetaData -> listOf(prop.elementClass)
                    is ListPrimitivePropMetaData -> listOf()
                    is ObjectPropMetaData -> listOf(prop.classDec)
                    is PrimitivePropMetaData -> listOf()
                    is EnumPropMetaData -> listOf(prop.enumClass)
                    is ListEnumPropMetaData -> listOf(prop.enumClass)
                    is MapEnumKeyEnumValue -> listOf(prop.enumKey)
                    is MapEnumKeyObjectValuePropMetaData -> listOf(prop.enumKey, prop.valueType)
                    is MapEnumKeyPrimitiveValuePropMetaData -> listOf(prop.enumKey)
                    is MapPrimitiveKeyEnumValue -> listOf(prop.enumValue)
                    is MapPrimitiveKeyObjectValueMetaData -> listOf(prop.valueClassDec)
                    is MapPrimitiveKeyValueMetaData -> listOf()
                }
            }.toSet().forEach { classDec ->
                val importName = getTypeSig(classDec)
                line(
                    "const ${importName} = preload('res://${root}/${
                        classDec.packageName.asString().replace(".", "/")
                    }/${importName}.gd').$importName"
                )
            }
            if (isAbstract) {
                classDec.getAllActualChild().forEach { kclass ->
                    val importName = getTypeSig(kclass)
                    line(
                        "const ${importName} = preload('res://${root}/${
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
                    "const ${importName} = preload('res://${root}/${
                        parentDec.packageName.asString().replace(".", "/")
                    }/${importName}.gd').$importName"
                )
            }

            line("class $classSig:")
            withTab {
                if (parent != null) {
                    val parentDec = parent.resolve().declaration as KSClassDeclaration
                    line("extends ${getTypeSig(parentDec)}")
                } else {
                    line("extends 'res://${root}/IPacket.gd'")
                }
                val tag = protocolUniqueId
                // const tag
                if (tag >= 0) {
                    line("const TAG = $tag")
                }
                // declaration
                props.mapNotNull { if (it.propDec.findOverridee() == null) it else null }.forEach { prop ->
                    line("var ${prop.name}: ${getTypeSig(prop)}")
                }
                // constructor
                if (!isAbstract && constructorProps.isNotEmpty()) {
                    line("")
                    val params = constructorProps.map { prop ->
                        "${prop.name}_"
                    }.joinToString(", ")
                    line("func _init($params):")
                    forWithTab(constructorProps) { prop ->
                        line("${prop.name} = ${prop.name}_")
                    }
                }
                // tag
                if (tag >= 0) {
                    line("")
                    line("func get_tag() -> int:")
                    withTab {
                        line("return $tag")
                    }
                }
                // serialize
                if (!isAbstract) {
                    line("")
                    line("func write_to(buffer: $bufferClass, with_tag: bool) -> void:")
                    withTab {
                        line("if with_tag: buffer.put_16($tag)")
                        for (prop in props) {
                            val varName = prop.name
                            when (prop) {
                                is ListObjectPropMetaData -> {
                                    line("# ListObjectPropMetaData")
                                    line("buffer.put_u16($varName.size())")
                                    line("for e in $varName:")
                                    withTab {
                                        if (isClassAbstract(prop.elementClass)) {
                                            line("e.write_to(buffer, true)")
                                        } else {
                                            line("e.write_to(buffer, false)")
                                        }
                                    }
                                }

                                is ListPrimitivePropMetaData -> {
                                    line("# ListPrimitivePropMetaData")
                                    line("buffer.put_u16($varName.size())")
                                    line("for e in $varName:")
                                    withTab {
                                        bufferWritePrimitive(prop.type, "e")
                                    }
                                }

                                is MapPrimitiveKeyObjectValueMetaData -> {
                                    line("# MapObjectValueMetaData")
                                    line("buffer.put_u16($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        if (isClassAbstract(prop.valueClassDec)) {
                                            line("$varName[key].write_to(buffer, true)")
                                        } else {
                                            line("$varName[key].write_to(buffer, false)")
                                        }
                                    }
                                }

                                is MapPrimitiveKeyValueMetaData -> {
                                    line("# MapPrimitiveValueMetaData")
                                    line("buffer.put_u16($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        bufferWritePrimitive(prop.keyType, "$varName[key]")
                                    }
                                }

                                is ObjectPropMetaData ->
                                    if (isClassAbstract(prop.classDec)) {
                                        line("$varName.write_to(buffer, true)")
                                    } else {
                                        line("$varName.write_to(buffer, false)")
                                    }

                                is PrimitivePropMetaData ->
                                    bufferWritePrimitive(prop.type, varName)

                                is EnumPropMetaData -> line("buffer.put_16($varName)")
                                is ListEnumPropMetaData -> {
                                    line("# ListEnumPropMetaData")
                                    line("buffer.put_u16($varName.size())")
                                    line("for e in $varName:")
                                    withTab {
                                        line("buffer.put_16(e)")
                                    }
                                }

                                is MapEnumKeyEnumValue -> {
                                    line("# MapEnumKeyEnumValue")
                                    line("buffer.put_u16($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        line("buffer.put_16(key)")
                                        line("buffer.put_16($varName[key])")
                                    }
                                }

                                is MapEnumKeyObjectValuePropMetaData -> {
                                    line("# MapPrimitiveKeyEnumValue")
                                    line("buffer.put_u16($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        line("buffer.put_16(key)")
                                        if (isClassAbstract(prop.valueType)) {
                                            line("$varName[key].write_to(buffer, true)")
                                        } else {
                                            line("$varName[key].write_to(buffer, false)")
                                        }
                                    }
                                }

                                is MapEnumKeyPrimitiveValuePropMetaData -> {
                                    line("# MapPrimitiveKeyEnumValue")
                                    line("buffer.put_u16($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        line("buffer.put_16(key)")
                                        bufferWritePrimitive(prop.valueType, "$varName[key]")
                                    }
                                }

                                is MapPrimitiveKeyEnumValue -> {
                                    line("# MapPrimitiveKeyEnumValue")
                                    line("buffer.put_u16($varName.size())")
                                    line("for key in $varName:")
                                    withTab {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        line("buffer.put_16($varName[key])")
                                    }
                                }
                            }
                        }
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
                                line("$varName.resize(buffer.get_u16())")
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
                                line("$varName.resize(buffer.get_u16())")
                                line("for i in $varName.size():")
                                withTab {
                                    bufferReadPrimitive(prop.type, "val")
                                    line("$varName[i] = val")
                                }
                            }

                            is MapPrimitiveKeyObjectValueMetaData -> {
                                line("# MapObjectValueMetaData")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.get_u16():")
                                withTab {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    val valueSig = getTypeSig(prop.valueClassDec)
                                    line("$varName[key] = $valueSig.read_${valueSig}_from(buffer)")
                                }
                            }

                            is MapPrimitiveKeyValueMetaData -> {
                                line("# MapPrimitiveValueMetaData")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.get_u16():")
                                withTab {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    bufferReadPrimitive(prop.valueType, "val")
                                    line("$varName[key] = val")
                                }
                            }

                            is ObjectPropMetaData -> {
                                val classSig = getTypeSig(prop.classDec)
                                line("var $varName := $classSig.read_${classSig}_from(buffer)")
                            }

                            is PrimitivePropMetaData ->
                                bufferReadPrimitive(prop.type, varName)

                            is EnumPropMetaData -> line("var $varName := buffer.get_16() as ${getTypeSig(prop.enumClass)}")
                            is ListEnumPropMetaData -> {
                                line("# ListEnumPropMetaData")
                                line("var $varName: $typeSig = []")
                                line("$varName.resize(buffer.get_u16())")
                                line("for i in $varName.size():")
                                withTab {
                                    line("$varName[i] = buffer.get_16() as ${getTypeSig(prop.enumClass)}")
                                }
                            }

                            is MapEnumKeyEnumValue -> {
                                line("# MapEnumKeyPrimitiveValuePropMetaData")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.get_u16():")
                                withTab {
                                    line("var key := buffer.get_16() as ${getTypeSig(prop.enumKey)}")
                                    line("$varName[key] = buffer.get_16() as ${getTypeSig(prop.enumValue)}")
                                }
                            }

                            is MapEnumKeyObjectValuePropMetaData -> {
                                line("# MapEnumKeyPrimitiveValuePropMetaData")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.get_u16():")
                                withTab {
                                    line("var key := buffer.get_16() as ${getTypeSig(prop.enumKey)}")
                                    val valueSig = getTypeSig(prop.valueType)
                                    line("$varName[key] = $valueSig.read_${valueSig}_from(buffer)")
                                }
                            }

                            is MapEnumKeyPrimitiveValuePropMetaData -> {
                                line("# MapEnumKeyPrimitiveValuePropMetaData")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.get_u16():")
                                withTab {
                                    line("var key := buffer.get_16() as ${getTypeSig(prop.enumKey)}")
                                    bufferReadPrimitive(prop.valueType, "val")
                                    line("$varName[key] = val")
                                }
                            }

                            is MapPrimitiveKeyEnumValue -> {
                                line("# MapPrimitiveKeyEnumValue")
                                line("var $varName: $typeSig = {}")
                                line("for i in buffer.get_u16():")
                                withTab {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    line("$varName[key] = buffer.get_16() as ${getTypeSig(prop.enumValue)}")
                                }
                            }
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
                    line("var tag = buffer.get_16()")
                    line("match tag:")
                    forWithTab(classDec.getAllActualChild()) { kclass ->
                        val tag = globalUniqueTag.getOrDefault(kclass, -1)
                        val typeSig = getTypeSig(kclass)
                        line("$typeSig.TAG: # $tag")
                        withTab {
                            line("return $typeSig.read_${typeSig}_from(buffer)")
                        }
                    }
                    line("print('matching $classSig, tag not recognized:', tag)")
                    line("breakpoint")
                    line("return null")
                }

                // utils
                run {
                    line("")
                    line("static func read_${classSig}_from_bytes(bytes: PackedByteArray, big_endian := true) -> $classSig:")
                    withTab {
                        line("var buffer := StreamPeerBuffer.new()")
                        line("buffer.data_array = bytes")
                        line("buffer.big_endian = big_endian")
                        line("return read_${classSig}_from(buffer)")
                    }
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
                        line("return '$classSig {' + ${params.ifEmpty { "''" }} + '\\n' + t0 + '}'")
                    }
                    line("func _to_string():")
                    withTab {
                        line("return _to_string_with_tab(0)")
                    }
                }
            }
        }
    }

    private fun genEnum(codeGenerator: CodeGenerator) {
        GdStream(
            codeGenerator.createNewFile(
                Dependencies(false),
                classDec.packageName.asString(),
                classDec.simpleName.asString(),
                "gd"
            )
        ).apply {
            line("enum ${classDec.simpleName.asString()} {")
            forWithTab(classDec.getAllEnumEntrySimpleName()) { enumName ->
                line("$enumName,")
            }
            line("}")
            line("")
        }
    }

    private fun getStr(varName: String, prop: AbstractPropMetadata) = when (prop) {
        is ListObjectPropMetaData -> "'Array<${getTypeSig(prop.elementClass)}>(%s) [\\n%s%s\\n%s]' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.map(func(v): return v._to_string_with_tab(tab + 2)))), t1]"
        is ListPrimitivePropMetaData -> "'Array<${getTypeSig(prop.type)}>(%s) [\\n%s%s\\n%s]' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.map(func(v): return str(v)))), t1]"
        is MapPrimitiveKeyObjectValueMetaData -> "'Map<${getTypeSig(prop.keyType)}, ${getTypeSig(prop.valueClassDec)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, $varName[key]._to_string_with_tab(tab + 2)]))), t1]"
        is MapPrimitiveKeyValueMetaData -> "'Map<${getTypeSig(prop.keyType)}, ${getTypeSig(prop.valueType)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, str($varName[key])]))), t1]"
        is ObjectPropMetaData -> "$varName._to_string_with_tab(tab + 1)"
        is PrimitivePropMetaData -> "str($varName)"
        is EnumPropMetaData -> "str($varName)"
        is ListEnumPropMetaData -> "'Array<${getTypeSig(prop.enumClass)}>(%s) [\\n%s%s\\n%s]' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.map(func(v): return str(v)))), t1]"
        is MapEnumKeyEnumValue -> "'Map<${getTypeSig(prop.enumKey)}, ${getTypeSig(prop.enumValue)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, str($varName[key])]))), t1]"
        is MapEnumKeyObjectValuePropMetaData -> "'Map<${getTypeSig(prop.enumKey)}, ${getTypeSig(prop.valueType)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, $varName[key]._to_string_with_tab(tab + 2)]))), t1]"
        is MapEnumKeyPrimitiveValuePropMetaData -> "'Map<${getTypeSig(prop.enumKey)}, ${getTypeSig(prop.valueType)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, str($varName[key])]))), t1]"
        is MapPrimitiveKeyEnumValue -> "'Map<${getTypeSig(prop.keyType)}, ${getTypeSig(prop.enumValue)}>(%s) {\\n%s%s\\n%s}' % [$varName.size(), t2, (',\\n' + t2).join(PackedStringArray($varName.keys().map(func(key): return '%s: %s' % [key, str($varName[key])]))), t1]"
    }

    private fun getTypeSig(prop: AbstractPropMetadata) = when (prop) {
        is ListObjectPropMetaData -> "Array[${getTypeSig(prop.elementClass)}]"
        is ListPrimitivePropMetaData -> "Array[${getTypeSig(prop.type)}]"
        is MapPrimitiveKeyObjectValueMetaData -> "Dictionary"
        is MapPrimitiveKeyValueMetaData -> "Dictionary"
        is ObjectPropMetaData -> getTypeSig(prop.classDec)
        is PrimitivePropMetaData -> getTypeSig(prop.type)
        is EnumPropMetaData -> getTypeSig(prop.enumClass)
        is ListEnumPropMetaData -> "Array"
        is MapEnumKeyEnumValue -> "Dictionary"
        is MapEnumKeyObjectValuePropMetaData -> "Dictionary"
        is MapEnumKeyPrimitiveValuePropMetaData -> "Dictionary"
        is MapPrimitiveKeyEnumValue -> "Dictionary"
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
        BYTE_ARRAY -> "PackedByteArray"
    }

    private fun name_join(name: String, prefix: String): String {
        val alphaNumRegex = Regex("[^A-Za-z0-9 ]")
        return alphaNumRegex.replace(name, "_") + prefix;
    }

    private fun GdStream.bufferReadPrimitive(
        varType: PrimitiveType,
        varName: String,
    ) {
        when (varType) {
            INT -> line("var $varName := buffer.get_32()")
            SHORT -> line("var $varName := buffer.get_16()")
            DOUBLE -> line("var $varName := buffer.get_double()")
            BYTE -> line("var $varName := buffer.get_8()")
            BOOL -> line("var $varName := buffer.get_8() != 0")
            FLOAT -> line("var $varName := buffer.get_float()")
            LONG -> line("var $varName := buffer.get_64()")
            STRING -> {
                val varNameLength = name_join(varName, "length")
                line("var $varNameLength := buffer.get_u16()")
                line("var $varName := PackedByteArray(buffer.get_data($varNameLength)[1]).get_string_from_utf8()")
            }

            BYTE_ARRAY -> {
                val varNameSize = name_join(varName, "size")
                line("var $varNameSize := buffer.get_u16()")
                line("var $varName := buffer.get_data($varNameSize)[1] as PackedByteArray")
            }
        }
    }

    private fun GdStream.bufferWritePrimitive(
        varType: PrimitiveType,
        varName: String,
    ) {
        when (varType) {
            INT -> line("buffer.put_32($varName)")
            SHORT -> line("buffer.put_16($varName)")
            DOUBLE -> line("buffer.put_double($varName)")
            BYTE -> line("buffer.put_8($varName)")
            BOOL -> line("buffer.put_8($varName)")
            FLOAT -> line("buffer.put_float($varName)")
            LONG -> line("buffer.put_64($varName)")
            STRING -> {
                val varNameBytes = name_join(varName, "bytes")
                line("var $varNameBytes = $varName.to_utf8_buffer()")
                line("buffer.put_u16($varNameBytes.size())")
                line("buffer.put_data($varNameBytes)")
            }

            BYTE_ARRAY -> {
                line("buffer.put_u16($varName.size())")
                line("buffer.put_data($varName)")
            }
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

object GdGenFileProtocolVersion : IGenFileProtocolVersion {

    const val template = """## Just a trait
##
## Since gdscript does not have trait, we use inheritance instead. All m classes derive from this class.

const PROTOCOL_VERSION = {PROTOCOL_VERSION}

func get_tag() -> int:
	return 0

## Write this packet to a stream
func write_to(buffer: StreamPeer, with_tag: bool) -> void:
	pass

## Pack this packet to a byte array
func to_byte_array(with_tag: bool, big_endian := true) -> PackedByteArray:
	var buffer := StreamPeerBuffer.new()
	buffer.big_endian = big_endian
	write_to(buffer, with_tag)
	return buffer.data_array
"""

    override fun genFileProtocolVersion(codeGenerator: CodeGenerator, protocolVersion: Int) {
        run {
            val stream = codeGenerator.createNewFile(
                Dependencies(false),
                "",
                "IPacket",
                "gd"
            )
            stream.write(template.replace("{PROTOCOL_VERSION}", protocolVersion.toString()).toByteArray())
        }
    }
}