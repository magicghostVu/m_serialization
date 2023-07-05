package pack.test_enum

import m_serialization.annotations.MSerialization
import m_serialization.annotations.TestEnum


fun main() {
    val c = MyEnum.E1
    //val id = MyEnumMSerializer.toId(c)
}

@MSerialization
enum class MyEnum {
    E1,
    E2,
    E3;
}


// code gen prototype
/*
object MyEnumMSerializer {

    private val map: Map<Short, MyEnum> = MyEnum
        .values()
        .asSequence()
        .associateBy { it.ordinal.toShort() }

    fun toId(enum: MyEnum): Short {
        return enum.ordinal.toShort()
    }

    fun fromId(code: Short): MyEnum {
        return map.getValue(code)
    }
}*/
