package pack.test_enum

import m_serialization.annotations.MSerialization


fun main() {
    val c = MyEnum.E2
//    val id = MyEnumMSerializer.toId(c)
//    println("id is $id")
}


@MSerialization
class EnumContain(val e: MyEnum) {
    var y: List<MyEnum> = emptyList()
}

@MSerialization
class MListEnum(val list: List<MyEnum>) {
    var ll = mutableListOf<EnumContain>()
    var kk = mutableListOf<EnumContain>()
    var map = mutableMapOf<Int, EnumContain>()
}

@MSerialization
enum class MyEnum {
    E1,
    E2,
    E3;
}

