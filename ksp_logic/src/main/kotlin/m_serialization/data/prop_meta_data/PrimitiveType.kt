package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSType


enum class PrimitiveType(val className: String) {


    INT(Int::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeInt(%s)",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) {
                "val $varNameToAssign = ${bufferVarName}.readInt()"
            } else {
                "$varNameToAssign = ${bufferVarName}.readInt()"
            }
        }

        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = 4//int",
                emptySet()
            )
        }
    },

    SHORT(Short::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeShort(%s.toInt())",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) {
                "val $varNameToAssign = ${bufferVarName}.readShort()"
            } else {
                "$varNameToAssign = ${bufferVarName}.readShort()"
            }
        }


        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = 2//short",
                emptySet()
            )
        }
    },
    DOUBLE(Double::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeDouble(%s)",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) {
                "val $varNameToAssign = ${bufferVarName}.readDouble()"
            } else {
                "$varNameToAssign = ${bufferVarName}.readDouble()"
            }
        }

        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = 8//double",
                emptySet()
            )
        }
    },
    BYTE(Byte::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeByte(%s.toInt())",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) {
                "val $varNameToAssign = ${bufferVarName}.readByte()"
            } else {
                "$varNameToAssign = ${bufferVarName}.readByte()"
            }
        }

        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = 1//byte",
                emptySet()
            )
        }
    },
    BOOL(Boolean::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeBool(%s)",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) "val $varNameToAssign = ${bufferVarName}.readBool()"
            else "$varNameToAssign = ${bufferVarName}.readBool()"
        }

        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = 1// bool",
                emptySet()
            )
        }

    },
    FLOAT(Float::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeFloat(%s)",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) "val $varNameToAssign = ${bufferVarName}.readFloat()"
            else "$varNameToAssign = ${bufferVarName}.readFloat()"
        }


        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = 4//float",
                emptySet()
            )
        }
    },
    LONG(Long::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeLong(%s)",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) "val $varNameToAssign = ${bufferVarName}.readLong()"
            else "$varNameToAssign = ${bufferVarName}.readLong()"
        }

        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = 8//long",
                emptySet()
            )
        }
    },
    STRING(String::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeString(%s)",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) "val $varNameToAssign = ${bufferVarName}.readString()"
            else "$varNameToAssign = ${bufferVarName}.readString()"
        }

        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = $propName.strSerializeSize()",
                setOf("m_serialization.utils.ByteBufUtils.strSerializeSize")
            )
        }
    },


    BYTE_ARRAY(ByteArray::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeByteArray(%s)",
                bufferVarName,
                varName
            )
        }

        override fun readFromBufferExpression(
            bufferVarName: String,
            varNameToAssign: String,
            declareNewVar: Boolean
        ): String {
            return if (declareNewVar) "val $varNameToAssign = ${bufferVarName}.readByteArray()"
            else "$varNameToAssign = ${bufferVarName}.readByteArray()"
        }

        override fun expressionAndImportForCalSerializeSize(
            varName: String,
            propName: String
        ): Pair<String, Set<String>> {
            return Pair(
                "var $varName = $propName.byteArraySerializeSize()",
                setOf("m_serialization.utils.ByteBufUtils.byteArraySerializeSize")
            )
        }
    };

    abstract fun writeToBufferExpression(bufferVarName: String, varName: String): String;


    // return kiểu như readInt(), readString()
    // các phép tạo biến gán bằng sẽ do người dùng bên kia viết
    abstract fun readFromBufferExpression(
        bufferVarName: String,
        varNameToAssign: String,
        declareNewVar: Boolean
    ): String


    abstract fun expressionAndImportForCalSerializeSize(varName: String, propName:String):Pair<String,Set<String>>


    companion object {

        fun simpleName(primitiveType: PrimitiveType): String {
            return when (primitiveType) {
                INT -> "Int"
                SHORT -> "Short"
                DOUBLE -> "Double"
                BYTE -> "Byte"
                BOOL -> "Boolean"
                FLOAT -> "Float"
                LONG -> "Long"
                STRING -> "String"
                BYTE_ARRAY -> "ByteArray"
            }
        }

        private val allPrimitiveNameToPrimitiveType = PrimitiveType
            .values()
            .asSequence()
            .associateBy { it.className }


        fun KSType.isPrimitive(): Boolean {
            return allPrimitiveNameToPrimitiveType.containsKey(declaration.qualifiedName!!.asString())
        }

        fun KSType.isPrimitiveNotByteArray(): Boolean {
            return (allPrimitiveNameToPrimitiveType.containsKey(declaration.qualifiedName!!.asString())
                    && declaration.qualifiedName!!.asString() != BYTE_ARRAY.className)
        }

        fun KSType.toPrimitiveType(): PrimitiveType {
            return allPrimitiveNameToPrimitiveType.getValue(declaration.qualifiedName!!.asString())
        }

        fun addImportExpressionForWrite(type: PrimitiveType): List<String> {
            val res: List<String> = when (type) {
                INT -> emptyList()
                SHORT -> emptyList()
                DOUBLE -> emptyList()
                BYTE -> emptyList()
                BOOL -> {
                    listOf(
                        "m_serialization.utils.ByteBufUtils.writeBool"
                    )
                }

                FLOAT -> emptyList()
                LONG -> emptyList()
                STRING -> {
                    listOf(
                        "m_serialization.utils.ByteBufUtils.writeString"
                    )
                }

                BYTE_ARRAY -> {
                    listOf(
                        "m_serialization.utils.ByteBufUtils.writeByteArray"
                    )
                }
            }
            return res
        }

        fun addImportExpressionForRead(type: PrimitiveType): List<String> {
            val res: List<String> = when (type) {
                INT -> emptyList()
                SHORT -> emptyList()
                DOUBLE -> emptyList()
                BYTE -> emptyList()
                BOOL -> {
                    listOf(
                        "m_serialization.utils.ByteBufUtils.readBool"
                    )
                }

                FLOAT -> emptyList()
                LONG -> emptyList()
                STRING -> {
                    listOf(
                        "m_serialization.utils.ByteBufUtils.readString"
                    )
                }

                BYTE_ARRAY -> {
                    listOf(
                        "m_serialization.utils.ByteBufUtils.readByteArray"
                    )
                }
            }
            return res
        }
    }
}
