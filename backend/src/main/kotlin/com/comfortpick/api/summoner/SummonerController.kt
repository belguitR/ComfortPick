package com.comfortpick.api.summoner

import com.comfortpick.application.usecase.SearchSummonerCommand
import com.comfortpick.application.usecase.ImportMatchHistoryCommand
import com.comfortpick.application.usecase.ImportMatchHistoryUseCase
import com.comfortpick.application.usecase.SearchSummonerSource
import com.comfortpick.application.usecase.SearchSummonerUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/summoners")
class SummonerController(
    private val searchSummonerUseCase: SearchSummonerUseCase,
    private val importMatchHistoryUseCase: ImportMatchHistoryUseCase,
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

    @PostMapping("/{summonerId}/matches/import")
    fun importMatchHistory(
        @PathVariable summonerId: UUID,
        @RequestParam(required = false) start: Int?,
        @RequestParam(required = false) count: Int?,
    ): MatchImportResponse {
        val result = importMatchHistoryUseCase.execute(
            ImportMatchHistoryCommand(
                summonerId = summonerId,
                matchStart = start ?: 0,
                matchCount = count ?: ImportMatchHistoryUseCase.DEFAULT_IMPORT_MATCH_COUNT,
            ),
        )

        return MatchImportResponse(
            fetchedMatchCount = result.fetchedMatchCount,
            importedMatchCount = result.importedMatchCount,
            existingMatchCount = result.existingMatchCount,
            importedMatchupCount = result.importedMatchupCount,
            skippedMatchupCount = result.skippedMatchupCount,
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

data class MatchImportResponse(
    val fetchedMatchCount: Int,
    val importedMatchCount: Int,
    val existingMatchCount: Int,
    val importedMatchupCount: Int,
    val skippedMatchupCount: Int,
)
