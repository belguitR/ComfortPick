package com.comfortpick.application.port.out

import java.time.LocalDateTime
import java.util.UUID

interface MatchImportStore {
    fun findExistingMatchIds(riotMatchIds: Collection<String>): Set<String>

    fun findStoredMatchIdByRiotMatchIds(riotMatchIds: Collection<String>): Map<String, UUID>

    fun findMatchIdsWithStoredMatchupForAccount(
        riotAccountId: UUID,
        riotMatchIds: Collection<String>,
    ): Set<String>

    fun saveMatch(command: SaveMatchCommand): UUID

    fun savePlayerMatchup(command: SavePlayerMatchupCommand)
}

data class SaveMatchCommand(
    val riotMatchId: String,
    val region: String,
    val queueId: Int,
    val gameCreation: LocalDateTime,
    val gameDurationSeconds: Int,
    val patch: String,
    val createdAt: LocalDateTime,
)

data class SavePlayerMatchupCommand(
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
    val totalCs: Int,
    val goldEarned: Int,
    val totalDamageToChampions: Int,
    val itemIds: List<Int>,
    val primaryRuneId: Int?,
    val secondaryRuneId: Int?,
    val createdAt: LocalDateTime,
)
