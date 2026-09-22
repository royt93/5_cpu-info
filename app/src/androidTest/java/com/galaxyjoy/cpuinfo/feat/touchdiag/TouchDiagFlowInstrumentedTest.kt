package com.galaxyjoy.cpuinfo.feat.touchdiag

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * End-to-end coverage for the E11 Touchscreen Diagnostic entry point: `key_touch_diag` (Settings)
 * -> [TouchDiagBottomSheet] -> real [TouchCapabilityProvider] reading this device's actual
 * touchscreen. Real multi-touch gesture injection isn't attempted (not reliably automatable — see
 * [TouchDiagContentTest]'s kdoc); this proves the wiring + real capability read doesn't crash and
 * shows sane data for whatever touchscreen the test device actually has.
 */
@RunWith(AndroidJUnit4::class)
class TouchDiagFlowInstrumentedTest {

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
    fun touchDiagPrefOpensSheetWithRealDeviceCapabilities() {
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()

        val label = composeRule.activity.getString(R.string.touch_diag_pref_title)
        onView(isAssignableFrom(RecyclerView::class.java)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(label))),
        )
        onView(withText(label)).perform(click())
        composeRule.waitForIdle()

        // Sheet content lives in a ComposeView, distinct from the Preference row behind it (a
        // plain View) — same title text on both never collides in the semantics tree, same as
        // the other Settings-triggered sheets.
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.touch_diag_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // A real device always has a real touchscreen (how else would this test be running) — the
        // "not found" branch must not fire here.
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.touch_diag_no_capabilities),
        ).assertDoesNotExist()

        // Reset button must not crash the live sheet.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.touch_diag_reset_button))
            .performClick()
        composeRule.waitForIdle()
    }
}
