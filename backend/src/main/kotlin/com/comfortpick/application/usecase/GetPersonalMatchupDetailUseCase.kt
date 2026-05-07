package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.PersonalMatchupDetailQuery
import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.domain.model.ConfidenceLevel
import com.comfortpick.domain.model.RecommendationStatus
import com.comfortpick.domain.service.RecommendationScoringService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class GetPersonalMatchupDetailUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val personalMatchupDetailQuery: PersonalMatchupDetailQuery,
    private val recommendationScoringService: RecommendationScoringService,
) {
    fun execute(command: GetPersonalMatchupDetailCommand): GetPersonalMatchupDetailResult {
        riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)

        val detail = personalMatchupDetailQuery.findMatchupDetail(
            summonerId = command.summonerId,
            enemyChampionId = command.enemyChampionId,
            userChampionId = command.userChampionId,
        ) ?: return GetPersonalMatchupDetailResult(
            hasData = false,
            enemyChampionId = command.enemyChampionId,
            userChampionId = command.userChampionId,
            role = null,
            games = 0,
            wins = 0,
            losses = 0,
            winrate = 0.0,
            averageKda = null,
            averageCs = null,
            averageGold = null,
            averageDamage = null,
            personalScore = null,
            confidence = ConfidenceLevel.NO_DATA,
            status = RecommendationStatus.NO_DATA,
            reasoning = "No personal data yet for this champion matchup.",
            lastUpdatedAt = null,
            recentGames = emptyList(),
        )

        val status = recommendationScoringService.calculateStatus(
            score = detail.personalScore,
            confidence = detail.confidence,
            games = detail.games,
        )

        return GetPersonalMatchupDetailResult(
            hasData = true,
            enemyChampionId = detail.enemyChampionId,
            userChampionId = detail.userChampionId,
            role = detail.role,
            games = detail.games,
            wins = detail.wins,
            losses = detail.losses,
            winrate = detail.winrate,
            averageKda = detail.averageKda,
            averageCs = detail.averageCs,
            averageGold = detail.averageGold,
            averageDamage = detail.averageDamage,
            personalScore = detail.personalScore,
            confidence = detail.confidence,
            status = status,
            reasoning = buildReasoning(
                status = status,
                confidence = detail.confidence,
                games = detail.games,
                winrate = detail.winrate,
                averageKda = detail.averageKda,
            ),
            lastUpdatedAt = detail.updatedAt,
            recentGames = detail.recentGames.map {
                PersonalMatchupRecentGame(
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
        )
    }

    private fun buildReasoning(
        status: RecommendationStatus,
        confidence: ConfidenceLevel,
        games: Int,
        winrate: Double,
        averageKda: Double,
    ): String =
        when (status) {
            RecommendationStatus.BEST_PICK ->
                "Strong personal matchup: $games games, ${format(winrate)}% win rate, ${format(averageKda)} KDA, and ${confidence.name.lowercase()} confidence."
            RecommendationStatus.GOOD_PICK ->
                "Positive personal results: $games games, ${format(winrate)}% win rate, ${format(averageKda)} KDA, and ${confidence.name.lowercase()} confidence."
            RecommendationStatus.OK_PICK ->
                "Playable but not dominant: $games games, ${format(winrate)}% win rate, ${format(averageKda)} KDA, and ${confidence.name.lowercase()} confidence."
            RecommendationStatus.LOW_DATA ->
                "Promising but low-confidence: only $games games, ${format(winrate)}% win rate, and ${format(averageKda)} KDA."
            RecommendationStatus.AVOID ->
                "This matchup has been poor for you: $games games, ${format(winrate)}% win rate, and ${format(averageKda)} KDA."
            RecommendationStatus.NO_DATA ->
                "No personal data yet for this champion matchup."
        }

    private fun format(value: Double): String = "%.1f".format(value)
}

data class GetPersonalMatchupDetailCommand(
    val summonerId: UUID,
    val enemyChampionId: Int,
    val userChampionId: Int,
)

data class GetPersonalMatchupDetailResult(
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
    val recentGames: List<PersonalMatchupRecentGame>,
)

data class PersonalMatchupRecentGame(
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
