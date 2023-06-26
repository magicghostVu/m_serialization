package pack.protocols

import kotlin.concurrent.thread


sealed class Container(
    open val s: Student,
    open val o: Int,




    var k: Thread = thread { },

    var l: Thread = thread { },
) {

    var cc: Thread = thread { }
}


class CC2(override val s: Student, override val o: Int) : Container(s, o)

// code gen prototype
object ContainerMSerializer {
    private val uniqueTag = 1;
}