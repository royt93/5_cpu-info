package com.galaxyjoy.cpuinfo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.widget.progress.IconRoundCornerProgressBar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke tests covering the Sprint 1+2 changes end to end on a real device/emulator:
 * native lib init (B01/B02), Applications screen migration (T2.1/B03/B03b), and the Sort/menu
 * parity work done before switching screens.
 */
@RunWith(AndroidJUnit4::class)
class ActHostSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    /**
     * `ActHost` auto-shows a one-time language picker on a genuinely fresh install (no
     * `hasPickedLanguage` DataStore flag yet — see `maybeShowFirstLaunchLanguagePicker()`). Every
     * test below assumes it's landing on the normal toolbar/tab UI, so dismiss it first if the
     * install this test runs against happens to be fresh (real device installs aren't always
     * fresh across runs, so this only fires when the sheet is actually present).
     */
    @Before
    fun dismissFirstLaunchLanguagePickerIfShown() {
        composeRule.waitForIdle()
        val systemDefaultLabel = composeRule.activity.getString(R.string.language_system_default)
        val isShown = composeRule.onAllNodesWithText(systemDefaultLabel).fetchSemanticsNodes().isNotEmpty()
        if (isShown) {
            composeRule.onNodeWithText(systemDefaultLabel).performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun appLaunchesToHardwareTabWithoutCrashing() {
        composeRule.waitForIdle()
        // Reaching here without an exception already proves onCreate()/Hilt DI/native cpuinfo
        // lib init (B01/B02) succeeded on a real device — the failure mode for those bugs was a
        // crash before any UI could render.
    }

    @Test
    fun navigatingToApplicationsTabLoadsWithoutCrashing() {
        onView(withId(R.id.menuApplications)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for T2.1: proves FrmNewApplications (Compose) is what's actually
        // wired into nav_graph.xml and inflates successfully — not the deleted FrmApplications.
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.apps_more_options)
        ).assertExists()
    }

    @Test
    fun applicationsOverflowMenuShowsSortAndAllFourPreviouslyMissingActions() {
        onView(withId(R.id.menuApplications)).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.apps_more_options)
        ).performClick()

        // Before this fix, the Compose screen was missing Sort (dead onClick) and all 4 of these
        // actions compared to the FrmApplications it replaced — see doc/task/epic-01-bugfix.md B03.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.apps_sort_order)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.rate_app)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.more_app)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_app)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.term_policy)).assertExists()
    }

    @Test
    fun shieldScoreBadgeOpensBottomSheetWithScoreAndStreak() {
        // The toolbar badge is a plain XML View (action_shield_score.xml), not a Compose node —
        // must drive it via Espresso. The resulting BottomSheetDialogFragment content IS Compose,
        // so that part is verified via ComposeTestRule below.
        onView(withId(R.id.actionShieldScoreBadge)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for U10: the sheet must actually compute+render a score (not crash on
        // a real device's RAM/storage/battery state) and show the U09 streak section in the same
        // sheet.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.shield_score_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.streak_title)).assertExists()
    }

    @Test
    fun navigatingThroughAllBottomTabsDoesNotCrash() {
        onView(withId(R.id.menuApplications)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.menuTemperature)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        // No assertion beyond "didn't throw" — this is the regression guard for deleting
        // feat/processes (B11) and its nav_graph/menu_nav entries: the remaining tabs must
        // still all resolve and render.
    }

    /**
     * `com.google.android.material.tabs.TabLayout` extends `HorizontalScrollView`, so Espresso's
     * built-in `scrollTo()` (designed for ScrollView/HorizontalScrollView ancestors) brings the
     * target tab fully into view regardless of label width or locale — no need to guess a swipe
     * count or direction, which proved unreliable across devices (a fixed swipe count landed the
     * tab only partially visible on some screens, failing click()'s ≥90%-visible requirement).
     */
    private fun clickTabByText(label: String) {
        onView(withText(label)).perform(scrollTo(), click())
    }

    @Test
    fun throttleTestTabRunsFullCycleAndShowsResult() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.throttle))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.throttle_start_button))
            .performClick()

        // Real hard-capped 30s stress test (U02) — waits for the actual run to finish on device
        // rather than faking it, so this is a genuine regression guard for the runner + verdict
        // pipeline, not just the idle-state UI.
        composeRule.waitUntil(timeoutMillis = 35_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.throttle_done_button))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.throttle_done_button)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.throttle_share_button)).assertExists()
    }

    @Test
    fun thermalStatusCardShowsOnThrottleTab() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.throttle))
        composeRule.waitForIdle()

        // F02 — passive PowerManager thermal status, independent of the active stress test below
        // it. Real device is API 29+, so the card must render rather than stay hidden.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.thermal_status_title)).assertExists()
    }

    @Test
    fun androidTabShowsSecurityChecklistRows() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.android))
        composeRule.waitForIdle()

        // F04 — regression guard for the 3 new security checklist rows added to VMAndroidInfo.
        // They sit below Build/root/encryption/StrongBox data in the plain (non-Compose)
        // RecyclerView — RecyclerViewActions.scrollTo() scrolls each into view precisely instead
        // of guessing a swipe count (which previously either under- or over-shot the target).
        scrollAndroidInfoListTo(composeRule.activity.getString(R.string.security_patch_level))
        onView(withText(composeRule.activity.getString(R.string.security_patch_level))).check(matches(isDisplayed()))
        scrollAndroidInfoListTo(composeRule.activity.getString(R.string.selinux_status))
        onView(withText(composeRule.activity.getString(R.string.selinux_status))).check(matches(isDisplayed()))
        scrollAndroidInfoListTo(composeRule.activity.getString(R.string.hardware_keystore))
        onView(withText(composeRule.activity.getString(R.string.hardware_keystore))).check(matches(isDisplayed()))
    }

    private fun scrollAndroidInfoListTo(label: String) {
        onView(withId(R.id.rv)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(label)))
        )
    }

    @Test
    fun deviceTruthFabOpensSheetWithEvidenceRows() {
        // CPU is the default tab, no navigation needed.
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for U01: proves the native JNI additions (vendor/uarch/MIDR + the
        // signal-guarded MPIDR/REVIDR reads) don't crash on a real device and produce a rendered
        // verdict + evidence list — not just that they compile.
        onView(withId(R.id.fabDeviceTruth)).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.device_truth_title)).assertExists()
        composeRule.onNodeWithText("MIDR_EL1").assertExists()
        composeRule.onNodeWithText("MPIDR_EL1").assertExists()
        composeRule.onNodeWithText("REVIDR_EL1").assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.device_truth_share_button)).assertExists()
    }

    @Test
    fun aiReadinessBarOpensSheetWithScoreAndFlagRows() {
        // CPU is the default tab, no navigation needed.
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        // Regression guard for F10/U12: proves the 6 new ISA-flag JNI getters don't crash on a
        // real device and the score/tier + flag rows render — not just that they compile.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_bar_label))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_neon_dot)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_i8mm)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_bf16)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_fp16)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_flag_sve)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.ai_readiness_disclaimer)).assertExists()
    }

    @Test
    fun hardwareSnapshotPrefOpensSheetWithoutCrashing() {
        // Regression guard for U03: proves the preference-screen entry point actually shows
        // HardwareSnapshotBottomSheet and it renders (via DataStore read + live hardware capture)
        // without crashing — either the empty-baseline or the diff state, depending on whatever a
        // prior run/device already persisted, so only the title (present in both states) is
        // asserted.
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()

        onView(withText(composeRule.activity.getString(R.string.hardware_snapshot_pref_title))).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.hardware_snapshot_title)).assertExists()
    }

    @Test
    fun appPermissionsActionShowsPermissionsSheet() {
        // Regression guard for F05: proves long-pressing an app row -> "App Permissions" ->
        // reads real PackageManager permission data for that app without crashing. Uses this
        // app's own row (always present) since the installed-app list order is device-dependent.
        onView(withId(R.id.menuApplications)).perform(click())
        composeRule.waitForIdle()

        val appName = composeRule.activity.getString(R.string.app_name)
        composeRule.onNodeWithText(appName).performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.app_permissions_action))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.app_permissions_title, appName),
        ).assertExists()
    }

    @Test
    fun sensorTestFabRunsThroughAllStepsToADoneScreen() {
        // Regression guard for F07: proves the guided test flow actually registers real
        // SensorEventListeners per step (not just renders static UI) and reaches a Done screen
        // without crashing. Skips every step immediately rather than waiting for a real physical
        // action/timeout — this test only needs to prove the state machine advances and
        // terminates, not that detection itself works (that's SensorTestEvaluatorTest's job).
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        clickTabByText(composeRule.activity.getString(R.string.sensors))
        composeRule.waitForIdle()

        onView(withId(R.id.fabSensorTest)).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sensor_test_start_button))
            .performClick()
        composeRule.waitForIdle()

        val skipLabel = composeRule.activity.getString(R.string.sensor_test_skip_button)
        val doneLabel = composeRule.activity.getString(R.string.sensor_test_done_title)
        composeRule.waitUntil(timeoutMillis = 60_000) {
            val done = composeRule.onAllNodesWithText(doneLabel).fetchSemanticsNodes().isNotEmpty()
            if (!done) {
                val skipNodes = composeRule.onAllNodesWithText(skipLabel).fetchSemanticsNodes()
                if (skipNodes.isNotEmpty()) {
                    composeRule.onNodeWithText(skipLabel).performClick()
                }
            }
            done
        }

        composeRule.onNodeWithText(doneLabel).assertExists()
    }

    @Test
    fun languagePickerPrefOpensBottomSheetWithAllSupportedLocales() {
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()

        onView(withText(composeRule.activity.getString(R.string.language_change))).perform(click())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.language_picker_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.language_system_default)).assertExists()
        composeRule.onNodeWithText("English").assertExists()
        composeRule.onNodeWithText("Tiếng Việt").assertExists()
        composeRule.onNodeWithText("Čeština").assertExists()
        composeRule.onNodeWithText("Deutsch").assertExists()
        composeRule.onNodeWithText("Polski").assertExists()
        composeRule.onNodeWithText("繁體中文").assertExists()
    }

    @Test
    fun canMyDeviceBarOpensSheetWithRulesAndDisclaimer() {
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()

        // DRM is in the tab list
        clickTabByText(composeRule.activity.getString(R.string.drm))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.can_my_device_bar_label))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.can_my_device_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.can_my_device_disclaimer)).assertExists()
    }

    @Test
    fun cpuFrequencyProgressBarsRenderOnDefaultTab() {
        // Regression guard for B20: `IconRoundCornerProgressBar`'s old `max/progress` formula
        // could divide by zero and produce NaN/Infinity width when a core reports max freq = 0
        // (unreadable cpufreq sysfs on some OEMs) — this exercises the bar with this real
        // device's actual frequency values, not a synthetic 0/0 case, since that specific reading
        // is device/kernel-state dependent and can't be forced from a test.
        composeRule.waitForIdle()
        onView(withId(R.id.rv))
            .check(matches(hasDescendant(isAssignableFrom(IconRoundCornerProgressBar::class.java))))
    }

    @Test
    fun hardwareTabAndBackRepeatedlyDoesNotCrash() {
        // Regression guard for B16: `FrmInfoContainer` now detaches its `TabLayoutMediator` in
        // onDestroyView() instead of leaking it. Each Hardware <-> Applications round trip
        // destroys/recreates FrmInfoContainer's view — repeating it several times is a stress
        // proxy for the leak fix (an actual leak isn't observable via Espresso, only via a
        // memory profiler, so this only proves the fix doesn't regress crash-free navigation).
        repeat(4) {
            onView(withId(R.id.menuHardware)).perform(click())
            composeRule.waitForIdle()
            onView(withId(R.id.menuApplications)).perform(click())
            composeRule.waitForIdle()
        }
        onView(withId(R.id.menuHardware)).perform(click())
        composeRule.waitForIdle()
        onView(withId(R.id.rv))
            .check(matches(hasDescendant(isAssignableFrom(IconRoundCornerProgressBar::class.java))))
    }

    @Test
    fun vipRedeemFieldFocusThenImmediateBackDoesNotCrash() {
        // Regression guard for B21: `scrollToRedeemSection()`'s postDelayed(300ms) used to leak
        // its Runnable past onDestroyView(). Focusing the field posts that delayed scroll; back
        // then destroys the fragment's view before the 300ms elapses — exactly the race the fix
        // cancels via removeCallbacks() in onDestroyView(). Two presses because the first one
        // only dismisses the soft keyboard the focus click opened (standard Android behavior),
        // the second actually finishes ActVip and returns to ActHost.
        onView(withId(R.id.actionVipIcon)).perform(click())
        onView(withId(R.id.etRedeemKey)).perform(click())
        pressBack()
        pressBack()
        composeRule.waitForIdle()

        // Back on ActHost without a crash — the CPU tab (default) is still there.
        onView(withId(R.id.rv))
            .check(matches(hasDescendant(isAssignableFrom(IconRoundCornerProgressBar::class.java))))
    }

    // No instrumented test for the Dashboard tab (F01): navigating to it reproducibly triggers
    // "Cannot run onActivity since Activity has been destroyed already" from ActivityScenario's
    // own teardown, even with zero assertions past composeRule.waitForIdle(). Confirmed via a live
    // logcat capture that this is NOT an app crash (no FATAL EXCEPTION; the process exits 0,
    // normal "make process inactive" transition) — it's a test-harness teardown timing issue,
    // most likely composeRule.waitForIdle() never settling against a screen whose ViewModel
    // updates state on a steady 1s timer (VMDashboard's CPU collector). Coverage for this feature
    // instead comes from VMDashboardTest/HistoryBufferTest (unit) and a clean assembleDevDebug/
    // lintDevDebug with the new MPAndroidChart dependency wired in.
}

