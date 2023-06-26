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
    },

    SHORT(Short::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeShort(%s.toInt())",
                bufferVarName,
                varName
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
    },
    BYTE(Byte::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeByte(%s.toInt())",
                bufferVarName,
                varName
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
    },
    FLOAT(Float::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeFloat(%s)",
                bufferVarName,
                varName
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
    },
    STRING(String::class.qualifiedName!!) {
        override fun writeToBufferExpression(bufferVarName: String, varName: String): String {
            return String.format(
                "%s.writeString(%s)",
                bufferVarName,
                varName
            )
        }
    };

    abstract fun writeToBufferExpression(bufferVarName: String, varName: String): String;

    companion object {
        private val allPrimitiveNameToPrimitiveType = PrimitiveType
            .values()
            .asSequence()
            .associateBy { it.className }


        fun KSType.isPrimitive(): Boolean {
            return allPrimitiveNameToPrimitiveType.containsKey(declaration.qualifiedName!!.asString())
        }

        fun KSType.toPrimitiveType(): PrimitiveType {
            return allPrimitiveNameToPrimitiveType.getValue(declaration.qualifiedName!!.asString())
        }

        fun addImportExpression(type: PrimitiveType): List<String> {
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
            }
            return res
        }
    }
}
