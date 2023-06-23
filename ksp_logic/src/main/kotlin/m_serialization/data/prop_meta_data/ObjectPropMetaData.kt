package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class ObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val classDec: KSClassDeclaration// class được khai báo trong code
) : AbstractPropMetadata() {

    // check class khai báo
    // nếu là sealed thì gọi serializer của lớp base
    override fun getWriteStatement(): String {
        TODO("Not yet implemented")
    }

    override fun addImport(): List<String> {
        TODO("Not yet implemented")
    }
}