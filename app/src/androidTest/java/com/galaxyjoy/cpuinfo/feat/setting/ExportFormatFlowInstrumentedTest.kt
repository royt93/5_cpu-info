package com.galaxyjoy.cpuinfo.feat.setting

import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.matcher.ViewMatchers.withId
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
 * End-to-end coverage for the export-format picker (`menuActionShare` -> [ExportFormatBottomSheet]
 * -> `SystemInfoExporter`/`DeviceCardExporter` -> `ACTION_SEND`). This flow had ZERO instrumented
 * coverage before — TEXT/JSON/IMAGE were only ever verified by hand (see `doc/task/quick_win.md` #9,
 * U14) — so this closes the gap for all 4 formats, including the new HTML one added 2026-09-21,
 * which also exercises the one code path ([SystemInfoExporter.buildSystemInfoHtml]) that can't run
 * under the JVM unit-test stub at all (`Build.MODEL` etc. NPE there — see `SystemInfoExporterTest`).
 *
 * Espresso-Intents stubs the resulting chooser so the test never actually leaves the app or depends
 * on a real share target being installed on the device.
 */
@RunWith(AndroidJUnit4::class)
class ExportFormatFlowInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ActHost>()

    @Before
    fun setUp() {
        Intents.init()
        dismissFirstLaunchLanguagePickerIfShown()
        Intents.intending(anyIntent()).respondWith(Instrumentation.ActivityResult(0, null))
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    // Same dismiss-by-back-press as ActHostSmokeTest — see its kdoc for why not to select a
    // language (selecting one recreates the Activity, which ComposeTestRule can't follow).
    private fun dismissFirstLaunchLanguagePickerIfShown() {
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
    fun textFormatSharesPlainTextBody() {
        val innerIntent = pickFormatAndCaptureSharedIntent(R.string.export_as_text)

        assertEquals("text/plain", innerIntent.type)
        val body = innerIntent.getStringExtra(Intent.EXTRA_TEXT)
        assertTrue("expected a non-blank body, got: $body", !body.isNullOrBlank())
        assertTrue("expected the plain-text report header", body!!.contains("SYSTEM INFORMATION"))
    }

    @Test
    fun jsonFormatSharesJsonBody() {
        val innerIntent = pickFormatAndCaptureSharedIntent(R.string.export_as_json)

        assertEquals("application/json", innerIntent.type)
        val body = innerIntent.getStringExtra(Intent.EXTRA_TEXT)
        assertTrue("expected a JSON object body, got: $body", body?.trim()?.startsWith("{") == true)
    }

    @Test
    fun htmlFormatSharesHtmlBody() {
        val innerIntent = pickFormatAndCaptureSharedIntent(R.string.export_as_html)

        assertEquals("text/html", innerIntent.type)
        val body = innerIntent.getStringExtra(Intent.EXTRA_TEXT)
        assertTrue("expected an html document body, got: $body", body?.startsWith("<!DOCTYPE html>") == true)
        assertTrue(body!!.contains("<h1>System Information</h1>"))
        // Proves every section actually rendered real device data end to end, not just the shell.
        for (section in listOf("Device", "CPU", "RAM", "Storage", "GPU", "Sensors", "Cameras", "Media Codecs", "DRM")) {
            assertTrue("missing section: $section", body.contains("<h2>$section</h2>"))
        }
    }

    @Test
    fun imageFormatSharesAPngFile() {
        val innerIntent = pickFormatAndCaptureSharedIntent(R.string.export_as_image, waitTimeoutMillis = 8_000)

        assertEquals("image/png", innerIntent.type)
        assertTrue(innerIntent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) != null)
    }

    /**
     * Opens the export sheet, picks [labelRes], waits for the resulting `ACTION_CHOOSER` intent
     * Espresso-Intents recorded, and returns the wrapped `ACTION_SEND` intent inside it
     * ([Intent.EXTRA_INTENT]) so callers can assert on its mime type/extras.
     */
    private fun pickFormatAndCaptureSharedIntent(labelRes: Int, waitTimeoutMillis: Long = 5_000): Intent {
        // `menuActionShare` is `showAsAction="ifRoom"` — on a wide enough toolbar (confirmed on
        // TECNO KJ7) it renders directly instead of collapsing into the overflow menu, so this
        // clicks the item by id rather than going through Espresso.openActionBarOverflowOrOptionsMenu().
        onView(withId(R.id.menuActionShare)).perform(click())
        composeRule.waitForIdle()

        val label = composeRule.activity.getString(labelRes)
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(label).performClick()

        composeRule.waitUntil(timeoutMillis = waitTimeoutMillis) { Intents.getIntents().isNotEmpty() }

        val chooserIntent = Intents.getIntents().last()
        return requireNotNull(chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT)) {
            "expected ACTION_CHOOSER to wrap an inner ACTION_SEND intent, got: $chooserIntent"
        }
    }
}
