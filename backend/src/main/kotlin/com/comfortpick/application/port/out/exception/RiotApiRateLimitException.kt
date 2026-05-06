package com.comfortpick.application.port.out.exception

class RiotApiRateLimitException(
    message: String,
    val retryAfterSeconds: Long?,
    cause: Throwable? = null,
) : RiotApiException(message, cause)
