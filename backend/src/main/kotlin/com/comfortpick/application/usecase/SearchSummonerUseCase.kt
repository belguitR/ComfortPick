package com.comfortpick.application.usecase

import com.comfortpick.application.port.out.RiotAccountStore
import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.domain.model.RiotAccount
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class SearchSummonerUseCase(
    private val riotAccountStore: RiotAccountStore,
    private val riotApiPort: RiotApiPort,
    private val clock: Clock,
) {
    fun execute(command: SearchSummonerCommand): SearchSummonerResult {
        val routingRegion = RiotRoutingRegion.fromValue(command.region)
        val existingAccount = riotAccountStore.findByRegionAndGameNameAndTagLine(
            region = routingRegion.name,
            gameName = command.gameName,
            tagLine = command.tagLine,
        )

        if (existingAccount != null && isFresh(existingAccount.updatedAt)) {
            return SearchSummonerResult(
                account = existingAccount,
                source = SearchSummonerSource.DATABASE,
            )
        }

        val snapshot = riotApiPort.getAccountByRiotId(
            routingRegion = routingRegion,
            gameName = command.gameName,
            tagLine = command.tagLine,
        )

        val now = LocalDateTime.now(clock)
        val accountToSave = riotAccountStore.findByPuuid(snapshot.puuid)?.copy(
            gameName = snapshot.gameName,
            tagLine = snapshot.tagLine,
            region = routingRegion.name,
            updatedAt = now,
        ) ?: RiotAccount(
            id = UUID.randomUUID(),
            puuid = snapshot.puuid,
            gameName = snapshot.gameName,
            tagLine = snapshot.tagLine,
            region = routingRegion.name,
            createdAt = now,
            updatedAt = now,
        )

        return SearchSummonerResult(
            account = riotAccountStore.save(accountToSave),
            source = SearchSummonerSource.RIOT_API,
        )
    }

    private fun isFresh(updatedAt: LocalDateTime): Boolean =
        updatedAt.isAfter(LocalDateTime.now(clock).minus(FRESHNESS_WINDOW))

    companion object {
        private val FRESHNESS_WINDOW: Duration = Duration.ofHours(24)
    }
}

data class SearchSummonerCommand(
    val region: String,
    val gameName: String,
    val tagLine: String,
)

data class SearchSummonerResult(
    val account: RiotAccount,
    val source: SearchSummonerSource,
)

enum class SearchSummonerSource {
    DATABASE,
    RIOT_API,
}
