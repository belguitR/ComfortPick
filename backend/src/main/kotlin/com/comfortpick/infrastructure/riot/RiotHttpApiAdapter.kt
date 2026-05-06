package com.comfortpick.infrastructure.riot

import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.application.port.out.exception.RiotApiException
import com.comfortpick.application.port.out.exception.RiotApiNotFoundException
import com.comfortpick.application.port.out.exception.RiotApiRateLimitException
import com.comfortpick.application.port.out.exception.RiotApiUnauthorizedException
import com.comfortpick.application.port.out.exception.RiotApiUnavailableException
import com.comfortpick.application.port.out.model.RiotAccountSnapshot
import com.comfortpick.application.port.out.model.RiotMatchDetails
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.infrastructure.riot.config.RiotApiProperties
import com.comfortpick.infrastructure.riot.dto.RiotAccountDto
import com.comfortpick.infrastructure.riot.dto.RiotMatchDto
import com.sun.org.apache.bcel.internal.util.Args.require
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.Collections.emptyList
import java.util.Collections.emptyMap

@Component
class RiotHttpApiAdapter(
    restClientBuilder: RestClient.Builder,
    private val properties: RiotApiProperties,
    private val mapper: RiotApiMapper,
) : RiotApiPort {
    private val restClient = restClientBuilder
        .defaultHeader("X-Riot-Token", properties.key)
        .build()

    override fun getAccountByRiotId(
        routingRegion: RiotRoutingRegion,
        gameName: String,
        tagLine: String,
    ): RiotAccountSnapshot =
        execute("fetch Riot account") {
            val dto = restClient.get()
                .uri(
                    buildUri(
                        routingRegion = routingRegion,
                        path = "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}",
                        pathVariables = mapOf(
                            "gameName" to gameName,
                            "tagLine" to tagLine,
                        ),
                    ),
                )
                .retrieve()
                .body(RiotAccountDto::class.java)
                ?: throw RiotApiUnavailableException("Riot account response was empty")

            mapper.toAccountSnapshot(dto)
        }

    override fun getMatchIdsByPuuid(
        routingRegion: RiotRoutingRegion,
        puuid: String,
        start: Int,
        count: Int,
    ): List<String> =
        execute("fetch Riot match ids") {
            restClient.get()
                .uri(
                    buildUri(
                        routingRegion = routingRegion,
                        path = "/lol/match/v5/matches/by-puuid/{puuid}/ids",
                        pathVariables = mapOf("puuid" to puuid),
                        queryParameters = linkedMapOf(
                            "start" to start.toString(),
                            "count" to count.toString(),
                        ),
                    ),
                )
                .retrieve()
                .body(Array<String>::class.java)
                ?.toList()
                ?: emptyList()
        }

    override fun getMatchDetails(
        routingRegion: RiotRoutingRegion,
        matchId: String,
    ): RiotMatchDetails =
        execute("fetch Riot match details") {
            val dto = restClient.get()
                .uri(
                    buildUri(
                        routingRegion = routingRegion,
                        path = "/lol/match/v5/matches/{matchId}",
                        pathVariables = mapOf("matchId" to matchId),
                    ),
                )
                .retrieve()
                .body(RiotMatchDto::class.java)
                ?: throw RiotApiUnavailableException("Riot match response was empty")

            mapper.toMatchDetails(dto)
        }

    private fun <T> execute(
        operation: String,
        block: () -> T,
    ): T =
        try {
            require(properties.key.isNotBlank()) { "RIOT_API_KEY is not configured" }
            block()
        } catch (exception: IllegalArgumentException) {
            throw RiotApiUnauthorizedException("Riot API key is missing", exception)
        } catch (exception: HttpClientErrorException.NotFound) {
            throw RiotApiNotFoundException("Riot resource not found for $operation", exception)
        } catch (exception: HttpClientErrorException.Unauthorized) {
            throw RiotApiUnauthorizedException("Riot API unauthorized for $operation", exception)
        } catch (exception: HttpClientErrorException.Forbidden) {
            throw RiotApiUnauthorizedException("Riot API forbidden for $operation", exception)
        } catch (exception: HttpClientErrorException.TooManyRequests) {
            throw RiotApiRateLimitException(
                message = "Riot API rate limit reached for $operation",
                retryAfterSeconds = exception.responseHeaders.retryAfterSeconds(),
                cause = exception,
            )
        } catch (exception: HttpServerErrorException) {
            throw RiotApiUnavailableException("Riot API is temporarily unavailable for $operation", exception)
        } catch (exception: ResourceAccessException) {
            throw RiotApiUnavailableException("Riot API could not be reached for $operation", exception)
        } catch (exception: RiotApiException) {
            throw exception
        } catch (exception: Exception) {
            throw RiotApiException("Unexpected Riot API error during $operation", exception)
        }

    private fun buildUri(
        routingRegion: RiotRoutingRegion,
        path: String,
        pathVariables: Map<String, String>,
        queryParameters: Map<String, String> = emptyMap(),
    ): URI {
        val query = LinkedMultiValueMap<String, String>()
        queryParameters.forEach { (key, value) -> query.add(key, value) }

        return UriComponentsBuilder.newInstance()
            .scheme("https")
            .host("${routingRegion.hostPrefix}.api.riotgames.com")
            .path(path)
            .queryParams(query)
            .buildAndExpand(pathVariables)
            .encode()
            .toUri()
    }

    private fun HttpHeaders?.retryAfterSeconds(): Long? =
        this?.getFirst("Retry-After")?.toLongOrNull()
}
