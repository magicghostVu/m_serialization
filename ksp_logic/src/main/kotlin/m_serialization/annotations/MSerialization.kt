package m_serialization.annotations

// đánh dấu một class là có thể được serialize/de-serialize
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MSerialization()
