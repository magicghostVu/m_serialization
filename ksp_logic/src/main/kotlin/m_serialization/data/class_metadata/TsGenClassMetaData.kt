package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import m_serialization.data.gen_protocol_version.IGenFileProtocolVersion
import m_serialization.data.prop_meta_data.*
import m_serialization.utils.KSClassDecUtils.getAllActualChild
import m_serialization.utils.KSClassDecUtils.getAllEnumEntrySimpleName
import java.io.OutputStream
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

class TsWriter(private val stream: OutputStream) {
    fun line(line: String) {
        if (line.isEmpty()) {
            stream.write("\n".toByteArray())
        } else {
            stream.write((line + "\n").toByteArray())
        }
    }

    fun withBlock(f: TsWriter.() -> Unit) {
        beginBlock()
        f(TsWriter(stream))
        endBlock()
    }

    fun <T> forWithBlock(iterable: Iterable<T>, f: TsWriter.(T) -> Unit) {
        beginBlock()
        for (value in iterable) {
            f(this, value)
        }
        endBlock()
    }

    private fun beginBlock() {
        stream.write("{\n".toByteArray());
    }

    private fun endBlock() {
        stream.write("}\n".toByteArray());
    }
}


class TsGenClassMetaData(val rootFolderGen: String) : ClassMetaData() {
    val root = if (rootFolderGen == "") {
        "m"
    } else {
        rootFolderGen
    }
    lateinit var full_pk: String

    private fun isClassAbstract(classDec: KSClassDeclaration): Boolean {
        return classDec.modifiers.contains(Modifier.SEALED)
    }

    override fun doGenCode(codeGenerator: CodeGenerator) {
        full_pk = "$root.${classDec.packageName.asString()}"
        val writer = TsWriter(
            codeGenerator.createNewFile(
                Dependencies(false),
                full_pk,
                classDec.simpleName.asString(),
                "ts"
            )
        )

        val props = constructorProps + otherProps
        val refPaths = props.flatMap { prop ->
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
        }.toSet()
        val currentPath = Path(full_pk.replace(".", "/"))
        refPaths.forEach { classDec ->
            val path =
                Path("$root.${classDec.packageName.asString()}.${classDec.simpleName.asString()}".replace(".", "/"))
            val rel = path.relativeTo(currentPath).invariantSeparatorsPathString
            writer.line(
                "/// <reference path='$rel.ts' />"
            )
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
            val path =
                Path("$root.${parentDec.packageName.asString()}.${parentDec.simpleName.asString()}".replace(".", "/"))
            val rel = path.relativeTo(currentPath).invariantSeparatorsPathString
            writer.line(
                "/// <reference path='$rel.ts' />"
            )
        }
        writer.line("namespace ${classDec.packageName.asString()}")
        writer.withBlock {
            if (classDec.modifiers.contains(Modifier.ENUM)) {
                genEnum(writer)
            } else {
                genClass(writer, refPaths)
            }
        }
    }


    private fun genClass(writer: TsWriter, refPaths: Set<KSClassDeclaration>) {
        val inBufferClass = "engine.InPacket"
        val outBufferClass = "engine.OutPacket"
        val isAbstract = isClassAbstract(classDec)
        val props = constructorProps + otherProps
        val classSig = classDec.simpleName.asString()
        writer.apply {
            // import deps
            /*refPaths.forEach { classDec ->
                val importName = getTypeSig(classDec)
                line(
                    "import $importName = $root.${classDec.packageName.asString()}.$importName"
                )
            }*/
            // import child class
            /*if (isAbstract) {
                classDec.getAllActualChild().forEach { kclass ->
                    val importName = getTypeSig(kclass)
                    line(
                        "import $importName = $root.${kclass.packageName.asString()}.$importName"
                    )
                }
            }*/
            val parent = run {
                val parent =
                    classDec.superTypes.find { (it.resolve().declaration as KSClassDeclaration).classKind == ClassKind.CLASS }
                if (parent != null && globalUniqueTag.contains(parent.resolve().declaration)) {
                    parent
                } else null
            }
            // import base class
            /*if (parent != null) {
                val parentDec = parent.resolve().declaration as KSClassDeclaration
                val importName = getTypeSig(parentDec)
                line(
                    "import $importName = $root.${parentDec.packageName.asString()}.$importName"
                )
            }*/

            if (parent != null) {
                val parentDec = parent.resolve().declaration as KSClassDeclaration
                line("export ${if (isAbstract) "abstract " else ""}class $classSig extends ${getTypeSig(parentDec)}")
            } else {
                line("export ${if (isAbstract) "abstract " else ""}class $classSig implements JavaClass")
            }
            withBlock {
                val tag = protocolUniqueId
                // const tag
                if (tag >= 0) {
                    line("static TAG = $tag;")
                }
                // declaration
                otherProps.mapNotNull { withoutOverridee(it) }.forEach { prop ->
                    line("public ${prop.name}?: ${getTypeSig(prop)}")
                }
                // constructor
                if (constructorProps.isNotEmpty()) {
                    val params = constructorProps.map { prop ->
                        "public ${prop.name}: ${getTypeSig(prop)}"
                    }.joinToString(", ")
                    line("constructor($params)")
                    withBlock {
                        if (parent != null) {
                            val parentDec = parent.resolve().declaration as KSClassDeclaration
                            val params =
                                parentDec.primaryConstructor!!.parameters.joinToString(", ") { it.name!!.asString() }
                            line("super($params);")
                        }
                    }
                    line("clone():$classSig")
                    withBlock {
                        line("return new $classSig(${
                            constructorProps.joinToString(", ") { prop -> "this.${prop.name}" }
                        })");
                    }
                    line("static create(${
                        constructorProps.joinToString(", ") { prop -> "${prop.name}: ${getTypeSig(prop)}" }
                    }):$classSig")
                    withBlock {
                        line("return new $classSig(${
                            constructorProps.joinToString(", ") { prop -> prop.name }
                        })");
                    }
                }

                // tag
                if (tag >= 0) {
                    line("get_tag(): number { return $tag; }")
                }
                // serialize
                if (!isAbstract) {
                    line("//@ts-ignore\nzip_in(buffer: $outBufferClass, with_tag: boolean): void")
                    withBlock {
                        line("if (with_tag) { buffer.putShort($tag); }")
                        for (prop in props) {
                            val varName = prop.name
                            when (prop) {
                                is ListObjectPropMetaData -> {
                                    line("// ListObjectPropMetaData")
                                    line("buffer.putShort(this.$varName!.length);")
                                    line("for (const e of this.$varName!)")
                                    withBlock {
                                        if (isClassAbstract(prop.elementClass)) {
                                            line("e.zip_in(buffer, true);")
                                        } else {
                                            line("e.zip_in(buffer, false);")
                                        }
                                    }
                                }

                                is ListPrimitivePropMetaData -> {
                                    line("// ListPrimitivePropMetaData")
                                    line("buffer.putShort(this.$varName!.length);")
                                    line("for (const e of this.$varName!)")
                                    withBlock {
                                        bufferWritePrimitive(prop.type, "e")
                                    }
                                }

                                is MapPrimitiveKeyObjectValueMetaData -> {
                                    line("// MapObjectValueMetaData")
                                    line("buffer.putShort(Object.keys(this.$varName).length)")
                                    line("for (const key in this.$varName)")
                                    withBlock {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        if (isClassAbstract(prop.valueClassDec)) {
                                            line("// @ts-ignore")
                                            line("this.$varName[key]!.zip_in(buffer, true)")
                                        } else {
                                            line("// @ts-ignore")
                                            line("this.$varName[key]!.zip_in(buffer, false)")
                                        }
                                    }
                                }

                                is MapPrimitiveKeyValueMetaData -> {
                                    line("// MapPrimitiveValueMetaData")
                                    line("buffer.putShort(Object.keys(this.$varName).length)")
                                    line("for (const key in this.$varName)")
                                    withBlock {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        line("// @ts-ignore")
                                        bufferWritePrimitive(prop.valueType, "this.$varName[key]")
                                    }
                                }

                                is ObjectPropMetaData ->
                                    if (isClassAbstract(prop.classDec)) {
                                        line("this.$varName!.zip_in(buffer, true)")
                                    } else {
                                        line("this.$varName!.zip_in(buffer, false)")
                                    }

                                is PrimitivePropMetaData ->
                                    bufferWritePrimitive(prop.type, "this.$varName")

                                is EnumPropMetaData -> line("buffer.putShort(this.$varName)")
                                is ListEnumPropMetaData -> {
                                    line("// ListEnumPropMetaData")
                                    line("buffer.putShort(this.$varName!.length)")
                                    line("for (const e of this.$varName!)")
                                    withBlock {
                                        line("buffer.putShort(e)")
                                    }
                                }

                                is MapEnumKeyEnumValue -> {
                                    line("// MapEnumKeyEnumValue")
                                    line("buffer.putShort(Object.keys(this.$varName!).length)")
                                    line("for (const key in this.$varName)")
                                    withBlock {
                                        line("buffer.putShort(key)")
                                        line("// @ts-ignore")
                                        line("buffer.putShort(this.$varName[key])")
                                    }
                                }

                                is MapEnumKeyObjectValuePropMetaData -> {
                                    line("// MapPrimitiveKeyEnumValue")
                                    line("buffer.putShort(Object.keys(this.$varName).length)")
                                    line("for (const key in this.$varName)")
                                    withBlock {
                                        line("buffer.putShort(key)")
                                        if (isClassAbstract(prop.valueType)) {
                                            line("// @ts-ignore")
                                            line("this.$varName[key]!.zip_in(buffer, true)")
                                        } else {
                                            line("// @ts-ignore")
                                            line("this.$varName[key]!.zip_in(buffer, false)")
                                        }
                                    }
                                }

                                is MapEnumKeyPrimitiveValuePropMetaData -> {
                                    line("// MapPrimitiveKeyEnumValue")
                                    line("buffer.putShort(Object.keys(this.$varName!).length)")
                                    line("for (const key in this.$varName)")
                                    withBlock {
                                        line("buffer.putShort(key)")
                                        line("// @ts-ignore")
                                        bufferWritePrimitive(prop.valueType, "this.$varName[key]")
                                    }
                                }

                                is MapPrimitiveKeyEnumValue -> {
                                    line("// MapPrimitiveKeyEnumValue")
                                    line("buffer.putShort(Object.keys(this.$varName).length)")
                                    line("for (const key in this.$varName)")
                                    withBlock {
                                        bufferWritePrimitive(prop.keyType, "key")
                                        line("// @ts-ignore")
                                        line("buffer.putShort(this.$varName[key])")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    line("//@ts-ignore\nabstract zip_in(buffer: $inBufferClass, with_tag: boolean): void")
                }
//                // to bytes
//                line("to_enet_bytes(with_tag: boolean): fr.GsnEnetPacket")
//                withBlock {
//                    line("const p = new fr.GsnEnetPacket();")
//                    line("p.setBigEndian(true);")
//                    line("this.zip_in(p, with_tag);")
//                    line("return p;")
//                }
                // deserialize
                line("//@ts-ignore\n static zip(buffer: $inBufferClass, msg:JavaClass)")
                withBlock {
                    line("if(!(msg instanceof $classSig)) throw new Error('msg not instance of $classSig')");
                    line("msg.zip_in(buffer,false)")
                }
                line("//@ts-ignore\n static extract(buffer: $inBufferClass): $classSig")
                if (!isAbstract) withBlock {
                    props.forEach { prop ->
                        val varName = prop.name
                        val typeSig = getTypeSig(prop)
                        when (prop) {
                            is ListObjectPropMetaData -> {
                                line("// ListObjectPropMetaData")
                                line("const $varName = $typeSig(buffer.getShort())")
                                line("for (let i = $varName.length;i>0;i--)")
                                withBlock {
                                    val elementSig = getTypeSig(prop.elementClass)
                                    line("const val = $elementSig.extract(buffer)")
                                    line("$varName[i] = val")
                                }
                            }

                            is ListPrimitivePropMetaData -> {
                                line("// ListPrimitivePropMetaData")
                                line("const $varName = $typeSig(buffer.getShort())")
                                line("for (let i = $varName.length;i>0;i--)")
                                withBlock {
                                    bufferReadPrimitive(prop.type, "val")
                                    line("$varName[i] = val")
                                }
                            }

                            is MapPrimitiveKeyObjectValueMetaData -> {
                                line("// MapObjectValueMetaData")
                                line("const $varName:$typeSig = {}")
                                line("for (let i = buffer.getShort();i>0;i--)")
                                withBlock {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    val valueSig = getTypeSig(prop.valueClassDec)
                                    line("$varName.set[key]= $valueSig.extract(buffer)")
                                }
                            }

                            is MapPrimitiveKeyValueMetaData -> {
                                line("// MapPrimitiveValueMetaData")
                                line("const $varName:$typeSig = {}")
                                line("for (let i = buffer.getShort();i>0;i--)")
                                withBlock {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    bufferReadPrimitive(prop.valueType, "val")
                                    line("$varName[key]= val")
                                }
                            }

                            is ObjectPropMetaData -> {
                                val classSig = getTypeSig(prop.classDec)
                                line("const $varName = $classSig.extract(buffer)")
                            }

                            is PrimitivePropMetaData ->
                                bufferReadPrimitive(prop.type, varName)

                            is EnumPropMetaData -> line("const $varName = buffer.getShort() as ${getTypeSig(prop.enumClass)}")
                            is ListEnumPropMetaData -> {
                                line("// ListEnumPropMetaData")
                                line("const $varName = $typeSig(buffer.getShort())")
                                line("for (let i = $varName!.length;i>0;i--)")
                                withBlock {
                                    line("$varName[i] = buffer.getShort() as ${getTypeSig(prop.enumClass)}")
                                }
                            }

                            is MapEnumKeyEnumValue -> {
                                line("// MapEnumKeyPrimitiveValuePropMetaData")
                                line("const $varName:$typeSig = {}")
                                line("for (let i = buffer.getShort();i>0;i--)")
                                withBlock {
                                    line("const key = buffer.getShort() as ${getTypeSig(prop.enumKey)}")
                                    line("$varName[key] = buffer.getShort() as ${getTypeSig(prop.enumValue)}")
                                }
                            }

                            is MapEnumKeyObjectValuePropMetaData -> {
                                line("// MapEnumKeyPrimitiveValuePropMetaData")
                                line("const $varName:$typeSig = {}")
                                line("for (let i =buffer.getShort();i>0;i--)")
                                withBlock {
                                    line("const key = buffer.getShort() as ${getTypeSig(prop.enumKey)}")
                                    val valueSig = getTypeSig(prop.valueType)
                                    line("$varName[key] = $valueSig.extract(buffer)")
                                }
                            }

                            is MapEnumKeyPrimitiveValuePropMetaData -> {
                                line("// MapEnumKeyPrimitiveValuePropMetaData")
                                line("const $varName:$typeSig = {}")
                                line("for (let i = buffer.getShort();i>0;i--)")
                                withBlock {
                                    line("const key = buffer.getShort() as ${getTypeSig(prop.enumKey)}")
                                    bufferReadPrimitive(prop.valueType, "val")
                                    line("$varName[key]= val")
                                }
                            }

                            is MapPrimitiveKeyEnumValue -> {
                                line("// MapPrimitiveKeyEnumValue")
                                line("const $varName:$typeSig = {}")
                                line("for (let i = buffer.getShort();i>0;i--)")
                                withBlock {
                                    bufferReadPrimitive(prop.keyType, "key")
                                    line("$varName[key]= buffer.getShort() as ${getTypeSig(prop.enumValue)}")
                                }
                            }
                        }
                    }
                    // constructor
                    val params = constructorProps.map { prop -> prop.name }.joinToString(", ")
                    line("const ret = new $classSig($params)")
                    for (prop in otherProps) {
                        val varName = prop.name
                        line("ret.$varName = $varName")
                    }
                    line("return ret;")
                }
                else withBlock {
                    line("const tag = buffer.getShort()")
                    line("switch (tag)")
                    forWithBlock(classDec.getAllActualChild()) { kclass ->
                        val tag = globalUniqueTag.getOrDefault(kclass, -1)
                        val typeSig = getTypeSig(kclass)
                        line("case $typeSig.TAG: // $tag")
                        withBlock {
                            line("return $typeSig.extract(buffer)")
                        }
                    }
                    line("LogUtils.info('matching $classSig, tag not recognized:', tag)")
                    line("throw Error('matching $classSig, tag not recognized: ' + tag)")
                }

//                // utils
//                run {
//                    line("_to_string_tab(tab: number): string")
//                    withBlock {
//                        val params = props.map { prop ->
//                            val varName = prop.name
//                            "'\\n' + t1 + '$varName: ' + ${getStr("this.$varName", prop)}"
//                        }.joinToString(" + ")
//                        line("var t0 = '  '.repeat(tab)")
//                        line("var t1 = t0 + '  '")
//                        line("var t2 = t1 + '  '")
//                        line("return `$classSig {` + ${params.ifEmpty { "''" }} + `\\n\${t0}}`")
//                    }
//                    line("toString(): string")
//                    withBlock {
//                        line("return this._to_string_tab(0)")
//                    }
//                }
            }
        }
    }

//    private fun getStr(varName: String, prop: AbstractPropMetadata) = when (prop) {
//        is ListObjectPropMetaData -> "`Array<${
//            getTypeSig(
//                prop.elementClass,
//                false
//            )
//        }>(\${$varName.length}) [\\n\${t2}\${$varName.map(n => n._to_string_tab(tab + 2)).join(',\\n' + t2)}\\n\${t1}]`"
//
//        is ListPrimitivePropMetaData -> "`Array<${
//            getTypeSig(
//                prop.type,
//                false
//            )
//        }>(\${$varName.length}) [\\n\${t2}\${$varName.join(',\\n' + t2)}\\n\${t1}]`"
//
//        is MapPrimitiveKeyObjectValueMetaData -> "`Map<${
//            getTypeSig(
//                prop.keyType,
//                false
//            )
//        }, ${
//            getTypeSig(
//                prop.valueClassDec,
//                false
//            )
//        }>(\${$varName.size}) [\\n\${t2}\${Array.from($varName.keys(), key => `\${key}: \${$varName.get(key)._to_string_tab(tab + 2)}`).join(',\\n' + t2)}\\n\${t1}]`"
//
//        is MapPrimitiveKeyValueMetaData -> "`Map<${getTypeSig(prop.keyType, false)}, ${
//            getTypeSig(
//                prop.valueType,
//                false
//            )
//        }>(\${$varName.size}) [\\n\${t2}\${Array.from($varName.keys(), key => `\${key}: \${$varName.get(key)}`).join(',\\n' + t2)}\\n\${t1}]`"
//
//        is ObjectPropMetaData -> "$varName._to_string_tab(tab + 1)"
//        is PrimitivePropMetaData -> varName
//        is EnumPropMetaData -> varName
//        is ListEnumPropMetaData -> "`Array<${
//            getTypeSig(
//                prop.enumClass,
//                false
//            )
//        }>(\${$varName.length}) [\\n\${t2}\${$varName.join(',\\n' + t2)}\\n\${t1}]`"
//
//        is MapEnumKeyEnumValue -> "`Record<${getTypeSig(prop.enumKey, false)}, ${
//            getTypeSig(
//                prop.enumValue,
//                false
//            )
//        }>(\${$varName.size}) [\\n\${t2}\${Array.from($varName.keys(), key => `\${key}: \${$varName.get(key)}`).join(',\\n' + t2)}\\n\${t1}]`"
//
//        is MapEnumKeyObjectValuePropMetaData -> "`Map<${getTypeSig(prop.enumKey, false)}, ${
//            getTypeSig(
//                prop.valueType,
//                false
//            )
//        }>(\${$varName.size}) [\\n\${t2}\${Array.from($varName.keys(), key => `\${key}: \${$varName.get(key)._to_string_tab(tab + 2)}`).join(',\\n' + t2)}\\n\${t1}]`"
//
//        is MapEnumKeyPrimitiveValuePropMetaData -> "`Map<${
//            getTypeSig(
//                prop.enumKey,
//                false
//            )
//        }, ${
//            getTypeSig(
//                prop.valueType,
//                false
//            )
//        }>(\${$varName.size}) [\\n\${t2}\${Array.from($varName.keys(), key => `\${key}: \${$varName.get(key)}`).join(',\\n' + t2)}\\n\${t1}]`"
//
//        is MapPrimitiveKeyEnumValue -> "`Map<${getTypeSig(prop.keyType, false)}, ${
//            getTypeSig(
//                prop.enumValue,
//                false
//            )
//        }>(\${$varName.size}) [\\n\${t2}\${Array.from($varName.keys(), key => `\${key}: \${$varName.get(key)}`).join(',\\n' + t2)}\\n\${t1}]`"
//    }

    private fun withOverridee(it: AbstractPropMetadata) =
        if (it.propDec.findOverridee() == null) null else it

    private fun withoutOverridee(it: AbstractPropMetadata) =
        if (it.propDec.findOverridee() == null) it else null

    private fun genEnum(writer: TsWriter) {
        writer.apply {
            line("export enum ${classDec.simpleName.asString()}")
            forWithBlock(classDec.getAllEnumEntrySimpleName()) { enumName ->
                line("$enumName,")
            }
        }
    }

    private fun getTypeSig(prop: AbstractPropMetadata, fullName: Boolean = true) = when (prop) {
        is ListObjectPropMetaData -> "Array<${getTypeSig(prop.elementClass, fullName)}>"
        is ListPrimitivePropMetaData -> "Array<${getTypeSig(prop.type, fullName)}>"
        is MapPrimitiveKeyObjectValueMetaData -> "Record<${
            getTypeSig(
                prop.keyType,
                fullName
            )
        }, ${getTypeSig(prop.valueClassDec, fullName)}>"

        is MapPrimitiveKeyValueMetaData -> "Record<${getTypeSig(prop.keyType, fullName)}, ${
            getTypeSig(
                prop.valueType,
                fullName
            )
        }>"

        is ObjectPropMetaData -> getTypeSig(prop.classDec, fullName)
        is PrimitivePropMetaData -> getTypeSig(prop.type, fullName)
        is EnumPropMetaData -> getTypeSig(prop.enumClass, fullName)
        is ListEnumPropMetaData -> "Array<${getTypeSig(prop.enumClass, fullName)}>"
        is MapEnumKeyEnumValue -> "Record<${getTypeSig(prop.enumKey, fullName)}, ${getTypeSig(prop.enumValue, fullName)}>"
        is MapEnumKeyObjectValuePropMetaData -> "Record<${getTypeSig(prop.enumKey, fullName)}, ${
            getTypeSig(
                prop.valueType,
                fullName
            )
        }>"

        is MapEnumKeyPrimitiveValuePropMetaData -> "Record<${
            getTypeSig(
                prop.enumKey,
                fullName
            )
        }, ${getTypeSig(prop.valueType, fullName)}>"

        is MapPrimitiveKeyEnumValue -> "Record<${getTypeSig(prop.keyType, fullName)}, ${
            getTypeSig(
                prop.enumValue,
                fullName
            )
        }>"
    }

    private fun getTypeSig(kclass: KSClassDeclaration, fullName: Boolean = true) =
        if (fullName) "${kclass.packageName.asString()}.${kclass.simpleName.asString()}" else kclass.simpleName.asString()

    private fun getTypeSig(primitive: PrimitiveType, fullName: Boolean = true) = when (primitive) {
        PrimitiveType.INT -> if (fullName) "number" else "int"
        PrimitiveType.SHORT -> if (fullName) "number" else "short"
        PrimitiveType.DOUBLE -> if (fullName) "number" else "double"
        PrimitiveType.BYTE -> if (fullName) "number" else "byte"
        PrimitiveType.BOOL -> if (fullName) "boolean" else "bool"
        PrimitiveType.FLOAT -> if (fullName) "number" else "float"
        PrimitiveType.LONG -> if (fullName) "number" else "long"
        PrimitiveType.STRING -> if (fullName) "string" else "string"
        PrimitiveType.BYTE_ARRAY -> if (fullName) "fr.GsnEnetPacket" else "byte_array"
    }

    private fun name_join(name: String, prefix: String): String {
        val alphaNumRegex = Regex("[^A-Za-z0-9 ]")
        return alphaNumRegex.replace(name, "_") + prefix;
    }

    private fun TsWriter.bufferReadPrimitive(
        varType: PrimitiveType,
        varName: String,
    ) {
        when (varType) {
            PrimitiveType.INT -> line("const $varName = buffer.getInt()")
            PrimitiveType.SHORT -> line("const $varName = buffer.getShort()")
            PrimitiveType.DOUBLE -> line("const $varName = buffer.getDouble()")
            PrimitiveType.BYTE -> line("const $varName = buffer.getByte()")
            PrimitiveType.BOOL -> line("const $varName = buffer.getByte() != 0")
            PrimitiveType.FLOAT -> line("const $varName = buffer.getFloat()")
            PrimitiveType.LONG -> line("const $varName = buffer.getLong()")
            PrimitiveType.STRING -> {
                line("const $varName = buffer.getString()")
            }

            PrimitiveType.BYTE_ARRAY -> {
                val varNameSize = name_join(varName, "size")
                line("const $varNameSize = buffer.getShort()")
                line("const $varName = buffer.getBytes($varNameSize)")
            }
        }
    }

    private fun TsWriter.bufferWritePrimitive(
        varType: PrimitiveType,
        varName: String,
    ) {
        when (varType) {
            PrimitiveType.INT -> line("buffer.putInt($varName)")
            PrimitiveType.SHORT -> line("buffer.putShort($varName)")
            PrimitiveType.DOUBLE -> line("buffer.putDouble($varName)")
            PrimitiveType.BYTE -> line("buffer.putByte($varName)")
            PrimitiveType.BOOL -> line("buffer.putByte($varName)")
            PrimitiveType.FLOAT -> line("buffer.putFloat($varName)")
            PrimitiveType.LONG -> line("buffer.putLong($varName)")
            PrimitiveType.STRING -> {
                line("buffer.putString($varName)")
            }

            PrimitiveType.BYTE_ARRAY -> {
                line("buffer.putShort($varName!.size())")
                line("buffer.putBytes($varName!)")
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


class TsGenFileProtocolVersion(val rootFolderGen: String) : IGenFileProtocolVersion {
    val root = if (rootFolderGen == "") {
        "m"
    } else {
        rootFolderGen
    }
    private val template = """
namespace {NAMESPACE} {
    export const PROTOCOL_VERSION = {PROTOCOL_VERSION}
}
"""

    override fun genFileProtocolVersion(codeGenerator: CodeGenerator, protocolVersion: Int) {
        run {
            val stream = codeGenerator.createNewFile(
                Dependencies(false),
                "",
                "IPacket",
                "ts"
            )
            stream.write(
                template
                    .replace("{NAMESPACE}", root)
                    .replace("{PROTOCOL_VERSION}", protocolVersion.toString()).toByteArray()
            )
        }
    }
}
