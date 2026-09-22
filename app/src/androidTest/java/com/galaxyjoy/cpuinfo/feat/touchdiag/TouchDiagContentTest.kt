package com.galaxyjoy.cpuinfo.feat.touchdiag

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test — renders [TouchDiagContent] directly with hand-built state, same pattern as
 * [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreContentTest]. The live touch canvas itself is
 * drawn on a [androidx.compose.foundation.Canvas] (not inspectable via the semantics tree, and
 * real multi-touch gesture injection isn't reliably automatable — same documented limitation as
 * elsewhere in this codebase), so this covers the capability/stat text rows bound to
 * [TouchDiagUiState] and the reset button callback. [TouchDiagFlowInstrumentedTest] covers opening
 * the real sheet against real device capabilities.
 */
@RunWith(AndroidJUnit4::class)
class TouchDiagContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(uiState: TouchDiagUiState, onResetSession: () -> Unit = {}) {
        composeRule.setContent {
            CpuInfoTheme {
                TouchDiagContent(
                    uiState = uiState,
                    onFrame = { _, _ -> },
                    onTouchEnd = {},
                    onResetSession = onResetSession,
                )
            }
        }
    }

    @Test
    fun noCapabilities_showsNotFoundMessage() {
        setContent(TouchDiagUiState(capabilities = null))

        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_no_capabilities)).assertExists()
    }

    @Test
    fun withCapabilities_showsDeviceNameAndPressureRange() {
        val caps = TouchCapabilities(
            deviceName = "test_touchscreen",
            pressureRange = 0f..1f,
            sizeRange = 0f..0.5f,
            touchMajorRange = null,
            orientationRange = null,
        )
        setContent(TouchDiagUiState(capabilities = caps))

        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_device_name, "test_touchscreen"))
            .assertExists()
        composeRule.onNodeWithText(
            appContext.getString(R.string.touch_diag_pressure_range, "0.00 – 1.00"),
        ).assertExists()
    }

    @Test
    fun missingAxisRange_showsEmDashInsteadOfCrashing() {
        val caps = TouchCapabilities(
            deviceName = "minimal_touchscreen",
            pressureRange = null,
            sizeRange = null,
            touchMajorRange = null,
            orientationRange = null,
        )
        setContent(TouchDiagUiState(capabilities = caps))

        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_pressure_range, "—"))
            .assertExists()
    }

    @Test
    fun activePointsAndMaxObserved_reflectUiState() {
        setContent(
            TouchDiagUiState(
                capabilities = null,
                activePoints = listOf(TouchPoint(0, 1f, 1f, 0.5f), TouchPoint(1, 2f, 2f, 0.5f)),
                maxPointersObserved = 3,
            ),
        )

        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_active_points, 2)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_max_observed, 3)).assertExists()
    }

    @Test
    fun nullSampleRate_showsUnavailableText() {
        setContent(TouchDiagUiState(capabilities = null, sampleRateHz = null))

        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_sample_rate_unavailable))
            .assertExists()
    }

    @Test
    fun presentSampleRate_showsFormattedHzText() {
        setContent(TouchDiagUiState(capabilities = null, sampleRateHz = 120.0))

        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_sample_rate, 120.0))
            .assertExists()
    }

    @Test
    fun tappingResetButton_invokesOnResetSessionExactlyOnce() {
        var resetCount = 0
        setContent(TouchDiagUiState(capabilities = null), onResetSession = { resetCount++ })

        composeRule.onNodeWithText(appContext.getString(R.string.touch_diag_reset_button)).performClick()

        assert(resetCount == 1) { "expected onResetSession to fire exactly once, fired $resetCount times" }
    }
}
