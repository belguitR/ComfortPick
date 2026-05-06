package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.PersonalMatchupStatsStore
import com.comfortpick.application.port.out.StoredPlayerMatchup
import com.comfortpick.application.port.out.StoredPersonalMatchupStat
import com.comfortpick.application.port.out.UpsertPersonalMatchupStatCommand
import com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PersonalMatchupStatsStoreAdapter(
    private val playerMatchupRepository: PlayerMatchupRepository,
    private val personalMatchupStatsRepository: PersonalMatchupStatsRepository,
    private val riotAccountRepository: RiotAccountRepository,
) : PersonalMatchupStatsStore {
    override fun findPlayerMatchupsByRiotAccountId(riotAccountId: UUID): List<StoredPlayerMatchup> =
        playerMatchupRepository.findAllByRiotAccountId(riotAccountId).map { matchup ->
            StoredPlayerMatchup(
                riotAccountId = matchup.riotAccount.id,
                userChampionId = matchup.userChampionId,
                enemyChampionId = matchup.enemyChampionId,
                role = matchup.role,
                win = matchup.win,
                kills = matchup.kills,
                deaths = matchup.deaths,
                assists = matchup.assists,
                totalCs = matchup.totalCs,
                goldEarned = matchup.goldEarned,
                totalDamageToChampions = matchup.totalDamageToChampions,
                createdAt = matchup.createdAt,
            )
        }

    override fun findExistingPersonalMatchupStatsByRiotAccountId(riotAccountId: UUID): List<StoredPersonalMatchupStat> =
        personalMatchupStatsRepository.findAllByRiotAccountId(riotAccountId).map { stat ->
            StoredPersonalMatchupStat(
                id = stat.id,
                riotAccountId = stat.riotAccount.id,
                enemyChampionId = stat.enemyChampionId,
                userChampionId = stat.userChampionId,
                role = stat.role,
            )
        }

    override fun savePersonalMatchupStats(commands: Collection<UpsertPersonalMatchupStatCommand>) {
        if (commands.isEmpty()) {
            return
        }

        val accountIds = commands.map(UpsertPersonalMatchupStatCommand::riotAccountId).distinct()
        require(accountIds.size == 1) { "Stats upsert must target exactly one riot account" }

        val riotAccount = riotAccountRepository.findById(accountIds.single())
            .orElseThrow { IllegalStateException("Riot account ${accountIds.single()} was not found during stats recalculation") }

        personalMatchupStatsRepository.saveAll(
            commands.map { command ->
                PersonalMatchupStatsEntity(
                    id = command.id,
                    riotAccount = riotAccount,
                    enemyChampionId = command.enemyChampionId,
                    userChampionId = command.userChampionId,
                    role = command.role,
                    games = command.games,
                    wins = command.wins,
                    losses = command.losses,
                    winrate = command.winrate,
                    averageKda = command.averageKda,
                    averageCs = command.averageCs,
                    averageGold = command.averageGold,
                    averageDamage = command.averageDamage,
                    personalScore = command.personalScore,
                    confidence = command.confidence.name,
                    updatedAt = command.updatedAt,
                )
            },
        )
    }

    override fun deletePersonalMatchupStats(ids: Collection<UUID>) {
        if (ids.isEmpty()) {
            return
        }

        personalMatchupStatsRepository.deleteAllByIdInBatch(ids)
    }
}
