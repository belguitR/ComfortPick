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
    val latestGames: List<ProfileRecentGameSnapshot>,
    val lastUpdateAt: LocalDateTime?,
    val sync: ProfileSyncSnapshot,
)

data class ChampionPlayCount(
    val championId: Int,
    val games: Int,
)

data class ProfileRecentGameSnapshot(
    val riotMatchId: String,
    val userChampionId: Int,
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
    val summonerSpell1Id: Int?,
    val summonerSpell2Id: Int?,
    val gameCreation: LocalDateTime,
)

data class ProfileSyncSnapshot(
    val enabled: Boolean,
    val status: String,
    val targetMatchCount: Int,
    val backfillCursor: Int,
    val nextRunAt: LocalDateTime?,
    val lastSyncAt: LocalDateTime?,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
)
