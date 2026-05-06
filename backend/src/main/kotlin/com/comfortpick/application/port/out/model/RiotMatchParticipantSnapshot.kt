package com.comfortpick.application.port.out.model

data class RiotMatchParticipantSnapshot(
    val puuid: String,
    val championId: Int,
    val championName: String,
    val teamId: Int,
    val teamPosition: String,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val totalCs: Int,
    val goldEarned: Int,
    val totalDamageToChampions: Int,
    val itemIds: List<Int>,
    val primaryRuneId: Int?,
    val secondaryRuneId: Int?,
)
