package com.galaxyjoy.cpuinfo.feat.foldable

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.ActHost
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end coverage for the E08 entry point: `key_foldable_display` (Settings) ->
 * [FoldableDisplayBottomSheet] -> real [DisplayInventoryProvider] + live `WindowInfoTracker`. The
 * test device (TECNO KJ7) isn't foldable, so this proves the real-device "no hinge" branch works
 * (not a hand-built fake) rather than a real fold posture — same honest scoping as documented in
 * `doc/task/epic-05-new-ideas.md` E08's write-up.
 */
@RunWith(AndroidJUnit4::class)
class FoldableDisplayFlowInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    @Before
    fun setUp() {
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
    fun foldableDisplayPrefOpensSheetWithRealDeviceData() {
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()

        val label = composeRule.activity.getString(R.string.foldable_pref_title)
        onView(isAssignableFrom(RecyclerView::class.java)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(label))),
        )
        onView(withText(label)).perform(click())
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.foldable_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Non-foldable test device -> the "no hinge" branch must be live, not the sheet just
        // being empty/broken.
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.foldable_no_hinge))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
