package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerMatchupRepository : JpaRepository<PlayerMatchupEntity, UUID> {
    fun findAllByRiotAccountId(riotAccountId: UUID): List<PlayerMatchupEntity>

    fun findAllByRiotAccountIdAndEnemyChampionIdAndUserChampionIdAndRole(
        riotAccountId: UUID,
        enemyChampionId: Int,
        userChampionId: Int,
        role: String,
    ): List<PlayerMatchupEntity>
}
