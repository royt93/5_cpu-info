package com.galaxyjoy.cpuinfo.feat.achievement

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementPrefsInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var prefs: AchievementPrefs

    @Before
    fun setUp() {
        appContext.getSharedPreferences("achievement_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = AchievementPrefs(appContext)
    }

    @Test
    fun freshInstall_countIsZero() {
        assertEquals(0, prefs.getRecordsBrokenCount())
    }

    @Test
    fun incrementRecordsBroken_returnsAndPersistsTheNewCount() {
        assertEquals(1, prefs.incrementRecordsBroken())
        assertEquals(2, prefs.incrementRecordsBroken())

        // New instance, same real SharedPreferences file — proves the write is actually persisted,
        // not just held in this instance's memory.
        assertEquals(2, AchievementPrefs(appContext).getRecordsBrokenCount())
    }
}
