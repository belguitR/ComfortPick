package com.comfortpick.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Match(
    val id: UUID,
    val riotMatchId: String,
    val region: String,
    val queueId: Int?,
    val gameCreation: LocalDateTime,
    val gameDurationSeconds: Int,
    val patch: String,
)
