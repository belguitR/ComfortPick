package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.PersonalCountersQuery
import com.comfortpick.application.port.out.StoredPersonalCounterStat
import com.comfortpick.domain.model.ConfidenceLevel
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PersonalCountersQueryAdapter(
    private val personalMatchupStatsRepository: PersonalMatchupStatsRepository,
) : PersonalCountersQuery {
    override fun findCountersBySummonerIdAndEnemyChampionId(
        summonerId: UUID,
        enemyChampionId: Int,
    ): List<StoredPersonalCounterStat> =
        personalMatchupStatsRepository.findAllByRiotAccountIdAndEnemyChampionIdOrderByPersonalScoreDesc(
            riotAccountId = summonerId,
            enemyChampionId = enemyChampionId,
        ).map { stat ->
            StoredPersonalCounterStat(
                summonerId = stat.riotAccount.id,
                enemyChampionId = stat.enemyChampionId,
                userChampionId = stat.userChampionId,
                role = stat.role,
                games = stat.games,
                wins = stat.wins,
                losses = stat.losses,
                winrate = stat.winrate,
                averageKda = stat.averageKda ?: 0.0,
                averageCs = stat.averageCs,
                averageGold = stat.averageGold,
                averageDamage = stat.averageDamage,
                personalScore = stat.personalScore,
                confidence = ConfidenceLevel.valueOf(stat.confidence),
            )
        }
}
