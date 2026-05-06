package com.comfortpick.api.summoner

import com.comfortpick.application.usecase.SearchSummonerCommand
import com.comfortpick.application.usecase.SearchSummonerSource
import com.comfortpick.application.usecase.SearchSummonerUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/summoners")
class SummonerController(
    private val searchSummonerUseCase: SearchSummonerUseCase,
) {
    @GetMapping("/{region}/{gameName}/{tagLine}")
    fun searchSummoner(
        @PathVariable region: String,
        @PathVariable gameName: String,
        @PathVariable tagLine: String,
    ): SummonerSearchResponse {
        val result = searchSummonerUseCase.execute(
            SearchSummonerCommand(
                region = region,
                gameName = gameName,
                tagLine = tagLine,
            ),
        )

        return SummonerSearchResponse(
            id = result.account.id,
            puuid = result.account.puuid,
            gameName = result.account.gameName,
            tagLine = result.account.tagLine,
            region = result.account.region,
            source = result.source,
        )
    }
}

data class SummonerSearchResponse(
    val id: UUID,
    val puuid: String,
    val gameName: String,
    val tagLine: String,
    val region: String,
    val source: SearchSummonerSource,
)
