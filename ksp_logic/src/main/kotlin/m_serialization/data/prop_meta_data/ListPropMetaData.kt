package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

sealed class ListPropMetaData : AbstractPropMetadata() {

}

class ListPrimitivePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val type: PrimitiveType
) : ListPropMetaData() {


    override fun getWriteStatement(): String {
        TODO("Not yet implemented")
    }

    override fun addImport(): List<String> {
        TODO("Not yet implemented")
    }
}

class ListObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val targetClass: KSClassDeclaration
) : ListPropMetaData() {


    override fun getWriteStatement(): String {
        TODO("Not yet implemented")
    }

    override fun addImport(): List<String> {
        TODO("Not yet implemented")
    }
}