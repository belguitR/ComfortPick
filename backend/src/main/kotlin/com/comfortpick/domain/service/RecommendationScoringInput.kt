package com.comfortpick.domain.service

data class RecommendationScoringInput(
    val enemyChampionId: Int,
    val userChampionId: Int,
    val role: String,
    val games: Int,
    val wins: Int,
    val overallChampionGames: Int,
    val averageKda: Double,
    val averageCs: Double?,
    val averageGold: Double?,
    val averageDamage: Double?,
    val recentWinrate: Double?,
    val recentKda: Double?,
)
