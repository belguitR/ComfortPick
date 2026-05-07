package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.domain.model.HistorySyncStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class RequestSummonerHistorySyncUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val clock: Clock,
) {
    fun execute(command: RequestSummonerHistorySyncCommand): RequestSummonerHistorySyncResult {
        val account = riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)
        val now = LocalDateTime.now(clock)

        val updatedAccount = account.copy(
            autoSyncEnabled = true,
            syncStatus = HistorySyncStatus.ACTIVE,
            syncTargetMatchCount = maxOf(account.syncTargetMatchCount, command.targetMatchCount),
            syncNextRunAt = now,
            syncLastErrorCode = null,
            syncLastErrorMessage = null,
        )

        val savedAccount = riotAccountStore.save(updatedAccount)
        return RequestSummonerHistorySyncResult(
            summonerId = savedAccount.id,
            syncStatus = savedAccount.syncStatus.name,
            targetMatchCount = savedAccount.syncTargetMatchCount,
            backfillCursor = savedAccount.syncBackfillCursor,
            nextRunAt = savedAccount.syncNextRunAt,
        )
    }
}

data class RequestSummonerHistorySyncCommand(
    val summonerId: UUID,
    val targetMatchCount: Int,
)

data class RequestSummonerHistorySyncResult(
    val summonerId: UUID,
    val syncStatus: String,
    val targetMatchCount: Int,
    val backfillCursor: Int,
    val nextRunAt: LocalDateTime?,
)
