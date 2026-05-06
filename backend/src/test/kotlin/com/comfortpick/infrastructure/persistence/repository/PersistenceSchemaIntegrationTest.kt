package com.comfortpick.infrastructure.persistence.repository

import com.comfortpick.infrastructure.persistence.entity.MatchEntity
import com.comfortpick.infrastructure.persistence.entity.PersonalBuildStatsEntity
import com.comfortpick.infrastructure.persistence.entity.PersonalMatchupStatsEntity
import com.comfortpick.infrastructure.persistence.entity.PlayerMatchupEntity
import com.comfortpick.infrastructure.persistence.entity.RiotAccountEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceSchemaIntegrationTest {
    @Autowired
    private lateinit var riotAccountRepository: RiotAccountRepository

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @Autowired
    private lateinit var playerMatchupRepository: PlayerMatchupRepository

    @Autowired
    private lateinit var personalMatchupStatsRepository: PersonalMatchupStatsRepository

    @Autowired
    private lateinit var personalBuildStatsRepository: PersonalBuildStatsRepository

    @Test
    fun `flyway creates schema and repositories persist linked records`() {
        val account = riotAccountRepository.save(
            RiotAccountEntity(
                puuid = "puuid-1",
                gameName = "Rami",
                tagLine = "EUW",
                region = "EUW1",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ),
        )
        val match = matchRepository.save(
            MatchEntity(
                riotMatchId = "EUW1_123",
                region = "EUW1",
                queueId = 420,
                gameCreation = LocalDateTime.now(),
                gameDurationSeconds = 1800,
                patch = "15.10",
                createdAt = LocalDateTime.now(),
            ),
        )

        playerMatchupRepository.save(
            PlayerMatchupEntity(
                riotAccount = account,
                match = match,
                userPuuid = account.puuid,
                userChampionId = 103,
                enemyChampionId = 238,
                role = "MIDDLE",
                win = true,
                kills = 9,
                deaths = 2,
                assists = 8,
                totalCs = 210,
                goldEarned = 12600,
                totalDamageToChampions = 27000,
                item0 = 6655,
                item1 = 3157,
                primaryRuneId = 8112,
                secondaryRuneId = 8473,
                createdAt = LocalDateTime.now(),
            ),
        )
        personalMatchupStatsRepository.save(
            PersonalMatchupStatsEntity(
                riotAccount = account,
                enemyChampionId = 238,
                userChampionId = 103,
                role = "MIDDLE",
                games = 7,
                wins = 5,
                losses = 2,
                winrate = 71.4,
                averageKda = 4.1,
                averageCs = 204.0,
                averageGold = 12300.0,
                averageDamage = 26500.0,
                personalScore = 86.0,
                confidence = "HIGH",
                updatedAt = LocalDateTime.now(),
            ),
        )
        personalBuildStatsRepository.save(
            PersonalBuildStatsEntity(
                riotAccount = account,
                enemyChampionId = 238,
                userChampionId = 103,
                role = "MIDDLE",
                itemSequence = "6655>3157>4645",
                games = 4,
                wins = 3,
                winrate = 75.0,
                buildScore = 82.0,
                updatedAt = LocalDateTime.now(),
            ),
        )

        val lookup = personalMatchupStatsRepository
            .findAllByRiotAccountIdAndEnemyChampionIdOrderByPersonalScoreDesc(account.id, 238)

        assertEquals(1, playerMatchupRepository.findAllByRiotAccountId(account.id).size)
        assertEquals(1, lookup.size)
        assertEquals(86.0, lookup.single().personalScore)
        assertNotNull(matchRepository.findByRiotMatchId("EUW1_123"))
    }

    @Test
    fun `duplicate riot match ids are rejected`() {
        matchRepository.save(
            MatchEntity(
                riotMatchId = "EUW1_DUPLICATE",
                region = "EUW1",
                queueId = 420,
                gameCreation = LocalDateTime.now(),
                gameDurationSeconds = 1800,
                patch = "15.10",
                createdAt = LocalDateTime.now(),
            ),
        )

        assertThrows<DataIntegrityViolationException> {
            matchRepository.saveAndFlush(
                MatchEntity(
                    riotMatchId = "EUW1_DUPLICATE",
                    region = "EUW1",
                    queueId = 450,
                    gameCreation = LocalDateTime.now(),
                    gameDurationSeconds = 1500,
                    patch = "15.10",
                    createdAt = LocalDateTime.now(),
                ),
            )
        }
    }

    @Test
    fun `duplicate puuids are rejected`() {
        riotAccountRepository.save(
            RiotAccountEntity(
                puuid = "puuid-duplicate",
                gameName = "Rami",
                tagLine = "EUW",
                region = "EUW1",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ),
        )

        assertThrows<DataIntegrityViolationException> {
            riotAccountRepository.saveAndFlush(
                RiotAccountEntity(
                    puuid = "puuid-duplicate",
                    gameName = "Another",
                    tagLine = "EUW",
                    region = "EUW1",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.url", postgres::getJdbcUrl)
            registry.add("spring.flyway.user", postgres::getUsername)
            registry.add("spring.flyway.password", postgres::getPassword)
            registry.add("spring.data.redis.repositories.enabled") { false }
        }
    }
}
