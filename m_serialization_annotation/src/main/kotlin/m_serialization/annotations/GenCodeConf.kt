package m_serialization.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenCodeConf(
    val sourceGenRootFolder: String = "",
    val genMetadata: Boolean = true,
    val tsTypeCodeGen: TSTypeCodeGen = TSTypeCodeGen.NAME_SPACE
) {
}

enum class TSTypeCodeGen {
    NAME_SPACE,
    MODULE
}