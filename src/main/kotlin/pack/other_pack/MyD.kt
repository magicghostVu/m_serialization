package pack.other_pack

import m_serialization.annotations.MSerialization
import pack.debug_protocols.MyData

@MSerialization
class MyD(val c: MyData, val s: String, var bytes: ByteArray) {
    var g: Int = 10
}

@MSerialization
enum class MyE {
    E1
}


@MSerialization
class TestEnum(val l: List<MyE>, val map: Map<MyE, MyE>)

@MSerialization
class TestEnum2(val l: List<MyE>, val map: Map<Int, MyE>)

@MSerialization
class TestEnum3(val l: List<MyE>) {
    var map: Map<MyE, TestEnum> = emptyMap()
}