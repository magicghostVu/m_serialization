package m_serialization.annotations

// mark a class can be serialize/de-serialize
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MSerialization(val name: String = "")
