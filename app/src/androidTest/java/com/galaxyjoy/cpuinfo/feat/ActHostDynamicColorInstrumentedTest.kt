package com.galaxyjoy.cpuinfo.feat

import android.os.Build
import android.util.TypedValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.R
import com.google.android.material.R as MaterialR
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the Material You widget audit's root-cause fix — [ActHost]'s
 * `onCreate()` calls `setTheme(R.style.AppThemeBase)` as its very first line (needed to switch
 * away from the manifest's `AppThemeBase.Launcher` variant), which silently discards the dynamic
 * color theme overlay `DynamicColorsInitializer` applied moments earlier via
 * `Application.ActivityLifecycleCallbacks.onActivityPreCreated()` (that callback always runs
 * before `onCreate()`, so it's always the overlay that loses). Confirmed live on device: before
 * this fix, `?attr/colorPrimaryContainer` resolved to Material3's static baseline purple
 * (`#EADDFF`-ish) instead of this device's real dynamic accent
 * (`android.R.color.system_accent1_100`) — visible on the CPU tab's "Kiểm tra thiết bị" FAB.
 */
@RunWith(AndroidJUnit4::class)
class ActHostDynamicColorInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    @Before
    fun dismissFirstLaunchLanguagePickerIfShown() {
        composeRule.waitForIdle()
        val systemDefaultLabel = composeRule.activity.getString(R.string.language_system_default)
        val appeared = runCatching {
            composeRule.waitUntil(timeoutMillis = 2_000) {
                composeRule.onAllNodesWithText(systemDefaultLabel).fetchSemanticsNodes().isNotEmpty()
            }
        }.isSuccess
        if (appeared) {
            pressBack()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun colorPrimaryContainer_resolvesToRealDynamicAccent_notTheStaticM3Baseline() {
        assumeTrue("dynamic color only exists on API 31+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

        val activity = composeRule.activity
        val resolved = TypedValue()
        activity.theme.resolveAttribute(MaterialR.attr.colorPrimaryContainer, resolved, true)
        val actual = resolved.data

        val expected = activity.resources.getColor(android.R.color.system_accent1_100, activity.theme)

        assertEquals(
            "ActHost's theme should carry the dynamic-color overlay (its ?attr/colorPrimaryContainer " +
                "should match this device's real android.R.color.system_accent1_100), not Material3's " +
                "static baseline default — setTheme() in onCreate() must not clobber it",
            expected,
            actual,
        )
    }
}
