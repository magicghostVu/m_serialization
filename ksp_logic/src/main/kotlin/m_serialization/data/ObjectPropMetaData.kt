package m_serialization.data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class ObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration
) : AbstractPropMetadata() {

    // check class khai báo
    // nếu là sealed thì gọi serializer
    override fun getWriteStatement(): String {
        TODO("Not yet implemented")
    }

    override fun addImport(): List<String> {
        TODO("Not yet implemented")
    }
}