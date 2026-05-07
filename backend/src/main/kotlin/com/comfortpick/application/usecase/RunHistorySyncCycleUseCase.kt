package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.application.port.out.exception.RiotApiRateLimitException
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.domain.model.HistorySyncStatus
import com.comfortpick.domain.model.RiotAccount
import com.comfortpick.infrastructure.config.HistorySyncProperties
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class RunHistorySyncCycleUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val importMatchHistoryUseCase: ImportMatchHistoryUseCase,
    private val historySyncProperties: HistorySyncProperties,
    private val clock: Clock,
) {
    fun execute() {
        val now = LocalDateTime.now(clock)
        val maxAccountsPerTick = maxOf(historySyncProperties.maxAccountsPerTick, 1)
        riotAccountStore.findDueForSync(
            before = now,
            limit = maxAccountsPerTick,
        ).forEach { dueAccount ->
            runForAccount(dueAccount, now)
        }
    }

    private fun runForAccount(
        dueAccount: RiotAccount,
        now: LocalDateTime,
    ) {
        var syncState = riotAccountStore.save(
            dueAccount.copy(
                syncStatus = HistorySyncStatus.RUNNING,
                syncNextRunAt = null,
                syncLastErrorCode = null,
                syncLastErrorMessage = null,
            ),
        )

        try {
            val updatedAccount = processOneCycle(syncState, now) { partialState ->
                syncState = partialState
            }
            riotAccountStore.save(updatedAccount)
        } catch (exception: RiotApiRateLimitException) {
            val retryAfterSeconds = exception.retryAfterSeconds?.toLong()
                ?: historySyncProperties.schedulerInterval.seconds
            riotAccountStore.save(
                syncState.copy(
                    syncStatus = HistorySyncStatus.RATE_LIMITED,
                    syncNextRunAt = now.plusSeconds(retryAfterSeconds),
                    syncLastSyncAt = now,
                    syncLastErrorCode = "RIOT_API_RATE_LIMIT",
                    syncLastErrorMessage = exception.message,
                ),
            )
        } catch (exception: Exception) {
            riotAccountStore.save(
                syncState.copy(
                    syncStatus = HistorySyncStatus.FAILED,
                    syncNextRunAt = now.plus(historySyncProperties.errorRetryDelay),
                    syncLastSyncAt = now,
                    syncLastErrorCode = exception::class.simpleName ?: "UNKNOWN_SYNC_ERROR",
                    syncLastErrorMessage = exception.message,
                ),
            )
        }
    }

    private fun processOneCycle(
        account: RiotAccount,
        now: LocalDateTime,
        onProgress: (RiotAccount) -> Unit,
    ): RiotAccount {
        val routingRegion = RiotRoutingRegion.fromValue(account.region)
        val batchSize = maxOf(historySyncProperties.batchSize, 1)

        val headBatch = importMatchHistoryUseCase.importBatch(
            account = account,
            routingRegion = routingRegion,
            matchStart = 0,
            matchCount = batchSize,
        )

        var nextCursor = account.syncBackfillCursor + headBatch.importedMatchCount
        var progressState = account.copy(
            autoSyncEnabled = true,
            syncStatus = HistorySyncStatus.RUNNING,
            syncBackfillCursor = nextCursor,
            syncLastSyncAt = now,
            syncLastErrorCode = null,
            syncLastErrorMessage = null,
        )
        onProgress(progressState)

        val shouldBackfillOlderHistory = nextCursor < account.syncTargetMatchCount &&
            headBatch.fetchedMatchCount >= batchSize

        val olderBatch = if (shouldBackfillOlderHistory) {
            importMatchHistoryUseCase.importBatch(
                account = progressState,
                routingRegion = routingRegion,
                matchStart = nextCursor,
                matchCount = batchSize,
            )
        } else {
            null
        }

        if (olderBatch != null) {
            nextCursor += olderBatch.fetchedMatchCount
            progressState = progressState.copy(syncBackfillCursor = nextCursor)
            onProgress(progressState)
        }

        val reachedTarget = nextCursor >= account.syncTargetMatchCount
        val reachedHistoryEnd = headBatch.fetchedMatchCount < batchSize ||
            (olderBatch != null && olderBatch.fetchedMatchCount < batchSize)
        val nextStatus = if (reachedTarget || reachedHistoryEnd) {
            HistorySyncStatus.COMPLETE
        } else {
            HistorySyncStatus.ACTIVE
        }

        return progressState.copy(
            autoSyncEnabled = true,
            syncStatus = nextStatus,
            syncBackfillCursor = nextCursor,
            syncNextRunAt = if (nextStatus == HistorySyncStatus.ACTIVE) now.plus(historySyncProperties.schedulerInterval) else null,
            syncLastSyncAt = now,
            syncLastErrorCode = null,
            syncLastErrorMessage = null,
        )
    }
}
