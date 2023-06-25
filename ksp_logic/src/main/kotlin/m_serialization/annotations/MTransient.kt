package m_serialization.annotations


//make a property not be contained in serialize/deserialize process
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class MTransient()
