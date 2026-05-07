package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.domain.model.HistorySyncStatus
import com.comfortpick.domain.model.RiotAccount
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RiotAccountStoreAdapter(
    private val riotAccountRepository: RiotAccountRepository,
) : RiotAccountStore {
    override fun findById(id: java.util.UUID): RiotAccount? =
        riotAccountRepository.findById(id)
            .orElse(null)
            ?.toDomain()

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

    override fun findDueForSync(
        before: LocalDateTime,
        limit: Int,
    ): List<RiotAccount> =
        riotAccountRepository.findAll()
            .asSequence()
            .filter { it.autoSyncEnabled }
            .filter { it.syncNextRunAt != null && !it.syncNextRunAt!!.isAfter(before) }
            .filter { it.syncStatus != HistorySyncStatus.RUNNING.name }
            .sortedWith(compareBy<RiotAccountEntity> { it.syncNextRunAt }.thenBy { it.updatedAt })
            .take(limit)
            .map { it.toDomain() }
            .toList()

    private fun RiotAccountEntity.toDomain(): RiotAccount =
        RiotAccount(
            id = id,
            puuid = puuid,
            gameName = gameName,
            tagLine = tagLine,
            region = region,
            createdAt = createdAt,
            updatedAt = updatedAt,
            autoSyncEnabled = autoSyncEnabled,
            syncStatus = HistorySyncStatus.valueOf(syncStatus),
            syncTargetMatchCount = syncTargetMatchCount,
            syncBackfillCursor = syncBackfillCursor,
            syncNextRunAt = syncNextRunAt,
            syncLastSyncAt = syncLastSyncAt,
            syncLastErrorCode = syncLastErrorCode,
            syncLastErrorMessage = syncLastErrorMessage,
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
            autoSyncEnabled = autoSyncEnabled,
            syncStatus = syncStatus.name,
            syncTargetMatchCount = syncTargetMatchCount,
            syncBackfillCursor = syncBackfillCursor,
            syncNextRunAt = syncNextRunAt,
            syncLastSyncAt = syncLastSyncAt,
            syncLastErrorCode = syncLastErrorCode,
            syncLastErrorMessage = syncLastErrorMessage,
        )
}
