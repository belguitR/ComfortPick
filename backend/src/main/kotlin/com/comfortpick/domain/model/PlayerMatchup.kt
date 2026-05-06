package com.comfortpick.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class PlayerMatchup(
    val id: UUID,
    val riotAccountId: UUID,
    val matchId: UUID,
    val userPuuid: String,
    val userChampionId: Int,
    val enemyChampionId: Int,
    val role: String,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val totalCs: Int?,
    val goldEarned: Int?,
    val totalDamageToChampions: Int?,
    val itemIds: List<Int>,
    val primaryRuneId: Int?,
    val secondaryRuneId: Int?,
    val createdAt: LocalDateTime,
) {
    val kda: Double
        get() = (kills + assists).toDouble() / maxOf(1, deaths)
}
