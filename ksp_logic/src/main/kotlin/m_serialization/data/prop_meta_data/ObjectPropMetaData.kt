package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

class ObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    private val classDec: KSClassDeclaration// class được khai báo trong code
) : AbstractPropMetadata() {

    // check class khai báo
    // nếu là sealed thì gọi serializer của lớp base
    override fun getWriteStatement(): String {
        val bufferVarName = "buffer"
        return if (classDec.modifiers.contains(Modifier.SEALED)) {
            val serializerObjectName = classDec.simpleName.asString() + serializerObjectNameSuffix
            val format = "%s.writeToAbstract(%s,%s)"
            String.format(format, name, serializerObjectName, bufferVarName)
        } else {
            val format = "%s.writeTo(%s)";
            String.format(format, name, bufferVarName)
        }
    }

    // import object serializer of this class
    override fun addImport(): List<String> {
        // import object
        return if (classDec.modifiers.contains(Modifier.SEALED)) {
            val packageName = classDec.packageName.asString()

            listOf(

            )
        } else {// import object and method
            listOf()
        }
    }
}