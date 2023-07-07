package pack.test_read_write

import m_serialization.annotations.MSerialization


@MSerialization
sealed class AClass(open val a:Int)
@MSerialization
class BClass(override val a:Int):AClass(a)

@MSerialization
class CClass(val value:List<AClass>, val v2:List<Int> , val V3:Map<String, BClass>)
fun main() {



}