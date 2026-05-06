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
    name = "player_matchups",
    indexes = [
        Index(name = "ix_player_matchups_match_id", columnList = "match_id"),
        Index(name = "ix_player_matchups_user_puuid", columnList = "user_puuid"),
        Index(name = "ix_player_matchups_user_champion_role", columnList = "user_champion_id, role"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "ux_player_matchups_account_match", columnNames = ["riot_account_id", "match_id"]),
    ],
)
class PlayerMatchupEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "riot_account_id", nullable = false)
    var riotAccount: RiotAccountEntity = RiotAccountEntity(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var match: MatchEntity = MatchEntity(),
    @Column(name = "user_puuid", nullable = false)
    var userPuuid: String = "",
    @Column(name = "user_champion_id", nullable = false)
    var userChampionId: Int = 0,
    @Column(name = "enemy_champion_id", nullable = false)
    var enemyChampionId: Int = 0,
    @Column(name = "role", nullable = false)
    var role: String = "",
    @Column(name = "win", nullable = false)
    var win: Boolean = false,
    @Column(name = "kills", nullable = false)
    var kills: Int = 0,
    @Column(name = "deaths", nullable = false)
    var deaths: Int = 0,
    @Column(name = "assists", nullable = false)
    var assists: Int = 0,
    @Column(name = "total_cs")
    var totalCs: Int? = null,
    @Column(name = "gold_earned")
    var goldEarned: Int? = null,
    @Column(name = "total_damage_to_champions")
    var totalDamageToChampions: Int? = null,
    @Column(name = "item_0")
    var item0: Int? = null,
    @Column(name = "item_1")
    var item1: Int? = null,
    @Column(name = "item_2")
    var item2: Int? = null,
    @Column(name = "item_3")
    var item3: Int? = null,
    @Column(name = "item_4")
    var item4: Int? = null,
    @Column(name = "item_5")
    var item5: Int? = null,
    @Column(name = "item_6")
    var item6: Int? = null,
    @Column(name = "primary_rune_id")
    var primaryRuneId: Int? = null,
    @Column(name = "secondary_rune_id")
    var secondaryRuneId: Int? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
