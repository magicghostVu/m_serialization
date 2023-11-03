package pack.test_collection

import io.netty.buffer.Unpooled
import m_serialization.annotations.MSerialization
import pack.test_collection.MCCCMSerializer.writeTo

fun main() {
    val c = listOf<Int>(111)
    val ccc = MCCC(c)
    val b = Unpooled.buffer()
    ccc.writeTo(b)
    val cc2 = MCCCMSerializer.readFrom(b)
}

@MSerialization
class MCCC(val cc: Collection<Int>)