package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.PersonalCountersQuery
import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.domain.model.RecommendationStatus
import com.comfortpick.domain.service.RecommendationScoringService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetEnemyChampionCountersUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val personalCountersQuery: PersonalCountersQuery,
    private val recommendationScoringService: RecommendationScoringService,
) {
    fun execute(command: GetEnemyChampionCountersCommand): GetEnemyChampionCountersResult {
        riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)

        val counters = personalCountersQuery.findCountersBySummonerIdAndEnemyChampionId(
            summonerId = command.summonerId,
            enemyChampionId = command.enemyChampionId,
        ).map { storedCounter ->
            EnemyChampionCounter(
                enemyChampionId = storedCounter.enemyChampionId,
                userChampionId = storedCounter.userChampionId,
                role = storedCounter.role,
                games = storedCounter.games,
                wins = storedCounter.wins,
                losses = storedCounter.losses,
                winrate = storedCounter.winrate,
                averageKda = storedCounter.averageKda,
                averageCs = storedCounter.averageCs,
                averageGold = storedCounter.averageGold,
                averageDamage = storedCounter.averageDamage,
                personalScore = storedCounter.personalScore,
                confidence = storedCounter.confidence,
                status = recommendationScoringService.calculateStatus(
                    score = storedCounter.personalScore,
                    confidence = storedCounter.confidence,
                    games = storedCounter.games,
                ),
            )
        }

        return GetEnemyChampionCountersResult(counters)
    }
}

data class GetEnemyChampionCountersCommand(
    val summonerId: UUID,
    val enemyChampionId: Int,
)

data class GetEnemyChampionCountersResult(
    val counters: List<EnemyChampionCounter>,
)

data class EnemyChampionCounter(
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
    val confidence: com.comfortpick.domain.model.ConfidenceLevel,
    val status: RecommendationStatus,
)
