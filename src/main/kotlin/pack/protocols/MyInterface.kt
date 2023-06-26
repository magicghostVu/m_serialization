package pack.protocols


sealed interface MyInterface {
    val s: Student
}


class K(val f: Float) : MyInterface {
    override val s: Student
        get() = KK("", "name", 17)
}


class OO(override val s: Student) : MyInterface