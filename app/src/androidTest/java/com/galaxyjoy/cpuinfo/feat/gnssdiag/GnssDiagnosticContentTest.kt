package com.galaxyjoy.cpuinfo.feat.gnssdiag

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
 * Widget test — renders [GnssDiagnosticContent] directly with hand-built state, same pattern as
 * [com.galaxyjoy.cpuinfo.feat.componentaudit.ComponentAuditContentTest].
 * [GnssDiagnosticFlowInstrumentedTest] covers the real permission grant + real
 * `LocationManager.registerGnssStatusCallback` on this device.
 */
@RunWith(AndroidJUnit4::class)
class GnssDiagnosticContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(uiState: VMGnssDiagnostic.UiState, onRequestPermission: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
        composeRule.setContent {
            CpuInfoTheme {
                GnssDiagnosticContent(uiState = uiState, onRequestPermission = onRequestPermission, onOpenSettings = onOpenSettings)
            }
        }
    }

    @Test
    fun noPermission_canRequest_showsGrantButton() {
        setContent(VMGnssDiagnostic.UiState(hasLocationPermission = false, canRequestPermission = true))

        composeRule.onNodeWithText(appContext.getString(R.string.gnss_diag_permission_rationale)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.network_grant_permission_button)).assertExists()
    }

    @Test
    fun noPermission_cannotRequest_showsOpenSettingsButton() {
        setContent(VMGnssDiagnostic.UiState(hasLocationPermission = false, canRequestPermission = false))

        composeRule.onNodeWithText(appContext.getString(R.string.network_open_settings_button)).assertExists()
    }

    @Test
    fun tappingGrantButton_invokesOnRequestPermissionExactlyOnce() {
        var count = 0
        setContent(
            VMGnssDiagnostic.UiState(hasLocationPermission = false, canRequestPermission = true),
            onRequestPermission = { count++ },
        )

        composeRule.onNodeWithText(appContext.getString(R.string.network_grant_permission_button)).performClick()

        assert(count == 1) { "expected onRequestPermission to fire exactly once, fired $count times" }
    }

    @Test
    fun hasPermission_noSnapshotYet_showsWaitingForFixMessage() {
        setContent(VMGnssDiagnostic.UiState(hasLocationPermission = true, snapshot = null))

        composeRule.onNodeWithText(appContext.getString(R.string.gnss_diag_waiting_for_fix)).assertExists()
    }

    @Test
    fun hasPermission_withSnapshot_showsUsedInFixAndSatelliteRow() {
        val snapshot = GnssStatusMapper.fromSatellites(
            listOf(GnssSatelliteInfo("GPS", 5, 32.5f, usedInFix = true, carrierFrequencyHz = 1575.42e6f)),
        )
        setContent(VMGnssDiagnostic.UiState(hasLocationPermission = true, snapshot = snapshot))

        composeRule.onNodeWithText(appContext.getString(R.string.gnss_diag_used_in_fix, 1, 1)).assertExists()
        val yes = appContext.getString(R.string.yes)
        composeRule.onNodeWithText(appContext.getString(R.string.gnss_diag_satellite_row, "GPS", 5, 32.5f, yes)).assertExists()
    }

    @Test
    fun dualFrequencySatellite_showsDualFrequencyMessage() {
        val snapshot = GnssStatusMapper.fromSatellites(
            listOf(
                GnssSatelliteInfo("GPS", 1, 30f, usedInFix = true, carrierFrequencyHz = 1575.42e6f),
                GnssSatelliteInfo("GPS", 1, 28f, usedInFix = true, carrierFrequencyHz = 1176.45e6f),
            ),
        )
        setContent(VMGnssDiagnostic.UiState(hasLocationPermission = true, snapshot = snapshot))

        composeRule.onNodeWithText(appContext.getString(R.string.gnss_diag_dual_frequency, 1)).assertExists()
    }
}
