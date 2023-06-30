package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.getWriteObjectStatement
import m_serialization.utils.KSClassDecUtils.importSerializer
import java.lang.StringBuilder

sealed class ListPropMetaData : AbstractPropMetadata() {

}

// deserialize
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


    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeInt(%s.size)// list size
                for (e in %s) {
                    ${type.writeToBufferExpression(bufferVarName, "e")}
                }
            """,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
        return r
    }

    override fun addImportForWrite(): List<String> {
        return PrimitiveType.addImportExpressionForWrite(type)
    }

    override fun getReadStatement(
        bufferVarName: String,
        varNameToAssign: String,
        declareNewVar: Boolean
    ): String {
        val readStatement = StringBuilder()

        val sizeVarName = "size${varNameToAssign}"


        val typeParamsForCreateList =
            (propDec.type.resolve().arguments[0].type!!.resolve().declaration as KSClassDeclaration).simpleName.asString()

        val listTmpName = "list$varNameToAssign"

        readStatement.append("val $sizeVarName = ${bufferVarName}.readInt()\n")

        // todo: construct lại đúng loại list ban đầu
        readStatement.append("val $listTmpName = mutableListOf<$typeParamsForCreateList>()\n")
        readStatement.append(
            """
            repeat($sizeVarName){
            ${type.readFromBufferExpression(bufferVarName, "e", true)}
            ${listTmpName}.add(e)
        }
        """
        )

        val assignExpression = if (declareNewVar) {
            "val $varNameToAssign = $listTmpName\n"
        } else {
            "$varNameToAssign = $listTmpName\n"
        }
        readStatement.append(assignExpression)
        return readStatement.toString()
    }

    override fun addImportForRead(): List<String> {
        return PrimitiveType.addImportExpressionForRead(type)
    }
}

// if element class is sealed so insert unique tag otherwise not
class ListObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val elementClass: KSClassDeclaration
) : ListPropMetaData() {


    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeInt(%s.size)// list size
                for (e in %s) {
                    ${elementClass.getWriteObjectStatement(bufferVarName, "e")}
                }
            """,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
        return r
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readStatement = StringBuilder()
        val sizeVarName = "size${varNameToAssign}"
        val typeParamsForCreateList =
            (propDec.type.resolve().arguments[0].type!!.resolve().declaration as KSClassDeclaration).simpleName.asString()

        val listTmpName = "list$varNameToAssign"

        readStatement.append("val $sizeVarName = ${bufferVarName}.readInt()\n")

        // todo: construct lại đúng loại list ban đầu

        // lấy serializer và read
        val objectNameToCallRead = elementClass.getSerializerObjectName()

        readStatement.append("val $listTmpName = mutableListOf<$typeParamsForCreateList>()\n")
        readStatement.append(
            """
            repeat($sizeVarName){
            val e = ${objectNameToCallRead}.$readFromFuncName($bufferVarName)
            ${listTmpName}.add(e)
        }
        """
        )

        val assignExpression = if (declareNewVar) {
            "val $varNameToAssign = $listTmpName\n"
        } else {
            "$varNameToAssign = $listTmpName\n"
        }
        readStatement.append(assignExpression)
        return readStatement.toString()
    }

    override fun addImportForRead(): List<String> {
        val packageName = elementClass.packageName.asString()
        return listOf(
            "${packageName}.${elementClass.getSerializerObjectName()}"
        )
    }

    override fun addImportForWrite(): List<String> {
        return elementClass.importSerializer()
    }
}