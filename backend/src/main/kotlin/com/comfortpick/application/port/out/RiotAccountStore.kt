package com.comfortpick.application.port.out

import com.comfortpick.domain.model.RiotAccount

interface RiotAccountStore {
    fun findById(id: java.util.UUID): RiotAccount?

    fun findByRegionAndGameNameAndTagLine(
        region: String,
        gameName: String,
        tagLine: String,
    ): RiotAccount?

    fun findByPuuid(puuid: String): RiotAccount?

    fun save(account: RiotAccount): RiotAccount

    fun findDueForSync(
        before: java.time.LocalDateTime,
        limit: Int,
    ): List<RiotAccount>
}
