package pack

import io.netty.buffer.Unpooled
import m_serialization.annotations.MSerialization

@MSerialization
sealed class pClass() {
    abstract var t: Short
    abstract var g: Int

}

@MSerialization
data class aSingle(val x: Int, val y: String, override var t: Short, override var g: Int) : pClass() {

}

@MSerialization
class bSingle(val z: Short, override var t: Short, override var g: Int) : pClass() {

}

@MSerialization
enum class E {
    E1
}

@MSerialization
class P(val c: Map<E, pClass>)

fun main() {
    println("${String::class.qualifiedName}")

    val c = mutableListOf<Int>()

    println("class is ${MutableList::class.java}")

    val cc = aSingle(1, "hello", 6, 7);
    val buffer = Unpooled.buffer();

    with(pClassMSerializer) {
        cc.writeTo(buffer)
    }


    val byte = ByteArray(buffer.readableBytes());
    buffer.readBytes(byte);
    buffer.resetReaderIndex()

    val pp = pClassMSerializer.readFrom(buffer)
    println("pp is $pp, asset ${cc == pp}")
    print(byte.contentToString());

}