package com.comfortpick.application.port.out

import java.time.LocalDateTime
import java.util.UUID

interface ProfileDashboardQuery {
    fun findProfileDashboardSnapshot(summonerId: UUID): ProfileDashboardSnapshot
}

data class ProfileDashboardSnapshot(
    val analyzedMatches: Int,
    val mainRole: String?,
    val mostPlayedChampions: List<ChampionPlayCount>,
    val bestCounters: List<StoredProfileCounter>,
    val worstMatchups: List<StoredProfileCounter>,
    val lastUpdateAt: LocalDateTime?,
)

data class ChampionPlayCount(
    val championId: Int,
    val games: Int,
)

data class StoredProfileCounter(
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String,
    val games: Int,
    val winrate: Double,
    val personalScore: Double,
)
