package com.comfortpick.infrastructure.riot

import com.comfortpick.application.port.out.exception.RiotApiNotFoundException
import com.comfortpick.application.port.out.exception.RiotApiRateLimitException
import com.comfortpick.application.port.out.exception.RiotApiUnauthorizedException
import com.comfortpick.application.port.out.exception.RiotApiUnavailableException
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.infrastructure.riot.config.RiotApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class RiotHttpApiAdapterTest {
    private val restClientBuilder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val adapter = RiotHttpApiAdapter(
        restClientBuilder = restClientBuilder,
        properties = RiotApiProperties(key = "riot-test-key"),
        mapper = RiotApiMapper(),
    )

    @Test
    fun `fetches Riot account by Riot id`() {
        server.expect(requestTo("https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Rami/EUW"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Riot-Token", "riot-test-key"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """
                        {
                          "puuid": "puuid-123",
                          "gameName": "Rami",
                          "tagLine": "EUW"
                        }
                        """.trimIndent(),
                    ),
            )

        val account = adapter.getAccountByRiotId(RiotRoutingRegion.EUROPE, "Rami", "EUW")

        assertEquals("puuid-123", account.puuid)
        assertEquals("Rami", account.gameName)
        assertEquals("EUW", account.tagLine)
    }

    @Test
    fun `fetches Riot match ids by puuid`() {
        server.expect(requestTo("https://europe.api.riotgames.com/lol/match/v5/matches/by-puuid/puuid-123/ids?start=0&count=2"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""["EUW1_1","EUW1_2"]"""),
            )

        val matchIds = adapter.getMatchIdsByPuuid(RiotRoutingRegion.EUROPE, "puuid-123", 0, 2)

        assertEquals(listOf("EUW1_1", "EUW1_2"), matchIds)
    }

    @Test
    fun `fetches Riot match details and maps participant data`() {
        server.expect(requestTo("https://europe.api.riotgames.com/lol/match/v5/matches/EUW1_1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """
                        {
                          "metadata": {
                            "dataVersion": "2",
                            "matchId": "EUW1_1",
                            "participants": ["puuid-123"]
                          },
                          "info": {
                            "gameCreation": 1710000000000,
                            "gameDuration": 1815,
                            "gameVersion": "15.10.123.456",
                            "platformId": "EUW1",
                            "queueId": 420,
                            "participants": [
                              {
                                "puuid": "puuid-123",
                                "championId": 103,
                                "championName": "Ahri",
                                "teamId": 100,
                                "teamPosition": "MIDDLE",
                                "win": true,
                                "kills": 9,
                                "deaths": 2,
                                "assists": 8,
                                "totalMinionsKilled": 180,
                                "neutralMinionsKilled": 12,
                                "goldEarned": 12600,
                                "totalDamageDealtToChampions": 27000,
                                "item0": 6655,
                                "item1": 3157,
                                "item2": 4645,
                                "item3": 3020,
                                "item4": 3135,
                                "item5": 0,
                                "item6": 3363,
                                "perks": {
                                  "styles": [
                                    {
                                      "style": 8100,
                                      "selections": [
                                        { "perk": 8112 }
                                      ]
                                    },
                                    {
                                      "style": 8400,
                                      "selections": []
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
            )

        val match = adapter.getMatchDetails(RiotRoutingRegion.EUROPE, "EUW1_1")

        assertEquals("EUW1_1", match.riotMatchId)
        assertEquals("15.10.123.456", match.patch)
        assertEquals(1, match.participants.size)
        assertEquals(192, match.participants.single().totalCs)
        assertEquals(8112, match.participants.single().primaryRuneId)
        assertEquals(8400, match.participants.single().secondaryRuneId)
    }

    @Test
    fun `maps 404 to not found exception`() {
        server.expect(requestTo("https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Missing/EUW"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertThrows(RiotApiNotFoundException::class.java) {
            adapter.getAccountByRiotId(RiotRoutingRegion.EUROPE, "Missing", "EUW")
        }
    }

    @Test
    fun `maps 401 to unauthorized exception`() {
        server.expect(requestTo("https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Rami/EUW"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        assertThrows(RiotApiUnauthorizedException::class.java) {
            adapter.getAccountByRiotId(RiotRoutingRegion.EUROPE, "Rami", "EUW")
        }
    }

    @Test
    fun `maps 429 to rate limit exception with retry after`() {
        server.expect(requestTo("https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Rami/EUW"))
            .andRespond(
                withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "10"),
            )

        val exception = assertThrows(RiotApiRateLimitException::class.java) {
            adapter.getAccountByRiotId(RiotRoutingRegion.EUROPE, "Rami", "EUW")
        }

        assertEquals(10L, exception.retryAfterSeconds)
    }

    @Test
    fun `maps 503 to unavailable exception`() {
        server.expect(requestTo("https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Rami/EUW"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertThrows(RiotApiUnavailableException::class.java) {
            adapter.getAccountByRiotId(RiotRoutingRegion.EUROPE, "Rami", "EUW")
        }
    }
}
