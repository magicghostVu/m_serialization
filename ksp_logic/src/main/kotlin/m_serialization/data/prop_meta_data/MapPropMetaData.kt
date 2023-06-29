package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.utils.KSClassDecUtils.getWriteObjectStatement
import m_serialization.utils.KSClassDecUtils.importSerializer

sealed class MapPropMetaData() : AbstractPropMetadata() {
    abstract val keyType: PrimitiveType
}

class MapPrimitiveValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    private val valueType: PrimitiveType
) : MapPropMetaData() {
    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeInt(%s.size)
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

    override fun addImport(): List<String> {
        val r = mutableListOf<String>()
        r.addAll(PrimitiveType.addImportExpression(keyType))
        r.addAll(PrimitiveType.addImportExpression(valueType))
        return r;
    }
}

class MapObjectValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    private val valueClassDec: KSClassDeclaration
) : MapPropMetaData() {
    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";
        val statement = """
            %s.writeInt(%s.size)
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

    override fun addImport(): List<String> {
        val list = mutableListOf<String>()
        list.addAll(PrimitiveType.addImportExpression(keyType))
        list.addAll(valueClassDec.importSerializer())
        return list
    }
}