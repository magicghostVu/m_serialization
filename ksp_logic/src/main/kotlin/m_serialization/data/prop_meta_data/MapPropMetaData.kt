package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.getWriteObjectStatement
import m_serialization.utils.KSClassDecUtils.importSerializer
import java.lang.StringBuilder

enum class MapTypeAtSource(val fullName: String) {
    ImmutableMap("kotlin.collections.Map"),
    MMutableMap("kotlin.collections.MutableMap"),
    MTreeMap("java.util.TreeMap");


    companion object {
        private val _tmpMap = MapTypeAtSource
            .values()
            .asSequence()
            .associateBy { it.fullName }

        fun fromType(typeAtDeclaration: KSType): MapTypeAtSource {
            return _tmpMap.getValue(typeAtDeclaration.declaration.qualifiedName!!.asString())
        }
    }
}


sealed class MapPropMetaData(val mapTypeAtSource: MapTypeAtSource) : AbstractPropMetadata() {

}


sealed class MapEnumKeyPropMetaData(
    mapTypeAtSource: MapTypeAtSource,
    val enumKey: KSClassDeclaration
) : MapPropMetaData(mapTypeAtSource) {

}


class MapEnumKeyPrimitiveValuePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    enumKey: KSClassDeclaration,
    val valueType: PrimitiveType,
    mapTypeAtSource: MapTypeAtSource
) : MapEnumKeyPropMetaData(mapTypeAtSource, enumKey)
{

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeShort(%s.size)
            %s.forEach{ (k,v) ->
                $bufferVarName.writeShort(${enumKey.getSerializerObjectName()}.toId(k).toInt())
                ${valueType.writeToBufferExpression(bufferVarName, "v")}
            }
        """
        return String.format(
            statement,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
    }

    override fun addImportForWrite(): List<String> {
        val l = mutableListOf<String>()
        l.addAll(enumKey.importSerializer())
        l.addAll(PrimitiveType.addImportExpressionForWrite(valueType))
        l.add(enumKey.qualifiedName!!.asString())
        return l;
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readExpression = StringBuilder();


        // trong trường hợp gán lại cho các prop không trong constructor
        val varNameToUse = if (!declareNewVar) {
            varNameToAssign.split(".")[1]
        } else varNameToAssign


        readExpression.append(
            "val size$varNameToUse = ${bufferVarName}.readShort().toInt()\n"
        )


        val mapTypeWillCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToUse = $mapTypeWillCreate<${enumKey.simpleName.asString()},${
                PrimitiveType.simpleName(
                    valueType
                )
            }>()\n"
        )

        readExpression.append(
            """
            repeat(size${varNameToUse}){
                val key = ${enumKey.getSerializerObjectName()}.fromId(${bufferVarName}.readShort())
                ${valueType.readFromBufferExpression(bufferVarName, "value", true)}
                tmpMap${varNameToUse}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToUse")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToUse")
        }
        return readExpression.toString()
    }

    override fun addImportForRead(): List<String> {
        val r = mutableListOf<String>()
        r.addAll(enumKey.importSerializer())
        r.addAll(PrimitiveType.addImportExpressionForRead(valueType))
        r.add(enumKey.qualifiedName!!.asString())
        return r;
    }

    override fun mtoString(): String {
        return "map<${enumKey.simpleName.asString()},${PrimitiveType.simpleName(valueType)}>"
    }
}


class MapEnumKeyObjectValuePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    enumKey: KSClassDeclaration,
    val valueType: KSClassDeclaration,
    mapTypeAtSource: MapTypeAtSource
) : MapEnumKeyPropMetaData(mapTypeAtSource, enumKey)
{

    override fun mtoString(): String {
        return "map<${enumKey.simpleName.asString()},${valueType.simpleName.asString()}>"
    }

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeShort(%s.size)
            %s.forEach{ (k,v) ->
                $bufferVarName.writeShort(${enumKey.getSerializerObjectName()}.toId(k).toInt())
                ${valueType.getWriteObjectStatement(bufferVarName, "v")}
            }
        """
        return String.format(
            statement,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
    }

    override fun addImportForWrite(): List<String> {
        return enumKey.importSerializer() +
                valueType.importSerializer() +
                enumKey.qualifiedName!!.asString() +
                valueType.qualifiedName!!.asString()
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readExpression = StringBuilder();


        // trong trường hợp gán lại cho các prop không trong constructor
        val varNameToUse = if (!declareNewVar) {
            varNameToAssign.split(".")[1]
        } else varNameToAssign


        readExpression.append(
            "val size$varNameToUse = ${bufferVarName}.readShort().toInt()\n"
        )


        val mapTypeWillCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToUse = $mapTypeWillCreate<${enumKey.simpleName.asString()},${
                valueType.simpleName.asString()
            }>()\n"
        )


        val objectNameToCallRead = valueType.getSerializerObjectName()

        readExpression.append(
            """
            repeat(size${varNameToUse}){
                val key = ${enumKey.getSerializerObjectName()}.fromId(${bufferVarName}.readShort())
                val value = ${objectNameToCallRead}.$readFromFuncName($bufferVarName)
                tmpMap${varNameToUse}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToUse")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToUse")
        }
        return readExpression.toString()
    }

    override fun addImportForRead(): List<String> {
        return valueType.importSerializer() +
                enumKey.importSerializer() +
                valueType.qualifiedName!!.asString() +
                enumKey.qualifiedName!!.asString()
    }
}


class MapEnumKeyEnumValue(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    enumKey: KSClassDeclaration,
    val enumValue: KSClassDeclaration,
    mapTypeAtSource: MapTypeAtSource
) : MapEnumKeyPropMetaData(mapTypeAtSource, enumKey)
{

    override fun mtoString(): String {
        return "map<${enumKey.simpleName.asString()},${enumValue.simpleName.asString()}>"
    }

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeShort(%s.size)
            %s.forEach{ (k,v) ->
                $bufferVarName.writeShort(${enumKey.getSerializerObjectName()}.toId(k).toInt())
                $bufferVarName.writeShort(${enumValue.getSerializerObjectName()}.toId(v).toInt())
            }
        """
        return String.format(
            statement,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
    }

    override fun addImportForWrite(): List<String> {
        return enumKey.importSerializer() +
                enumValue.importSerializer() +
                enumKey.qualifiedName!!.asString() +
                enumValue.qualifiedName!!.asString()
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readExpression = StringBuilder();


        // trong trường hợp gán lại cho các prop không trong constructor
        val varNameToUse = if (!declareNewVar) {
            varNameToAssign.split(".")[1]
        } else varNameToAssign


        readExpression.append(
            "val size$varNameToUse = ${bufferVarName}.readShort().toInt()\n"
        )


        val mapTypeWillCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToUse = $mapTypeWillCreate<${enumKey.simpleName.asString()},${
                enumValue.simpleName.asString()
            }>()\n"
        )

        readExpression.append(
            """
            repeat(size${varNameToUse}){
                val key = ${enumKey.getSerializerObjectName()}.fromId(${bufferVarName}.readShort())
                val value = ${enumValue.getSerializerObjectName()}.fromId(${bufferVarName}.readShort())
                tmpMap${varNameToUse}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToUse")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToUse")
        }
        return readExpression.toString()
    }

    override fun addImportForRead(): List<String> {
        return enumKey.importSerializer() +
                enumValue.importSerializer() +
                enumKey.qualifiedName!!.asString() +
                enumValue.qualifiedName!!.asString()
    }
}


sealed class MapPrimitiveKeyPropMetaData(mapTypeAtSource: MapTypeAtSource) : MapPropMetaData(mapTypeAtSource)
{
    abstract val keyType: PrimitiveType
}


class MapPrimitiveKeyValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    val valueType: PrimitiveType,
    mapTypeAtSource: MapTypeAtSource
) : MapPrimitiveKeyPropMetaData(mapTypeAtSource)
{
    override fun mtoString(): String {
        return "map<${PrimitiveType.simpleName(keyType)},${PrimitiveType.simpleName(valueType)}>"
    }

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeShort(%s.size)
            %s.forEach{ (k,v) ->
                ${keyType.writeToBufferExpression(bufferVarName, "k")}
                ${valueType.writeToBufferExpression(bufferVarName, "v")}
            }
        """
        return String.format(
            statement,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readExpression = StringBuilder();


        // trong trường hợp gán lại cho các prop không trong constructor
        val varNameToUse = if (!declareNewVar) {
            varNameToAssign.split(".")[1]
        } else varNameToAssign


        readExpression.append(
            "val size$varNameToUse = ${bufferVarName}.readShort().toInt()\n"
        )


        val mapTypeWillCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToUse = $mapTypeWillCreate<${PrimitiveType.simpleName(keyType)},${
                PrimitiveType.simpleName(
                    valueType
                )
            }>()\n"
        )

        readExpression.append(
            """
            repeat(size${varNameToUse}){
                ${keyType.readFromBufferExpression(bufferVarName, "key", true)}
                ${valueType.readFromBufferExpression(bufferVarName, "value", true)}
                tmpMap${varNameToUse}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToUse")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToUse")
        }
        return readExpression.toString()
    }

    override fun addImportForRead(): List<String> {
        val r = mutableSetOf<String>()
        r.addAll(PrimitiveType.addImportExpressionForRead(keyType))
        r.addAll(PrimitiveType.addImportExpressionForRead(valueType))
        when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> {}
            MapTypeAtSource.MMutableMap -> {}
            MapTypeAtSource.MTreeMap -> {
                r.add("java.util.TreeMap")
            }
        }
        return r.toList();
    }

    override fun addImportForWrite(): List<String> {
        val r = mutableListOf<String>()
        r.addAll(PrimitiveType.addImportExpressionForWrite(keyType))
        r.addAll(PrimitiveType.addImportExpressionForWrite(valueType))
        return r;
    }
}

class MapPrimitiveKeyObjectValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    val valueClassDec: KSClassDeclaration,
    mapTypeAtSource: MapTypeAtSource
) : MapPrimitiveKeyPropMetaData(mapTypeAtSource)
{
    override fun mtoString(): String {
        return "map<${PrimitiveType.simpleName(keyType)},${valueClassDec.simpleName.asString()}>"
    }

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeShort(%s.size)
            %s.forEach{ (k,v) ->
                ${keyType.writeToBufferExpression(bufferVarName, "k")}
                ${valueClassDec.getWriteObjectStatement(bufferVarName, "v")}
            }
        """
        return String.format(
            statement,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readExpression = StringBuilder();


        val varNameToUse = if (!declareNewVar) {
            varNameToAssign.split(".")[1]
        } else varNameToAssign


        readExpression.append(
            "val size$varNameToUse = ${bufferVarName}.readShort().toInt()\n"
        )


        // create
        val mapToCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToUse = $mapToCreate<${PrimitiveType.simpleName(keyType)},${valueClassDec.simpleName.asString()}>()\n"
        )

        val objectNameToCallRead = valueClassDec.getSerializerObjectName()

        readExpression.append(
            """
            repeat(size${varNameToUse}){
                ${keyType.readFromBufferExpression(bufferVarName, "key", true)}
                val value = ${objectNameToCallRead}.$readFromFuncName($bufferVarName)
                tmpMap${varNameToUse}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToUse")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToUse")
        }
        return readExpression.toString()
    }

    override fun addImportForRead(): List<String> {
        val res = mutableSetOf<String>()
        res.add(valueClassDec.qualifiedName!!.asString())
        res.addAll(PrimitiveType.addImportExpressionForRead(keyType))
        res.addAll(valueClassDec.importSerializer())
        when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> {}
            MapTypeAtSource.MMutableMap -> {}
            MapTypeAtSource.MTreeMap -> {
                res.add("java.util.TreeMap")
            }
        }
        return res.toList()
    }

    override fun addImportForWrite(): List<String> {
        val list = mutableListOf<String>()
        list.addAll(PrimitiveType.addImportExpressionForWrite(keyType))
        list.addAll(valueClassDec.importSerializer())
        list.add(valueClassDec.qualifiedName!!.asString())
        return list
    }
}


class MapPrimitiveKeyEnumValue(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    val enumValue: KSClassDeclaration,
    mapTypeAtSource: MapTypeAtSource
) : MapPrimitiveKeyPropMetaData(mapTypeAtSource)
{

    override fun mtoString(): String {
        return "map<${PrimitiveType.simpleName(keyType)},${enumValue.simpleName.asString()}>"
    }

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeShort(%s.size)
            %s.forEach{ (k,v) ->
                ${keyType.writeToBufferExpression(bufferVarName, "k")}
                $bufferVarName.writeShort(${enumValue.getSerializerObjectName()}.toId(v).toInt())
            }
        """
        return String.format(
            statement,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
    }

    override fun addImportForWrite(): List<String> {
        return PrimitiveType.addImportExpressionForWrite(keyType) +
                enumValue.importSerializer() +
                enumValue.qualifiedName!!.asString()
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readExpression = StringBuilder();


        val varNameToUse = if (!declareNewVar) {
            varNameToAssign.split(".")[1]
        } else varNameToAssign


        readExpression.append(
            "val size$varNameToUse = ${bufferVarName}.readShort().toInt()\n"
        )


        // create
        val mapToCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToUse = $mapToCreate<${PrimitiveType.simpleName(keyType)},${enumValue.simpleName.asString()}>()\n"
        )

        val objectNameToCallRead = enumValue.getSerializerObjectName()

        readExpression.append(
            """
            repeat(size${varNameToUse}){
                ${keyType.readFromBufferExpression(bufferVarName, "key", true)}
                val value = ${objectNameToCallRead}.fromId(${bufferVarName}.readShort())
                tmpMap${varNameToUse}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToUse")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToUse")
        }
        return readExpression.toString()
    }

    override fun addImportForRead(): List<String> {
        return PrimitiveType.addImportExpressionForRead(keyType) +
                enumValue.importSerializer() +
                enumValue.qualifiedName!!.asString()
    }
}