package com.comfortpick.application.port.out

import java.util.UUID

interface MatchupCoverageQuery {
    fun countStoredMatchupsForAccount(riotAccountId: UUID): Long
}
