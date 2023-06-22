package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class PrimitivePropMetaData(
    override val name: String,
    override val propDec: KSPropertyDeclaration,
    val type: PrimitiveType
) : AbstractPropMetadata() {

    override fun getWriteStatement(): String {
        val valName = "buffer"
        val res = when (type) {
            PrimitiveType.INT -> {
                String.format("%s.writeInt(%s)", valName, name)
            }

            PrimitiveType.SHORT -> {
                String.format("%s.writeShort(%s.toInt())", valName, name)
            }

            PrimitiveType.DOUBLE -> {
                String.format("%s.writeDouble(%s)", valName, name)
            }

            PrimitiveType.BYTE -> {
                String.format("%s.writeShort(%s.toInt())", valName, name)
            }

            PrimitiveType.BOOL -> {
                String.format("%s.writeBool(%s)", valName, name)
            }

            PrimitiveType.FLOAT -> {
                String.format("%s.writeFloat(%s)", valName, name)
            }

            PrimitiveType.LONG -> {
                String.format("%s.writeLong(%s)", valName, name)
            }

            PrimitiveType.STRING -> {
                String.format("%s.writeString(%s)", valName, name)
            }
        }
        return res
    }

    override fun addImport(): List<String> {
        val res: List<String> = when (type) {
            PrimitiveType.INT -> emptyList()
            PrimitiveType.SHORT -> emptyList()
            PrimitiveType.DOUBLE -> emptyList()
            PrimitiveType.BYTE -> emptyList()
            PrimitiveType.BOOL -> {
                listOf(
                    "m_serialization.utils.ByteBufUtils.writeBool"
                )
            }
            PrimitiveType.FLOAT -> emptyList()
            PrimitiveType.LONG -> emptyList()
            PrimitiveType.STRING -> {
                listOf(
                    "m_serialization.utils.ByteBufUtils.writeString"
                )
            }
        }

        return res
    }
}