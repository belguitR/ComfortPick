package com.comfortpick.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "riot_accounts",
    indexes = [
        Index(name = "ix_riot_accounts_puuid", columnList = "puuid"),
        Index(name = "ux_riot_accounts_region_game_tag", columnList = "region, game_name, tag_line", unique = true),
        Index(name = "ix_riot_accounts_sync_next_run_at", columnList = "sync_next_run_at"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_riot_accounts_puuid", columnNames = ["puuid"]),
    ],
)
class RiotAccountEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "puuid", nullable = false)
    var puuid: String = "",
    @Column(name = "game_name", nullable = false)
    var gameName: String = "",
    @Column(name = "tag_line", nullable = false)
    var tagLine: String = "",
    @Column(name = "region", nullable = false)
    var region: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "auto_sync_enabled", nullable = false)
    var autoSyncEnabled: Boolean = false,
    @Column(name = "sync_status", nullable = false)
    var syncStatus: String = "IDLE",
    @Column(name = "sync_target_match_count", nullable = false)
    var syncTargetMatchCount: Int = 500,
    @Column(name = "sync_backfill_cursor", nullable = false)
    var syncBackfillCursor: Int = 0,
    @Column(name = "sync_next_run_at")
    var syncNextRunAt: LocalDateTime? = null,
    @Column(name = "sync_last_sync_at")
    var syncLastSyncAt: LocalDateTime? = null,
    @Column(name = "sync_last_error_code")
    var syncLastErrorCode: String? = null,
    @Column(name = "sync_last_error_message")
    var syncLastErrorMessage: String? = null,
)
