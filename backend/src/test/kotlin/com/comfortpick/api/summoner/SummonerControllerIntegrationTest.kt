package com.comfortpick.api.summoner

import com.comfortpick.application.port.out.RiotApiPort
import com.comfortpick.application.port.out.exception.RiotApiNotFoundException
import com.comfortpick.application.port.out.model.RiotAccountSnapshot
import com.comfortpick.application.port.out.model.RiotRoutingRegion
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import com.comfortpick.infrastructure.persistence.repository.RiotAccountRepository
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SummonerControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var riotAccountRepository: RiotAccountRepository

    @MockBean
    private lateinit var riotApiPort: RiotApiPort

    @BeforeEach
    fun setUp() {
        riotAccountRepository.deleteAll()
    }

    @Test
    fun `returns fresh summoner from database without Riot API call`() {
        riotAccountRepository.save(
            RiotAccountEntity(
                id = UUID.randomUUID(),
                puuid = "stored-puuid",
                gameName = "Rami",
                tagLine = "EUW",
                region = "EUROPE",
                createdAt = LocalDateTime.now().minusHours(2),
                updatedAt = LocalDateTime.now().minusHours(1),
            ),
        )

        mockMvc.get("/api/summoners/EUROPE/Rami/EUW")
            .andExpect {
                status { isOk() }
                jsonPath("$.puuid", equalTo("stored-puuid"))
                jsonPath("$.source", equalTo("DATABASE"))
            }

        verify(riotApiPort, never()).getAccountByRiotId(RiotRoutingRegion.EUROPE, "Rami", "EUW")
    }

    @Test
    fun `loads missing summoner from Riot API and stores it`() {
        given(
            riotApiPort.getAccountByRiotId(
                RiotRoutingRegion.EUROPE,
                "Rami",
                "EUW",
            ),
        ).willReturn(
            RiotAccountSnapshot(
                puuid = "riot-puuid",
                gameName = "Rami",
                tagLine = "EUW",
            ),
        )

        mockMvc.get("/api/summoners/EUROPE/Rami/EUW")
            .andExpect {
                status { isOk() }
                jsonPath("$.puuid", equalTo("riot-puuid"))
                jsonPath("$.source", equalTo("RIOT_API"))
                jsonPath("$.region", equalTo("EUROPE"))
            }

        val storedAccount = riotAccountRepository.findByPuuid("riot-puuid")
        checkNotNull(storedAccount)
        verify(riotApiPort).getAccountByRiotId(RiotRoutingRegion.EUROPE, "Rami", "EUW")
    }

    @Test
    fun `returns clear not found error when Riot API has no summoner`() {
        given(
            riotApiPort.getAccountByRiotId(
                RiotRoutingRegion.EUROPE,
                "Missing",
                "EUW",
            ),
        ).willThrow(RiotApiNotFoundException("not found"))

        mockMvc.get("/api/summoners/EUROPE/Missing/EUW")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code", equalTo("SUMMONER_NOT_FOUND"))
                jsonPath("$.message", equalTo("Summoner was not found."))
            }
    }
}
