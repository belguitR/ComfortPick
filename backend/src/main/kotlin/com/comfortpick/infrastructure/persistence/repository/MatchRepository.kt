package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.MatchEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MatchRepository : JpaRepository<MatchEntity, UUID> {
    fun findByRiotMatchId(riotMatchId: String): MatchEntity?
}
