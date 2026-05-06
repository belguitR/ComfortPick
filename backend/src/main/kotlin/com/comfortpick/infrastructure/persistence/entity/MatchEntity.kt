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
    name = "matches",
    indexes = [
        Index(name = "ix_matches_riot_match_id", columnList = "riot_match_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_matches_riot_match_id", columnNames = ["riot_match_id"]),
    ],
)
class MatchEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "riot_match_id", nullable = false)
    var riotMatchId: String = "",
    @Column(name = "region", nullable = false)
    var region: String = "",
    @Column(name = "queue_id")
    var queueId: Int? = null,
    @Column(name = "game_creation", nullable = false)
    var gameCreation: LocalDateTime = LocalDateTime.now(),
    @Column(name = "game_duration_seconds", nullable = false)
    var gameDurationSeconds: Int = 0,
    @Column(name = "patch", nullable = false)
    var patch: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
