package pack.test_read_write

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import m_serialization.annotations.MSerialization
import pack.test_read_write.CClassMSerializer.writeTo


@MSerialization
sealed class AClass(open val a:Int)
@MSerialization
class BClass(override val a:Int):AClass(a)

@MSerialization
class CClass(val v1:List<AClass>, val v2:List<Int> , val v3:Map<String, BClass>, val v4: Double)
fun main() {
    var c = CClass(listOf(BClass(1), BClass(2)), listOf(3,4,5), mapOf(Pair("a", BClass(6)), Pair("b",BClass(7))), 3.456)
    var bf = PooledByteBufAllocator.DEFAULT.buffer();
    c.writeTo(bf)
    var outPut = ByteArray(bf.readableBytes())
    bf.readBytes(outPut);
    print(outPut.joinToString());
}