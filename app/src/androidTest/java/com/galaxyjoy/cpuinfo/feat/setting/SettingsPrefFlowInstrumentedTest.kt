package com.galaxyjoy.cpuinfo.feat.setting

import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.ActHost
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Coverage for the Settings preference rows that had ZERO instrumented coverage before this file
 * (surveyed 2026-09-21: only `key_hardware_snapshot` and `key_vip_diagnostic_history` out of ~15
 * in-app prefs were tested). `key_rate_app`/`key_more_app`/`key_share_app`/`key_policy_app` launch
 * external apps (Play Store/browser/share sheet) and `key_export_backup`/`key_import_backup` drive
 * a system SAF file picker — neither is practical or valuable to automate here, so this covers the
 * remaining in-app-navigable and share-via-ACTION_SEND ones only.
 */
@RunWith(AndroidJUnit4::class)
class SettingsPrefFlowInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    @Before
    fun setUp() {
        Intents.init()
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
        Intents.intending(anyIntent()).respondWith(Instrumentation.ActivityResult(0, null))

        onView(withId(R.id.menuSettings)).perform(click())
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun usbBluetoothPrefOpensBottomSheetWithRealContent() {
        clickSettingsRow(R.string.usb_bt_pref_title)

        // Sheet content lives in a ComposeView, distinct from the Preference RecyclerView row
        // behind it (a plain View) — same title text on both never collides in the semantics tree.
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.usb_bt_bluetooth_section))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun fleetComparePrefOpensBottomSheetWithRealContent() {
        clickSettingsRow(R.string.fleet_compare_pref_title)

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.fleet_compare_title))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun exportBenchHistoryPrefSharesCsvBody() {
        clickSettingsRow(R.string.export_bench_history_pref_title)

        val innerIntent = captureSharedIntent()
        assertEquals("text/csv", innerIntent.type)
        val body = innerIntent.getStringExtra(Intent.EXTRA_TEXT)
        assertTrue("expected a non-blank CSV body, got: $body", !body.isNullOrBlank())
    }

    @Test
    fun exportFullReportPrefSharesZipFile() {
        clickSettingsRow(R.string.export_full_report_pref_title)

        val innerIntent = captureSharedIntent()
        assertEquals("application/zip", innerIntent.type)
        assertTrue(innerIntent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) != null)
    }

    private fun clickSettingsRow(titleRes: Int) {
        val label = composeRule.activity.getString(titleRes)
        onView(isAssignableFrom(RecyclerView::class.java)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(label))),
        )
        onView(withText(label)).perform(click())
        composeRule.waitForIdle()
    }

    private fun captureSharedIntent(waitTimeoutMillis: Long = 5_000): Intent {
        composeRule.waitUntil(timeoutMillis = waitTimeoutMillis) { Intents.getIntents().isNotEmpty() }
        val chooserIntent = Intents.getIntents().last()
        return requireNotNull(chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT)) {
            "expected ACTION_CHOOSER to wrap an inner ACTION_SEND intent, got: $chooserIntent"
        }
    }
}
