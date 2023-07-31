package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.getWriteObjectStatement
import m_serialization.utils.KSClassDecUtils.importSerializer

class ObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val classDec: KSClassDeclaration,// class được khai báo trong code
    val nullable: Boolean
) : AbstractPropMetadata() {

    override fun mtoString(): String {
        return classDec.qualifiedName!!.asString()
    }

    // check class khai báo
    // nếu là sealed thì gọi serializer của lớp base
    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer"
        val rBuilder = StringBuilder()
        val readExpression = classDec.getWriteObjectStatement(bufferVarName, "${objectNameContainThisProp}.${name}")

        if (nullable) {
            rBuilder.append("if(${objectNameContainThisProp}.${name} == null){\n")
            rBuilder.append("${bufferVarName}.writeByte(0);\n")
            rBuilder.append("}\n")
            rBuilder.append("\nelse{\n")
            rBuilder.append("${bufferVarName}.writeByte(1)\n")
            rBuilder.append(readExpression).append("\n")
            rBuilder.append("}\n")
        } else {
            rBuilder.append(readExpression)
                .append("\n")
        }
        return rBuilder.toString()
    }

    // import object serializer of this class
    override fun addImportForWrite(): List<String> {
        return classDec.importSerializer() +
                classDec.qualifiedName!!.asString()
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val serializerObjectName = classDec.getSerializerObjectName()


        val functionNameWillCall = if (nullable) {
            readFromNullableFuncName
        } else {
            readFromFuncName
        }

        return if (declareNewVar) {
            "val $varNameToAssign = ${serializerObjectName}.${functionNameWillCall}($bufferVarName)"
        } else {
            "$varNameToAssign = ${serializerObjectName}.${functionNameWillCall}($bufferVarName)"
        }
    }

    override fun addImportForRead(): List<String> {
        return classDec.importSerializer() +
                classDec.qualifiedName!!.asString()
    }
}