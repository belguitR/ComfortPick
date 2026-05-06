package com.comfortpick.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "personal_build_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "ux_personal_build_stats_lookup",
            columnNames = ["riot_account_id", "enemy_champion_id", "user_champion_id", "role", "item_sequence"],
        ),
    ],
)
class PersonalBuildStatsEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "riot_account_id", nullable = false)
    var riotAccount: RiotAccountEntity = RiotAccountEntity(),
    @Column(name = "enemy_champion_id", nullable = false)
    var enemyChampionId: Int = 0,
    @Column(name = "user_champion_id", nullable = false)
    var userChampionId: Int = 0,
    @Column(name = "role", nullable = false)
    var role: String = "",
    @Column(name = "item_sequence", nullable = false)
    var itemSequence: String = "",
    @Column(name = "games", nullable = false)
    var games: Int = 0,
    @Column(name = "wins", nullable = false)
    var wins: Int = 0,
    @Column(name = "winrate", nullable = false)
    var winrate: Double = 0.0,
    @Column(name = "build_score", nullable = false)
    var buildScore: Double = 0.0,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
