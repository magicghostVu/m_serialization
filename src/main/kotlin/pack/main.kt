package pack

import io.netty.buffer.Unpooled
import m_serialization.annotations.MSerialization
import pack.pClassMSerializer.writeTo

@MSerialization
sealed class pClass(open var g: Int) {
    abstract var t: Short

}

@MSerialization
data class aSingle(val x: Int, val y: String, override var t: Short, override var g: Int) : pClass(g) {

}

@MSerialization
class bSingle(val z: Short, override var t: Short, override var g: Int) : pClass(g) {

}

fun main() {
    println("${String::class.qualifiedName}")

    val c = mutableListOf<Int>()

    println("class is ${MutableList::class.java}")

    val cc: pClass = aSingle(1, "hello", 6, 7);
    val buffer = Unpooled.buffer();
    cc.writeTo(cc, buffer)
    val byte = ByteArray(buffer.readableBytes());
    buffer.readBytes(byte);
    buffer.resetReaderIndex()

    val pp = pClassMSerializer.readFrom(buffer)
    println("pp is $pp")
    print(byte.contentToString());

}

fun ccc.A() {

}

enum class ccc {

}