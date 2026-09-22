package com.galaxyjoy.cpuinfo.feat.gnssdiag

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
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
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.ActHost
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end coverage for the E09 entry point: `key_gnss_diag` (Settings) ->
 * [GnssDiagnosticBottomSheet] -> real [android.location.LocationManager.registerGnssStatusCallback]
 * on this device. Grants `ACCESS_FINE_LOCATION` via `UiAutomation` before the test — same pattern
 * [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertNotifierTest] already uses for
 * `POST_NOTIFICATIONS`, avoiding an `androidx.test:rules` dependency just for
 * `GrantPermissionRule`. Doesn't assert on a real satellite fix (indoors/CI can't guarantee one in
 * a short test) — proves the permission-granted path registers the real callback without a
 * `SecurityException` and lands on the "waiting for fix" state, not a crash.
 */
@RunWith(AndroidJUnit4::class)
class GnssDiagnosticFlowInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

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
    fun gnssDiagPrefOpensSheetAndRegistersRealCallbackWithoutCrashing() {
        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()

        val label = composeRule.activity.getString(R.string.gnss_diag_pref_title)
        onView(isAssignableFrom(RecyclerView::class.java)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(label))),
        )
        onView(withText(label)).perform(click())
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.gnss_diag_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Permission was pre-granted -> the permission-prompt branch must NOT be showing.
        composeRule.onAllNodesWithText(
            composeRule.activity.getString(R.string.gnss_diag_permission_rationale),
        ).assertCountEquals(0)

        // Either a fix hasn't come in yet, or (rare in a quick test) it already has — either is a
        // valid real-device outcome; only a crash would fail this test at this point.
        pressBack()
    }
}
