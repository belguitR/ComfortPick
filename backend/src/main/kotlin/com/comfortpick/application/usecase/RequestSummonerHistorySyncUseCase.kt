package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.MatchupCoverageQuery
import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.domain.model.HistorySyncStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class RequestSummonerHistorySyncUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val matchupCoverageQuery: MatchupCoverageQuery,
    private val clock: Clock,
) {
    fun execute(command: RequestSummonerHistorySyncCommand): RequestSummonerHistorySyncResult {
        val account = riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)
        val now = LocalDateTime.now(clock)
        val analyzedMatchupCount = matchupCoverageQuery.countStoredMatchupsForAccount(account.id)
        val shouldRepairCoverage = account.syncBackfillCursor >= MIN_REPAIR_CURSOR &&
            analyzedMatchupCount * COVERAGE_REPAIR_RATIO_DENOMINATOR < account.syncBackfillCursor.toLong() * COVERAGE_REPAIR_RATIO_NUMERATOR
        val nextBackfillCursor = if (shouldRepairCoverage) 0 else account.syncBackfillCursor

        val updatedAccount = account.copy(
            autoSyncEnabled = true,
            syncStatus = HistorySyncStatus.ACTIVE,
            syncTargetMatchCount = maxOf(account.syncTargetMatchCount, command.targetMatchCount),
            syncBackfillCursor = nextBackfillCursor,
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

    companion object {
        private const val MIN_REPAIR_CURSOR = 50
        private const val COVERAGE_REPAIR_RATIO_NUMERATOR = 1
        private const val COVERAGE_REPAIR_RATIO_DENOMINATOR = 2
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
