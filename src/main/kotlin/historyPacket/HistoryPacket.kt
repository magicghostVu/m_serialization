package historyPacket

import m_serialization.annotations.MSerialization
import serverEnum.GameResult

@MSerialization
class HistoryPlayerInfo(
    val uId: Int,
    val score: Int,
    val uName: String,
    val avatarURL: String
) {}
@MSerialization
class HistoryMatchInfo(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val numPlayer: Int,
    val result: GameResult,
    val players: List<HistoryPlayerInfo>
) {}

@MSerialization
class HistoryMatchGetCmd(val matchId: String) {
}

@MSerialization
class HistoryListMatchMsg(val matches: List<HistoryMatchInfo>) {

}