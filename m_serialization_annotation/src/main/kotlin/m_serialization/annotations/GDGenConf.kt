package m_serialization.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GDGenConf(val sourceGenRootFolder: String = "MSerilizer") {
}