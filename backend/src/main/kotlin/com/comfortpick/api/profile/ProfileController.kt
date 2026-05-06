package com.comfortpick.api.profile

import com.comfortpick.application.usecase.GetEnemyChampionCountersCommand
import com.comfortpick.application.usecase.GetEnemyChampionCountersUseCase
import com.comfortpick.domain.model.ConfidenceLevel
import com.comfortpick.domain.model.RecommendationStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/profiles")
class ProfileController(
    private val getEnemyChampionCountersUseCase: GetEnemyChampionCountersUseCase,
) {
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
