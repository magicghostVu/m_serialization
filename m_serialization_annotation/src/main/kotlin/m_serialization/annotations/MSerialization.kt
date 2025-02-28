package m_serialization.annotations

// mark a class can be serialized/de-serialized
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MSerialization()
