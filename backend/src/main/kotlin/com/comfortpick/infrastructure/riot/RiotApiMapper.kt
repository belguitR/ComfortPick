package com.comfortpick.infrastructure.riot

import com.comfortpick.application.port.out.model.RiotAccountSnapshot
import com.comfortpick.application.port.out.model.RiotMatchDetails
import com.comfortpick.application.port.out.model.RiotMatchParticipantSnapshot
import com.comfortpick.infrastructure.riot.dto.RiotAccountDto
import com.comfortpick.infrastructure.riot.dto.RiotMatchDto
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RiotApiMapper {
    fun toAccountSnapshot(dto: RiotAccountDto): RiotAccountSnapshot =
        RiotAccountSnapshot(
            puuid = dto.puuid,
            gameName = dto.gameName,
            tagLine = dto.tagLine,
        )

    fun toMatchDetails(dto: RiotMatchDto): RiotMatchDetails =
        RiotMatchDetails(
            riotMatchId = dto.metadata.matchId,
            platformId = dto.info.platformId,
            queueId = dto.info.queueId,
            gameCreation = Instant.ofEpochMilli(dto.info.gameCreation),
            gameDurationSeconds = dto.info.gameDuration.toInt(),
            patch = dto.info.gameVersion.substringBefore(' '),
            participants = dto.info.participants.map(::toParticipantSnapshot),
        )

    private fun toParticipantSnapshot(participant: com.comfortpick.infrastructure.riot.dto.RiotMatchParticipantDto): RiotMatchParticipantSnapshot =
        RiotMatchParticipantSnapshot(
            puuid = participant.puuid,
            championId = participant.championId,
            championName = participant.championName,
            teamId = participant.teamId,
            teamPosition = participant.teamPosition.orEmpty(),
            individualPosition = participant.individualPosition.orEmpty(),
            win = participant.win,
            kills = participant.kills,
            deaths = participant.deaths,
            assists = participant.assists,
            totalCs = (participant.totalMinionsKilled ?: 0) + (participant.neutralMinionsKilled ?: 0),
            goldEarned = participant.goldEarned,
            totalDamageToChampions = participant.totalDamageDealtToChampions,
            itemIds = listOf(
                participant.item0,
                participant.item1,
                participant.item2,
                participant.item3,
                participant.item4,
                participant.item5,
                participant.item6,
            ),
            primaryRuneId = participant.perks
                ?.styles
                ?.firstOrNull()
                ?.selections
                ?.firstOrNull()
                ?.perk,
            secondaryRuneId = participant.perks
                ?.styles
                ?.getOrNull(1)
                ?.style,
        )
}
