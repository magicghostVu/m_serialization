package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.getWriteObjectStatement
import m_serialization.utils.KSClassDecUtils.importSerializer
import java.lang.StringBuilder


sealed class ListPropMetaData(val listType: KSType) : AbstractPropMetadata() {
    fun listTypeAtSource(): ListTypeAtSource {
        return ListTypeAtSource.fromName(listType.declaration.qualifiedName!!.asString())
    }
}

// deserialize
enum class ListTypeAtSource(val fullName: String) {
    List("kotlin.collections.List"),// immutable list
    MutableList("kotlin.collections.MutableList"),
    MLinkedList("java.util.LinkedList");

    companion object {
        private val map = ListTypeAtSource
            .values()
            .asSequence()
            .associateBy { it.fullName }

        fun fromName(fullName: String): ListTypeAtSource {
            return map.getValue(fullName)
        }


        fun createNewExpression(listType: ListTypeAtSource, typeParam: String): String {
            return when (listType) {
                List -> "mutableListOf<$typeParam>()"
                MutableList -> "mutableListOf<$typeParam>()"
                MLinkedList -> "LinkedList<$typeParam>()"
            }
        }

    }
}

class ListPrimitivePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    private val type: PrimitiveType,
    listType: KSType
) : ListPropMetaData(listType) {


    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeShort(%s.size.toInt())// list size
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

        readStatement.append("val $sizeVarName = ${bufferVarName}.readShort().toInt()\n")

        // todo: construct lại đúng loại list ban đầu
        //readStatement.append("val $listTmpName = mutableListOf<$typeParamsForCreateList>()\n")
        readStatement.append(
            "val $listTmpName = ${
                ListTypeAtSource.createNewExpression(
                    listTypeAtSource(),
                    typeParamsForCreateList
                )
            }\n"
        )
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
        val l = PrimitiveType.addImportExpressionForRead(type);
        val s = "java.util.LinkedList"
        val f = if (listTypeAtSource() == ListTypeAtSource.MLinkedList) {
            listOf(s)
        } else {
            emptyList()
        }
        return l + f
    }
}

// if element class is sealed so insert unique tag otherwise not
class ListObjectPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val elementClass: KSClassDeclaration,
    listType: KSType
) : ListPropMetaData(listType) {


    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeShort(%s.size)// list size
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
        val typeParamsForCreateList = elementClass.simpleName.asString()

        val listTmpName = "list$varNameToAssign"

        readStatement.append("val $sizeVarName = ${bufferVarName}.readShort().toInt()\n")

        // todo: construct lại đúng loại list ban đầu

        // lấy serializer và read
        val objectNameToCallRead = elementClass.getSerializerObjectName()

        readStatement.append(
            "val $listTmpName = ${
                ListTypeAtSource.createNewExpression(
                    listTypeAtSource(),
                    typeParamsForCreateList
                )
            }\n"
        )
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
        val t = listOf(
            "${packageName}.${elementClass.getSerializerObjectName()}"
        )
        val importIfLinkedList = if (listTypeAtSource() == ListTypeAtSource.MLinkedList) {
            listOf("java.util.LinkedList")
        } else emptyList()
        return t + importIfLinkedList
    }

    override fun addImportForWrite(): List<String> {
        return elementClass.importSerializer()
    }
}

class ListEnumPropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val enumClass: KSClassDeclaration,
    listType: KSType
) : ListPropMetaData(listType) {
    override fun getWriteStatement(objectNameContainThisProp: String): String {
        val bufferVarName = "buffer";// ByteBuf
        val r = String.format(
            """%s.writeShort(%s.size)// list size
                for (e in %s) {
                    buffer.writeShort(${enumClass.getSerializerObjectName()}.toId(e).toInt())
                }
            """,
            bufferVarName,
            "${objectNameContainThisProp}.$name",
            "${objectNameContainThisProp}.$name"
        )
        return r
    }

    override fun addImportForWrite(): List<String> {
        return enumClass.importSerializer()
    }

    override fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String {
        val readStatement = StringBuilder()
        val sizeVarName = "size${varNameToAssign}"
        val typeParamsForCreateList = enumClass.simpleName.asString()

        val listTmpName = "list$varNameToAssign"

        readStatement.append("val $sizeVarName = ${bufferVarName}.readShort().toInt()\n")

        // todo: construct lại đúng loại list ban đầu

        // lấy serializer và read
        //val objectNameToCallRead = elementClass.getSerializerObjectName()

        readStatement.append(
            "val $listTmpName = ${
                ListTypeAtSource.createNewExpression(
                    listTypeAtSource(),
                    typeParamsForCreateList
                )
            }\n"
        )
        readStatement.append(
            """
            repeat($sizeVarName){
            val e = ${enumClass.getSerializerObjectName()}.fromId($bufferVarName.readShort())
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
        return enumClass.importSerializer()
    }
}