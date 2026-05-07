package com.comfortpick.api.profile

import com.comfortpick.application.usecase.GetProfileDashboardCommand
import com.comfortpick.application.usecase.GetProfileDashboardUseCase
import com.comfortpick.application.usecase.GetEnemyChampionCountersCommand
import com.comfortpick.application.usecase.GetEnemyChampionCountersUseCase
import com.comfortpick.application.usecase.GetPersonalMatchupDetailCommand
import com.comfortpick.application.usecase.GetPersonalMatchupDetailUseCase
import com.comfortpick.application.usecase.RequestSummonerHistorySyncCommand
import com.comfortpick.application.usecase.RequestSummonerHistorySyncUseCase
import com.comfortpick.infrastructure.config.HistorySyncProperties
import com.comfortpick.domain.model.ConfidenceLevel
import com.comfortpick.domain.model.RecommendationStatus
import java.time.LocalDateTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/profiles")
class ProfileController(
    private val getProfileDashboardUseCase: GetProfileDashboardUseCase,
    private val getEnemyChampionCountersUseCase: GetEnemyChampionCountersUseCase,
    private val getPersonalMatchupDetailUseCase: GetPersonalMatchupDetailUseCase,
    private val requestSummonerHistorySyncUseCase: RequestSummonerHistorySyncUseCase,
    private val historySyncProperties: HistorySyncProperties,
) {
    @GetMapping("/{summonerId}")
    fun getProfileDashboard(
        @PathVariable summonerId: UUID,
    ): ProfileDashboardResponse {
        val result = getProfileDashboardUseCase.execute(
            GetProfileDashboardCommand(
                summonerId = summonerId,
            ),
        )

        return ProfileDashboardResponse(
            summoner = ProfileDashboardSummonerResponse(
                id = result.summoner.id,
                gameName = result.summoner.gameName,
                tagLine = result.summoner.tagLine,
                region = result.summoner.region,
            ),
            analyzedMatches = result.analyzedMatches,
            mainRole = result.mainRole,
            mostPlayedChampions = result.mostPlayedChampions.map {
                ProfileChampionPlayCountResponse(
                    championId = it.championId,
                    games = it.games,
                )
            },
            bestCounters = result.bestCounters.map {
                ProfileCounterSummaryResponse(
                    enemyChampionId = it.enemyChampionId,
                    userChampionId = it.userChampionId,
                    role = it.role,
                    games = it.games,
                    winrate = it.winrate,
                    personalScore = it.personalScore,
                )
            },
            worstMatchups = result.worstMatchups.map {
                ProfileCounterSummaryResponse(
                    enemyChampionId = it.enemyChampionId,
                    userChampionId = it.userChampionId,
                    role = it.role,
                    games = it.games,
                    winrate = it.winrate,
                    personalScore = it.personalScore,
                )
            },
            lastUpdateAt = result.lastUpdateAt,
            sync = ProfileSyncResponse(
                enabled = result.sync.enabled,
                status = result.sync.status,
                targetMatchCount = result.sync.targetMatchCount,
                backfillCursor = result.sync.backfillCursor,
                remainingMatchCount = result.sync.remainingMatchCount,
                nextRunAt = result.sync.nextRunAt,
                lastSyncAt = result.sync.lastSyncAt,
                lastErrorCode = result.sync.lastErrorCode,
                lastErrorMessage = result.sync.lastErrorMessage,
                dashboardPollIntervalSeconds = historySyncProperties.dashboardPollInterval.seconds,
            ),
        )
    }

    @PostMapping("/{summonerId}/sync")
    fun requestProfileSync(
        @PathVariable summonerId: UUID,
    ): ProfileSyncRequestResponse {
        val result = requestSummonerHistorySyncUseCase.execute(
            RequestSummonerHistorySyncCommand(
                summonerId = summonerId,
                targetMatchCount = historySyncProperties.targetMatches,
            ),
        )

        return ProfileSyncRequestResponse(
            summonerId = result.summonerId,
            status = result.syncStatus,
            targetMatchCount = result.targetMatchCount,
            backfillCursor = result.backfillCursor,
            nextRunAt = result.nextRunAt,
        )
    }

    @GetMapping("/{summonerId}/enemies/{enemyChampionId}/counters")
    fun getEnemyChampionCounters(
        @PathVariable summonerId: UUID,
        @PathVariable enemyChampionId: Int,
    ): PersonalCountersResponse {
        val result = getEnemyChampionCountersUseCase.execute(
            GetEnemyChampionCountersCommand(
                summonerId = summonerId,
                enemyChampionId = enemyChampionId,
            ),
        )

        return PersonalCountersResponse(
            counters = result.counters.map { counter ->
                PersonalCounterResponse(
                    enemyChampionId = counter.enemyChampionId,
                    userChampionId = counter.userChampionId,
                    role = counter.role,
                    games = counter.games,
                    wins = counter.wins,
                    losses = counter.losses,
                    winrate = counter.winrate,
                    averageKda = counter.averageKda,
                    averageCs = counter.averageCs,
                    averageGold = counter.averageGold,
                    averageDamage = counter.averageDamage,
                    personalScore = counter.personalScore,
                    confidence = counter.confidence,
                    status = counter.status,
                )
            },
        )
    }

    @GetMapping("/{summonerId}/enemies/{enemyChampionId}/counters/{userChampionId}")
    fun getPersonalMatchupDetail(
        @PathVariable summonerId: UUID,
        @PathVariable enemyChampionId: Int,
        @PathVariable userChampionId: Int,
    ): PersonalMatchupDetailResponse {
        val result = getPersonalMatchupDetailUseCase.execute(
            GetPersonalMatchupDetailCommand(
                summonerId = summonerId,
                enemyChampionId = enemyChampionId,
                userChampionId = userChampionId,
            ),
        )

        return PersonalMatchupDetailResponse(
            hasData = result.hasData,
            enemyChampionId = result.enemyChampionId,
            userChampionId = result.userChampionId,
            role = result.role,
            games = result.games,
            wins = result.wins,
            losses = result.losses,
            winrate = result.winrate,
            averageKda = result.averageKda,
            averageCs = result.averageCs,
            averageGold = result.averageGold,
            averageDamage = result.averageDamage,
            personalScore = result.personalScore,
            confidence = result.confidence,
            status = result.status,
            reasoning = result.reasoning,
            lastUpdatedAt = result.lastUpdatedAt,
            recentGames = result.recentGames.map {
                PersonalMatchupRecentGameResponse(
                    riotMatchId = it.riotMatchId,
                    gameCreation = it.gameCreation,
                    win = it.win,
                    kills = it.kills,
                    deaths = it.deaths,
                    assists = it.assists,
                    totalCs = it.totalCs,
                    goldEarned = it.goldEarned,
                    totalDamageToChampions = it.totalDamageToChampions,
                )
            },
            build = PersonalBuildRecommendationResponse(
                firstCompletedItemId = result.build.firstCompletedItemId,
                firstCompletedItemGames = result.build.firstCompletedItemGames,
                itemSet = result.build.itemSet,
                itemSetGames = result.build.itemSetGames,
                score = result.build.score,
            ),
            runes = PersonalRuneRecommendationResponse(
                primaryRuneId = result.runes.primaryRuneId,
                primaryRuneGames = result.runes.primaryRuneGames,
                secondaryRuneId = result.runes.secondaryRuneId,
                secondaryRuneGames = result.runes.secondaryRuneGames,
                score = result.runes.score,
            ),
        )
    }
}

data class PersonalCountersResponse(
    val counters: List<PersonalCounterResponse>,
)

data class PersonalCounterResponse(
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
    val status: RecommendationStatus,
)

data class ProfileDashboardResponse(
    val summoner: ProfileDashboardSummonerResponse,
    val analyzedMatches: Int,
    val mainRole: String?,
    val mostPlayedChampions: List<ProfileChampionPlayCountResponse>,
    val bestCounters: List<ProfileCounterSummaryResponse>,
    val worstMatchups: List<ProfileCounterSummaryResponse>,
    val lastUpdateAt: java.time.LocalDateTime?,
    val sync: ProfileSyncResponse,
)

data class ProfileDashboardSummonerResponse(
    val id: UUID,
    val gameName: String,
    val tagLine: String,
    val region: String,
)

data class ProfileChampionPlayCountResponse(
    val championId: Int,
    val games: Int,
)

data class ProfileCounterSummaryResponse(
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String,
    val games: Int,
    val winrate: Double,
    val personalScore: Double,
)

data class ProfileSyncResponse(
    val enabled: Boolean,
    val status: String,
    val targetMatchCount: Int,
    val backfillCursor: Int,
    val remainingMatchCount: Int,
    val nextRunAt: LocalDateTime?,
    val lastSyncAt: LocalDateTime?,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
    val dashboardPollIntervalSeconds: Long,
)

data class ProfileSyncRequestResponse(
    val summonerId: UUID,
    val status: String,
    val targetMatchCount: Int,
    val backfillCursor: Int,
    val nextRunAt: LocalDateTime?,
)

data class PersonalMatchupDetailResponse(
    val hasData: Boolean,
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String?,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winrate: Double,
    val averageKda: Double?,
    val averageCs: Double?,
    val averageGold: Double?,
    val averageDamage: Double?,
    val personalScore: Double?,
    val confidence: ConfidenceLevel,
    val status: RecommendationStatus,
    val reasoning: String,
    val lastUpdatedAt: LocalDateTime?,
    val recentGames: List<PersonalMatchupRecentGameResponse>,
    val build: PersonalBuildRecommendationResponse,
    val runes: PersonalRuneRecommendationResponse,
)

data class PersonalMatchupRecentGameResponse(
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

data class PersonalBuildRecommendationResponse(
    val firstCompletedItemId: Int?,
    val firstCompletedItemGames: Int,
    val itemSet: String?,
    val itemSetGames: Int,
    val score: Double?,
)

data class PersonalRuneRecommendationResponse(
    val primaryRuneId: Int?,
    val primaryRuneGames: Int,
    val secondaryRuneId: Int?,
    val secondaryRuneGames: Int,
    val score: Double?,
)
