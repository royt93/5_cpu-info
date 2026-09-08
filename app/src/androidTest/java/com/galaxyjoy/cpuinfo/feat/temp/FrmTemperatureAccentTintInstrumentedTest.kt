package com.galaxyjoy.cpuinfo.feat.temp

import android.widget.ProgressBar
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.util.isNightMode
import com.galaxyjoy.cpuinfo.util.resolveAccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the Material You widget audit's Temperature-tab fix —
 * `style="@style/TintedProgressBar"` pins `colorAccent` to a static `@color/accent` inside its
 * own self-contained `Theme.AppCompat.Dialog.Alert`-derived style, which the global
 * `DynamicColorsInitializer` can't reach, so `FrmTemperature` sets `indeterminateTintList` at
 * runtime instead — same reasoning as [com.galaxyjoy.cpuinfo.feat.setting.FrmSettingsAccentTintInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class FrmTemperatureAccentTintInstrumentedTest {

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
            // See FrmSettingsAccentTintInstrumentedTest for why this is pressBack(), not a pick.
            pressBack()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun progressBar_indeterminateTint_matchesResolvedAccentColor() {
        onView(withId(R.id.menuTemperature)).perform(click())
        composeRule.waitForIdle()

        lateinit var pb: ProgressBar
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            pb = composeRule.activity.findViewById(R.id.pb)
        }
        val expected = pb.context.resolveAccentColor(pb.context.isNightMode())

        val tintList = pb.indeterminateTintList
        assertNotNull("progress bar should have a non-null indeterminate tint after the audit fix", tintList)
        val actual = tintList!!.defaultColor
        assertEquals(
            "Temperature tab's loading spinner should use the resolved dynamic accent color, " +
                "not the static @color/accent baked into TintedProgressBar's style",
            expected,
            actual,
        )
    }
}
