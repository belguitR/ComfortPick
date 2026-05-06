package com.comfortpick.application.port.out.exception

class RiotApiUnauthorizedException(
    message: String,
    cause: Throwable? = null,
) : RiotApiException(message, cause)
