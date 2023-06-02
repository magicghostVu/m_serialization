package m_serialization.data

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument

sealed class MPropsData(val name: String) {
    abstract fun isValid(): Boolean
}


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

class PrimitivePropsData(
    name: String,
    val primitiveType: PrimitiveType
) : MPropsData(name) {
    override fun isValid(): Boolean {
        return true
    }
}

// các prop không có generic
class ClassBasedPropData(name: String, val type: KSType) : MPropsData(name) {

    // hợp lệ nếu như
    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }
}

class GenericPropData(
    name: String,
    val type: KSType,
    val allKSTypeArgument: List<KSTypeArgument>
) : MPropsData(name) {
    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }
}