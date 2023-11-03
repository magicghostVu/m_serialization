package m_serialization.utils

object MEqualsUtils {
    fun <T> equalsTwoCollections(
        collectionA: Collection<T>,
        collectionB: Collection<T>,
        compareElement: (a: T, b: T) -> Boolean
    ): Boolean {
        return if (collectionA.size != collectionB.size) false
        else {
            val i1 = collectionA.iterator()
            val i2 = collectionB.iterator()
            var hadDiff = false
            while (i1.hasNext()) {
                val e1 = i1.next()
                val e2 = i2.next()
                if (!compareElement(e1, e2)) {
                    hadDiff = true
                    break
                }
            }
            !hadDiff
        }
    }

    // vì hiện tại chỉ hỗ trợ key là các primitive nên là đối với key sẽ dùng equals mặc định để so sánh
    fun <K, V> equalsToMap(mapA: Map<K, V>, mapB: Map<K, V>, compareValue: (v1: V, v2: V) -> Boolean): Boolean {
        return equalsTwoCollections(mapA.entries, mapB.entries) { ea, eb ->
            val (ka, va) = ea
            val (kb, vb) = eb
            ka == kb && compareValue(va, vb)
        }
    }
}