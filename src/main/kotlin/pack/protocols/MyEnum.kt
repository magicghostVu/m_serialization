package pack.protocols



enum class MyEnum {
    E1,
    E2;


    // code gen
    companion object {
        private val map = MyEnum.values()
            .asSequence()
            .associateBy { it.ordinal }

        fun fromCode(ordinal: Int): MyEnum {
            return map.getValue(ordinal)
        }
    }
}