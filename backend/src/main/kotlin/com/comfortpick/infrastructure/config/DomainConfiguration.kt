package com.comfortpick.infrastructure.config

import com.comfortpick.domain.service.RecommendationScoringService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainConfiguration {
    @Bean
    fun recommendationScoringService(): RecommendationScoringService = RecommendationScoringService()
}
