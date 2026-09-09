package com.galaxyjoy.cpuinfo.feat

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.platform.app.InstrumentationRegistry
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.InternalAdApi
import com.roy.sdkadbmob.setConsentHangForTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the consent watchdog our own [SplashActivity] depends on — audit
 * 2026-09-09 (QC flagged the ad SDK integration as still wrong vs the SDK's own guide, focused on
 * consent). `SplashActivity.onCreate()` only ever calls `runSplashFlow()` from inside
 * `AdManager.requestConsentInfoUpdate(this) { ... }`'s callback — if that callback never fires
 * (UMP/network hang), our splash would hang forever UNLESS the SDK's own internal watchdog fires
 * it anyway. `AdManager.setConsentHangForTest(true)` forces exactly that failure mode (UMP callback
 * never fires) so this test exercises the watchdog path, not the fast/happy path.
 *
 * Mirrors the SDK's own `SplashNoHangTest.kt` (`app/src/androidTest/.../SplashNoHangTest.kt` in the
 * AdmobApplovinWrapper repo) — same test-hook, adapted to this app's SplashActivity → ActHost flow
 * (the SDK demo's own equivalent navigates to its own MainActivity).
 */
@OptIn(InternalAdApi::class)
@RunWith(AndroidJUnit4::class)
class SplashConsentNoHangInstrumentedTest {

    @After
    fun tearDown() {
        AdManager.setConsentHangForTest(false)
    }

    @Test
    fun splash_alwaysNavigatesToActHost_evenWhenUmpConsentCallbackHangs() {
        AdManager.setConsentHangForTest(true)

        // Not .use{} — SplashActivity finishes itself on navigate, so scenario.close() can throw;
        // this test only needs ActHost to appear, teardown resets the test hook regardless.
        ActivityScenario.launch(SplashActivity::class.java)

        assertTrue(
            "SplashActivity hung — ActHost never appeared within 25s (consent watchdog broken?)",
            waitForActivityPresent(ActHost::class.java, timeoutMs = 25_000L),
        )
    }

    /** Poll until an instance of [clazz] exists in any alive lifecycle stage (any task). */
    private fun waitForActivityPresent(clazz: Class<*>, timeoutMs: Long): Boolean {
        val aliveStages = listOf(
            Stage.CREATED, Stage.STARTED, Stage.RESUMED,
            Stage.PAUSED, Stage.STOPPED, Stage.RESTARTED,
        )
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            var found = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val reg = ActivityLifecycleMonitorRegistry.getInstance()
                found = aliveStages.any { stage ->
                    reg.getActivitiesInStage(stage).any { clazz.isInstance(it) }
                }
            }
            if (found) return true
            SystemClock.sleep(250)
        }
        return false
    }
}
