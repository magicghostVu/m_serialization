package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.utils.KSClassDecUtils.getWriteObjectStatement
import m_serialization.utils.KSClassDecUtils.importSerializer

sealed class ListPropMetaData : AbstractPropMetadata() {

}

enum class ListTypeAtSource {
    List,// immutable list
    MutableList,
    LinkedList
}

class ListPrimitivePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    private val type: PrimitiveType
) : ListPropMetaData() {


    override fun getWriteStatement(): String {
        val bufferVarName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeInt(%s.size)// list size
                for (e in %s) {
                    ${type.writeToBufferExpression(bufferVarName, "e")}
                }
            """,
            bufferVarName,
            name,
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
        val bufferVarName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeInt(%s.size)// list size
                for (e in %s) {
                    ${elementClass.getWriteObjectStatement(bufferVarName, "e")}
                }
            """,
            bufferVarName,
            name,
            name
        )
        return r
    }

    override fun addImport(): List<String> {
        return elementClass.importSerializer()
    }
}