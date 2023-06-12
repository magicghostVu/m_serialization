package pack.protocols

import m_serialization.annotations.MSerialization

@MSerialization
class AClass(val allStudent: MutableList<Student>, val className: String) {

}


// code gen
object AClassSerializer {

}