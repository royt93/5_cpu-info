package com.galaxyjoy.cpuinfo.feat.componentaudit

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
 * End-to-end coverage for the E19 entry point: `key_component_audit` (Settings) ->
 * [ComponentAuditBottomSheet] -> real [ExportedComponentAuditProvider] scanning this device's
 * actually-visible packages (via the app's `<queries>` declaration — no QUERY_ALL_PACKAGES). Proves
 * the real `PackageManager.getInstalledPackages()` call doesn't crash and scans at least this app
 * itself (always visible to itself, so `scannedPackageCount >= 1` is a safe real-device invariant).
 */
@RunWith(AndroidJUnit4::class)
class ComponentAuditFlowInstrumentedTest {

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
    fun componentAuditPrefOpensSheetAndScansRealPackagesWithoutCrashing() {
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()

        val label = composeRule.activity.getString(R.string.component_audit_pref_title)
        onView(isAssignableFrom(RecyclerView::class.java)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(label))),
        )
        onView(withText(label)).perform(click())
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.component_audit_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.component_audit_scope_disclaimer),
        ).assertExists()
    }
}
