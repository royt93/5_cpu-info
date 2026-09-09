package com.galaxyjoy.cpuinfo

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
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
 * 2026-09-09: previous KDoc here wrongly blamed TECNO's "Griffin/KeepAlive" OEM task-killer for
 * an intermittent 1-of-4 failure. Re-verified by running this class isolated
 * (`--tests ClipboardCopyInstrumentedTest`, no other class in the same invocation, so no
 * Griffin-triggering install/uninstall churn from sibling classes): `longPressingGpuRowCopiesItsValue`
 * and `longPressingSensorRowCopiesItsValue` failed 100% reproducibly, every run, not
 * intermittently. Real cause: GPU and Sensors tabs now prepend a non-copyable Compose header
 * (Vulkan/GLES detail bar / waveform chart) as RecyclerView item 0 via `ConcatAdapter` — see
 * `FrmGpuInfo.kt`/`FrmSensorsInfo.kt` — so `longPressFirstRowAndVerifyCopy()`'s hardcoded position
 * 0 was long-pressing the header, which has no copy handler, instead of a real value row. Fixed by
 * targeting position 1 for those two tabs.
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
            // Dismiss via back-press, NOT by selecting a language — same fix as
            // ActHostSmokeTest.kt's dismissFirstLaunchLanguagePickerIfShown() (see its KDoc):
            // selecting anything calls LocaleManager.applyNoFlicker() (finish()+startActivity()),
            // creating a genuinely NEW ActHost instance that ComposeTestRule's ActivityScenario
            // doesn't adopt — every later composeRule call then throws "Cannot run onActivity
            // since Activity has been destroyed already". Reproduced live in this file (2026-09-09
            // full-suite run) before this fix.
            pressBack()
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

    /**
     * @param position RecyclerView item index to long-press. Default 0 works for tabs whose
     * adapter has no header (RAM). GPU and Sensors now prepend a non-copyable Compose header
     * (Vulkan/GLES detail bar / waveform chart) as item 0 via `ConcatAdapter` — see
     * `FrmGpuInfo.kt`/`FrmSensorsInfo.kt` — so those tabs must target position 1, the first real
     * value row, same reasoning as the CPU test's label-based row lookup below.
     */
    private fun longPressFirstRowAndVerifyCopy(position: Int = 0) = verifyClipboardAndSnackbarAfter {
        onView(withId(R.id.rv)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, longClick())
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

        longPressFirstRowAndVerifyCopy(position = 1)
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

        longPressFirstRowAndVerifyCopy(position = 1)
    }
}
