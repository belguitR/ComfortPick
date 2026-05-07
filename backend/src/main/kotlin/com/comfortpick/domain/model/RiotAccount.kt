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
    val autoSyncEnabled: Boolean = false,
    val syncStatus: HistorySyncStatus = HistorySyncStatus.IDLE,
    val syncTargetMatchCount: Int = 500,
    val syncBackfillCursor: Int = 0,
    val syncNextRunAt: LocalDateTime? = null,
    val syncLastSyncAt: LocalDateTime? = null,
    val syncLastErrorCode: String? = null,
    val syncLastErrorMessage: String? = null,
)
