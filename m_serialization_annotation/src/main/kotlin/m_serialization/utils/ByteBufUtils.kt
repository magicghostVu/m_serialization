package m_serialization.utils

import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets

// maybe add some technique from zfoo to compact write for int,short, long
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
        val arraySize = readUnsignedShort()
        val array = ByteArray(arraySize)
        readBytes(array)
        return String(array, StandardCharsets.UTF_8)
    }

    fun ByteBuf.writeBool(boolean: Boolean) {
        val bb: Byte = if (boolean) 1 else 0
        writeByte(bb.toInt())
    }

    fun ByteBuf.readBool(): Boolean {
        val b = readByte()
        return b == 1.toByte()
    }

    fun ByteBuf.writeByteArray(byteArray: ByteArray) {
        val size = byteArray.size
        if (size > UShort.MAX_VALUE.toInt()) {
            throw IllegalArgumentException("size of byte array is to big, max value is ${UShort.MAX_VALUE}")
        }
        writeShort(size)
        writeBytes(byteArray)
    }

    fun ByteBuf.readByteArray(): ByteArray {
        val size = readUnsignedShort();
        if (size > UShort.MAX_VALUE.toInt()) {
            throw IllegalArgumentException("impossible, review serialize logic")
        }
        val res = ByteArray(size)
        readBytes(res)
        return res;
    }
}