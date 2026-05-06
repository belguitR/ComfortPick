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
)
