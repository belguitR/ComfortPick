package com.comfortpick.domain.model

data class PersonalCounter(
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String,
    val personalScore: Double,
    val confidence: ConfidenceLevel,
    val status: RecommendationStatus,
    val stats: PersonalMatchupStats,
)
