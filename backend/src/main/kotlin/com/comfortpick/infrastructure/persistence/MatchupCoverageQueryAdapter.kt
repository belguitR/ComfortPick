package com.comfortpick.infrastructure.persistence

import com.comfortpick.application.port.out.MatchupCoverageQuery
import com.comfortpick.infrastructure.persistence.repository.PlayerMatchupRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MatchupCoverageQueryAdapter(
    private val playerMatchupRepository: PlayerMatchupRepository,
) : MatchupCoverageQuery {
    override fun countStoredMatchupsForAccount(riotAccountId: UUID): Long =
        playerMatchupRepository.countByRiotAccountId(riotAccountId)
}
