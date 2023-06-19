package m_serialization.utils

import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets

object ByteBufUtils {
    fun ByteBuf.writeString(str: String) {
        val array = str.toByteArray(StandardCharsets.UTF_8)
        val sizeShort = array.size.toUShort()
        if (sizeShort > UShort.MAX_VALUE) {
            throw IllegalArgumentException("size of string too big")
        }
        writeShort(sizeShort.toInt())
        writeBytes(array)
    }

    fun ByteBuf.readString(): String {
        val arraySize = readUnsignedShort();
        val array = ByteArray(arraySize)
        readBytes(array)
        return String(array, StandardCharsets.UTF_8)
    }

    fun ByteBuf.writeBool(boolean: Boolean) {
        val bb: Byte = if (boolean) 1 else 0
        writeByte(bb.toInt())
    }
}