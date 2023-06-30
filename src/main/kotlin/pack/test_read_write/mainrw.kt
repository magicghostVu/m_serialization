package pack.test_read_write

import io.netty.buffer.Unpooled
import pack.debug_protocols.B1
import pack.debug_protocols.C
import pack.debug_protocols.MyData
import pack.other_pack.MyD
import pack.other_pack.MyDMSerializer
import pack.other_pack.MyDMSerializer.writeTo

fun main() {

    val b = B1(3)
    val c = C(1, b, listOf(b, b, b))
    val myData = MyData(1, 2, listOf(1, 2, 3), listOf(c, c, c))
    myData.hello = c

    val myD = MyD(myData, "phuvh")

    val buffer  = Unpooled.buffer()
    myD.writeTo(buffer)

    println("after write data size is ${buffer.readableBytes()}")

    val myD2 = MyDMSerializer.readFrom(buffer)

    println("after read ${buffer.readableBytes()}")

}