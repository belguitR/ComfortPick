package com.comfortpick.application.port.out.exception

class RiotApiUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RiotApiException(message, cause)
