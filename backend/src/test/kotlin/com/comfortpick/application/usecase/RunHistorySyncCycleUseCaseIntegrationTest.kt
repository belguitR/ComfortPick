package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.application.port.out.model.RiotMatchDetails
import com.comfortpick.application.port.out.model.RiotMatchParticipantSnapshot
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.MatchRepository
import com.comfortpick.infrastructure.persistence.repository.PersonalMatchupStatsRepository
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class RunHistorySyncCycleUseCaseIntegrationTest {
    @Autowired
    private lateinit var runHistorySyncCycleUseCase: RunHistorySyncCycleUseCase

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

    @BeforeEach
    fun setUp() {
        personalMatchupStatsRepository.deleteAll()
        playerMatchupRepository.deleteAll()
        matchRepository.deleteAll()
        riotAccountRepository.deleteAll()
    }

    @Test
    fun `runs one sync cycle with head check and older backfill batch`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val account = riotAccountRepository.save(
            RiotAccountEntity(
                id = UUID.randomUUID(),
                puuid = "sync-puuid",
                gameName = "Rami",
                tagLine = "EUW",
                region = "EUROPE",
                createdAt = now.minusDays(7),
                updatedAt = now,
                autoSyncEnabled = true,
                syncStatus = "ACTIVE",
                syncTargetMatchCount = 500,
                syncBackfillCursor = 0,
                syncNextRunAt = now.minusSeconds(5),
            ),
        )

        val headIds = (1..10).map { "EUW1_HEAD_$it" }
        val olderIds = (11..20).map { "EUW1_OLD_$it" }
        given(riotApiPort.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, account.puuid, 0, 10))
            .willReturn(headIds)
        given(riotApiPort.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, account.puuid, 10, 10))
            .willReturn(olderIds)
        (headIds + olderIds).forEachIndexed { index, matchId ->
            given(riotApiPort.getMatchDetails(RiotRoutingRegion.EUROPE, matchId))
                .willReturn(buildMatchDetails(matchId, account.puuid, index))
        }

        runHistorySyncCycleUseCase.execute()

        val storedAccount = riotAccountRepository.findById(account.id).orElseThrow()
        assertEquals("ACTIVE", storedAccount.syncStatus)
        assertEquals(20, storedAccount.syncBackfillCursor)
        assertNotNull(storedAccount.syncLastSyncAt)
        assertNotNull(storedAccount.syncNextRunAt)
        assertEquals(20, matchRepository.count())
        assertEquals(20, playerMatchupRepository.count())
        assertEquals(20, personalMatchupStatsRepository.findAll().sumOf { it.games })
    }

    private fun buildMatchDetails(
        matchId: String,
        userPuuid: String,
        dayOffset: Int,
    ): RiotMatchDetails =
        RiotMatchDetails(
            riotMatchId = matchId,
            platformId = "EUW1",
            queueId = 420,
            gameCreation = Instant.parse("2026-05-06T00:00:00Z").plusSeconds(dayOffset.toLong()),
            gameDurationSeconds = 1820,
            patch = "15.10.1",
            participants = listOf(
                buildParticipant(userPuuid, 103, 100, "MIDDLE", true),
                buildParticipant("enemy-puuid-$dayOffset", 238, 200, "MIDDLE", false),
            ),
        )

    private fun buildParticipant(
        puuid: String,
        championId: Int,
        teamId: Int,
        teamPosition: String,
        win: Boolean,
    ): RiotMatchParticipantSnapshot =
        RiotMatchParticipantSnapshot(
            puuid = puuid,
            championId = championId,
            championName = "Champion-$championId",
            teamId = teamId,
            teamPosition = teamPosition,
            individualPosition = "",
            win = win,
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
