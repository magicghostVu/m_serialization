package pack.test_enum

import m_serialization.annotations.MSerialization


fun main() {
    val c = MyEnum.E2
    val id = MyEnumMSerializer.toId(c)
    println("id is $id")
}

@MSerialization
enum class MyEnum {
    E1,
    E2,
    E3;
}

