package pack

import io.netty.buffer.Unpooled
import m_serialization.annotations.MSerialization
import pack.MM1MSerializer.writeTo


fun main() {
    val a1: MyO? = null;
    val a2 = MyO2(1)
    val c: MM1 = MM1(1, a1, a2)
    val buffer = Unpooled.buffer()
    c.g = 10000f;
    c.writeTo(buffer)
    val deserializationObject = MM1MSerializer.readFrom(buffer)
    println()
}

@MSerialization
class MyO(val k: Int)

@MSerialization
class MM1(val c: Int, val c1: MyO?, val c2: MyO2) {
    var g: Float = 0.0f;
}

@MSerialization
class MyO2(val f: Int) {

}