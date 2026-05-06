package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.domain.model.RiotAccount
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.springframework.stereotype.Component

@Component
class RiotAccountStoreAdapter(
    private val riotAccountRepository: RiotAccountRepository,
) : RiotAccountStore {
    override fun findByRegionAndGameNameAndTagLine(
        region: String,
        gameName: String,
        tagLine: String,
    ): RiotAccount? =
        riotAccountRepository.findByRegionAndGameNameAndTagLine(
            region = region,
            gameName = gameName,
            tagLine = tagLine,
        )?.toDomain()

    override fun findByPuuid(puuid: String): RiotAccount? =
        riotAccountRepository.findByPuuid(puuid)?.toDomain()

    override fun save(account: RiotAccount): RiotAccount =
        riotAccountRepository.save(account.toEntity()).toDomain()

    private fun RiotAccountEntity.toDomain(): RiotAccount =
        RiotAccount(
            id = id,
            puuid = puuid,
            gameName = gameName,
            tagLine = tagLine,
            region = region,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun RiotAccount.toEntity(): RiotAccountEntity =
        RiotAccountEntity(
            id = id,
            puuid = puuid,
            gameName = gameName,
            tagLine = tagLine,
            region = region,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
