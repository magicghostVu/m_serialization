package pack.debug_protocols

import m_serialization.annotations.MSerialization

@MSerialization
class MyData(val a:Int, val b:Int, val listInt:List<Long>, val listC:List<C>) {
}

@MSerialization
class C(val k:Int)