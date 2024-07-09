package pp

import io.netty.buffer.Unpooled
import m_serialization.annotations.GenCodeConf
import m_serialization.annotations.MSerialization
import m_serialization.annotations.TSTypeCodeGen
import pp.EnumMapMSerializer.serializeSize
import pp.EnumMapMSerializer.writeTo
import pp.FFMSerializer.serializeSize
import pp.FFMSerializer.writeTo

fun main() {
    val ff = FF(listOf("phuvh"), emptyList(), emptyList())
    val buffer = Unpooled.buffer()
    ff.writeTo(buffer)
    val gg = ff.serializeSize()
    println("serialize size is $gg, written size is ${buffer.writerIndex()}")

    val enumMap = EnumMap(
        mapOf(EEE.E1 to EEE.E1),
        0,
        mapOf(EEE.E1 to 10),
        mapOf(EEE.E1 to C1(9)),
        mapOf(1 to EEE.E1, 2 to EEE.E1),
    )
    buffer.resetWriterIndex()
    enumMap.writeTo(buffer)
    println("data written is ${buffer.writerIndex()}, data calculated is ${enumMap.serializeSize()}")
}

@MSerialization
@GenCodeConf("test", false, TSTypeCodeGen.NAME_SPACE)
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