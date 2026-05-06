package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.PersonalBuildStatsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PersonalBuildStatsRepository : JpaRepository<PersonalBuildStatsEntity, UUID>
