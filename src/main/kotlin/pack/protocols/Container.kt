package pack.protocols

import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient
import kotlin.concurrent.thread

@MSerialization
class Container(
    val s: Student,
    val o: Int,
    @MTransient()
    var k: Thread = thread { },
    @MTransient
    var l: Thread = thread { },
) {
    @MTransient
    var cc: Thread = thread { }
}

// code gen prototype
object ContainerMSerializer {
    private val uniqueTag = 1;
}