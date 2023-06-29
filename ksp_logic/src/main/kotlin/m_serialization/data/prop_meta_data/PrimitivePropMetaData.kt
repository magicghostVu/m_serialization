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

    override fun addImport(): List<String> {
        return PrimitiveType.addImportExpression(type)
    }
}