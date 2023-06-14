package pack.protocols

import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient
import kotlin.concurrent.thread

@MSerialization
sealed class Container(
    open val s: Student,
    open val o: Int,
    @MTransient()
    var k: Thread = thread { },
    @MTransient
    var l: Thread = thread { },
) {
    @MTransient
    var cc: Thread = thread { }
}

@MSerialization
class CC2(override val s: Student, override val o: Int) : Container(s, o)

// code gen prototype
object ContainerMSerializer {
    private val uniqueTag = 1;
}