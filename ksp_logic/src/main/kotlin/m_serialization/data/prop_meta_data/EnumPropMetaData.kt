package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.importSerializer

class EnumPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val enumClass: KSClassDeclaration
) : AbstractPropMetadata() {

    override fun getWriteStatement(objectNameContainThisProp: String): String {
        return "buffer.writeShort(${enumClass.getSerializerObjectName()}" +
                ".toId(${objectNameContainThisProp}.$name).toInt())"
    }

    override fun addImportForWrite(): List<String> {
        return enumClass.importSerializer() + enumClass.qualifiedName!!.asString()
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val builder = StringBuilder()
        if (declareNewVar) {
            builder.append("val $varNameToAssign = ")
        } else {
            builder.append("$varNameToAssign = ")
        }
        builder.append("${enumClass.getSerializerObjectName()}.fromId($bufferVarName.readShort())")
        return builder.toString()
    }

    override fun addImportForRead(): List<String> {
        return enumClass.importSerializer() + enumClass.qualifiedName!!.asString()
    }


    override fun addImportForCalculateSize(): List<String> = emptyList()


    override fun expressionForCalSize(varNameToAssign: String): String {
        return "var $varNameToAssign=2"
    }
    override fun mtoString(): String {
        return enumClass.simpleName.asString()
    }
}
