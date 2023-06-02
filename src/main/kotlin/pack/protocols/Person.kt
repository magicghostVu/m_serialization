package pack.protocols

import m_serialization.annotations.MSerialization

@MSerialization
class Person(val id: Long, val height: Float) {
}