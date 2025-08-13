package pack.debug_protocols

import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient
import java.util.TreeMap


@MSerialization
sealed class X() {
    abstract val i: Int
}

@MSerialization
class X1(override val i: Int) : X()

@MSerialization
class NormalClass(val a: Int, val b: Int, val c: Int, val list: List<X>, val m: TreeMap<String, X>)

@MSerialization
class MyData(val a: Int, val b: Int, val listInt: List<Long>, val listC: List<C>) {

    lateinit var hello: C


    @MTransient
    lateinit var v: Thread
}


@MSerialization
class C(val k: Int, val b: B, val listB: List<B>) {
    lateinit var c: List<String>
    lateinit var map: Map<String, Int>
}


@MSerialization
sealed class B() {
    abstract val c: Int
}


@MSerialization
class B1(override val c: Int) : B()


@MSerialization
class B2(override val c: Int, val g: Long) : B()

@MSerialization
sealed class B3() : B() {
    abstract val d: Int
}


@MSerialization
class B4(override val c: Int, override val d: Int) : B3()

