package pack.protocols

import m_serialization.annotations.MSerialization


@MSerialization
sealed class Student(val name: String, val age: Int) {
}

@MSerialization
class V(val k: String) : Student("s", 4) {
    val p = 0;

    val pp: Int
        get() = p

    companion object {
    }
}