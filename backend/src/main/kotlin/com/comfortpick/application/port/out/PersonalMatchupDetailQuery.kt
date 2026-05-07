package com.comfortpick.application.port.out

import com.comfortpick.domain.model.ConfidenceLevel
import java.time.LocalDateTime
import java.util.UUID

interface PersonalMatchupDetailQuery {
    fun findMatchupDetail(
        summonerId: UUID,
        enemyChampionId: Int,
        userChampionId: Int,
    ): StoredPersonalMatchupDetail?
}

data class StoredPersonalMatchupDetail(
    val summonerId: UUID,
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winrate: Double,
    val averageKda: Double,
    val averageCs: Double?,
    val averageGold: Double?,
    val averageDamage: Double?,
    val personalScore: Double,
    val confidence: ConfidenceLevel,
    val updatedAt: LocalDateTime,
    val recentGames: List<StoredRecentMatchupGame>,
)

data class StoredRecentMatchupGame(
    val riotMatchId: String,
    val gameCreation: LocalDateTime,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val totalCs: Int?,
    val goldEarned: Int?,
    val totalDamageToChampions: Int?,
)
