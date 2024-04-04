package pp

import m_serialization.annotations.MSerialization
import pp.FFMSerializer.serializeSize

fun main() {
    val ff = FF(emptyList(), emptyList(), emptyList())
    val gg = ff.serializeSize()
    println("serialize size is $gg")
}

@MSerialization
class EnumMap(
    val m: Map<EEE, EEE>,
    val f: Int,
    val mm2: Map<EEE, Int>,
    val mm: Map<EEE, P>,
    val mm3: Map<Int, EEE>,
) {
    var mm4: Map<String, EEE> = emptyMap()
    var mm5: Map<Long, EEE> = emptyMap()
    var mm6: Map<String, Int> = emptyMap()
    var mm7: Map<String, ByteArray> = emptyMap()
}

@MSerialization
class TestMap(val c: Map<EEE, String>, val c2: Map<Int, C1>)

@MSerialization
sealed class P

@MSerialization
class C1(val g: Long) : P()

@MSerialization
class C2(val f: Float) : P()


@MSerialization
class FF(
    val listR: List<String>,
    val c: List<Int>,
    val ccc: List<ByteArray>,
    val listE: List<EEE> = emptyList(),
    val jj: Int = 0,
    val l: String = "",
    val cv: ByteArray = ByteArray(1),
    val p: EEE = EEE.E1
) {
    var ggh: Double = 1.0
    var fff: MyO = MyO()
    var g: P = C1(6)
}

@MSerialization
enum class EEE {
    E1
}

@MSerialization
class MyO(val f: Int = 0)