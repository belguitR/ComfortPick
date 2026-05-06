package com.comfortpick.application.usecase

import com.comfortpick.infrastructure.persistence.entity.MatchEntity
import com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity
import com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.MatchRepository
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class RecalculatePersonalMatchupStatsUseCaseIntegrationTest {
    @Autowired
    private lateinit var recalculatePersonalMatchupStatsUseCase: RecalculatePersonalMatchupStatsUseCase

    @Autowired
    private lateinit var riotAccountRepository: RiotAccountRepository

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @Autowired
    private lateinit var playerMatchupRepository: PlayerMatchupRepository

    @Autowired
    private lateinit var personalMatchupStatsRepository: PersonalMatchupStatsRepository

    @BeforeEach
    fun setUp() {
        personalMatchupStatsRepository.deleteAll()
        playerMatchupRepository.deleteAll()
        matchRepository.deleteAll()
        riotAccountRepository.deleteAll()
    }

    @Test
    fun `recalculates affected summoner stats and upserts deterministically`() {
        val targetAccount = saveAccount("target-puuid", "Target")
        val otherAccount = saveAccount("other-puuid", "Other")

        saveMatchup(targetAccount, "EUW1_A1", 103, 238, "MIDDLE", true, 10, 2, 7, 220, 13200, 28000, LocalDateTime.now().minusDays(5))
        saveMatchup(targetAccount, "EUW1_A2", 103, 238, "MIDDLE", true, 8, 3, 6, 205, 12600, 25500, LocalDateTime.now().minusDays(4))
        saveMatchup(targetAccount, "EUW1_A3", 103, 238, "MIDDLE", false, 4, 5, 4, 190, 11100, 21000, LocalDateTime.now().minusDays(3))
        saveMatchup(targetAccount, "EUW1_B1", 7, 238, "MIDDLE", true, 6, 1, 9, 175, 11800, 19500, LocalDateTime.now().minusDays(2))
        saveMatchup(targetAccount, "EUW1_B2", 7, 238, "MIDDLE", false, 2, 4, 5, 160, 9800, 17000, LocalDateTime.now().minusDays(1))
        saveMatchup(otherAccount, "EUW1_C1", 84, 238, "TOP", true, 7, 2, 5, 198, 12400, 22100, LocalDateTime.now().minusDays(1))

        val existingId = UUID.randomUUID()
        val staleId = UUID.randomUUID()
        val untouchedOtherId = UUID.randomUUID()

        personalMatchupStatsRepository.save(
            PersonalMatchupStatsEntity(
                id = existingId,
                riotAccount = targetAccount,
                enemyChampionId = 238,
                userChampionId = 103,
                role = "MIDDLE",
                games = 1,
                wins = 1,
                losses = 0,
                winrate = 100.0,
                averageKda = 9.0,
                averageCs = 250.0,
                averageGold = 15000.0,
                averageDamage = 30000.0,
                personalScore = 95.0,
                confidence = "LOW",
                updatedAt = LocalDateTime.now().minusDays(10),
            ),
        )
        personalMatchupStatsRepository.save(
            PersonalMatchupStatsEntity(
                id = staleId,
                riotAccount = targetAccount,
                enemyChampionId = 555,
                userChampionId = 103,
                role = "MIDDLE",
                games = 2,
                wins = 0,
                losses = 2,
                winrate = 0.0,
                averageKda = 1.0,
                averageCs = 90.0,
                averageGold = 7000.0,
                averageDamage = 12000.0,
                personalScore = 15.0,
                confidence = "LOW",
                updatedAt = LocalDateTime.now().minusDays(10),
            ),
        )
        personalMatchupStatsRepository.save(
            PersonalMatchupStatsEntity(
                id = untouchedOtherId,
                riotAccount = otherAccount,
                enemyChampionId = 238,
                userChampionId = 84,
                role = "TOP",
                games = 1,
                wins = 1,
                losses = 0,
                winrate = 100.0,
                averageKda = 6.0,
                averageCs = 198.0,
                averageGold = 12400.0,
                averageDamage = 22100.0,
                personalScore = 70.0,
                confidence = "LOW",
                updatedAt = LocalDateTime.now().minusDays(1),
            ),
        )

        val firstResult = recalculatePersonalMatchupStatsUseCase.execute(
            RecalculatePersonalMatchupStatsCommand(targetAccount.id),
        )

        assertEquals(2, firstResult.updatedStatCount)
        assertEquals(1, firstResult.removedStatCount)

        val targetStats = personalMatchupStatsRepository.findAllByRiotAccountId(targetAccount.id)
        assertEquals(2, targetStats.size)
        assertFalse(targetStats.any { it.id == staleId })

        val ahriIntoZed = targetStats.firstOrNull {
            it.enemyChampionId == 238 && it.userChampionId == 103 && it.role == "MIDDLE"
        }
        assertNotNull(ahriIntoZed)
        assertEquals(existingId, ahriIntoZed.id)
        assertEquals(3, ahriIntoZed.games)
        assertEquals(2, ahriIntoZed.wins)
        assertEquals(1, ahriIntoZed.losses)
        assertEquals(66.7, ahriIntoZed.winrate)
        assertEquals("MEDIUM", ahriIntoZed.confidence)

        val secondResult = recalculatePersonalMatchupStatsUseCase.execute(
            RecalculatePersonalMatchupStatsCommand(targetAccount.id),
        )

        assertEquals(2, secondResult.updatedStatCount)
        assertEquals(0, secondResult.removedStatCount)
        assertEquals(2, personalMatchupStatsRepository.findAllByRiotAccountId(targetAccount.id).size)

        val otherStats = personalMatchupStatsRepository.findAllByRiotAccountId(otherAccount.id)
        assertEquals(1, otherStats.size)
        assertEquals(untouchedOtherId, otherStats.single().id)
    }

    private fun saveAccount(
        puuid: String,
        gameName: String,
    ): RiotAccountEntity =
        riotAccountRepository.save(
            RiotAccountEntity(
                puuid = puuid,
                gameName = gameName,
                tagLine = "EUW",
                region = "EUROPE",
                createdAt = LocalDateTime.now().minusDays(10),
                updatedAt = LocalDateTime.now().minusDays(1),
            ),
        )

    private fun saveMatchup(
        account: RiotAccountEntity,
        riotMatchId: String,
        userChampionId: Int,
        enemyChampionId: Int,
        role: String,
        win: Boolean,
        kills: Int,
        deaths: Int,
        assists: Int,
        totalCs: Int,
        goldEarned: Int,
        damage: Int,
        createdAt: LocalDateTime,
    ) {
        val match = matchRepository.save(
            MatchEntity(
                riotMatchId = riotMatchId,
                region = "EUW1",
                queueId = 420,
                gameCreation = createdAt,
                gameDurationSeconds = 1800,
                patch = "15.10.1",
                createdAt = createdAt,
            ),
        )

        playerMatchupRepository.save(
            PlayerMatchupEntity(
                riotAccount = account,
                match = match,
                userPuuid = account.puuid,
                userChampionId = userChampionId,
                enemyChampionId = enemyChampionId,
                role = role,
                win = win,
                kills = kills,
                deaths = deaths,
                assists = assists,
                totalCs = totalCs,
                goldEarned = goldEarned,
                totalDamageToChampions = damage,
                createdAt = createdAt,
            ),
        )
    }
}
