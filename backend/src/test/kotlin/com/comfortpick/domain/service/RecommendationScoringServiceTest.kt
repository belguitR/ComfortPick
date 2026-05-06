package com.comfortpick.domain.service

import com.comfortpick.domain.model.ConfidenceLevel
import com.comfortpick.domain.model.RecommendationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecommendationScoringServiceTest {
    private val service = RecommendationScoringService()

    @Test
    fun `one game perfect winrate does not outrank stronger reliable result`() {
        val oneGameSample = service.score(
            RecommendationScoringInput(
                enemyChampionId = 238,
                userChampionId = 127,
                role = "MIDDLE",
                games = 1,
                wins = 1,
                overallChampionGames = 1,
                averageKda = 3.0,
                averageCs = 145.0,
                averageGold = 9000.0,
                averageDamage = 18000.0,
                recentWinrate = 100.0,
                recentKda = 3.0,
            ),
        )
        val reliableCounter = service.score(
            RecommendationScoringInput(
                enemyChampionId = 238,
                userChampionId = 103,
                role = "MIDDLE",
                games = 7,
                wins = 5,
                overallChampionGames = 22,
                averageKda = 4.1,
                averageCs = 204.0,
                averageGold = 12300.0,
                averageDamage = 26500.0,
                recentWinrate = 70.0,
                recentKda = 4.0,
            ),
        )

        assertTrue(reliableCounter.personalScore > oneGameSample.personalScore)
        assertEquals(ConfidenceLevel.LOW, oneGameSample.confidence)
        assertEquals(ConfidenceLevel.HIGH, reliableCounter.confidence)
    }

    @Test
    fun `low sample penalties follow the mvp rules`() {
        assertEquals(100.0, service.calculateLowSamplePenalty(0))
        assertEquals(30.0, service.calculateLowSamplePenalty(1))
        assertEquals(20.0, service.calculateLowSamplePenalty(2))
        assertEquals(10.0, service.calculateLowSamplePenalty(3))
        assertEquals(0.0, service.calculateLowSamplePenalty(4))
    }

    @Test
    fun `confidence levels follow sample thresholds`() {
        assertEquals(ConfidenceLevel.NO_DATA, service.calculateConfidence(0))
        assertEquals(ConfidenceLevel.LOW, service.calculateConfidence(2))
        assertEquals(ConfidenceLevel.MEDIUM, service.calculateConfidence(5))
        assertEquals(ConfidenceLevel.HIGH, service.calculateConfidence(7))
    }

    @Test
    fun `recommendation status is deterministic`() {
        assertEquals(
            RecommendationStatus.NO_DATA,
            service.calculateStatus(0.0, ConfidenceLevel.NO_DATA, 0),
        )
        assertEquals(
            RecommendationStatus.LOW_DATA,
            service.calculateStatus(61.0, ConfidenceLevel.LOW, 2),
        )
        assertEquals(
            RecommendationStatus.BEST_PICK,
            service.calculateStatus(82.0, ConfidenceLevel.MEDIUM, 4),
        )
        assertEquals(
            RecommendationStatus.GOOD_PICK,
            service.calculateStatus(70.0, ConfidenceLevel.HIGH, 10),
        )
        assertEquals(
            RecommendationStatus.OK_PICK,
            service.calculateStatus(52.0, ConfidenceLevel.HIGH, 10),
        )
        assertEquals(
            RecommendationStatus.AVOID,
            service.calculateStatus(41.0, ConfidenceLevel.HIGH, 10),
        )
    }

    @Test
    fun `score result carries matchup stats`() {
        val result = service.score(
            RecommendationScoringInput(
                enemyChampionId = 238,
                userChampionId = 103,
                role = "MIDDLE",
                games = 7,
                wins = 5,
                overallChampionGames = 22,
                averageKda = 4.1,
                averageCs = 204.0,
                averageGold = 12300.0,
                averageDamage = 26500.0,
                recentWinrate = 70.0,
                recentKda = 4.0,
            ),
        )

        assertEquals(71.4, result.stats.winrate)
        assertEquals(RecommendationStatus.BEST_PICK, result.status)
        assertTrue(result.personalScore >= 80.0)
    }
}
