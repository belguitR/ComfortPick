package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.ProfileDashboardQuery
import com.comfortpick.application.port.out.RiotAccountStore
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class GetProfileDashboardUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val profileDashboardQuery: ProfileDashboardQuery,
) {
    fun execute(command: GetProfileDashboardCommand): GetProfileDashboardResult {
        val account = riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)

        val snapshot = profileDashboardQuery.findProfileDashboardSnapshot(command.summonerId)

        return GetProfileDashboardResult(
            summoner = DashboardSummoner(
                id = account.id,
                gameName = account.gameName,
                tagLine = account.tagLine,
                region = account.region,
            ),
            analyzedMatches = snapshot.analyzedMatches,
            mainRole = snapshot.mainRole,
            mostPlayedChampions = snapshot.mostPlayedChampions.map {
                DashboardChampionPlayCount(
                    championId = it.championId,
                    games = it.games,
                )
            },
            latestGames = snapshot.latestGames.map {
                DashboardRecentGame(
                    riotMatchId = it.riotMatchId,
                    userChampionId = it.userChampionId,
                    role = it.role,
                    win = it.win,
                    kills = it.kills,
                    deaths = it.deaths,
                    assists = it.assists,
                    totalCs = it.totalCs,
                    goldEarned = it.goldEarned,
                    totalDamageToChampions = it.totalDamageToChampions,
                    itemIds = it.itemIds,
                    primaryRuneId = it.primaryRuneId,
                    secondaryRuneId = it.secondaryRuneId,
                    summonerSpell1Id = it.summonerSpell1Id,
                    summonerSpell2Id = it.summonerSpell2Id,
                    gameCreation = it.gameCreation,
                )
            },
            lastUpdateAt = snapshot.lastUpdateAt,
            sync = DashboardSyncState(
                enabled = snapshot.sync.enabled,
                status = snapshot.sync.status,
                targetMatchCount = snapshot.sync.targetMatchCount,
                backfillCursor = snapshot.sync.backfillCursor,
                remainingMatchCount = maxOf(snapshot.sync.targetMatchCount - snapshot.sync.backfillCursor, 0),
                nextRunAt = snapshot.sync.nextRunAt,
                lastSyncAt = snapshot.sync.lastSyncAt,
                lastErrorCode = snapshot.sync.lastErrorCode,
                lastErrorMessage = snapshot.sync.lastErrorMessage,
            ),
        )
    }
}

data class GetProfileDashboardCommand(
    val summonerId: UUID,
)

data class GetProfileDashboardResult(
    val summoner: DashboardSummoner,
    val analyzedMatches: Int,
    val mainRole: String?,
    val mostPlayedChampions: List<DashboardChampionPlayCount>,
    val latestGames: List<DashboardRecentGame>,
    val lastUpdateAt: LocalDateTime?,
    val sync: DashboardSyncState,
)

data class DashboardSummoner(
    val id: UUID,
    val gameName: String,
    val tagLine: String,
    val region: String,
)

data class DashboardChampionPlayCount(
    val championId: Int,
    val games: Int,
)

data class DashboardRecentGame(
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

data class DashboardSyncState(
    val enabled: Boolean,
    val status: String,
    val targetMatchCount: Int,
    val backfillCursor: Int,
    val remainingMatchCount: Int,
    val nextRunAt: LocalDateTime?,
    val lastSyncAt: LocalDateTime?,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
)
