package com.comfortpick.infrastructure.config

import com.comfortpick.application.usecase.RunHistorySyncCycleUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class HistorySyncScheduler(
    private val runHistorySyncCycleUseCase: RunHistorySyncCycleUseCase,
) {
    @Scheduled(fixedDelayString = "\${comfortpick.sync.scheduler-interval:30s}")
    fun run() {
        runHistorySyncCycleUseCase.execute()
    }
}
