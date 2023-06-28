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
class C(val k: Int)