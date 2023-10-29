package pack.test_read_write

import io.netty.buffer.PooledByteBufAllocator
import m_serialization.annotations.MSerialization
import pack.test_read_write.CClassMSerializer.writeTo


@MSerialization
sealed class AClass() {
    abstract val a: Int
}

@MSerialization
class BClass(override val a: Int, val b: Int) : AClass()

@MSerialization
class CClass(val v1: List<AClass>, val v2: List<Int>, val v3: Map<String, BClass>, val v4: Double)

fun main() {
    val c = CClass(
        listOf(BClass(1, 2), BClass(2, 3)),
        listOf(3, 4, 5),
        mapOf(Pair("a", BClass(6, 7)), Pair("b", BClass(7, 8))),
        3.456
    )
    val bf = PooledByteBufAllocator.DEFAULT.buffer();
    c.writeTo(bf)
    val outPut = ByteArray(bf.readableBytes())
    bf.readBytes(outPut);
    print(outPut.joinToString());
}