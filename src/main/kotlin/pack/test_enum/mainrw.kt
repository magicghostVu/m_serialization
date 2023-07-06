package pack.test_enum

import m_serialization.annotations.MSerialization


fun main() {
    val c = MyEnum.E2
    val id = MyEnumMSerializer.toId(c)
    println("id is $id")
}


@MSerialization
class EnumContain(val e: MyEnum)

@MSerialization
class MListEnum(val list: List<MyEnum>)

@MSerialization
enum class MyEnum {
    E1,
    E2,
    E3;
}

