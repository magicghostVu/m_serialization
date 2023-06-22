package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

sealed class MapPropMetaData() : AbstractPropMetadata() {
    abstract val keyType: PrimitiveType
}

class MapPrimitiveValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    val valueType: PrimitiveType
) : MapPropMetaData() {
    override fun getWriteStatement(): String {
        TODO("Not yet implemented")
    }

    override fun addImport(): List<String> {
        TODO("Not yet implemented")
    }
}

class MapObjectValueMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    override val keyType: PrimitiveType,
    val valueClassDec: KSClassDeclaration
) : MapPropMetaData() {
    override fun getWriteStatement(): String {
        TODO("Not yet implemented")
    }

    override fun addImport(): List<String> {
        TODO("Not yet implemented")
    }
}