package m_serialization.data

import com.google.devtools.ksp.symbol.KSType




enum class PrimitiveType(val className: String) {
    INT(Int::class.qualifiedName!!),
    SHORT(Short::class.qualifiedName!!),
    DOUBLE(Double::class.qualifiedName!!),
    BYTE(Byte::class.qualifiedName!!),
    BOOL(Boolean::class.qualifiedName!!),
    FLOAT(Float::class.qualifiedName!!),
    LONG(Long::class.qualifiedName!!),
    STRING(String::class.qualifiedName!!);

    companion object {
        private val allPrimitiveName = PrimitiveType
            .values()
            .asSequence()
            .map { it.className }
            .toSet()

        fun KSType.isPrimitive(): Boolean {
            return allPrimitiveName.contains(declaration.qualifiedName!!.asString())
        }
    }
}
