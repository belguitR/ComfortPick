package com.comfortpick.application.port.out.model

import java.time.Instant

data class RiotMatchDetails(
    val riotMatchId: String,
    val platformId: String,
    val queueId: Int,
    val gameCreation: Instant,
    val gameDurationSeconds: Int,
    val patch: String,
    val participants: List<RiotMatchParticipantSnapshot>,
)
