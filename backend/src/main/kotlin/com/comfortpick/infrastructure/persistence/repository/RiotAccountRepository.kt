package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID
import java.time.LocalDateTime

interface RiotAccountRepository : JpaRepository<RiotAccountEntity, UUID> {
    fun findByPuuid(puuid: String): RiotAccountEntity?
    fun findByRegionAndGameNameAndTagLine(region: String, gameName: String, tagLine: String): RiotAccountEntity?

    @Query(
        """
        select account
        from RiotAccountEntity account
        where account.autoSyncEnabled = true
          and account.syncNextRunAt is not null
          and account.syncNextRunAt <= :before
          and account.syncStatus <> 'RUNNING'
        order by account.syncNextRunAt asc, account.updatedAt asc
        """,
    )
    fun findDueForSync(
        @Param("before") before: LocalDateTime,
        pageable: Pageable,
    ): List<RiotAccountEntity>
}
