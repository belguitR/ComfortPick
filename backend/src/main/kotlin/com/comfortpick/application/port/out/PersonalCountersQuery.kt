package com.comfortpick.application.port.out

import com.comfortpick.domain.model.ConfidenceLevel
import java.util.UUID

interface PersonalCountersQuery {
    fun findCountersBySummonerIdAndEnemyChampionId(
        summonerId: UUID,
        enemyChampionId: Int,
    ): List<StoredPersonalCounterStat>
}

data class StoredPersonalCounterStat(
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
)
