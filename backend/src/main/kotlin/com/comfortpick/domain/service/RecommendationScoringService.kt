package com.comfortpick.domain.service

import com.comfortpick.domain.model.ConfidenceLevel
import com.comfortpick.domain.model.PersonalCounter
import com.comfortpick.domain.model.PersonalMatchupStats
import com.comfortpick.domain.model.RecommendationStatus
import kotlin.math.round

class RecommendationScoringService {
    fun score(input: RecommendationScoringInput): PersonalCounter {
        val wins = input.wins.coerceIn(0, input.games)
        val losses = (input.games - wins).coerceAtLeast(0)
        val winrate = calculateWinrate(wins, input.games)
        val confidence = calculateConfidence(input.games)

        val score = roundToSingleDecimal(
            (
                calculateWinrateScore(winrate) * 0.35 +
                    calculateChampionComfortScore(input.overallChampionGames) * 0.25 +
                    calculateKdaScore(input.averageKda) * 0.15 +
                    calculateCsGoldScore(input.averageCs, input.averageGold) * 0.10 +
                    calculateRecentPerformanceScore(input.recentWinrate, input.recentKda) * 0.10 +
                    GLOBAL_FALLBACK_SCORE * 0.05
            ) - calculateLowSamplePenalty(input.games),
        ).coerceIn(0.0, 100.0)

        val stats = PersonalMatchupStats(
            enemyChampionId = input.enemyChampionId,
            userChampionId = input.userChampionId,
            role = input.role,
            games = input.games,
            wins = wins,
            losses = losses,
            winrate = roundToSingleDecimal(winrate),
            averageKda = roundToSingleDecimal(input.averageKda),
            averageCs = input.averageCs?.let(::roundToSingleDecimal),
            averageGold = input.averageGold?.let(::roundToSingleDecimal),
            averageDamage = input.averageDamage?.let(::roundToSingleDecimal),
            confidence = confidence,
        )

        return PersonalCounter(
            enemyChampionId = input.enemyChampionId,
            userChampionId = input.userChampionId,
            role = input.role,
            personalScore = score,
            confidence = confidence,
            status = calculateStatus(score, confidence, input.games),
            stats = stats,
        )
    }

    fun calculateConfidence(games: Int): ConfidenceLevel =
        when {
            games <= 0 -> ConfidenceLevel.NO_DATA
            games <= 2 -> ConfidenceLevel.LOW
            games <= 6 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.HIGH
        }

    fun calculateStatus(
        score: Double,
        confidence: ConfidenceLevel,
        games: Int,
    ): RecommendationStatus =
        when {
            games <= 0 -> RecommendationStatus.NO_DATA
            score < 45.0 && confidence != ConfidenceLevel.NO_DATA -> RecommendationStatus.AVOID
            confidence == ConfidenceLevel.LOW && score >= 50.0 -> RecommendationStatus.LOW_DATA
            score >= 80.0 && confidence >= ConfidenceLevel.MEDIUM -> RecommendationStatus.BEST_PICK
            score >= 65.0 -> RecommendationStatus.GOOD_PICK
            score >= 50.0 -> RecommendationStatus.OK_PICK
            else -> RecommendationStatus.AVOID
        }

    fun calculateWinrate(wins: Int, games: Int): Double =
        if (games <= 0) {
            0.0
        } else {
            wins.toDouble() / games.toDouble() * 100.0
        }

    fun calculateWinrateScore(winrate: Double): Double = winrate.coerceIn(0.0, 100.0)

    fun calculateChampionComfortScore(overallChampionGames: Int): Double =
        when {
            overallChampionGames <= 0 -> 0.0
            overallChampionGames <= 3 -> 25.0
            overallChampionGames <= 10 -> 50.0
            overallChampionGames <= 25 -> 75.0 + ((overallChampionGames - 11).coerceAtLeast(0).toDouble() / 14.0 * 15.0)
            else -> 100.0
        }

    fun calculateKdaScore(averageKda: Double): Double =
        when {
            averageKda < 1.5 -> 25.0
            averageKda < 2.5 -> 50.0
            averageKda <= 4.0 -> 75.0
            else -> 100.0
        }

    fun calculateCsGoldScore(
        averageCs: Double?,
        averageGold: Double?,
    ): Double {
        val csScore = when {
            averageCs == null -> 50.0
            averageCs < 120.0 -> 30.0
            averageCs < 160.0 -> 50.0
            averageCs < 200.0 -> 75.0
            else -> 100.0
        }
        val goldScore = when {
            averageGold == null -> 50.0
            averageGold < 8000.0 -> 30.0
            averageGold < 10000.0 -> 50.0
            averageGold < 12000.0 -> 75.0
            else -> 100.0
        }

        return (csScore + goldScore) / 2.0
    }

    fun calculateRecentPerformanceScore(
        recentWinrate: Double?,
        recentKda: Double?,
    ): Double {
        val recentWinrateScore = recentWinrate?.coerceIn(0.0, 100.0) ?: 50.0
        val recentKdaScore = recentKda?.let(::calculateKdaScore) ?: 50.0

        return (recentWinrateScore + recentKdaScore) / 2.0
    }

    fun calculateLowSamplePenalty(games: Int): Double =
        when (games) {
            0 -> 100.0
            1 -> 30.0
            2 -> 20.0
            3 -> 10.0
            else -> 0.0
        }

    private fun roundToSingleDecimal(value: Double): Double = round(value * 10.0) / 10.0

    private companion object {
        const val GLOBAL_FALLBACK_SCORE = 50.0
    }
}
