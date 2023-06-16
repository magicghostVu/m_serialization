package pack.protocols

import m_serialization.annotations.MSerialization

@MSerialization
sealed interface MyInterface {
    val s: Student
}

@MSerialization
class K(val f: Float) : MyInterface {
    override val s: Student
        get() = KK("", "name", 17)
}

@MSerialization
class OO(override val s: Student) : MyInterface