package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.getWriteObjectStatement
import m_serialization.utils.KSClassDecUtils.importSerializer

class ObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val classDec: KSClassDeclaration// class được khai báo trong code
) : AbstractPropMetadata() {

    override fun mtoString(): String {
        return classDec.qualifiedName!!.asString()
    }

    // check class khai báo
    // nếu là sealed thì gọi serializer của lớp base
    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer"
        return classDec.getWriteObjectStatement(bufferVarName, "${objectNameContainThisProp}.${name}")
    }

    // import object serializer of this class
    override fun addImportForWrite(): List<String> {
        return classDec.importSerializer() +
                classDec.qualifiedName!!.asString()
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val serializerObjectName = classDec.getSerializerObjectName()
        return if (declareNewVar) {
            "val $varNameToAssign = ${serializerObjectName}.${readFromFuncName}($bufferVarName)"
        } else {
            "$varNameToAssign = ${serializerObjectName}.${readFromFuncName}($bufferVarName)"
        }
    }

    override fun addImportForRead(): List<String> {
        return classDec.importSerializer() +
                classDec.qualifiedName!!.asString()
    }

    override fun addImportForCalculateSize(): List<String> {
        return classDec
            .importSerializer()
            .map { "$it.serializeSize" }
    }

    override fun expressionForCalSize(varNameToAssign: String): String {
        return if (classDec.modifiers.contains(Modifier.SEALED)) {
            "var $varNameToAssign = $name.serializeSize($name)"
        } else {
            "var $varNameToAssign = $name.serializeSize()"
        }
    }

}