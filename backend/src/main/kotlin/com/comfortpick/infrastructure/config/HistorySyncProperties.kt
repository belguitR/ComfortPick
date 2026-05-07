package com.comfortpick.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "comfortpick.sync")
data class HistorySyncProperties(
    val targetMatches: Int = 500,
    val batchSize: Int = 10,
    val schedulerInterval: Duration = Duration.ofSeconds(30),
    val dashboardPollInterval: Duration = Duration.ofSeconds(10),
    val maxAccountsPerTick: Int = 1,
    val errorRetryDelay: Duration = Duration.ofMinutes(5),
)
