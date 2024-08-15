package userPacket


import m_serialization.annotations.MSerialization
import serverEnum.GameResult
import serverEnum.TutorialStep


@MSerialization
class SendMetricCmd(val log: String, val params: List<String>) {}

@MSerialization
sealed class PlayerInfo(
) {
    abstract val uId: Int
    abstract val userName: String
    abstract val displayName: String
    abstract val elo: Int
    abstract val activityScore: Long
    abstract val matches: Map<GameResult, Int>
}

@MSerialization
class MyPlayerInfoMsg(
    override val uId: Int,
    override val userName: String,
    override val displayName: String,
    val avatar: String,
    val tutorialStep: TutorialStep,
    val crrTime: Long,
    override val elo: Int,
    override val activityScore: Long,
    override val matches: Map<GameResult, Int>,
    val specialCard:List<Int>
) : PlayerInfo() {}

@MSerialization
class OtherPlayerInfoMsg(
    override val uId: Int,
    override val userName: String,
    override val displayName: String,
    val exp: Long,
    override val activityScore: Long,
    val allTimeActivityScore: Long,
    override val elo: Int,
    val allTimeElo: Int,
    override val matches: Map<GameResult, Int>
) : PlayerInfo() {}

@MSerialization
class SetTutorialStepCmd(val step: TutorialStep) {};
