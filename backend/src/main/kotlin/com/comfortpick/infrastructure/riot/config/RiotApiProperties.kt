package com.comfortpick.infrastructure.riot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "riot.api")
data class RiotApiProperties(
    val key: String,
    val timeout: Duration = Duration.ofSeconds(5),
)
