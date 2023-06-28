package pack.debug_protocols

import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient

@MSerialization
class MyData(val a: Int, val b: Int, val listInt: List<Long>, val listC: List<C>) {

    lateinit var hello: C


    @MTransient
    lateinit var v: Thread
}

@MSerialization
class C(val k: Int, val b: B)

@MSerialization
sealed class B(open val c: Int)

@MSerialization
class B1(override val c: Int) : B(c)

@MSerialization
class B2(override val c: Int, val g: Long) : B(c)

@MSerialization
sealed class B3(override val c: Int) : B(c)

@MSerialization
class B4(override val c: Int) : B3(c)