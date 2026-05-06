package com.comfortpick.application.port.out

import com.comfortpick.application.port.out.model.RiotAccountSnapshot
import com.comfortpick.application.port.out.model.RiotMatchDetails
import com.comfortpick.application.port.out.model.RiotRoutingRegion

interface RiotApiPort {
    fun getAccountByRiotId(
        routingRegion: RiotRoutingRegion,
        gameName: String,
        tagLine: String,
    ): RiotAccountSnapshot

    fun getMatchIdsByPuuid(
        routingRegion: RiotRoutingRegion,
        puuid: String,
        start: Int = 0,
        count: Int = 20,
    ): List<String>

    fun getMatchDetails(
        routingRegion: RiotRoutingRegion,
        matchId: String,
    ): RiotMatchDetails
}
