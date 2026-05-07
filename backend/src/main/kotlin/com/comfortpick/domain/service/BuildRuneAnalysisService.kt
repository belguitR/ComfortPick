package com.comfortpick.domain.service

import kotlin.math.round

class BuildRuneAnalysisService {
    fun analyze(samples: List<BuildRuneSample>): BuildRuneAnalysis {
        val wins = samples.filter { it.win }
        if (wins.isEmpty()) {
            return BuildRuneAnalysis.empty()
        }

        val firstItemCounts = wins.mapNotNull(::extractFirstCompletedItem)
            .groupingBy { it }
            .eachCount()
        val bestFirstItem = firstItemCounts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()

        val itemSetCounts = wins.mapNotNull(::extractItemSet)
            .groupingBy { it }
            .eachCount()
        val bestItemSet = itemSetCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()

        val primaryRuneCounts = wins.mapNotNull { it.primaryRuneId }
            .groupingBy { it }
            .eachCount()
        val bestPrimaryRune = primaryRuneCounts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()

        val secondaryRuneCounts = wins.mapNotNull { it.secondaryRuneId }
            .groupingBy { it }
            .eachCount()
        val bestSecondaryRune = secondaryRuneCounts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()

        val winrate = wins.size.toDouble() / samples.size.toDouble() * 100.0
        val score = calculateRecommendationScore(winrate = winrate, winGames = wins.size)

        return BuildRuneAnalysis(
            build = BuildRecommendation(
                firstCompletedItemId = bestFirstItem?.key,
                firstCompletedItemGames = bestFirstItem?.value ?: 0,
                itemSet = bestItemSet?.key,
                itemSetGames = bestItemSet?.value ?: 0,
                score = score,
            ),
            runes = RuneRecommendation(
                primaryRuneId = bestPrimaryRune?.key,
                primaryRuneGames = bestPrimaryRune?.value ?: 0,
                secondaryRuneId = bestSecondaryRune?.key,
                secondaryRuneGames = bestSecondaryRune?.value ?: 0,
                score = score,
            ),
        )
    }

    private fun extractFirstCompletedItem(sample: BuildRuneSample): Int? =
        sample.items.firstOrNull { it != null && it > 0 }

    private fun extractItemSet(sample: BuildRuneSample): String? {
        val items = sample.items.filterNotNull().filter { it > 0 }
        return if (items.isEmpty()) null else items.joinToString(separator = ">")
    }

    private fun calculateRecommendationScore(
        winrate: Double,
        winGames: Int,
    ): Double {
        val sampleBoost = when {
            winGames >= 5 -> 100.0
            winGames == 4 -> 90.0
            winGames == 3 -> 75.0
            winGames == 2 -> 60.0
            else -> 40.0
        }
        val lowSamplePenalty = when (winGames) {
            0 -> 100.0
            1 -> 25.0
            2 -> 10.0
            else -> 0.0
        }

        return round(((winrate * 0.65) + (sampleBoost * 0.35) - lowSamplePenalty) * 10.0) / 10.0
            .coerceIn(0.0, 100.0)
    }
}

data class BuildRuneSample(
    val win: Boolean,
    val items: List<Int?>,
    val primaryRuneId: Int?,
    val secondaryRuneId: Int?,
)

data class BuildRuneAnalysis(
    val build: BuildRecommendation,
    val runes: RuneRecommendation,
) {
    companion object {
        fun empty(): BuildRuneAnalysis =
            BuildRuneAnalysis(
                build = BuildRecommendation.empty(),
                runes = RuneRecommendation.empty(),
            )
    }
}

data class BuildRecommendation(
    val firstCompletedItemId: Int?,
    val firstCompletedItemGames: Int,
    val itemSet: String?,
    val itemSetGames: Int,
    val score: Double?,
) {
    companion object {
        fun empty(): BuildRecommendation =
            BuildRecommendation(
                firstCompletedItemId = null,
                firstCompletedItemGames = 0,
                itemSet = null,
                itemSetGames = 0,
                score = null,
            )
    }
}

data class RuneRecommendation(
    val primaryRuneId: Int?,
    val primaryRuneGames: Int,
    val secondaryRuneId: Int?,
    val secondaryRuneGames: Int,
    val score: Double?,
) {
    companion object {
        fun empty(): RuneRecommendation =
            RuneRecommendation(
                primaryRuneId = null,
                primaryRuneGames = 0,
                secondaryRuneId = null,
                secondaryRuneGames = 0,
                score = null,
            )
    }
}
