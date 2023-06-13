package pack.protocols

import m_serialization.annotations.MSerialization
import java.util.LinkedList
import java.util.TreeMap

@MSerialization
class AClass(
    val allStudent: MutableList<Student>,
    val a2: LinkedList<Student>,
    val a3: List<Student>,
    val a4: Map<Int, Student>,
    val a5 :TreeMap<Int,Student>,
    val className: String
) {

}


// code gen
object AClassSerializer {

}