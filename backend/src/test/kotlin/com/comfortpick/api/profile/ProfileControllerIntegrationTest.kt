package com.comfortpick.api.profile

import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import com.comfortpick.infrastructure.persistence.repository.MatchRepository
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var riotAccountRepository: RiotAccountRepository

    @Autowired
    private lateinit var personalMatchupStatsRepository: PersonalMatchupStatsRepository

    @Autowired
    private lateinit var playerMatchupRepository: PlayerMatchupRepository

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @MockitoBean
    private lateinit var riotApiPort: RiotApiPort

    @BeforeEach
    fun setUp() {
        personalMatchupStatsRepository.deleteAll()
        playerMatchupRepository.deleteAll()
        matchRepository.deleteAll()
        riotAccountRepository.deleteAll()
    }

    @Test
    fun `returns sorted personal counters from precomputed stats only`() {
        val account = saveAccount("sorted-puuid")
        saveStat(account, enemyChampionId = 238, userChampionId = 103, games = 8, wins = 6, personalScore = 84.0, confidence = "HIGH")
        saveStat(account, enemyChampionId = 238, userChampionId = 7, games = 4, wins = 3, personalScore = 69.0, confidence = "MEDIUM")

        mockMvc.get("/api/profiles/${account.id}/enemies/238/counters")
            .andExpect {
                status { isOk() }
                jsonPath("$.counters.length()", equalTo(2))
                jsonPath("$.counters[0].userChampionId", equalTo(103))
                jsonPath("$.counters[0].status", equalTo("BEST_PICK"))
                jsonPath("$.counters[1].userChampionId", equalTo(7))
                jsonPath("$.counters[1].status", equalTo("GOOD_PICK"))
            }

        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `returns empty counters when profile has no data for enemy champion`() {
        val account = saveAccount("empty-puuid")

        mockMvc.get("/api/profiles/${account.id}/enemies/238/counters")
            .andExpect {
                status { isOk() }
                jsonPath("$.counters.length()", equalTo(0))
            }

        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `returns low data status for low confidence counter`() {
        val account = saveAccount("low-data-puuid")
        saveStat(account, enemyChampionId = 238, userChampionId = 84, games = 2, wins = 2, personalScore = 73.0, confidence = "LOW")

        mockMvc.get("/api/profiles/${account.id}/enemies/238/counters")
            .andExpect {
                status { isOk() }
                jsonPath("$.counters[0].confidence", equalTo("LOW"))
                jsonPath("$.counters[0].status", equalTo("LOW_DATA"))
            }

        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `returns avoid status for poor personal counter`() {
        val account = saveAccount("avoid-puuid")
        saveStat(account, enemyChampionId = 238, userChampionId = 55, games = 7, wins = 1, personalScore = 38.0, confidence = "HIGH")

        mockMvc.get("/api/profiles/${account.id}/enemies/238/counters")
            .andExpect {
                status { isOk() }
                jsonPath("$.counters[0].status", equalTo("AVOID"))
            }

        verifyNoInteractions(riotApiPort)
    }

    private fun saveAccount(puuid: String): RiotAccountEntity =
        riotAccountRepository.save(
            RiotAccountEntity(
                id = UUID.randomUUID(),
                puuid = puuid,
                gameName = "Rami",
                tagLine = "EUW",
                region = "EUROPE",
                createdAt = LocalDateTime.now().minusDays(7),
                updatedAt = LocalDateTime.now().minusDays(1),
            ),
        )

    private fun saveStat(
        account: RiotAccountEntity,
        enemyChampionId: Int,
        userChampionId: Int,
        games: Int,
        wins: Int,
        personalScore: Double,
        confidence: String,
    ) {
        personalMatchupStatsRepository.save(
            PersonalMatchupStatsEntity(
                riotAccount = account,
                enemyChampionId = enemyChampionId,
                userChampionId = userChampionId,
                role = "MIDDLE",
                games = games,
                wins = wins,
                losses = games - wins,
                winrate = if (games == 0) 0.0 else wins.toDouble() / games.toDouble() * 100.0,
                averageKda = 3.5,
                averageCs = 180.0,
                averageGold = 11200.0,
                averageDamage = 22000.0,
                personalScore = personalScore,
                confidence = confidence,
                updatedAt = LocalDateTime.now(),
            ),
        )
    }
}
