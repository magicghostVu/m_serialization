package pack.protocols


import java.util.*

class AClass(
    val allStudent: MutableList<Student>,
    val a2: LinkedList<Student?>,
    val a3: List<Student>,
    val a4: Map<Int, Student>,
    val a5: TreeMap<Int, Student>,
    val className: String,
    val a6: List<Thread> = emptyList()
) {

}
