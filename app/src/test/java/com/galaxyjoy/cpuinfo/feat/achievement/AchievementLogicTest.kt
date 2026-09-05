package com.galaxyjoy.cpuinfo.feat.achievement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementLogicTest {

    @Test
    fun `first-ever run is never a record`() {
        assertFalse(AchievementLogic.isNewRecord(previousBest = null, current = 100.0))
    }

    @Test
    fun `beating the previous best is a new record`() {
        assertTrue(AchievementLogic.isNewRecord(previousBest = 100.0, current = 100.1))
    }

    @Test
    fun `matching the previous best exactly is not a new record`() {
        assertFalse(AchievementLogic.isNewRecord(previousBest = 100.0, current = 100.0))
    }

    @Test
    fun `falling short of the previous best is not a new record`() {
        assertFalse(AchievementLogic.isNewRecord(previousBest = 100.0, current = 99.9))
    }
}
