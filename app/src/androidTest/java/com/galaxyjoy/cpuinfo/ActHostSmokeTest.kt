package com.galaxyjoy.cpuinfo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
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
}
