package com.comfortpick.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BuildRuneAnalysisServiceTest {
    private val service = BuildRuneAnalysisService()

    @Test
    fun `returns most common winning build and runes`() {
        val result = service.analyze(
            listOf(
                BuildRuneSample(true, listOf(6655, 3020, 3165, null, null, null), 8112, 8226),
                BuildRuneSample(true, listOf(6655, 3020, 3165, null, null, null), 8112, 8226),
                BuildRuneSample(true, listOf(3157, 3020, 3089, null, null, null), 8214, 8226),
                BuildRuneSample(false, listOf(3157, 3020, 3089, null, null, null), 8214, 8226),
            ),
        )

        assertEquals(6655, result.build.firstCompletedItemId)
        assertEquals(2, result.build.firstCompletedItemGames)
        assertEquals("6655>3020>3165", result.build.itemSet)
        assertEquals(2, result.build.itemSetGames)
        assertEquals(8112, result.runes.primaryRuneId)
        assertEquals(2, result.runes.primaryRuneGames)
        assertEquals(8226, result.runes.secondaryRuneId)
        assertEquals(3, result.runes.secondaryRuneGames)
        assertEquals(75.0, result.build.score)
        assertEquals(75.0, result.runes.score)
    }

    @Test
    fun `breaks ties deterministically`() {
        val result = service.analyze(
            listOf(
                BuildRuneSample(true, listOf(3157, 3020, null, null, null, null), 8214, 8226),
                BuildRuneSample(true, listOf(6655, 3020, null, null, null, null), 8112, 8226),
            ),
        )

        assertEquals(3157, result.build.firstCompletedItemId)
        assertEquals("3157>3020", result.build.itemSet)
        assertEquals(8112, result.runes.primaryRuneId)
        assertEquals(8226, result.runes.secondaryRuneId)
    }

    @Test
    fun `returns empty recommendations when no wins exist`() {
        val result = service.analyze(
            listOf(
                BuildRuneSample(false, listOf(3157, 3020, null, null, null, null), 8214, 8226),
            ),
        )

        assertNull(result.build.firstCompletedItemId)
        assertNull(result.build.itemSet)
        assertNull(result.build.score)
        assertNull(result.runes.primaryRuneId)
        assertNull(result.runes.secondaryRuneId)
        assertNull(result.runes.score)
        assertEquals(0, result.build.firstCompletedItemGames)
        assertEquals(0, result.runes.primaryRuneGames)
    }
}
