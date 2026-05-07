package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.MatchImportStore
import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.application.port.out.SaveMatchCommand
import com.comfortpick.application.port.out.SavePlayerMatchupCommand
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.application.service.PlayerMatchupExtractionResult
import com.comfortpick.application.service.PlayerMatchupExtractor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class ImportMatchHistoryUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val riotApiPort: RiotApiPort,
    private val matchImportStore: MatchImportStore,
    private val playerMatchupExtractor: PlayerMatchupExtractor,
    private val recalculatePersonalMatchupStatsUseCase: RecalculatePersonalMatchupStatsUseCase,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: ImportMatchHistoryCommand): ImportMatchHistoryResult {
        val account = riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)
        val routingRegion = RiotRoutingRegion.fromValue(account.region)

        return importBatch(
            account = account,
            routingRegion = routingRegion,
            matchStart = command.matchStart,
            matchCount = command.matchCount.coerceIn(1, MAX_IMPORT_MATCH_COUNT),
        )
    }

    @Transactional
    fun importBatch(
        account: com.comfortpick.domain.model.RiotAccount,
        routingRegion: RiotRoutingRegion,
        matchStart: Int,
        matchCount: Int,
    ): ImportMatchHistoryResult {
        val recentMatchIds = riotApiPort.getMatchIdsByPuuid(
            routingRegion = routingRegion,
            puuid = account.puuid,
            start = matchStart,
            count = matchCount,
        )

        val existingMatchIds = matchImportStore.findExistingMatchIds(recentMatchIds)
        val storedMatchIdsByRiotMatchId = matchImportStore.findStoredMatchIdByRiotMatchIds(recentMatchIds)
        val matchIdsWithStoredMatchup = matchImportStore.findMatchIdsWithStoredMatchupForAccount(account.id, recentMatchIds)
        val matchIdsNeedingProcessing = recentMatchIds.filterNot(matchIdsWithStoredMatchup::contains)
        val now = LocalDateTime.now(clock)
        var importedMatchCount = 0
        var importedMatchupCount = 0
        var skippedMatchupCount = 0

        matchIdsNeedingProcessing.forEach { matchId ->
            val matchDetails = riotApiPort.getMatchDetails(routingRegion, matchId)
            val persistedMatchId = storedMatchIdsByRiotMatchId[matchId]
                ?: matchImportStore.saveMatch(
                    SaveMatchCommand(
                        riotMatchId = matchDetails.riotMatchId,
                        region = matchDetails.platformId,
                        queueId = matchDetails.queueId,
                        gameCreation = LocalDateTime.ofInstant(matchDetails.gameCreation, ZoneOffset.UTC),
                        gameDurationSeconds = matchDetails.gameDurationSeconds,
                        patch = matchDetails.patch,
                        createdAt = now,
                    ),
                ).also {
                    importedMatchCount += 1
                }

            when (
                val extractionResult = playerMatchupExtractor.extract(
                    userPuuid = account.puuid,
                    matchDetails = matchDetails,
                )
            ) {
                is PlayerMatchupExtractionResult.Success -> {
                    val extractedMatchup = extractionResult.matchup
                    matchImportStore.savePlayerMatchup(
                        SavePlayerMatchupCommand(
                            riotAccountId = account.id,
                            matchId = persistedMatchId,
                            userPuuid = extractedMatchup.userPuuid,
                            userChampionId = extractedMatchup.userChampionId,
                            enemyChampionId = extractedMatchup.enemyChampionId,
                            role = extractedMatchup.role,
                            win = extractedMatchup.win,
                            kills = extractedMatchup.kills,
                            deaths = extractedMatchup.deaths,
                            assists = extractedMatchup.assists,
                            totalCs = extractedMatchup.totalCs,
                            goldEarned = extractedMatchup.goldEarned,
                            totalDamageToChampions = extractedMatchup.totalDamageToChampions,
                            itemIds = extractedMatchup.itemIds,
                            primaryRuneId = extractedMatchup.primaryRuneId,
                            secondaryRuneId = extractedMatchup.secondaryRuneId,
                            summonerSpell1Id = extractedMatchup.summonerSpell1Id,
                            summonerSpell2Id = extractedMatchup.summonerSpell2Id,
                            createdAt = now,
                        ),
                    )
                    importedMatchupCount += 1
                }

                is PlayerMatchupExtractionResult.Failure -> {
                    // Match remains stored so it is not refetched; no personal matchup row is written.
                    skippedMatchupCount += 1
                }
            }
        }

        if (importedMatchupCount > 0) {
            recalculatePersonalMatchupStatsUseCase.execute(
                RecalculatePersonalMatchupStatsCommand(
                    summonerId = account.id,
                ),
            )
        }

        return ImportMatchHistoryResult(
            fetchedMatchCount = recentMatchIds.size,
            importedMatchCount = importedMatchCount,
            existingMatchCount = existingMatchIds.size,
            importedMatchupCount = importedMatchupCount,
            skippedMatchupCount = skippedMatchupCount,
        )
    }

    companion object {
        const val DEFAULT_IMPORT_MATCH_COUNT = 10
        const val MAX_IMPORT_MATCH_COUNT = 20
    }
}

data class ImportMatchHistoryCommand(
    val summonerId: UUID,
    val matchStart: Int = 0,
    val matchCount: Int = ImportMatchHistoryUseCase.DEFAULT_IMPORT_MATCH_COUNT,
)

data class ImportMatchHistoryResult(
    val fetchedMatchCount: Int,
    val importedMatchCount: Int,
    val existingMatchCount: Int,
    val importedMatchupCount: Int,
    val skippedMatchupCount: Int,
)

class SummonerProfileNotFoundException(
    val summonerId: UUID,
) : RuntimeException("Summoner profile $summonerId was not found")
