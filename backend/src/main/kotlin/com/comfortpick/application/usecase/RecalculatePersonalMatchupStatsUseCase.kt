package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.PersonalMatchupStatsStore
import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.application.port.out.StoredPlayerMatchup
import com.comfortpick.application.port.out.StoredPersonalMatchupStat
import com.comfortpick.application.port.out.UpsertPersonalMatchupStatCommand
import com.comfortpick.domain.service.RecommendationScoringInput
import com.comfortpick.domain.service.RecommendationScoringService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class RecalculatePersonalMatchupStatsUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val personalMatchupStatsStore: PersonalMatchupStatsStore,
    private val recommendationScoringService: RecommendationScoringService,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: RecalculatePersonalMatchupStatsCommand): RecalculatePersonalMatchupStatsResult {
        riotAccountStore.findById(command.summonerId)
            ?: throw SummonerProfileNotFoundException(command.summonerId)

        val matchups = personalMatchupStatsStore.findPlayerMatchupsByRiotAccountId(command.summonerId)
        val existingStats = personalMatchupStatsStore.findExistingPersonalMatchupStatsByRiotAccountId(command.summonerId)
        val now = LocalDateTime.now(clock)

        if (matchups.isEmpty()) {
            personalMatchupStatsStore.deletePersonalMatchupStats(existingStats.map(StoredPersonalMatchupStat::id))
            return RecalculatePersonalMatchupStatsResult(updatedStatCount = 0, removedStatCount = existingStats.size)
        }

        val existingStatsByKey = existingStats.associateBy { MatchupKey(it.enemyChampionId, it.userChampionId, it.role) }
        val overallChampionGames = matchups.groupingBy { it.userChampionId }.eachCount()
        val recalculatedByKey = matchups
            .groupBy { MatchupKey(it.enemyChampionId, it.userChampionId, it.role) }
            .mapValues { (key, groupedMatchups) ->
                buildStatCommand(
                    key = key,
                    matchups = groupedMatchups,
                    overallChampionGames = overallChampionGames.getValue(key.userChampionId),
                    existingStat = existingStatsByKey[key],
                    updatedAt = now,
                    riotAccountId = command.summonerId,
                )
            }

        personalMatchupStatsStore.savePersonalMatchupStats(recalculatedByKey.values)

        val staleIds = existingStats
            .filterNot { existing -> recalculatedByKey.containsKey(MatchupKey(existing.enemyChampionId, existing.userChampionId, existing.role)) }
            .map(StoredPersonalMatchupStat::id)
        personalMatchupStatsStore.deletePersonalMatchupStats(staleIds)

        return RecalculatePersonalMatchupStatsResult(
            updatedStatCount = recalculatedByKey.size,
            removedStatCount = staleIds.size,
        )
    }

    private fun buildStatCommand(
        key: MatchupKey,
        matchups: List<StoredPlayerMatchup>,
        overallChampionGames: Int,
        existingStat: StoredPersonalMatchupStat?,
        updatedAt: LocalDateTime,
        riotAccountId: UUID,
    ): UpsertPersonalMatchupStatCommand {
        val orderedMatchups = matchups.sortedByDescending(StoredPlayerMatchup::createdAt)
        val wins = orderedMatchups.count(StoredPlayerMatchup::win)
        val averageKda = orderedMatchups
            .map(::calculateKda)
            .average()
        val averageCs = orderedMatchups.mapNotNull(StoredPlayerMatchup::totalCs).map(Int::toDouble).averageOrNull()
        val averageGold = orderedMatchups.mapNotNull(StoredPlayerMatchup::goldEarned).map(Int::toDouble).averageOrNull()
        val averageDamage = orderedMatchups.mapNotNull(StoredPlayerMatchup::totalDamageToChampions).map(Int::toDouble).averageOrNull()

        val recentMatchups = orderedMatchups.take(RECENT_MATCHUP_SAMPLE_SIZE)
        val recentWins = recentMatchups.count(StoredPlayerMatchup::win)
        val recentWinrate = if (recentMatchups.isEmpty()) null else recentWins.toDouble() / recentMatchups.size.toDouble() * 100.0
        val recentKda = recentMatchups.map(::calculateKda).averageOrNull()

        val scoredCounter = recommendationScoringService.score(
            RecommendationScoringInput(
                enemyChampionId = key.enemyChampionId,
                userChampionId = key.userChampionId,
                role = key.role,
                games = orderedMatchups.size,
                wins = wins,
                overallChampionGames = overallChampionGames,
                averageKda = averageKda,
                averageCs = averageCs,
                averageGold = averageGold,
                averageDamage = averageDamage,
                recentWinrate = recentWinrate,
                recentKda = recentKda,
            ),
        )

        return UpsertPersonalMatchupStatCommand(
            id = existingStat?.id ?: UUID.randomUUID(),
            riotAccountId = riotAccountId,
            enemyChampionId = key.enemyChampionId,
            userChampionId = key.userChampionId,
            role = key.role,
            games = scoredCounter.stats.games,
            wins = scoredCounter.stats.wins,
            losses = scoredCounter.stats.losses,
            winrate = scoredCounter.stats.winrate,
            averageKda = scoredCounter.stats.averageKda,
            averageCs = scoredCounter.stats.averageCs,
            averageGold = scoredCounter.stats.averageGold,
            averageDamage = scoredCounter.stats.averageDamage,
            personalScore = scoredCounter.personalScore,
            confidence = scoredCounter.confidence,
            updatedAt = updatedAt,
        )
    }

    private fun calculateKda(matchup: StoredPlayerMatchup): Double =
        if (matchup.deaths <= 0) {
            (matchup.kills + matchup.assists).toDouble()
        } else {
            (matchup.kills + matchup.assists).toDouble() / matchup.deaths.toDouble()
        }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private data class MatchupKey(
        val enemyChampionId: Int,
        val userChampionId: Int,
        val role: String,
    )

    companion object {
        private const val RECENT_MATCHUP_SAMPLE_SIZE = 5
    }
}

data class RecalculatePersonalMatchupStatsCommand(
    val summonerId: UUID,
)

data class RecalculatePersonalMatchupStatsResult(
    val updatedStatCount: Int,
    val removedStatCount: Int,
)
