package com.galaxyjoy.cpuinfo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.feat.ActHost
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
}
