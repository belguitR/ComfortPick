package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RiotAccountRepository : JpaRepository<RiotAccountEntity, UUID> {
    fun findByPuuid(puuid: String): RiotAccountEntity?
    fun findByRegionAndGameNameAndTagLine(region: String, gameName: String, tagLine: String): RiotAccountEntity?
}
