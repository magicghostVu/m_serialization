package basicPacket

import m_serialization.annotations.MSerialization

@MSerialization
class LoginCmd(val ssk: String, val deviceId: String, val platform: Int, val faceBookID: String, val version: Int) {}

@MSerialization
class CheatCollectible(val cards:List<Int>) {}

@MSerialization
class LoginMsg(
    val uId: Int,
    val timestamp: Long,
    val timestampNextDay: Long,
    val social: String,
    val userName: String
) {}