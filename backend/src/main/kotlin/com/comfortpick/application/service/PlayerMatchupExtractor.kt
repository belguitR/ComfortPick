package com.comfortpick.application.service

import com.comfortpick.application.port.out.model.RiotMatchDetails
import org.springframework.stereotype.Component

@Component
class PlayerMatchupExtractor {
    fun extract(
        userPuuid: String,
        matchDetails: RiotMatchDetails,
    ): PlayerMatchupExtractionResult {
        val userParticipant = matchDetails.participants.firstOrNull { it.puuid == userPuuid }
            ?: return PlayerMatchupExtractionResult.Failure(
                reason = PlayerMatchupExtractionFailureReason.USER_PARTICIPANT_NOT_FOUND,
                message = "Imported match ${matchDetails.riotMatchId} does not contain the target summoner",
            )

        val role = resolveRole(userParticipant)
        if (role.isEmpty()) {
            return PlayerMatchupExtractionResult.Failure(
                reason = PlayerMatchupExtractionFailureReason.MISSING_ROLE,
                message = "Imported match ${matchDetails.riotMatchId} has no valid role for the target summoner",
            )
        }

        val enemyParticipant = matchDetails.participants.firstOrNull { participant ->
            participant.teamId != userParticipant.teamId && resolveRole(participant).equals(role, ignoreCase = true)
        } ?: return PlayerMatchupExtractionResult.Failure(
            reason = PlayerMatchupExtractionFailureReason.OPPONENT_NOT_FOUND,
            message = "Imported match ${matchDetails.riotMatchId} has no enemy participant for role $role",
        )

        return PlayerMatchupExtractionResult.Success(
            ExtractedPlayerMatchup(
                userPuuid = userParticipant.puuid,
                userChampionId = userParticipant.championId,
                enemyChampionId = enemyParticipant.championId,
                role = role,
                win = userParticipant.win,
                kills = userParticipant.kills,
                deaths = userParticipant.deaths,
                assists = userParticipant.assists,
                totalCs = userParticipant.totalCs,
                goldEarned = userParticipant.goldEarned,
                totalDamageToChampions = userParticipant.totalDamageToChampions,
                itemIds = userParticipant.itemIds,
                primaryRuneId = userParticipant.primaryRuneId,
                secondaryRuneId = userParticipant.secondaryRuneId,
                summonerSpell1Id = userParticipant.summonerSpell1Id,
                summonerSpell2Id = userParticipant.summonerSpell2Id,
            ),
        )
    }

    private fun resolveRole(participant: com.comfortpick.application.port.out.model.RiotMatchParticipantSnapshot): String {
        val teamPosition = normalizeRole(participant.teamPosition)
        if (teamPosition.isNotEmpty()) {
            return teamPosition
        }

        return normalizeRole(participant.individualPosition)
    }

    private fun normalizeRole(rawRole: String): String =
        when (rawRole.trim().uppercase()) {
            "TOP" -> "TOP"
            "JUNGLE" -> "JUNGLE"
            "MIDDLE", "MID" -> "MIDDLE"
            "BOTTOM", "BOT", "ADC" -> "BOTTOM"
            "UTILITY", "SUPPORT", "SUP" -> "UTILITY"
            else -> ""
        }
}

sealed interface PlayerMatchupExtractionResult {
    data class Success(
        val matchup: ExtractedPlayerMatchup,
    ) : PlayerMatchupExtractionResult

    data class Failure(
        val reason: PlayerMatchupExtractionFailureReason,
        val message: String,
    ) : PlayerMatchupExtractionResult
}

enum class PlayerMatchupExtractionFailureReason {
    USER_PARTICIPANT_NOT_FOUND,
    MISSING_ROLE,
    OPPONENT_NOT_FOUND,
}

data class ExtractedPlayerMatchup(
    val userPuuid: String,
    val userChampionId: Int,
    val enemyChampionId: Int,
    val role: String,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val totalCs: Int,
    val goldEarned: Int,
    val totalDamageToChampions: Int,
    val itemIds: List<Int>,
    val primaryRuneId: Int?,
    val secondaryRuneId: Int?,
    val summonerSpell1Id: Int?,
    val summonerSpell2Id: Int?,
)
