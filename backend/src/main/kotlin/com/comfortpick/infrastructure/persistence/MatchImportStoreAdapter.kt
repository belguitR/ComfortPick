package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.MatchImportStore
import com.comfortpick.application.port.out.SaveMatchCommand
import com.comfortpick.application.port.out.SavePlayerMatchupCommand
import com.comfortpick.infrastructure.persistence.entity.MatchEntity
import com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity
import com.comfortpick.infrastructure.persistence.repository.MatchRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MatchImportStoreAdapter(
    private val matchRepository: MatchRepository,
    private val playerMatchupRepository: PlayerMatchupRepository,
    private val riotAccountRepository: RiotAccountRepository,
) : MatchImportStore {
    override fun findExistingMatchIds(riotMatchIds: Collection<String>): Set<String> {
        if (riotMatchIds.isEmpty()) {
            return emptySet()
        }

        return matchRepository.findAllByRiotMatchIdIn(riotMatchIds)
            .mapTo(linkedSetOf()) { it.riotMatchId }
    }

    override fun findStoredMatchIdByRiotMatchIds(riotMatchIds: Collection<String>): Map<String, UUID> {
        if (riotMatchIds.isEmpty()) {
            return emptyMap()
        }

        return matchRepository.findAllByRiotMatchIdIn(riotMatchIds)
            .associate { it.riotMatchId to it.id }
    }

    override fun findMatchIdsWithStoredMatchupForAccount(
        riotAccountId: UUID,
        riotMatchIds: Collection<String>,
    ): Set<String> {
        if (riotMatchIds.isEmpty()) {
            return emptySet()
        }

        return playerMatchupRepository.findRiotMatchIdsByRiotAccountIdAndRiotMatchIdIn(riotAccountId, riotMatchIds)
            .toSet()
    }

    override fun saveMatch(command: SaveMatchCommand): UUID =
        matchRepository.save(
            MatchEntity(
                riotMatchId = command.riotMatchId,
                region = command.region,
                queueId = command.queueId,
                gameCreation = command.gameCreation,
                gameDurationSeconds = command.gameDurationSeconds,
                patch = command.patch,
                createdAt = command.createdAt,
            ),
        ).id

    override fun savePlayerMatchup(command: SavePlayerMatchupCommand) {
        val riotAccount = riotAccountRepository.findById(command.riotAccountId)
            .orElseThrow { IllegalStateException("Riot account ${command.riotAccountId} was not found during match import") }
        val match = matchRepository.findById(command.matchId)
            .orElseThrow { IllegalStateException("Match ${command.matchId} was not found during matchup import") }

        playerMatchupRepository.save(
            PlayerMatchupEntity(
                riotAccount = riotAccount,
                match = match,
                userPuuid = command.userPuuid,
                userChampionId = command.userChampionId,
                enemyChampionId = command.enemyChampionId,
                role = command.role,
                win = command.win,
                kills = command.kills,
                deaths = command.deaths,
                assists = command.assists,
                totalCs = command.totalCs,
                goldEarned = command.goldEarned,
                totalDamageToChampions = command.totalDamageToChampions,
                item0 = command.itemIds.getOrNull(0),
                item1 = command.itemIds.getOrNull(1),
                item2 = command.itemIds.getOrNull(2),
                item3 = command.itemIds.getOrNull(3),
                item4 = command.itemIds.getOrNull(4),
                item5 = command.itemIds.getOrNull(5),
                item6 = command.itemIds.getOrNull(6),
                primaryRuneId = command.primaryRuneId,
                secondaryRuneId = command.secondaryRuneId,
                createdAt = command.createdAt,
            ),
        )
    }
}
