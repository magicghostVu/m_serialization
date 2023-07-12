package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toClassName
import m_serialization.data.prop_meta_data.*
import m_serialization.utils.KSClassDecUtils.getAllActualChild
import m_serialization.utils.KSClassDecUtils.getAllEnumEntryWithIndex
import m_serialization.utils.KSClassDecUtils.getSuperClassNameJS
import java.io.BufferedWriter

class JSGenClassMetaData() : ClassMetaData() {
    companion object {
        var outputFile = JSFile(AbstractPropMetadata.serializerObjectNameSuffix)
        fun save(codeGenerator: CodeGenerator) {
            outputFile.writeTo(
                codeGenerator.createNewFile(
                    Dependencies(false),
                    "javascript", AbstractPropMetadata.serializerObjectNameSuffix, "js"
                ).bufferedWriter(Charsets.UTF_8)
            )
        }

    }

    override fun doGenCode(codeGenerator: CodeGenerator) {
        if (classDec.classKind == ClassKind.ENUM_CLASS)
            outputFile.addEnum(JSEnum(classDec))
        else
            outputFile.addClass(JSClass(protocolUniqueId, classDec, constructorProps))
        //logger.warn("$protocolUniqueId ${classDec.toClassName().toString()}")
    }

}

sealed class JSElement() {
    abstract fun treeElementString(): String
    abstract fun jsDocTree(): String
    fun jsDocType(type: AbstractPropMetadata): String {
        return when (type) {
            is PrimitivePropMetaData -> primitiveJsDocType(type.type)
            is ObjectPropMetaData -> type.classDec.toClassName().toString()
            is EnumPropMetaData -> type.enumClass.toClassName().toString()
            is ListEnumPropMetaData -> "${type.enumClass.toClassName()}[]"
            is ListObjectPropMetaData -> "${type.elementClass.toClassName()}[]"
            is ListPrimitivePropMetaData -> "${primitiveJsDocType(type.type)}[]"
            is MapEnumKeyEnumValue -> "Map<${type.enumKey.toClassName()},${type.enumValue.toClassName()}>"
            is MapEnumKeyObjectValuePropMetaData -> "Map<${type.enumKey.toClassName()},${type.valueType.toClassName()}>"
            is MapEnumKeyPrimitiveValuePropMetaData -> "Map<${type.enumKey.toClassName()},${primitiveJsDocType(type.valueType)}>"
            is MapPrimitiveKeyEnumValue -> "Map<${primitiveJsDocType(type.keyType)},${type.enumValue.toClassName()}>"
            is MapPrimitiveKeyObjectValueMetaData -> "Map<${primitiveJsDocType(type.keyType)},${type.valueClassDec.toClassName()}>"
            is MapPrimitiveKeyValueMetaData -> "Map<${primitiveJsDocType(type.keyType)},${primitiveJsDocType(type.valueType)}>"
        }
    }

    private fun primitiveJsDocType(type: PrimitiveType): String {
        return when (type) {
            PrimitiveType.SHORT,
            PrimitiveType.DOUBLE,
            PrimitiveType.BYTE,
            PrimitiveType.FLOAT,
            PrimitiveType.LONG,
            PrimitiveType.INT -> "number"
            PrimitiveType.BOOL -> "boolean"
            PrimitiveType.STRING -> "string"
            PrimitiveType.BYTE_ARRAY -> "number[]"
        }
    }

}

class JSClass(
    val classId: Short,
    val classDec: KSClassDeclaration,
    val constructorProps: List<AbstractPropMetadata>
) : JSElement() {
    fun bufferVar(): String {
        return JSFile.bufferVar;
    }

    fun objVar(): String {
        return JSFile.objVar;
    }

    fun subObjVar(): String {
        return JSFile.subObjVar;
    }

    fun zipMapFun(): String {
        return JSFile.zipMapFunction;
    }


    private fun errorType(classId: Short, varName: String): String {
        return "\"Error class $classId diff with\" + $varName"
    }

    fun rawFunName(): String {
        return "fun0x${classId.toString(16)}"//classDec.simpleName.asString();
    }

    private fun rawFunName(classDec: KSClassDeclaration, classToTag: Map<String, Short>): String {
        return "fun0x${classToTag[classDec.toClassName().toString()]?.toString(16)}"//classDec.simpleName.asString()
    }

    private fun getExtractFunction(it: AbstractPropMetadata, classToTag: MutableMap<String, Short>): String {
        return when (it) {
            is PrimitivePropMetaData -> getExtractPrimitive(it.type)
            is ObjectPropMetaData -> "this.${rawFunName(it.classDec, classToTag)}0(${bufferVar()})"
            is EnumPropMetaData -> "this.${bufferVar()}.readEnum()"

            //list
            is ListObjectPropMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return this.${rawFunName(it.elementClass, classToTag)}0(${bufferVar()})"
            }}.bind(this,${bufferVar()}))"
            is ListPrimitivePropMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return ${getExtractPrimitive(it.type)}"
            }}.bind(this,${bufferVar()}))"
            is ListEnumPropMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return ${bufferVar()}.readEnum()"
            }}.bind(this,${bufferVar()}))"

            //map
            is MapPrimitiveKeyValueMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType)},value:${getExtractPrimitive(it.valueType)}}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"
            is MapPrimitiveKeyObjectValueMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType)},value: this.${
                    rawFunName(
                        it.valueClassDec,
                        classToTag
                    )
                }0(${bufferVar()})}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"
            is MapPrimitiveKeyEnumValue -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType)},value: this.${bufferVar()}.readEnum()}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"

            is MapEnumKeyEnumValue -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${bufferVar()}.readEnum(),value: this.${bufferVar()}.readEnum()}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"
            is MapEnumKeyObjectValuePropMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${bufferVar()}.readEnum(),value: value: this.${
                    rawFunName(
                        it.valueType,
                        classToTag
                    )
                }0(${bufferVar()})}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"
            is MapEnumKeyPrimitiveValuePropMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${bufferVar()}.readEnum(),value: value:${getExtractPrimitive(it.valueType)}}}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"
        }
    }


    private fun getExtractPrimitive(it: PrimitiveType): String {
        return when (it) {
            PrimitiveType.BOOL -> "${bufferVar()}.readBool()"
            PrimitiveType.INT -> "${bufferVar()}.readInt()"
            PrimitiveType.SHORT -> "${bufferVar()}.readShort()"
            PrimitiveType.DOUBLE -> "${bufferVar()}.readDouble()"
            PrimitiveType.BYTE -> "${bufferVar()}.readByte()"
            PrimitiveType.FLOAT -> "${bufferVar()}.readFloat()"
            PrimitiveType.LONG -> "${bufferVar()}.readLong()"
            PrimitiveType.STRING -> "${bufferVar()}.readString()"
            PrimitiveType.BYTE_ARRAY -> "${bufferVar()}.readBytes()"
        }
    }

    private fun getZipPrimitive(it: PrimitiveType, buffer: String, obj: String): String {
        return when (it) {
            PrimitiveType.BOOL -> "$buffer.writeBool($obj);"
            PrimitiveType.INT -> "$buffer.writeInt($obj);"
            PrimitiveType.SHORT -> "$buffer.writeShort($obj);"
            PrimitiveType.DOUBLE -> "$buffer.writeDouble($obj);"
            PrimitiveType.BYTE -> "$buffer.writeByte($obj);"
            PrimitiveType.FLOAT -> "$buffer.writeFloat($obj);"
            PrimitiveType.LONG -> "$buffer.writeLong($obj);"
            PrimitiveType.STRING -> "$buffer.writeString($obj);"
            PrimitiveType.BYTE_ARRAY -> "$buffer.writeBytes($obj);"
        }
    }

    private fun getZipFunction(it: AbstractPropMetadata, classToTag: Map<String, Short>): String {
        return when (it) {
            is PrimitivePropMetaData -> getZipPrimitive(it.type, bufferVar(), "${objVar()}.${it.name}")
            is ObjectPropMetaData -> "this.${
                rawFunName(
                    it.classDec,
                    classToTag
                )
            }2(${bufferVar()},${objVar()}.${it.name});"
            is EnumPropMetaData -> "${bufferVar()}.writeEnum(${objVar()}.${it.name});"

            //list
            is ListObjectPropMetaData -> "${bufferVar()}.writeSize(${objVar()}.${it.name}.length);" +
                    "${objVar()}.${it.name}.forEach(function(${bufferVar()}, ${subObjVar()}){${
                        "this.${rawFunName(it.elementClass, classToTag)}2(${bufferVar()}, ${subObjVar()})"
                    }}.bind(this,${bufferVar()}));"
            is ListPrimitivePropMetaData -> "${bufferVar()}.writeSize(${objVar()}.${it.name}.length);" +
                    "${objVar()}.${it.name}.forEach(function(${bufferVar()}, ${subObjVar()}){${
                        getZipPrimitive(it.type, bufferVar(), subObjVar())
                    }}.bind(this,${bufferVar()}));"
            is ListEnumPropMetaData -> "${bufferVar()}.writeSize(${objVar()}.${it.name}.length);" +
                    "${objVar()}.${it.name}.forEach(function(${bufferVar()}, ${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}.bind(this,${bufferVar()}));"

            //map
            is MapPrimitiveKeyValueMetaData -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType, bufferVar(), subObjVar())
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType, bufferVar(), subObjVar())
                    }});"

            is MapPrimitiveKeyObjectValueMetaData -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType, bufferVar(), subObjVar())
                    }}," +
                    "this.${rawFunName(it.valueClassDec, classToTag)}2.bind(this));"

            is MapPrimitiveKeyEnumValue -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType, bufferVar(), subObjVar())
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }});"

            is MapEnumKeyEnumValue -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }});"

            is MapEnumKeyObjectValuePropMetaData -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}," +
                    "this.${rawFunName(it.valueType, classToTag)}2.bind(this));"

            is MapEnumKeyPrimitiveValuePropMetaData -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.valueType, bufferVar(), subObjVar())
                    }});"

        }
    }

    private fun classExtractFunction(classToTag: MutableMap<String, Short>): String {
        return if (classDec.modifiers.contains(Modifier.SEALED))
            "var javaClass= ${bufferVar()}.readJavaClass(); switch(javaClass){${
                classDec.getAllActualChild().joinToString("") {
                    "case ${
                        classToTag[it.toClassName().toString()]
                    }:{return this.${rawFunName(it, classToTag)}0(${bufferVar()})}"
                }
            }default: throw new Error(${errorType(classId, "javaClass")})}"
        else
            "return this.${rawFunName()}1(${
                constructorProps.joinToString(",") { getExtractFunction(it, classToTag) }
            })"
    }


    private fun classZipFunction(classToTag: MutableMap<String, Short>): String {
        return if (classDec.modifiers.contains(Modifier.SEALED))
            "${bufferVar()}.writeJavaClass(${objVar()}.javaClass);switch(${objVar()}.javaClass){${
                classDec.getAllActualChild().joinToString("") {
                    "case ${
                        classToTag[it.toClassName().toString()]
                    }:{ this.${rawFunName(it, classToTag)}2(${bufferVar()}, ${objVar()});break;}"
                }
            }default: throw new Error(${errorType(classId, "${objVar()}.javaClass")})}"
        else
            constructorProps.joinToString("") { getZipFunction(it, classToTag) }
    }

    fun writeClassFunction(file: BufferedWriter, classDecToUniqueTag: MutableMap<String, Short>) {
        file.write(
            "${rawFunName()}0:function(${bufferVar()}){${
                classExtractFunction(
                    classDecToUniqueTag
                )
            }},"
        )
        file.write("${rawFunName()}1:function(${
            constructorProps.joinToString(",") { it.name }
        }){return {javaClass:${classId}${if (constructorProps.isNotEmpty()) "," else ""}${
            constructorProps.joinToString(",") { "${it.name}:${it.name}" }
        }}},")
        file.write(
            "${rawFunName()}2:function(${bufferVar()}, ${objVar()}){${classZipFunction(classDecToUniqueTag)}},"
        )
    }

    override fun treeElementString(): String {
        return "{extract:${AbstractPropMetadata.serializerObjectNameSuffix}.${rawFunName()}0.bind(${
            AbstractPropMetadata.serializerObjectNameSuffix
        }),zip:${AbstractPropMetadata.serializerObjectNameSuffix}.${rawFunName()}2.bind(${
            AbstractPropMetadata.serializerObjectNameSuffix
        }),create:${AbstractPropMetadata.serializerObjectNameSuffix}.${rawFunName()}1.bind(${
            AbstractPropMetadata.serializerObjectNameSuffix
        }),javaClass:$classId}"
    }

    override fun jsDocTree(): String {
        return "/** @type {${classDec.toClassName()}${AbstractPropMetadata.serializerObjectNameSuffix}}*/"
    }

    fun writeClassJSDOC(file: BufferedWriter) {
        file.write("/** @typedef {${classDec.getSuperClassNameJS()}} ${classDec.toClassName()}\n")
        constructorProps.forEach { file.write("* @property {${jsDocType(it)}} ${it.name}\n") }
        file.write("*/\n")
        file.write("/**@typedef {Object} ${classDec.toClassName()}${AbstractPropMetadata.serializerObjectNameSuffix}\n");
        file.write(" * @property {function(JReadBuffer):${classDec.toClassName()}} extract\n")
        file.write(" * @property {function(JWriteBuffer,${classDec.toClassName()}):void} zip\n")
        file.write(" * @property {function(${constructorProps.joinToString { jsDocType(it) }}):${classDec.toClassName()}} create\n")
        file.write("*/\n")
    }

}

class JSEnum(var classDec: KSClassDeclaration) : JSElement() {
    override fun treeElementString(): String {
        return "{${classDec.getAllEnumEntryWithIndex().joinToString { "${it.first}:${it.second}" }}}"
    }

    override fun jsDocTree(): String {
        return "/** @Enum {number} */"
    }

}

class TreeNode() {
    val child = mutableMapOf<String, TreeNode>()
    var type: JSElement? = null
}

class JSFile(val fileName: String) {
    companion object {
        val subObjVar = "subObject"
        val zipMapFunction = "zipMap"
        val bufferVar = "buffer"
        val objVar = "object"
        val keyPushConsumer = "_key"
        val valuePushConsumer = "_value"
    }

    private var classes = mutableListOf<JSClass>()
    private var enums = mutableListOf<JSEnum>()
    private var classDecToUniqueTag: MutableMap<String, Short> = mutableMapOf()
    private var classTree = TreeNode()
    private var version = 0
    private var setVersion = false
    fun writeTo(file: BufferedWriter) {
        writeFileStart(file)

        classes.forEach {
            it.writeClassFunction(file, classDecToUniqueTag)
        }

        writeFileEnd(file);
        file.flush()
        file.close()
    }

    private fun writeFileStart(file: BufferedWriter) {

        file.write("/** @typedef {Object} JavaClass \n")
        file.write("* @property {number} javaClass */")

        classes.forEach {
            it.writeClassJSDOC(file)
        }


        file.write("var ${AbstractPropMetadata.serializerObjectNameSuffix}={")

        file.write("$zipMapFunction:function($bufferVar, $objVar, $keyPushConsumer, $valuePushConsumer){")
        file.write("var var0x000=Object.keys($objVar).filter(function(_k){return $objVar[_k]!=null});")
        file.write("$bufferVar.writeSize(var0x000.length);var0x000.forEach(function(_k){")
        file.write("$keyPushConsumer($bufferVar,_k);$valuePushConsumer($bufferVar,$objVar[_k])")
        file.write("})")
        file.write("},")
    }

    private fun writeFileEnd(file: BufferedWriter) {
        file.write("arrayToMap:function(_m,_o){ _m[_o.key]=_o.value;return _m},version=${version}")
        file.write("};\n")
        file.write("${AbstractPropMetadata.serializerObjectNameSuffix}.instance=${createTree(classTree)}")
    }

    private fun createTree(classTree: TreeNode): String {
        return if (classTree.type == null) "{${
            classTree.child.entries.joinToString {
                "${if(it.value.type==null) "" else it.value.type!!.jsDocTree()}${it.key}: ${
                    createTree(
                        it.value
                    )
                }"
            }
        }}"
        else classTree.type!!.treeElementString()
    }


    fun addClass(jsClass: JSClass) {
        this.classes.add(jsClass)
        this.classDecToUniqueTag[jsClass.classDec.toClassName().toString()] = jsClass.classId

        this.addTree(
            jsClass.classDec.packageName.asString().split("."),
            jsClass.classDec.simpleName.asString(),
            jsClass
        )
    }

    fun addEnum(jsEnum: JSEnum) {
        this.enums.add(jsEnum)
        this.addTree(jsEnum.classDec.packageName.asString().split("."), jsEnum.classDec.simpleName.asString(), jsEnum)
    }

    private fun addTree(packet: List<String>, name: String, element: JSElement) {
        var node = classTree;
        for (pack in packet) {
            if (!node.child.containsKey(pack))
                node.child[pack] = TreeNode()
            node = node.child[pack]!!
        }
        if (!node.child.containsKey(name))
            node.child[name] = TreeNode()
        node.child[name]?.type = element
    }

    fun setVersion(protocolVersion: Int) {
        if(this.setVersion)
            return
        this.version = protocolVersion
        this.setVersion = true
    }

}