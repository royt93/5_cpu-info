package com.galaxyjoy.cpuinfo

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.feat.ActHost
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for the `copyToClipboardAndNotify()` extraction (previously 5 identical
 * copy-paste implementations across `BaseRvFragment` + Sensor/RAM/GPU/CPU fragments) — proves the
 * shared helper still copies the real row value to the clipboard and shows the confirmation
 * Snackbar, on every one of the 4 fragments that can't extend `BaseRvFragment`.
 *
 * Each test passes reliably run in isolation (`--tests ClipboardCopyInstrumentedTest#<name>`).
 * Running the whole class in one `connectedDevDebugAndroidTest` invocation can non-deterministically
 * fail 1 of the 4 (a different one each run) on this specific TECNO KJ7 device — confirmed via
 * live logcat that the OEM's own "Griffin/KeepAlive" task manager force-kills the freshly
 * relaunched `ActHost` task between orchestrated tests (`Griffin/KeepAlive:removeTask: ... kill:true`),
 * same class of TECNO-specific harness instability already documented at the bottom of
 * `ActHostSmokeTest` (4 known-flaky failures out of 23 there too) — not a regression in this code.
 */
@RunWith(AndroidJUnit4::class)
class ClipboardCopyInstrumentedTest {

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
            composeRule.onAllNodesWithText(systemDefaultLabel)[0].performClick()
            composeRule.waitForIdle()
        }
    }

    private fun clearClipboard() {
        val clipboard = composeRule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.clearPrimaryClip()
    }

    private fun verifyClipboardAndSnackbarAfter(longPress: () -> Unit) {
        clearClipboard()

        longPress()

        // Snackbar.LENGTH_SHORT auto-dismisses after ~1.5s — check the clipboard (permanent)
        // first, Snackbar text (transient) second, so a slow device doesn't race the dismissal.
        val clipboard = composeRule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copiedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertTrue("expected a non-empty value copied to clipboard, was: $copiedText", !copiedText.isNullOrEmpty())

        onView(withText(R.string.text_copied)).check(matches(isDisplayed()))
    }

    private fun longPressFirstRowAndVerifyCopy() = verifyClipboardAndSnackbarAfter {
        onView(withId(R.id.rv)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
        )
    }

    @Test
    fun longPressingCpuRowCopiesItsValue() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        // CPU is the default sub-tab, no extra navigation needed.

        // Position 0 is a live per-core frequency gauge (CpuRow.FrequencyRow) which has no
        // long-press handler at all (a progress bar, not a copyable label/value row) — target the
        // first CpuRow.ValueRow (SoC name) by its label instead of a hardcoded position.
        val socNameLabel = composeRule.activity.getString(R.string.cpu_soc_name)
        verifyClipboardAndSnackbarAfter {
            onView(withId(R.id.rv)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(socNameLabel)), longClick(),
                )
            )
        }
    }

    @Test
    fun longPressingGpuRowCopiesItsValue() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        onView(withText(composeRule.activity.getString(R.string.gpu))).perform(scrollTo(), click())
        composeRule.waitForIdle()

        longPressFirstRowAndVerifyCopy()
    }

    @Test
    fun longPressingRamRowCopiesItsValue() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        onView(withText(composeRule.activity.getString(R.string.ram))).perform(scrollTo(), click())
        composeRule.waitForIdle()

        longPressFirstRowAndVerifyCopy()
    }

    @Test
    fun longPressingSensorRowCopiesItsValue() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        onView(withText(composeRule.activity.getString(R.string.sensors))).perform(scrollTo(), click())
        composeRule.waitForIdle()

        longPressFirstRowAndVerifyCopy()
    }
}
