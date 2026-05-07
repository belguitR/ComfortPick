package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PlayerMatchupRepository : JpaRepository<PlayerMatchupEntity, UUID> {
    fun findAllByRiotAccountId(riotAccountId: UUID): List<PlayerMatchupEntity>
    fun countByRiotAccountId(riotAccountId: UUID): Long

    fun findAllByRiotAccountIdAndEnemyChampionIdAndUserChampionIdAndRole(
        riotAccountId: UUID,
        enemyChampionId: Int,
        userChampionId: Int,
        role: String,
    ): List<PlayerMatchupEntity>

    @Query(
        """
        select pm
        from PlayerMatchupEntity pm
        join fetch pm.match m
        where pm.riotAccount.id = :riotAccountId
        order by m.gameCreation desc, pm.createdAt desc
        """,
    )
    fun findRecentByRiotAccountId(
        @Param("riotAccountId") riotAccountId: UUID,
    ): List<PlayerMatchupEntity>

    @Query(
        """
        select pm.match.riotMatchId
        from PlayerMatchupEntity pm
        where pm.riotAccount.id = :riotAccountId
          and pm.match.riotMatchId in :riotMatchIds
        """,
    )
    fun findRiotMatchIdsByRiotAccountIdAndRiotMatchIdIn(
        @Param("riotAccountId") riotAccountId: UUID,
        @Param("riotMatchIds") riotMatchIds: Collection<String>,
    ): List<String>
}
