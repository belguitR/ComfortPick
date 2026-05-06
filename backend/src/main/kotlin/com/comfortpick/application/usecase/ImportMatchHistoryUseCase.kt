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
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: ImportMatchHistoryCommand): ImportMatchHistoryResult {
        val account = riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)

        val routingRegion = RiotRoutingRegion.fromValue(account.region)
        val recentMatchIds = riotApiPort.getMatchIdsByPuuid(
            routingRegion = routingRegion,
            puuid = account.puuid,
            start = 0,
            count = RECENT_MATCH_COUNT,
        )

        val existingMatchIds = matchImportStore.findExistingMatchIds(recentMatchIds)
        val newMatchIds = recentMatchIds.filterNot(existingMatchIds::contains)
        val now = LocalDateTime.now(clock)
        var importedMatchCount = 0
        var importedMatchupCount = 0
        var skippedMatchupCount = 0

        newMatchIds.forEach { matchId ->
            val matchDetails = riotApiPort.getMatchDetails(routingRegion, matchId)
            val persistedMatchId = matchImportStore.saveMatch(
                SaveMatchCommand(
                    riotMatchId = matchDetails.riotMatchId,
                    region = matchDetails.platformId,
                    queueId = matchDetails.queueId,
                    gameCreation = LocalDateTime.ofInstant(matchDetails.gameCreation, ZoneOffset.UTC),
                    gameDurationSeconds = matchDetails.gameDurationSeconds,
                    patch = matchDetails.patch,
                    createdAt = now,
                ),
            )

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

            importedMatchCount += 1
        }

        return ImportMatchHistoryResult(
            importedMatchCount = importedMatchCount,
            existingMatchCount = existingMatchIds.size,
            importedMatchupCount = importedMatchupCount,
            skippedMatchupCount = skippedMatchupCount,
        )
    }

    companion object {
        private const val RECENT_MATCH_COUNT = 100
    }
}

data class ImportMatchHistoryCommand(
    val summonerId: UUID,
)

data class ImportMatchHistoryResult(
    val importedMatchCount: Int,
    val existingMatchCount: Int,
    val importedMatchupCount: Int,
    val skippedMatchupCount: Int,
)

class SummonerProfileNotFoundException(
    val summonerId: UUID,
) : RuntimeException("Summoner profile $summonerId was not found")
