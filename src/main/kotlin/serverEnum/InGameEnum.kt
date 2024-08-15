package serverEnum

import m_serialization.annotations.MSerialization

@MSerialization
enum class GameType {
    NORMAL,
    GAME_WITH_SUGGESTION
}

@MSerialization
enum class GameHintActionType {
    PickResCard,
    PickScoreCard,
    CollectUsedResCards,
    UseProduceResCard,
    UseUpgradeResCard,
    UseExchangeResCard,
    Returnores;
}

@MSerialization
enum class GameResult {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    FIFTH
}

@MSerialization
enum class EndGameType {
    NORMAL,
    ONE_PLAYER_REMAIN
}

@MSerialization
enum class GameState {
    IDLE,
    PREPARE,
    PLAYING,
    REMOVE_ORE,
    RESULT
}

@MSerialization
enum class PlayerState {
    WAIT_RE_GAME,
    IDLE,
    READY,
    PLAYING,
    KICKED
}

@MSerialization
enum class OreLevel {
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4
}

@MSerialization
enum class CoinType {
    GOLD,
    SILVER;
}

@MSerialization
enum class LeaveRoomReason {
    USER_QUIT,
    NOT_MATCH_CONFIG,
    AFK,
    TIMEOUT_REPLAY
}