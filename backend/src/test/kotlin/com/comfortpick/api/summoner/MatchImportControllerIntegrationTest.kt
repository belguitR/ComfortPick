package com.comfortpick.api.summoner

import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.application.port.out.model.RiotMatchDetails
import com.comfortpick.application.port.out.model.RiotMatchParticipantSnapshot
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.application.service.PlayerMatchupExtractionFailureReason
import com.comfortpick.application.service.PlayerMatchupExtractionResult
import com.comfortpick.application.service.PlayerMatchupExtractor
import com.comfortpick.application.usecase.RecalculatePersonalMatchupStatsCommand
import com.comfortpick.application.usecase.RecalculatePersonalMatchupStatsUseCase
import com.comfortpick.infrastructure.persistence.entity.MatchEntity
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.MatchRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchImportControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var riotAccountRepository: RiotAccountRepository

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @Autowired
    private lateinit var playerMatchupRepository: PlayerMatchupRepository

    @Autowired
    private lateinit var personalMatchupStatsRepository: PersonalMatchupStatsRepository

    @MockitoBean
    private lateinit var riotApiPort: RiotApiPort

    @MockitoSpyBean
    private lateinit var playerMatchupExtractor: PlayerMatchupExtractor

    @MockitoSpyBean
    private lateinit var recalculatePersonalMatchupStatsUseCase: RecalculatePersonalMatchupStatsUseCase

    @BeforeEach
    fun setUp() {
        personalMatchupStatsRepository.deleteAll()
        playerMatchupRepository.deleteAll()
        matchRepository.deleteAll()
        riotAccountRepository.deleteAll()
    }

    @Test
    fun `imports only missing matches and skips already stored details fetch`() {
        val account = saveAccount()
        val existingMatch = matchRepository.save(
            MatchEntity(
                riotMatchId = "EUW1_EXISTING",
                region = "EUW1",
                queueId = 420,
                gameCreation = LocalDateTime.now().minusDays(1),
                gameDurationSeconds = 1800,
                patch = "15.10.1",
            ),
        )
        playerMatchupRepository.save(
            PlayerMatchupEntity(
                riotAccount = account,
                match = existingMatch,
                userPuuid = account.puuid,
                userChampionId = 103,
                enemyChampionId = 238,
                role = "MIDDLE",
                win = true,
                kills = 7,
                deaths = 2,
                assists = 5,
                totalCs = 200,
                goldEarned = 12000,
                totalDamageToChampions = 25000,
            ),
        )

        given(riotApiPort.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, account.puuid, 0, 10))
            .willReturn(listOf("EUW1_EXISTING", "EUW1_NEW"))
        given(riotApiPort.getMatchDetails(RiotRoutingRegion.EUROPE, "EUW1_NEW"))
            .willReturn(buildMatchDetails("EUW1_NEW", account.puuid))

        mockMvc.post("/api/summoners/${account.id}/matches/import")
            .andExpect {
                status { isOk() }
                jsonPath("$.importedMatchCount", equalTo(1))
                jsonPath("$.existingMatchCount", equalTo(1))
                jsonPath("$.importedMatchupCount", equalTo(1))
                jsonPath("$.skippedMatchupCount", equalTo(0))
            }

        verify(riotApiPort, never()).getMatchDetails(RiotRoutingRegion.EUROPE, "EUW1_EXISTING")
        verify(recalculatePersonalMatchupStatsUseCase)
            .execute(RecalculatePersonalMatchupStatsCommand(account.id))
        assertEquals(2, matchRepository.count())
        assertEquals(2, playerMatchupRepository.count())
        assertEquals(1, personalMatchupStatsRepository.count())
    }

    @Test
    fun `duplicate imports are safe`() {
        val account = saveAccount()
        given(riotApiPort.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, account.puuid, 0, 10))
            .willReturn(listOf("EUW1_DUPLICATE"))
        given(riotApiPort.getMatchDetails(RiotRoutingRegion.EUROPE, "EUW1_DUPLICATE"))
            .willReturn(buildMatchDetails("EUW1_DUPLICATE", account.puuid))

        mockMvc.post("/api/summoners/${account.id}/matches/import")
            .andExpect {
                status { isOk() }
                jsonPath("$.importedMatchCount", equalTo(1))
                jsonPath("$.existingMatchCount", equalTo(0))
                jsonPath("$.importedMatchupCount", equalTo(1))
                jsonPath("$.skippedMatchupCount", equalTo(0))
            }

        mockMvc.post("/api/summoners/${account.id}/matches/import")
            .andExpect {
                status { isOk() }
                jsonPath("$.importedMatchCount", equalTo(0))
                jsonPath("$.existingMatchCount", equalTo(1))
                jsonPath("$.importedMatchupCount", equalTo(0))
                jsonPath("$.skippedMatchupCount", equalTo(0))
            }

        verify(riotApiPort).getMatchDetails(RiotRoutingRegion.EUROPE, "EUW1_DUPLICATE")
        verify(recalculatePersonalMatchupStatsUseCase)
            .execute(RecalculatePersonalMatchupStatsCommand(account.id))
        assertEquals(1, matchRepository.count())
        assertEquals(1, playerMatchupRepository.count())
        assertEquals(1, personalMatchupStatsRepository.count())
    }

    @Test
    fun `keeps imported match but skips matchup row when extraction cannot determine opponent`() {
        val account = saveAccount()
        given(riotApiPort.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, account.puuid, 0, 10))
            .willReturn(listOf("EUW1_FAIL"))
        given(riotApiPort.getMatchDetails(RiotRoutingRegion.EUROPE, "EUW1_FAIL"))
            .willReturn(buildMatchDetails("EUW1_FAIL", account.puuid))
        given(playerMatchupExtractor.extract(account.puuid, buildMatchDetails("EUW1_FAIL", account.puuid)))
            .willReturn(
                PlayerMatchupExtractionResult.Failure(
                    reason = PlayerMatchupExtractionFailureReason.OPPONENT_NOT_FOUND,
                    message = "no opponent",
                ),
            )

        mockMvc.post("/api/summoners/${account.id}/matches/import")
            .andExpect {
                status { isOk() }
                jsonPath("$.importedMatchCount", equalTo(1))
                jsonPath("$.existingMatchCount", equalTo(0))
                jsonPath("$.importedMatchupCount", equalTo(0))
                jsonPath("$.skippedMatchupCount", equalTo(1))
            }

        verify(recalculatePersonalMatchupStatsUseCase, never())
            .execute(RecalculatePersonalMatchupStatsCommand(account.id))
        assertEquals(1, matchRepository.count())
        assertEquals(0, playerMatchupRepository.count())
        assertEquals(0, personalMatchupStatsRepository.count())
    }

    @Test
    fun `rolls back stored match when matchup persistence fails`() {
        val account = saveAccount()
        given(riotApiPort.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, account.puuid, 0, 10))
            .willReturn(listOf("EUW1_FAIL"))
        given(riotApiPort.getMatchDetails(RiotRoutingRegion.EUROPE, "EUW1_FAIL"))
            .willReturn(buildMatchDetails("EUW1_FAIL", account.puuid))
        doThrow(IllegalStateException("boom"))
            .`when`(playerMatchupExtractor)
            .extract(account.puuid, buildMatchDetails("EUW1_FAIL", account.puuid))

        assertFailsWith<jakarta.servlet.ServletException> {
            mockMvc.post("/api/summoners/${account.id}/matches/import")
        }

        assertEquals(0, matchRepository.count())
        assertEquals(0, playerMatchupRepository.count())
    }

    @Test
    fun `caps requested import count at max limit`() {
        val account = saveAccount()
        given(riotApiPort.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, account.puuid, 0, 20))
            .willReturn(emptyList())

        mockMvc.post("/api/summoners/${account.id}/matches/import?count=999")
            .andExpect {
                status { isOk() }
                jsonPath("$.importedMatchCount", equalTo(0))
                jsonPath("$.existingMatchCount", equalTo(0))
                jsonPath("$.importedMatchupCount", equalTo(0))
                jsonPath("$.skippedMatchupCount", equalTo(0))
            }
    }

    private fun saveAccount(): RiotAccountEntity =
        riotAccountRepository.save(
            RiotAccountEntity(
                id = UUID.randomUUID(),
                puuid = "user-puuid",
                gameName = "Rami",
                tagLine = "EUW",
                region = "EUROPE",
                createdAt = LocalDateTime.now().minusDays(1),
                updatedAt = LocalDateTime.now(),
            ),
        )

    private fun buildMatchDetails(
        matchId: String,
        userPuuid: String,
    ): RiotMatchDetails =
        RiotMatchDetails(
            riotMatchId = matchId,
            platformId = "EUW1",
            queueId = 420,
            gameCreation = Instant.parse("2026-05-06T00:00:00Z"),
            gameDurationSeconds = 1820,
            patch = "15.10.1",
            participants = listOf(
                buildParticipant(userPuuid, 103, 100, "MIDDLE"),
                buildParticipant("enemy-puuid", 238, 200, "MIDDLE"),
            ),
        )

    private fun buildParticipant(
        puuid: String,
        championId: Int,
        teamId: Int,
        teamPosition: String,
    ): RiotMatchParticipantSnapshot =
        RiotMatchParticipantSnapshot(
            puuid = puuid,
            championId = championId,
            championName = "Champion-$championId",
            teamId = teamId,
            teamPosition = teamPosition,
            win = true,
            kills = 9,
            deaths = 3,
            assists = 7,
            totalCs = 215,
            goldEarned = 13400,
            totalDamageToChampions = 27800,
            itemIds = listOf(1, 2, 3, 4, 5, 6, 7),
            primaryRuneId = 8005,
            secondaryRuneId = 8100,
        )
}
