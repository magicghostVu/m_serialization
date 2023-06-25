package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

sealed class ListPropMetaData : AbstractPropMetadata() {

}

class ListPrimitivePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    private val type: PrimitiveType
) : ListPropMetaData() {


    override fun getWriteStatement(): String {
        val valName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeInt(%s.size)// list size
                for (e in %s) {
                    ${type.writeToBufferExpression(valName, "e")}
                }
            """.trimIndent(),
            valName,
            name
        )
        return r
    }

    override fun addImport(): List<String> {
        return PrimitiveType.addImportExpression(type)
    }
}

// if element class is sealed so insert unique tag otherwise not
class ListObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val elementClass: KSClassDeclaration
) : ListPropMetaData() {


    override fun getWriteStatement(): String {
        TODO("Not yet implemented")
    }

    override fun addImport(): List<String> {
        TODO("Not yet implemented")
    }
}