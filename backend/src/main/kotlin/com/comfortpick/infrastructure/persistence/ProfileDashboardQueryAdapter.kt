package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.ChampionPlayCount
import com.comfortpick.application.port.out.ProfileDashboardQuery
import com.comfortpick.application.port.out.ProfileRecentGameSnapshot
import com.comfortpick.application.port.out.ProfileDashboardSnapshot
import com.comfortpick.application.port.out.ProfileSyncSnapshot
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class ProfileDashboardQueryAdapter(
    private val riotAccountRepository: RiotAccountRepository,
    private val playerMatchupRepository: PlayerMatchupRepository,
    private val personalMatchupStatsRepository: PersonalMatchupStatsRepository,
) : ProfileDashboardQuery {
    override fun findProfileDashboardSnapshot(summonerId: UUID): ProfileDashboardSnapshot {
        val account = riotAccountRepository.findById(summonerId)
            .orElseThrow { IllegalStateException("Riot account $summonerId was not found during dashboard query") }
        val playerMatchups = playerMatchupRepository.findAllByRiotAccountId(summonerId)
        val personalStats = personalMatchupStatsRepository.findAllByRiotAccountId(summonerId)

        val analyzedMatches = playerMatchups.size
        val mainRole = playerMatchups
            .groupingBy { it.role }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()
            ?.key

        val mostPlayedChampions = playerMatchups
            .groupingBy { it.userChampionId }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(MOST_PLAYED_LIMIT)
            .map { ChampionPlayCount(championId = it.key, games = it.value) }

        val latestGames = playerMatchupRepository.findRecentByRiotAccountId(summonerId)
            .take(RECENT_GAMES_LIMIT)
            .map { matchup ->
                ProfileRecentGameSnapshot(
                    riotMatchId = matchup.match.riotMatchId,
                    userChampionId = matchup.userChampionId,
                    role = matchup.role,
                    win = matchup.win,
                    kills = matchup.kills,
                    deaths = matchup.deaths,
                    assists = matchup.assists,
                    totalCs = matchup.totalCs,
                    goldEarned = matchup.goldEarned,
                    totalDamageToChampions = matchup.totalDamageToChampions,
                    itemIds = listOfNotNull(
                        matchup.item0,
                        matchup.item1,
                        matchup.item2,
                        matchup.item3,
                        matchup.item4,
                        matchup.item5,
                        matchup.item6,
                    ),
                    primaryRuneId = matchup.primaryRuneId,
                    secondaryRuneId = matchup.secondaryRuneId,
                    summonerSpell1Id = matchup.summonerSpell1Id,
                    summonerSpell2Id = matchup.summonerSpell2Id,
                    gameCreation = matchup.match.gameCreation,
                )
            }

        val lastUpdateAt = listOfNotNull(
            personalStats.maxOfOrNull { it.updatedAt },
            playerMatchups.maxOfOrNull { it.createdAt },
        ).maxOrNull()

        return ProfileDashboardSnapshot(
            analyzedMatches = analyzedMatches,
            mainRole = mainRole,
            mostPlayedChampions = mostPlayedChampions,
            latestGames = latestGames,
            lastUpdateAt = lastUpdateAt,
            sync = ProfileSyncSnapshot(
                enabled = account.autoSyncEnabled,
                status = account.syncStatus,
                targetMatchCount = account.syncTargetMatchCount,
                backfillCursor = account.syncBackfillCursor,
                nextRunAt = account.syncNextRunAt,
                lastSyncAt = account.syncLastSyncAt,
                lastErrorCode = account.syncLastErrorCode,
                lastErrorMessage = account.syncLastErrorMessage,
            ),
        )
    }

    private companion object {
        const val MOST_PLAYED_LIMIT = 5
        const val RECENT_GAMES_LIMIT = 8
    }
}
