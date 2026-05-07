package com.comfortpick.application.service

import com.comfortpick.application.port.out.model.RiotMatchDetails
import com.comfortpick.application.port.out.model.RiotMatchParticipantSnapshot
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlayerMatchupExtractorTest {
    private val extractor = PlayerMatchupExtractor()

    @Test
    fun `extracts mid against mid matchup`() {
        val result = extractor.extract(
            userPuuid = "user-puuid",
            matchDetails = buildMatchDetails(
                participants = listOf(
                    buildParticipant("user-puuid", 103, 100, "MIDDLE"),
                    buildParticipant("enemy-puuid", 238, 200, "MIDDLE"),
                ),
            ),
        )

        val success = assertIs<PlayerMatchupExtractionResult.Success>(result)
        assertEquals(103, success.matchup.userChampionId)
        assertEquals(238, success.matchup.enemyChampionId)
        assertEquals("MIDDLE", success.matchup.role)
    }

    @Test
    fun `extracts top jungle bottom and support same-role opponents`() {
        assertSuccessfulRole("TOP")
        assertSuccessfulRole("JUNGLE")
        assertSuccessfulRole("BOTTOM")
        assertSuccessfulRole("UTILITY")
    }

    @Test
    fun `falls back to individual position when team position is blank`() {
        val result = extractor.extract(
            userPuuid = "user-puuid",
            matchDetails = buildMatchDetails(
                participants = listOf(
                    buildParticipant("user-puuid", 103, 100, " ", "MIDDLE"),
                    buildParticipant("enemy-puuid", 238, 200, "", "MIDDLE"),
                ),
            ),
        )

        val success = assertIs<PlayerMatchupExtractionResult.Success>(result)
        assertEquals("MIDDLE", success.matchup.role)
        assertEquals(238, success.matchup.enemyChampionId)
    }

    @Test
    fun `returns missing role when user team position is blank`() {
        val result = extractor.extract(
            userPuuid = "user-puuid",
            matchDetails = buildMatchDetails(
                participants = listOf(
                    buildParticipant("user-puuid", 103, 100, " "),
                    buildParticipant("enemy-puuid", 238, 200, "MIDDLE"),
                ),
            ),
        )

        val failure = assertIs<PlayerMatchupExtractionResult.Failure>(result)
        assertEquals(PlayerMatchupExtractionFailureReason.MISSING_ROLE, failure.reason)
    }

    @Test
    fun `returns opponent not found when same role enemy is missing`() {
        val result = extractor.extract(
            userPuuid = "user-puuid",
            matchDetails = buildMatchDetails(
                participants = listOf(
                    buildParticipant("user-puuid", 103, 100, "MIDDLE"),
                    buildParticipant("enemy-puuid", 238, 200, "TOP"),
                ),
            ),
        )

        val failure = assertIs<PlayerMatchupExtractionResult.Failure>(result)
        assertEquals(PlayerMatchupExtractionFailureReason.OPPONENT_NOT_FOUND, failure.reason)
    }

    private fun buildMatchDetails(
        participants: List<RiotMatchParticipantSnapshot>,
    ): RiotMatchDetails =
        RiotMatchDetails(
            riotMatchId = "EUW1_TEST",
            platformId = "EUW1",
            queueId = 420,
            gameCreation = Instant.parse("2026-05-06T00:00:00Z"),
            gameDurationSeconds = 1800,
            patch = "15.10.1",
            participants = participants,
        )

    private fun buildParticipant(
        puuid: String,
        championId: Int,
        teamId: Int,
        teamPosition: String,
        individualPosition: String = "",
    ): RiotMatchParticipantSnapshot =
        RiotMatchParticipantSnapshot(
            puuid = puuid,
            championId = championId,
            championName = "Champion-$championId",
            teamId = teamId,
            teamPosition = teamPosition,
            individualPosition = individualPosition,
            win = true,
            kills = 8,
            deaths = 2,
            assists = 5,
            totalCs = 210,
            goldEarned = 13000,
            totalDamageToChampions = 26000,
            itemIds = listOf(1, 2, 3, 4, 5, 6, 7),
            primaryRuneId = 8005,
            secondaryRuneId = 8100,
        )

    private fun assertSuccessfulRole(role: String) {
        val result = extractor.extract(
            userPuuid = "user-puuid",
            matchDetails = buildMatchDetails(
                participants = listOf(
                    buildParticipant("user-puuid", 103, 100, role),
                    buildParticipant("enemy-puuid", 238, 200, role),
                ),
            ),
        )

        val success = assertIs<PlayerMatchupExtractionResult.Success>(result)
        assertEquals(role, success.matchup.role)
    }
}
