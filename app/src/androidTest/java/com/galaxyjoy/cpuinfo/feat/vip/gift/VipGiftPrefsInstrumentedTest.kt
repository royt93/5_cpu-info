package com.galaxyjoy.cpuinfo.feat.vip.gift

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VipGiftPrefsInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var prefs: VipGiftPrefs

    @Before
    fun setUp() {
        appContext.getSharedPreferences("vip_gift_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = VipGiftPrefs(appContext)
    }

    @Test
    fun freshInstall_bothDaysAreNull() {
        assertNull(prefs.getLastGeneratedEpochDay())
        assertNull(prefs.getLastRedeemedEpochDay())
    }

    @Test
    fun saveAndGetLastGeneratedEpochDay_realSharedPreferencesRoundTrip() {
        prefs.saveLastGeneratedEpochDay(20_000L)

        assertEquals(20_000L, prefs.getLastGeneratedEpochDay())
    }

    @Test
    fun saveAndGetLastRedeemedEpochDay_realSharedPreferencesRoundTrip() {
        prefs.saveLastRedeemedEpochDay(20_001L)

        assertEquals(20_001L, prefs.getLastRedeemedEpochDay())
    }

    @Test
    fun todayEpochDay_isAPlausibleRealValue() {
        // Epoch day for any date since ~2024 is comfortably > 19_700 (1970-01-01 + 19_700 days
        // lands in mid-2023) — just confirms this reads the real system clock, not a stub.
        assertTrue(VipGiftPrefs.todayEpochDay() > 19_700L)
    }
}
