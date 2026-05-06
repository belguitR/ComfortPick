package com.comfortpick.infrastructure.riot.dto

data class RiotMatchDto(
    val metadata: RiotMatchMetadataDto,
    val info: RiotMatchInfoDto,
)

data class RiotMatchMetadataDto(
    val dataVersion: String,
    val matchId: String,
    val participants: List<String>,
)

data class RiotMatchInfoDto(
    val gameCreation: Long,
    val gameDuration: Long,
    val gameVersion: String,
    val participants: List<RiotMatchParticipantDto>,
    val platformId: String,
    val queueId: Int,
)

data class RiotMatchParticipantDto(
    val puuid: String,
    val championId: Int,
    val championName: String,
    val teamId: Int,
    val teamPosition: String?,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val totalMinionsKilled: Int?,
    val neutralMinionsKilled: Int?,
    val goldEarned: Int,
    val totalDamageDealtToChampions: Int,
    val item0: Int,
    val item1: Int,
    val item2: Int,
    val item3: Int,
    val item4: Int,
    val item5: Int,
    val item6: Int,
    val perks: RiotPerksDto?,
)

data class RiotPerksDto(
    val styles: List<RiotPerkStyleDto>,
)

data class RiotPerkStyleDto(
    val description: String?,
    val selections: List<RiotPerkSelectionDto>?,
    val style: Int?,
)

data class RiotPerkSelectionDto(
    val perk: Int?,
)
