package com.comfortpick.application.port.out.exception

class RiotApiNotFoundException(
    message: String,
    cause: Throwable? = null,
) : RiotApiException(message, cause)
