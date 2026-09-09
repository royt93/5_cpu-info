package com.galaxyjoy.cpuinfo.feat.vip

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.feat.SplashActivity
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.InternalAdApi
import com.roy.sdkadbmob.isAppOpenExcludedForTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for a real bug found via manual smoke test on Pixel 7 Pro (2026-09-08):
 * the earlier fix used `AdManager.suppressAppOpenTemporarily(true/false)` in
 * [FVipManagement]'s `onResume`/`onPause`, wrongly assuming VIP was a Fragment hosted inside
 * [ActHost]. It's actually a separate [ActVip] `Activity` — and `onPause` also fires when the
 * whole app backgrounds (HOME/App Switcher), so the old code un-suppressed App Open at exactly
 * the moment it needed to stay suppressed. Confirmed live: App Open showed over the VIP screen
 * after backgrounding then resuming. Fixed by registering `ActVip::class.java` in
 * `AdSdkConfig.appOpenExcludedActivities` (GalaxyApp.kt) instead — the SDK's own deterministic,
 * Activity-level mechanism (same one already used for [SplashActivity]), no Fragment-lifecycle
 * timing involved at all.
 *
 * `AdManager.isAppOpenExcludedForTest` is the SDK's own test seam for this exact check (see
 * `AdManagerTestHooks.kt`), used here instead of poking the real App Open ad flow.
 */
@OptIn(InternalAdApi::class)
@RunWith(AndroidJUnit4::class)
class AppOpenExclusionInstrumentedTest {

    @Test
    fun actVip_isExcludedFromAppOpen() {
        ActivityScenario.launch(ActVip::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    "ActVip must be excluded — this is the exact screen App Open showed over " +
                        "before the fix",
                    AdManager.isAppOpenExcludedForTest(activity),
                )
            }
        }
    }

    @Test
    fun splashActivity_isStillExcludedFromAppOpen() {
        ActivityScenario.launch(SplashActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(AdManager.isAppOpenExcludedForTest(activity))
            }
        }
    }

    @Test
    fun actHost_isNotExcludedFromAppOpen() {
        ActivityScenario.launch(ActHost::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(
                    "ActHost (main screen) must NOT be excluded — App Open should still show " +
                        "there on background/foreground",
                    AdManager.isAppOpenExcludedForTest(activity),
                )
            }
        }
    }
}
