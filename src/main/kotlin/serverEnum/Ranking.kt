package serverEnum

import m_serialization.annotations.MSerialization

@MSerialization
enum class RankingType() {
    ACTIVITY_RANKING,
    ELO_RANKING;
}
