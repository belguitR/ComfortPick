package com.comfortpick.api.profile

import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.infrastructure.persistence.entity.MatchEntity
import com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity
import com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.MatchRepository
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.hamcrest.Matchers.startsWith
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
import org.springframework.test.web.servlet.post
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

    @Test
    fun `returns populated profile dashboard from stored data only`() {
        val account = saveAccount("dashboard-puuid")
        val now = LocalDateTime.now()
        val firstMatch = saveMatch("EUW1_1", now.minusDays(3))
        val secondMatch = saveMatch("EUW1_2", now.minusDays(2))
        val thirdMatch = saveMatch("EUW1_3", now.minusDays(1))

        savePlayerMatchup(account, firstMatch, userChampionId = 103, enemyChampionId = 238, role = "MIDDLE", createdAt = now.minusDays(3))
        savePlayerMatchup(account, secondMatch, userChampionId = 103, enemyChampionId = 84, role = "MIDDLE", createdAt = now.minusDays(2))
        savePlayerMatchup(account, thirdMatch, userChampionId = 7, enemyChampionId = 238, role = "TOP", createdAt = now.minusDays(1))

        saveStat(account, enemyChampionId = 238, userChampionId = 103, role = "MIDDLE", games = 8, wins = 6, personalScore = 84.0, confidence = "HIGH", updatedAt = now.minusHours(12))
        saveStat(account, enemyChampionId = 84, userChampionId = 7, role = "TOP", games = 5, wins = 1, personalScore = 42.0, confidence = "MEDIUM", updatedAt = now.minusHours(8))
        saveStat(account, enemyChampionId = 126, userChampionId = 55, role = "BOTTOM", games = 4, wins = 3, personalScore = 67.0, confidence = "MEDIUM", updatedAt = now.minusHours(6))

        mockMvc.get("/api/profiles/${account.id}")
            .andExpect {
                status { isOk() }
                jsonPath("$.summoner.id", equalTo(account.id.toString()))
                jsonPath("$.summoner.gameName", equalTo("Rami"))
                jsonPath("$.analyzedMatches", equalTo(3))
                jsonPath("$.mainRole", equalTo("MIDDLE"))
                jsonPath("$.mostPlayedChampions", hasSize<Any>(2))
                jsonPath("$.mostPlayedChampions[0].championId", equalTo(103))
                jsonPath("$.mostPlayedChampions[0].games", equalTo(2))
                jsonPath("$.bestCounters", hasSize<Any>(3))
                jsonPath("$.bestCounters[0].userChampionId", equalTo(103))
                jsonPath("$.bestCounters[0].personalScore", equalTo(84.0))
                jsonPath("$.worstMatchups", hasSize<Any>(3))
                jsonPath("$.worstMatchups[0].userChampionId", equalTo(7))
                jsonPath("$.worstMatchups[0].personalScore", equalTo(42.0))
                jsonPath("$.lastUpdateAt", startsWith(now.minusHours(6).toLocalDate().toString()))
                jsonPath("$.sync.status", equalTo("IDLE"))
                jsonPath("$.sync.targetMatchCount", equalTo(500))
                jsonPath("$.sync.backfillCursor", equalTo(0))
            }

        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `returns empty profile dashboard when summoner has no stored analysis yet`() {
        val account = saveAccount("dashboard-empty-puuid")

        mockMvc.get("/api/profiles/${account.id}")
            .andExpect {
                status { isOk() }
                jsonPath("$.summoner.id", equalTo(account.id.toString()))
                jsonPath("$.analyzedMatches", equalTo(0))
                jsonPath("$.mainRole", nullValue())
                jsonPath("$.mostPlayedChampions", hasSize<Any>(0))
                jsonPath("$.bestCounters", hasSize<Any>(0))
                jsonPath("$.worstMatchups", hasSize<Any>(0))
                jsonPath("$.lastUpdateAt", nullValue())
                jsonPath("$.sync.status", equalTo("IDLE"))
            }

        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `queues profile sync without calling Riot immediately`() {
        val account = saveAccount("sync-request-puuid")

        mockMvc.post("/api/profiles/${account.id}/sync")
            .andExpect {
                status { isOk() }
                jsonPath("$.summonerId", equalTo(account.id.toString()))
                jsonPath("$.status", equalTo("ACTIVE"))
                jsonPath("$.targetMatchCount", equalTo(500))
                jsonPath("$.backfillCursor", equalTo(0))
            }

        val storedAccount = riotAccountRepository.findById(account.id).orElseThrow()
        kotlin.test.assertEquals(true, storedAccount.autoSyncEnabled)
        kotlin.test.assertEquals("ACTIVE", storedAccount.syncStatus)
        kotlin.test.assertNotNull(storedAccount.syncNextRunAt)
        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `sync request resets poisoned backfill cursor when analyzed coverage is too low`() {
        val account = riotAccountRepository.save(
            RiotAccountEntity(
                id = UUID.randomUUID(),
                puuid = "repair-sync-puuid",
                gameName = "Rami",
                tagLine = "EUW",
                region = "EUROPE",
                createdAt = LocalDateTime.now().minusDays(7),
                updatedAt = LocalDateTime.now().minusDays(1),
                autoSyncEnabled = true,
                syncStatus = "ACTIVE",
                syncTargetMatchCount = 500,
                syncBackfillCursor = 150,
            ),
        )
        val match = saveMatch("EUW1_REPAIR_1", LocalDateTime.now().minusDays(1))
        repeat(40) { index ->
            savePlayerMatchup(
                account = account,
                match = if (index == 0) match else saveMatch("EUW1_REPAIR_${index + 2}", LocalDateTime.now().minusDays(1).minusMinutes(index.toLong())),
                userChampionId = 103,
                enemyChampionId = 238,
                role = "MIDDLE",
                createdAt = LocalDateTime.now().minusDays(1).minusMinutes(index.toLong()),
            )
        }

        mockMvc.post("/api/profiles/${account.id}/sync")
            .andExpect {
                status { isOk() }
                jsonPath("$.backfillCursor", equalTo(0))
            }

        val storedAccount = riotAccountRepository.findById(account.id).orElseThrow()
        kotlin.test.assertEquals(0, storedAccount.syncBackfillCursor)
        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `returns matchup detail with newest recent games first`() {
        val account = saveAccount("detail-puuid")
        val now = LocalDateTime.now()
        val topMatch = saveMatch("EUW1_TOP", now.minusDays(5))
        val middleMatchOne = saveMatch("EUW1_MID_1", now.minusDays(4))
        val middleMatchTwo = saveMatch("EUW1_MID_2", now.minusDays(1))

        savePlayerMatchup(account, topMatch, userChampionId = 103, enemyChampionId = 238, role = "TOP", createdAt = now.minusDays(5), win = false, kills = 1, deaths = 5, assists = 2)
        savePlayerMatchup(account, middleMatchOne, userChampionId = 103, enemyChampionId = 238, role = "MIDDLE", createdAt = now.minusDays(4), win = true, kills = 9, deaths = 2, assists = 7, item0 = 6655, item1 = 3020, primaryRuneId = 8112, secondaryRuneId = 8226)
        savePlayerMatchup(account, middleMatchTwo, userChampionId = 103, enemyChampionId = 238, role = "MIDDLE", createdAt = now.minusDays(1), win = false, kills = 3, deaths = 4, assists = 5, item0 = 3157, item1 = 3020, primaryRuneId = 8214, secondaryRuneId = 8226)

        saveStat(account, enemyChampionId = 238, userChampionId = 103, role = "TOP", games = 3, wins = 1, personalScore = 41.0, confidence = "MEDIUM", updatedAt = now.minusHours(9))
        saveStat(account, enemyChampionId = 238, userChampionId = 103, role = "MIDDLE", games = 8, wins = 6, personalScore = 84.0, confidence = "HIGH", updatedAt = now.minusHours(2))

        mockMvc.get("/api/profiles/${account.id}/enemies/238/counters/103")
            .andExpect {
                status { isOk() }
                jsonPath("$.hasData", equalTo(true))
                jsonPath("$.role", equalTo("MIDDLE"))
                jsonPath("$.games", equalTo(8))
                jsonPath("$.status", equalTo("BEST_PICK"))
                jsonPath("$.reasoning", equalTo("Strong personal matchup: 8 games, 75.0% win rate, 3.5 KDA, and high confidence."))
                jsonPath("$.recentGames", hasSize<Any>(2))
                jsonPath("$.recentGames[0].riotMatchId", equalTo("EUW1_MID_2"))
                jsonPath("$.recentGames[1].riotMatchId", equalTo("EUW1_MID_1"))
                jsonPath("$.build.firstCompletedItemId", equalTo(6655))
                jsonPath("$.build.itemSet", equalTo("6655>3020>3100>3089>4645>3135"))
                jsonPath("$.runes.primaryRuneId", equalTo(8112))
                jsonPath("$.runes.secondaryRuneId", equalTo(8226))
            }

        verifyNoInteractions(riotApiPort)
    }

    @Test
    fun `returns clear no data matchup detail when pair has no stored history`() {
        val account = saveAccount("detail-empty-puuid")

        mockMvc.get("/api/profiles/${account.id}/enemies/238/counters/103")
            .andExpect {
                status { isOk() }
                jsonPath("$.hasData", equalTo(false))
                jsonPath("$.status", equalTo("NO_DATA"))
                jsonPath("$.reasoning", equalTo("No personal data yet for this champion matchup."))
                jsonPath("$.recentGames", hasSize<Any>(0))
                jsonPath("$.build.firstCompletedItemId", nullValue())
                jsonPath("$.runes.primaryRuneId", nullValue())
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
        role: String = "MIDDLE",
        games: Int,
        wins: Int,
        personalScore: Double,
        confidence: String,
        updatedAt: LocalDateTime = LocalDateTime.now(),
    ) {
        personalMatchupStatsRepository.save(
            PersonalMatchupStatsEntity(
                riotAccount = account,
                enemyChampionId = enemyChampionId,
                userChampionId = userChampionId,
                role = role,
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
                updatedAt = updatedAt,
            ),
        )
    }

    private fun saveMatch(
        riotMatchId: String,
        gameCreation: LocalDateTime,
    ): MatchEntity =
        matchRepository.save(
            MatchEntity(
                riotMatchId = riotMatchId,
                region = "EUROPE",
                queueId = 420,
                gameCreation = gameCreation,
                gameDurationSeconds = 1800,
                patch = "15.10",
                createdAt = gameCreation,
            ),
        )

    private fun savePlayerMatchup(
        account: RiotAccountEntity,
        match: MatchEntity,
        userChampionId: Int,
        enemyChampionId: Int,
        role: String,
        createdAt: LocalDateTime,
        win: Boolean = true,
        kills: Int = 8,
        deaths: Int = 2,
        assists: Int = 6,
        item0: Int? = 1056,
        item1: Int? = 3020,
        item2: Int? = 3100,
        item3: Int? = 3089,
        item4: Int? = 4645,
        item5: Int? = 3135,
        item6: Int? = 3363,
        primaryRuneId: Int? = 8112,
        secondaryRuneId: Int? = 8226,
    ) {
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
                totalCs = 185,
                goldEarned = 11800,
                totalDamageToChampions = 24000,
                item0 = item0,
                item1 = item1,
                item2 = item2,
                item3 = item3,
                item4 = item4,
                item5 = item5,
                item6 = item6,
                primaryRuneId = primaryRuneId,
                secondaryRuneId = secondaryRuneId,
                createdAt = createdAt,
            ),
        )
    }
}
