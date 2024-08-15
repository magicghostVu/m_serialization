package gamePacket

import m_serialization.annotations.MSerialization
import serverEnum.*

//========== MSg ==========
@MSerialization
class PlayerInGame(
    val uId: Int,
    val name: String,
    val playerIndex: Int,
    val state: PlayerState,
    val cardInHand: List<Int>,
    val cardPlayed: List<Int>,
    val weaponCard: List<Int>,
    val ores: Map<OreLevel, Int>,
    val coins: Map<CoinType, Int>,
    val numAfkTurn: Int,
    val elo: Int,
    val activityScore: Long
) {};
@MSerialization
class OreInCard(val ores: Map<OreLevel, Int>) {}

@MSerialization
class GameActionHintMsg(
    val actionType: GameHintActionType,
    val playerIndex: Int,
    val cardId: Int,
    val cardIndex: Int,
    val timeUse: Int,
    val ores: List<Int>
) {}

@MSerialization
class GameInfoMsg(
    val tableId: Int,
    val gameId: Int,
    val gameState: GameState,
    val players: List<PlayerInGame>,
    val actionCard: List<Int>,
    val weaponCard: List<Int>,
    val remainCoin: Map<CoinType, Int>,
    val crrIndex: Int,
    val numActionCardInPool: Short,
    val numWeaponCardInPool: Short,
    val timeRemainInMillis: Long,
    val oreInActionCard: List<OreInCard>,
    val maxPlayer: Int,
    val firstPlayerIndex: Int,
    val gameType: GameType
) {}

@MSerialization
class PlayerSpecialCardList(val uId: Int, val cards: List<Int>) {}

@MSerialization
class GameReadyMsg(val uId: Int) {}

@MSerialization
class GameInfoStartMsg(
    val playerIndex: Int,
    val actionCardPoolSize: Int,
    val weaponCardPoolSize: Int,
    val actionCard: List<Int>,
    val weaponCard: List<Int>,
    val coins: Map<CoinType, Int>,
    val specialCards: List<PlayerSpecialCardList>
) {}

@MSerialization
class GameNewUserJoinMsg(
    val player: PlayerInGame
) {}

@MSerialization
class GamePlayOreCardMsg(
    val playerId: Int,
    val cardId: Int,
    val cardRemainInHand: Int,
    val totalOreAfter: Int,
    val numAfkTurn: Int
)


@MSerialization
class GamePlayUpgradeCardMsg(
    val playerId: Int,
    val cardId: Int,
    val oreUpdates: List<OreLevel>,
    val cardRemainInHand: Int,
    val oreAfter: Map<OreLevel, Int>,
    val numAfkTurn: Int
)

@MSerialization
class GamePickCardMsg(
    val playerId: Int,
    val cardIndex: Int,
    val cardId: Int,
    val newCardId: Int,
    val oreUse: List<OreLevel>,
    val cardRemainInHand: Int,
    val totalOreAfter: Int,
    val numAfkTurn: Int
)

@MSerialization
class GamePickWeaponMsg(
    val playerId: Int,
    val cardIndex: Int,
    val cardId: Int,
    val newCardId: Int,
    val totalPoint: Int,
    val numAfkTurn: Int
)

@MSerialization
class GameExchangeOreCardMsg(
    val playerId: Int,
    val cardId: Int,
    val timePlay: Int,
    val cardRemainInHand: Int,
    val totalOreAfter: Int,
    val numAfkTurn: Int
)

@MSerialization
class GameClaimOreRestCardMsg(
    val playerId: Int,
    val cardId: Int,
    val cardRemainInHand: Int,
    val totalOreAfter: Int,
    val numAfkTurn: Int,
    val cardRest: List<Int>
)

@MSerialization
class GameClaimOreUpgradeCardMsg(
    val playerId: Int,
    val cardId: Int,
    val oreUpdates: List<OreLevel>,
    val cardRemainInHand: Int,
    val oreAfter: Map<OreLevel, Int>,
    val numAfkTurn: Int,
)

@MSerialization
class GameUpgradeRestCardCardMsg(
    val playerId: Int,
    val cardId: Int,
    val oreUpdates: List<OreLevel>,
    val cardRemainInHand: Int,
    val oreAfter: Map<OreLevel, Int>,
    val numAfkTurn: Int,
    val restCards: List<Int>
)

@MSerialization
class GameExchangeOptionCardMsg(
    val playerId: Int,
    val cardId: Int,
    val timePlays: List<Int>,
    val cardRemainInHand: Int,
    val totalOreAfter: Int,
    val numAfkTurn: Int
)

@MSerialization
class GameDiscardOreMsg(
    val playerId: Int,
    val oreDiscard: Map<OreLevel, Int>,
    val oreAfter: Map<OreLevel, Int>,
    val numAfkTurn: Int
)

@MSerialization
class GameRestCardMsg(
    val playerId: Int,
    val numCardRes: Int,
    val numCardInHand: Int,
    val numAfkTurn: Int
)

@MSerialization
class GameResultPlayer(
    val uId: Int,
    val score: Int,
    val isQuited: Boolean,
    val eloScore: Int,
    val lastEloScore: Int,
    val actScore: Long,
    val lastActScore: Long
) {};

@MSerialization
class GameResultMsg(
    val result: List<GameResultPlayer>,
    val type: EndGameType
) {}


//======== CMD ========
@MSerialization
class GamePlayOreCardCmd(val cardId: Int) {}

@MSerialization
class GamePlayUpgradeCardCmd(val cardId: Int, val ores: List<OreLevel>) {}

@MSerialization
class GamePlayExchangeCardCmd(val cardId: Int, val time: Int) {}

@MSerialization
class GamePickCardCmd(val cardIndex: Int, val ores: List<OreLevel>) {}

@MSerialization
class GamePickWeaponCmd(val cardIndex: Int) {}

@MSerialization
class GameDiscardOreCmd(val ores: List<Int>) {}

@MSerialization
class GameClaimOreAndUpgradeCmd(val cardId: Int, val ores: List<OreLevel>) {}

@MSerialization
class GameClaimOreAndRestCmd(val cardId: Int, val restCards: List<Int>) {}

@MSerialization
class GameUpgradeOreAndRestCmd(val cardId: Int, val restCards: List<Int>, val ores: List<OreLevel>) {}

@MSerialization
class GameExchangeCardOptionsCmd(val cardId: Int, val timePlay: List<Int>) {}
@MSerialization
class GameCheatShuffleCardCmd(val weaponIds: List<Int>, val actionIds: List<Int>) {}