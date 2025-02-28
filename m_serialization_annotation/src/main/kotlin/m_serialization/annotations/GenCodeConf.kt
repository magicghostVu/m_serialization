package m_serialization.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenCodeConf(val sourceGenRootFolder: String = "", val genMetadata: Boolean = true) {
}