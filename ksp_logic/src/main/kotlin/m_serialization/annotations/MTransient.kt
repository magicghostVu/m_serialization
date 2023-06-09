package m_serialization.annotations


@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
// defaultValueExpression will be used in gen code phase
// import will be pasted in import
annotation class MTransient(val defaultValueExpression: String, val import: String = "")
