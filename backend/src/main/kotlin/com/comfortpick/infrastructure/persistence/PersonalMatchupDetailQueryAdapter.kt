package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.PersonalMatchupDetailQuery
import com.comfortpick.application.port.out.StoredBuildRuneSample
import com.comfortpick.application.port.out.StoredPersonalMatchupDetail
import com.comfortpick.application.port.out.StoredRecentMatchupGame
import com.comfortpick.domain.model.ConfidenceLevel
import com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class PersonalMatchupDetailQueryAdapter(
    private val personalMatchupStatsRepository: PersonalMatchupStatsRepository,
    private val playerMatchupRepository: PlayerMatchupRepository,
) : PersonalMatchupDetailQuery {
    @Transactional(readOnly = true)
    override fun findMatchupDetail(
        summonerId: UUID,
        enemyChampionId: Int,
        userChampionId: Int,
    ): StoredPersonalMatchupDetail? {
        val selectedStat = personalMatchupStatsRepository.findAllByRiotAccountIdAndEnemyChampionIdAndUserChampionId(
            riotAccountId = summonerId,
            enemyChampionId = enemyChampionId,
            userChampionId = userChampionId,
        ).sortedWith(
            compareByDescending<PersonalMatchupStatsEntity> { it.personalScore }
                .thenByDescending { it.games }
                .thenBy { it.role },
        ).firstOrNull() ?: return null

        val matchingRoleGames = playerMatchupRepository.findAllByRiotAccountIdAndEnemyChampionIdAndUserChampionIdAndRole(
            riotAccountId = summonerId,
            enemyChampionId = enemyChampionId,
            userChampionId = userChampionId,
            role = selectedStat.role,
        )

        val recentGames = matchingRoleGames.sortedWith(
            compareByDescending<com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity> { it.match.gameCreation }
                .thenByDescending { it.createdAt },
        ).take(RECENT_GAMES_LIMIT)
            .map { matchup ->
                StoredRecentMatchupGame(
                    riotMatchId = matchup.match.riotMatchId,
                    gameCreation = matchup.match.gameCreation,
                    win = matchup.win,
                    kills = matchup.kills,
                    deaths = matchup.deaths,
                    assists = matchup.assists,
                    totalCs = matchup.totalCs,
                    goldEarned = matchup.goldEarned,
                    totalDamageToChampions = matchup.totalDamageToChampions,
                )
            }

        return StoredPersonalMatchupDetail(
            summonerId = summonerId,
            enemyChampionId = selectedStat.enemyChampionId,
            userChampionId = selectedStat.userChampionId,
            role = selectedStat.role,
            games = selectedStat.games,
            wins = selectedStat.wins,
            losses = selectedStat.losses,
            winrate = selectedStat.winrate,
            averageKda = selectedStat.averageKda ?: 0.0,
            averageCs = selectedStat.averageCs,
            averageGold = selectedStat.averageGold,
            averageDamage = selectedStat.averageDamage,
            personalScore = selectedStat.personalScore,
            confidence = ConfidenceLevel.valueOf(selectedStat.confidence),
            updatedAt = selectedStat.updatedAt,
            recentGames = recentGames,
            buildRuneSamples = matchingRoleGames.map { matchup ->
                StoredBuildRuneSample(
                    win = matchup.win,
                    items = listOf(
                        matchup.item0,
                        matchup.item1,
                        matchup.item2,
                        matchup.item3,
                        matchup.item4,
                        matchup.item5,
                    ),
                    primaryRuneId = matchup.primaryRuneId,
                    secondaryRuneId = matchup.secondaryRuneId,
                )
            },
        )
    }

    private companion object {
        const val RECENT_GAMES_LIMIT = 5
    }
}
