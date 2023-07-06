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





sealed class MapPrimitiveKeyPropMetaData(val mapTypeAtSource: MapTypeAtSource) : AbstractPropMetadata() {
    abstract val keyType: PrimitiveType
}


class MapPrimitiveValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    private val valueType: PrimitiveType,
    mapTypeAtSource: MapTypeAtSource
) : MapPrimitiveKeyPropMetaData(mapTypeAtSource) {
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
        readExpression.append(
            "val size$varNameToAssign = ${bufferVarName}.readShort().toInt()\n"
        )


        val mapTypeWillCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToAssign = $mapTypeWillCreate<${PrimitiveType.simpleName(keyType)},${
                PrimitiveType.simpleName(
                    valueType
                )
            }>()\n"
        )

        readExpression.append(
            """
            repeat(size${varNameToAssign}){
                ${keyType.readFromBufferExpression(bufferVarName, "key", true)}
                ${valueType.readFromBufferExpression(bufferVarName, "value", true)}
                tmpMap${varNameToAssign}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToAssign")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToAssign")
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

class MapObjectValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    private val valueClassDec: KSClassDeclaration,
    mapTypeAtSource: MapTypeAtSource
) : MapPrimitiveKeyPropMetaData(mapTypeAtSource) {
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
        readExpression.append(
            "val size$varNameToAssign = ${bufferVarName}.readShort().toInt()\n"
        )


        // create
        val mapToCreate: String = when (mapTypeAtSource) {
            MapTypeAtSource.ImmutableMap -> "mutableMapOf"
            MapTypeAtSource.MMutableMap -> "mutableMapOf"
            MapTypeAtSource.MTreeMap -> "TreeMap"
        }

        readExpression.append(
            "val tmpMap$varNameToAssign = $mapToCreate<${PrimitiveType.simpleName(keyType)},${valueClassDec.simpleName.asString()}>()\n"
        )

        val objectNameToCallRead = valueClassDec.getSerializerObjectName()

        readExpression.append(
            """
            repeat(size${varNameToAssign}){
                ${keyType.readFromBufferExpression(bufferVarName, "key", true)}
                val value = ${objectNameToCallRead}.$readFromFuncName($bufferVarName)
                tmpMap${varNameToAssign}[key] = value
        }
        
        """
        );


        if (declareNewVar) {
            readExpression.append("val $varNameToAssign = tmpMap$varNameToAssign")
        } else {
            readExpression.append("$varNameToAssign = tmpMap$varNameToAssign")
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
        return list
    }
}