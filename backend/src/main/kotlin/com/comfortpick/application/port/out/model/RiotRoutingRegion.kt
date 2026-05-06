package com.comfortpick.application.port.out.model

enum class RiotRoutingRegion(val hostPrefix: String) {
    AMERICAS("americas"),
    ASIA("asia"),
    EUROPE("europe"),
    SEA("sea");

    companion object {
        fun fromValue(value: String): RiotRoutingRegion =
            entries.firstOrNull { entry ->
                entry.name.equals(value.trim(), ignoreCase = true)
            } ?: throw IllegalArgumentException("Unsupported Riot routing region: $value")
    }
}
