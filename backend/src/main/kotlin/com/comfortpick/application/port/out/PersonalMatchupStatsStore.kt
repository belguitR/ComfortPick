package com.comfortpick.application.port.out

import com.comfortpick.domain.model.ConfidenceLevel
import java.time.LocalDateTime
import java.util.UUID

interface PersonalMatchupStatsStore {
    fun findPlayerMatchupsByRiotAccountId(riotAccountId: UUID): List<StoredPlayerMatchup>

    fun findExistingPersonalMatchupStatsByRiotAccountId(riotAccountId: UUID): List<StoredPersonalMatchupStat>

    fun savePersonalMatchupStats(commands: Collection<UpsertPersonalMatchupStatCommand>)

    fun deletePersonalMatchupStats(ids: Collection<UUID>)
}

data class StoredPlayerMatchup(
    val riotAccountId: UUID,
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
    val createdAt: LocalDateTime,
)

data class StoredPersonalMatchupStat(
    val id: UUID,
    val riotAccountId: UUID,
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String,
)

data class UpsertPersonalMatchupStatCommand(
    val id: UUID,
    val riotAccountId: UUID,
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
)
