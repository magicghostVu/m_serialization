package pack.protocols

import io.netty.buffer.ByteBuf
import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient
import m_serialization.utils.ByteBufUtils.readString
import m_serialization.utils.ByteBufUtils.writeString


@MSerialization
sealed class Student() {
    abstract val age: Int
    abstract val name: String
}

@MSerialization
class V(
    val k: String,
    override val age: Int,
    override val name: String
) : Student() {
    var p = 0;


    @MTransient("mutableMapOf()")
    var mapp: MutableMap<String, Int> = mutableMapOf()

    val pp: Int
        get() = p
}

// code gen prototype
object VMSerializer {

    private val uniqueTag: Short = 0;

    fun V.writeTo(buffer: ByteBuf) {
        buffer.writeString(k)
        buffer.writeInt(age)
        buffer.writeString(name)
        buffer.writeInt(p)
    }

    // nếu class này có class cha thì mới sinh ra function này
    fun V.writeToWithTag(buffer: ByteBuf) {
        buffer.writeShort(uniqueTag.toInt())
        writeTo(buffer)
    }

    fun read(buffer: ByteBuf): V {
        val k = buffer.readString()
        val age = buffer.readInt()
        val name = buffer.readString()
        val p = buffer.readInt()
        val v = V(k, age, name)
        v.p = p
        return v
    }

    // nếu class này có class cha thì mới sinh ra function này
    fun checkTagRead(buffer: ByteBuf): V {
        val tag = buffer.readShort()
        if (tag != uniqueTag) {
            throw IllegalArgumentException("tag not match, expected $uniqueTag, actual $tag")
        }
        return read(buffer)
    }
}

class KK(
    val oo: String,
    override val name: String,
    override val age: Int
) : Student()


// code gen prototype
object KKMSerializer {

    private val uniqueTag: Short = 1;

    fun KK.writeTo(buffer: ByteBuf) {
        buffer.writeString(oo)
        buffer.writeString(name)
        buffer.writeInt(age)
    }

    fun KK.writeToWithTag(buffer: ByteBuf) {
        buffer.writeShort(uniqueTag.toInt())
        writeTo(buffer)
    }
}

