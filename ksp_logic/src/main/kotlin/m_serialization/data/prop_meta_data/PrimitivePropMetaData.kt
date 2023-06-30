package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class PrimitivePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    private val type: PrimitiveType
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
}