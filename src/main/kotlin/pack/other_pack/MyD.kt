package pack.other_pack

import m_serialization.annotations.MSerialization
import pack.debug_protocols.MyData

@MSerialization
class MyD(val c: MyData, val s: String, var bytes: ByteArray) {
    var g: Int = 10
}