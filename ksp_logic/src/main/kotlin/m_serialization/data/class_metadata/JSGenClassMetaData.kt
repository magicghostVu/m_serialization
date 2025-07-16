package m_serialization.data.class_metadata

import com.google.devtools.ksp.ExceptionMessage
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
import kotlin.reflect.KClass

class JSClassDec {
    val className: String
    val superClassName: String
    public val packageName: String
    public val simpleName: String
    private val allEnumEntry: List<Pair<String, Int>>
    val isSealed: Boolean
    val childClasses: List<JSClassDec>


    constructor(dec: KSClassDeclaration) {
        superClassName = dec.getSuperClassNameJS()
        allEnumEntry = dec.getAllEnumEntryWithIndex()
        className = dec.toClassName().toString()
        packageName = dec.packageName.asString()
        simpleName = dec.simpleName.asString()
        isSealed = dec.modifiers.contains(Modifier.SEALED)
        childClasses = dec.getAllActualChild().map { x -> JSClassDec(x) }
    }

    fun getAllEnumEntryWithIndex(): List<Pair<String, Int>> {
        return allEnumEntry
    }

    fun toClassName(): String {
        return className
    }

    fun getAllActualChild(): List<JSClassDec> {
        return childClasses;
    }

}

class JSPropMetaData {
    val propClass: KClass<out AbstractPropMetadata>
    val name: String
    var type: PrimitiveType? = null
    var classDec: JSClassDec? = null
    var elementClass: JSClassDec? = null
    var keyType: PrimitiveType? = null
    var keyClassDec: JSClassDec? = null
    var valueType: PrimitiveType? = null
    var valueClassDec: JSClassDec? = null

    constructor(prop: AbstractPropMetadata) {
        propClass = prop::class
        name = prop.name
        when (prop) {
            is PrimitivePropMetaData -> this.type = prop.type
            is ObjectPropMetaData -> this.classDec = JSClassDec(prop.classDec)
            is EnumPropMetaData -> this.classDec = JSClassDec(prop.enumClass)

            //list
            is ListObjectPropMetaData -> this.elementClass = JSClassDec(prop.elementClass)

            is ListPrimitivePropMetaData -> this.type = prop.type

            is ListEnumPropMetaData -> this.elementClass = JSClassDec(prop.enumClass)

            //map
            is MapPrimitiveKeyValueMetaData -> {
                this.keyType = prop.keyType
                this.valueType = prop.valueType
            }

            is MapPrimitiveKeyObjectValueMetaData -> {
                this.keyType = prop.keyType
                this.valueClassDec = JSClassDec(prop.valueClassDec)
            }

            is MapPrimitiveKeyEnumValue -> {
                this.keyType = prop.keyType
                this.valueClassDec = JSClassDec(prop.enumValue)
            }

            is MapEnumKeyEnumValue -> {
                this.keyClassDec = JSClassDec(prop.enumValue)
                this.valueClassDec = JSClassDec(prop.enumValue)
            }

            is MapEnumKeyObjectValuePropMetaData -> {
                this.keyClassDec = JSClassDec(prop.enumKey)
                this.valueClassDec = JSClassDec(prop.valueType)
            }

            is MapEnumKeyPrimitiveValuePropMetaData -> {
                this.keyClassDec = JSClassDec(prop.enumKey)
                this.valueType = prop.valueType
            }

        }
    }
}

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

    override fun languageGen(): LanguageGen {
        return LanguageGen.JAVASCRIPT
    }

    override fun doGenCode(codeGenerator: CodeGenerator) {
        if (classDec.classKind == ClassKind.ENUM_CLASS)
            outputFile.addEnum(JSEnum(classDec))
        else
            outputFile.addClass(JSClass(protocolUniqueId, classDec, constructorProps + otherProps))
        //logger.warn("$protocolUniqueId ${classDec.toClassName().toString()}")
    }

}

sealed class JSElement(dec: KSClassDeclaration) {
    val classDec = JSClassDec(dec)
    abstract fun treeElementString(): String
    abstract fun jsDocTree(): String
    fun jsDocType(type: JSPropMetaData): String {
        return when (type.propClass) {
            PrimitivePropMetaData::class -> primitiveJsDocType(type.type!!)
            ObjectPropMetaData::class -> type.classDec!!.toClassName().toString()
            EnumPropMetaData::class -> type.classDec!!.toClassName().toString()
            ListEnumPropMetaData::class -> "${type.elementClass!!.toClassName()}[]"
            ListObjectPropMetaData::class -> "${type.elementClass!!.toClassName()}[]"
            ListPrimitivePropMetaData::class -> "${primitiveJsDocType(type.type!!)}[]"
            MapEnumKeyEnumValue::class -> "Object.<${type.keyClassDec!!.toClassName()},${type.valueClassDec!!.toClassName()}>"
            MapEnumKeyObjectValuePropMetaData::class -> "Object.<${type.keyClassDec!!.toClassName()},${type.valueClassDec!!.toClassName()}>"
            MapEnumKeyPrimitiveValuePropMetaData::class -> "Object.<${type.keyClassDec!!.toClassName()},${
                primitiveJsDocType(
                    type.valueType!!
                )
            }>"

            MapPrimitiveKeyEnumValue::class -> "Object.<${primitiveJsDocType(type.keyType!!)},${type.valueClassDec!!.toClassName()}>"
            MapPrimitiveKeyObjectValueMetaData::class -> "Object.<${primitiveJsDocType(type.keyType!!)},${type.valueClassDec!!.toClassName()}>"
            MapPrimitiveKeyValueMetaData::class -> "Object.<${primitiveJsDocType(type.keyType!!)},${
                primitiveJsDocType(
                    type.valueType!!
                )
            }>"

            else -> error("class ${type.propClass} not implement.")
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

class JSClass(val classId: Short, classDec: KSClassDeclaration, props: List<AbstractPropMetadata>) :
    JSElement(classDec) {

    val constructorProps = props.map { it -> JSPropMetaData(it) }

    companion object {
        var lastId = 0;
        val MapClassToID: MutableMap<String, Int> = mutableMapOf();
    }

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
        return "fun0x${
            MapClassToID.computeIfAbsent(classDec.toClassName()) {
                lastId++
            }.toString(16)
        }"
    }

    private fun rawFunName(classDec: JSClassDec): String {
        return "fun0x${
            MapClassToID.computeIfAbsent(classDec.toClassName()) {
                lastId++
            }.toString(16)
        }"
    }

    private fun getExtractFunction(it: JSPropMetaData, classToTag: MutableMap<String, Short>): String {
        return when (it.propClass) {
            PrimitivePropMetaData::class -> getExtractPrimitive(it.type!!)
            ObjectPropMetaData::class -> "this.${rawFunName(it.classDec!!)}0(${bufferVar()})"
            EnumPropMetaData::class -> "${bufferVar()}.readEnum()"

            //list
            ListObjectPropMetaData::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return this.${rawFunName(it.elementClass!!)}0(${bufferVar()})"
            }}.bind(this,${bufferVar()}))"

            ListPrimitivePropMetaData::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return ${getExtractPrimitive(it.type!!)}"
            }}.bind(this,${bufferVar()}))"

            ListEnumPropMetaData::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return ${bufferVar()}.readEnum()"
            }}.bind(this,${bufferVar()}))"

            //map
            MapPrimitiveKeyValueMetaData::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType!!)},value: ${getExtractPrimitive(it.valueType!!)}}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"

            MapPrimitiveKeyObjectValueMetaData::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType!!)},value: this.${
                    rawFunName(it.valueClassDec!!)
                }0(${bufferVar()})}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"

            MapPrimitiveKeyEnumValue::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType!!)},value: ${bufferVar()}.readEnum()}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"

            MapEnumKeyEnumValue::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${bufferVar()}.readEnum(),value: ${bufferVar()}.readEnum()}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"

            MapEnumKeyObjectValuePropMetaData::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${bufferVar()}.readEnum(),value: this.${
                    rawFunName(
                        it.valueClassDec!!
                    )
                }0(${bufferVar()})}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"

            MapEnumKeyPrimitiveValuePropMetaData::class -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${bufferVar()}.readEnum(),value: ${getExtractPrimitive(it.valueType!!)}}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"

            else -> error("class ${it.propClass.simpleName} not implement.")
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

    private fun getZipFunction(it: JSPropMetaData, classToTag: Map<String, Short>): String {
        return when (it.propClass) {
            PrimitivePropMetaData::class -> getZipPrimitive(it.type!!, bufferVar(), "${objVar()}.${it.name}")
            ObjectPropMetaData::class -> "this.${
                rawFunName(it.classDec!!)
            }2(${bufferVar()},${objVar()}.${it.name});"

            EnumPropMetaData::class -> "${bufferVar()}.writeEnum(${objVar()}.${it.name});"

            //list
            ListObjectPropMetaData::class -> "${bufferVar()}.writeSize(${objVar()}.${it.name}.length);" +
                    "${objVar()}.${it.name}.forEach(function(${bufferVar()}, ${subObjVar()}){${
                        "this.${rawFunName(it.elementClass!!)}2(${bufferVar()}, ${subObjVar()})"
                    }}.bind(this,${bufferVar()}));"

            ListPrimitivePropMetaData::class -> "${bufferVar()}.writeSize(${objVar()}.${it.name}.length);" +
                    "${objVar()}.${it.name}.forEach(function(${bufferVar()}, ${subObjVar()}){${
                        getZipPrimitive(it.type!!, bufferVar(), subObjVar())
                    }}.bind(this,${bufferVar()}));"

            ListEnumPropMetaData::class -> "${bufferVar()}.writeSize(${objVar()}.${it.name}.length);" +
                    "${objVar()}.${it.name}.forEach(function(${bufferVar()}, ${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}.bind(this,${bufferVar()}));"

            //map
            MapPrimitiveKeyValueMetaData::class -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType!!, bufferVar(), subObjVar())
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType!!, bufferVar(), subObjVar())
                    }});"

            MapPrimitiveKeyObjectValueMetaData::class -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType!!, bufferVar(), subObjVar())
                    }}," +
                    "this.${rawFunName(it.valueClassDec!!)}2.bind(this));"

            MapPrimitiveKeyEnumValue::class -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.keyType!!, bufferVar(), subObjVar())
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }});"

            MapEnumKeyEnumValue::class -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }});"

            MapEnumKeyObjectValuePropMetaData::class -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}," +
                    "this.${rawFunName(it.valueClassDec!!)}2.bind(this));"

            MapEnumKeyPrimitiveValuePropMetaData::class -> "this.${zipMapFun()}(${bufferVar()},${objVar()}.${it.name}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        "${bufferVar()}.writeEnum(${subObjVar()})"
                    }}," +
                    "function(${bufferVar()},${subObjVar()}){${
                        getZipPrimitive(it.valueType!!, bufferVar(), subObjVar())
                    }});"

            else -> error("class ${it.propClass.simpleName} not implement.")
        }
    }

    private fun classExtractFunction(classToTag: MutableMap<String, Short>): String {
        return if (classDec.isSealed)
            "var javaClass= ${bufferVar()}.readJavaClass(); switch(javaClass){${
                classDec.getAllActualChild().joinToString("") {
                    "case ${
                        classToTag[it.toClassName().toString()]
                    }:{return this.${rawFunName(it)}0(${bufferVar()})}"
                }
            }default: throw new Error(${errorType(classId, "javaClass")})}"
        else
            "return this.${rawFunName()}1(${
                constructorProps.joinToString(",") { getExtractFunction(it, classToTag) }
            })"
    }


    private fun classZipFunction(classToTag: MutableMap<String, Short>): String {
        return if (classDec.isSealed)
            "${bufferVar()}.writeJavaClass(${objVar()}.javaClass);switch(${objVar()}.javaClass){${
                classDec.getAllActualChild().joinToString("") {
                    "case ${
                        classToTag[it.toClassName()]
                    }:{ this.${rawFunName(it)}2(${bufferVar()}, ${objVar()});break;}"
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
        file.write(
            "${rawFunName()}1:function(${
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
        file.write("/** @typedef {${classDec.superClassName}} ${classDec.toClassName()}\n")
        constructorProps.forEach { file.write("* @property {${jsDocType(it)}} ${it.name}\n") }
        file.write("*/\n")
        file.write("/**@typedef {JavaClass} ${classDec.toClassName()}${AbstractPropMetadata.serializerObjectNameSuffix}\n");
        file.write(" * @property {function(JReadBuffer):${classDec.toClassName()}} extract\n")
        file.write(" * @property {function(JWriteBuffer,${classDec.toClassName()}):void} zip\n")
        file.write(" * @property {function(${constructorProps.joinToString { "${it.name}:${jsDocType(it)}" }}):${classDec.toClassName()}} create\n")
        file.write("*/\n")
    }

}

class JSEnum(classDec: KSClassDeclaration) : JSElement(classDec) {


    override fun treeElementString(): String {
        return "{${classDec.getAllEnumEntryWithIndex().joinToString { "${it.first}:${it.second}" }}}"
    }

    override fun jsDocTree(): String {
        return "/** @Enum {number} */"
    }

    fun writeEnumJSDoc(file: BufferedWriter) {
        file.write("/** @Enum {number} ${classDec.toClassName()} */\n")
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
        var maxTag = classDecToUniqueTag.maxBy { e -> e.value }.value + 1;
        classDecToUniqueTag.replaceAll { _, value -> if (value < 0) (maxTag++).toShort() else value };
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
        enums.forEach {
            it.writeEnumJSDoc(file)
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
        file.write("arrayToMap:function(_m,_o){ _m[_o.key]=_o.value;return _m},version:${version}")
        file.write("};\n")
        file.write("${AbstractPropMetadata.serializerObjectNameSuffix}.instance=${createTree(classTree)}")
    }

    private fun createTree(classTree: TreeNode): String {
        return if (classTree.type == null) "{${
            classTree.child.entries.joinToString {
                "${if (it.value.type == null) "" else it.value.type!!.jsDocTree()}${it.key}: ${
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
        this.classDecToUniqueTag[jsClass.classDec.toClassName()] = jsClass.classId

        this.addTree(
            jsClass.classDec.packageName.split("."),
            jsClass.classDec.simpleName,
            jsClass
        )
    }

    fun addEnum(jsEnum: JSEnum) {
        this.enums.add(jsEnum)
        this.addTree(jsEnum.classDec.packageName.split("."), jsEnum.classDec.simpleName, jsEnum)
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
        if (this.setVersion)
            return
        this.version = protocolVersion
        this.setVersion = true
    }

}