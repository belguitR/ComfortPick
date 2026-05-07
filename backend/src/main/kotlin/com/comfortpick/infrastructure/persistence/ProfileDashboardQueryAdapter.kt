package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.ChampionPlayCount
import com.comfortpick.application.port.out.ProfileDashboardQuery
import com.comfortpick.application.port.out.ProfileDashboardSnapshot
import com.comfortpick.application.port.out.StoredProfileCounter
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class ProfileDashboardQueryAdapter(
    private val playerMatchupRepository: PlayerMatchupRepository,
    private val personalMatchupStatsRepository: PersonalMatchupStatsRepository,
) : ProfileDashboardQuery {
    override fun findProfileDashboardSnapshot(summonerId: UUID): ProfileDashboardSnapshot {
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

        val sortedStats = personalStats.sortedWith(
            compareByDescending<com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity> { it.personalScore }
                .thenByDescending { it.games }
                .thenBy { it.userChampionId },
        )
        val bestCounters = sortedStats
            .take(COUNTER_SUMMARY_LIMIT)
            .map(::toCounterSummary)

        val worstMatchups = personalStats
            .sortedWith(
                compareBy<com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity> { it.personalScore }
                    .thenByDescending { it.games }
                    .thenBy { it.userChampionId },
            )
            .take(COUNTER_SUMMARY_LIMIT)
            .map(::toCounterSummary)

        val lastUpdateAt = listOfNotNull(
            personalStats.maxOfOrNull { it.updatedAt },
            playerMatchups.maxOfOrNull { it.createdAt },
        ).maxOrNull()

        return ProfileDashboardSnapshot(
            analyzedMatches = analyzedMatches,
            mainRole = mainRole,
            mostPlayedChampions = mostPlayedChampions,
            bestCounters = bestCounters,
            worstMatchups = worstMatchups,
            lastUpdateAt = lastUpdateAt,
        )
    }

    private fun toCounterSummary(
        stat: com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity,
    ): StoredProfileCounter =
        StoredProfileCounter(
            enemyChampionId = stat.enemyChampionId,
            userChampionId = stat.userChampionId,
            role = stat.role,
            games = stat.games,
            winrate = stat.winrate,
            personalScore = stat.personalScore,
        )

    private companion object {
        const val MOST_PLAYED_LIMIT = 5
        const val COUNTER_SUMMARY_LIMIT = 5
    }
}
