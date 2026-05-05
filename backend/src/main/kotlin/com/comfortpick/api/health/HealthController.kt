package com.comfortpick.api.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class HealthController {
    @GetMapping
    fun getHealth(): HealthResponse =
        HealthResponse(
            status = "UP",
            service = "comfortpick-backend",
        )
}

data class HealthResponse(
    val status: String,
    val service: String,
)
