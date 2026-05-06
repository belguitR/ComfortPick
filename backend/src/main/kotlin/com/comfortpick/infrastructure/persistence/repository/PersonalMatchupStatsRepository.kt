package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PersonalMatchupStatsRepository : JpaRepository<PersonalMatchupStatsEntity, UUID> {
    fun findAllByRiotAccountIdAndEnemyChampionIdOrderByPersonalScoreDesc(
        riotAccountId: UUID,
        enemyChampionId: Int,
    ): List<PersonalMatchupStatsEntity>
}
