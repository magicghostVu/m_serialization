package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class PrimitivePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val type: PrimitiveType
) : AbstractPropMetadata() {

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val valName = "buffer"
        return type.writeToBufferExpression(valName, "${objectNameContainThisProp}.$name")
    }

    override fun addImportForWrite(): List<String> {
        return PrimitiveType.addImportExpressionForWrite(type)
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        return type.readFromBufferExpression(bufferVarName, varNameToAssign, declareNewVar)
    }

    override fun addImportForRead(): List<String> {
        return PrimitiveType.addImportExpressionForRead(type)
    }

    override fun mtoString(): String {
        return PrimitiveType.simpleName(type)
    }

    override fun addImportForCalculateSize(): List<String> {
        TODO("Not yet implemented")
    }

    override fun expressionForCalSize(varNameToAssign: String): String {
        TODO("Not yet implemented")
    }
}