package com.comfortpick.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class RiotAccount(
    val id: UUID,
    val puuid: String,
    val gameName: String,
    val tagLine: String,
    val region: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
