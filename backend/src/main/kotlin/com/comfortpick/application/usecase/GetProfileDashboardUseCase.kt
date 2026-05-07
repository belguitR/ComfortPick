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
            bestCounters = snapshot.bestCounters.map {
                DashboardCounterSummary(
                    enemyChampionId = it.enemyChampionId,
                    userChampionId = it.userChampionId,
                    role = it.role,
                    games = it.games,
                    winrate = it.winrate,
                    personalScore = it.personalScore,
                )
            },
            worstMatchups = snapshot.worstMatchups.map {
                DashboardCounterSummary(
                    enemyChampionId = it.enemyChampionId,
                    userChampionId = it.userChampionId,
                    role = it.role,
                    games = it.games,
                    winrate = it.winrate,
                    personalScore = it.personalScore,
                )
            },
            lastUpdateAt = snapshot.lastUpdateAt,
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
    val bestCounters: List<DashboardCounterSummary>,
    val worstMatchups: List<DashboardCounterSummary>,
    val lastUpdateAt: LocalDateTime?,
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

data class DashboardCounterSummary(
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String,
    val games: Int,
    val winrate: Double,
    val personalScore: Double,
)
