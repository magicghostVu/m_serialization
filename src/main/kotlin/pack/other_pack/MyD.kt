package pack.other_pack

import io.netty.buffer.Unpooled
import m_serialization.annotations.MSerialization
import pack.debug_protocols.MyData
import pack.other_pack.TestEnum3MSerializer.writeTo

@MSerialization
class MyD(val c: MyData, val s: String, var bytes: ByteArray) {
    var g: Int = 10
}

@MSerialization
enum class MyE {
    E1,
    E2
}


@MSerialization
class TestEnum(val l: List<MyE>, val map: Map<MyE, MyE>)

@MSerialization
class TestEnum2(val l: List<MyE>, val map: Map<Int, MyE>)

@MSerialization
class TestEnum3(val l: List<MyE>) {
    var map: Map<MyE, TestEnum> = emptyMap()
}

fun main() {
    val buffer = Unpooled.buffer()
    val t = TestEnum3(listOf(MyE.E1))
    t.writeTo(buffer)
}