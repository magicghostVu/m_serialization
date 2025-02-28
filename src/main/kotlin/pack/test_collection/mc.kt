package pack.test_collection

import io.netty.buffer.Unpooled
import m_serialization.annotations.MSerialization
import pack.test_collection.MCCCMSerializer.writeTo

fun main() {
    val map = mutableMapOf<Int,O>()
    map[0]= O("phuvh")
    val ccc = MCCC(map.values)
    val b = Unpooled.buffer()
    ccc.writeTo(b)
    val cc2 = MCCCMSerializer.readFrom(b)
}

@MSerialization
class O(val s:String)

@MSerialization
class MCCC(val cc: Collection<O>)