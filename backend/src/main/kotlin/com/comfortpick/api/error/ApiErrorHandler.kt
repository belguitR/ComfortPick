package com.comfortpick.api.error

import com.comfortpick.application.port.out.exception.RiotApiNotFoundException
import com.comfortpick.application.port.out.exception.RiotApiRateLimitException
import com.comfortpick.application.port.out.exception.RiotApiUnauthorizedException
import com.comfortpick.application.port.out.exception.RiotApiUnavailableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiErrorHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(exception: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        buildResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "BAD_REQUEST",
            message = exception.message ?: "Invalid request",
        )

    @ExceptionHandler(RiotApiNotFoundException::class)
    fun handleNotFound(exception: RiotApiNotFoundException): ResponseEntity<ApiErrorResponse> =
        buildResponse(
            status = HttpStatus.NOT_FOUND,
            code = "SUMMONER_NOT_FOUND",
            message = "Summoner was not found.",
        )

    @ExceptionHandler(RiotApiUnauthorizedException::class)
    fun handleUnauthorized(exception: RiotApiUnauthorizedException): ResponseEntity<ApiErrorResponse> =
        buildResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = "RIOT_API_UNAUTHORIZED",
            message = "Riot API credentials are unavailable or invalid.",
        )

    @ExceptionHandler(RiotApiRateLimitException::class)
    fun handleRateLimit(exception: RiotApiRateLimitException): ResponseEntity<ApiErrorResponse> =
        buildResponse(
            status = HttpStatus.TOO_MANY_REQUESTS,
            code = "RIOT_API_RATE_LIMIT",
            message = "Riot API rate limit reached.",
            retryAfterSeconds = exception.retryAfterSeconds,
        )

    @ExceptionHandler(RiotApiUnavailableException::class)
    fun handleUnavailable(exception: RiotApiUnavailableException): ResponseEntity<ApiErrorResponse> =
        buildResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = "RIOT_API_UNAVAILABLE",
            message = "Riot API is temporarily unavailable.",
        )

    private fun buildResponse(
        status: HttpStatus,
        code: String,
        message: String,
        retryAfterSeconds: Long? = null,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(
            ApiErrorResponse(
                code = code,
                message = message,
                retryAfterSeconds = retryAfterSeconds,
            ),
        )
}

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val retryAfterSeconds: Long? = null,
)
