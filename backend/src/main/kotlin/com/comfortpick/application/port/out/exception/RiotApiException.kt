package com.comfortpick.application.port.out.exception

open class RiotApiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
