package com.comfortpick.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "personal_matchup_stats",
    indexes = [
        Index(name = "ix_personal_matchup_stats_account_enemy", columnList = "riot_account_id, enemy_champion_id"),
        Index(name = "ix_personal_matchup_stats_account_enemy_user", columnList = "riot_account_id, enemy_champion_id, user_champion_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "ux_personal_matchup_stats_lookup",
            columnNames = ["riot_account_id", "enemy_champion_id", "user_champion_id", "role"],
        ),
    ],
)
class PersonalMatchupStatsEntity(
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
    @Column(name = "games", nullable = false)
    var games: Int = 0,
    @Column(name = "wins", nullable = false)
    var wins: Int = 0,
    @Column(name = "losses", nullable = false)
    var losses: Int = 0,
    @Column(name = "winrate", nullable = false)
    var winrate: Double = 0.0,
    @Column(name = "average_kda")
    var averageKda: Double? = null,
    @Column(name = "average_cs")
    var averageCs: Double? = null,
    @Column(name = "average_gold")
    var averageGold: Double? = null,
    @Column(name = "average_damage")
    var averageDamage: Double? = null,
    @Column(name = "personal_score", nullable = false)
    var personalScore: Double = 0.0,
    @Column(name = "confidence", nullable = false)
    var confidence: String = "",
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
