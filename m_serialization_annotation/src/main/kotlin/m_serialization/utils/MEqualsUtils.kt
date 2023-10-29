package m_serialization.utils

object MEqualsUtils {
    fun <T> equalsTwoLists(listA: List<T>, listB: List<T>, compareElement: (a: T, b: T) -> Boolean): Boolean {
        return if (listA.size != listB.size) false
        else {
            val i1 = listA.listIterator()
            val i2 = listB.listIterator()
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
        return if (mapA.size != mapB.size) false
        else {
            val ia = mapA.iterator()
            val ib = mapB.iterator()
            var hadDiff = false
            while (ia.hasNext()) {
                val (ka, va) = ia.next()
                val (kb, vb) = ib.next()
                if (ka != kb || !compareValue(va, vb)) {
                    hadDiff = true
                }
            }
            !hadDiff
        }
    }
}