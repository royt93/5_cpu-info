package com.galaxyjoy.cpuinfo.feat.gnssdiag

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * E09 "GNSS Satellite Signal Diagnostic" — see `doc/task/epic-05-new-ideas.md`. This app's second
 * runtime permission request ([com.galaxyjoy.cpuinfo.feat.infor.network.FrmNetworkInfo] was the
 * first) — same grant/rationale/"open Settings after denial" flow, reused deliberately rather than
 * inventing a new one.
 */
@AndroidEntryPoint
class GnssDiagnosticBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var locationManager: LocationManager

    private val vm: VMGnssDiagnostic by viewModels()

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            vm.onSatelliteStatusChanged(GnssStatusMapper.map(status))
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Timber.d("GNSS diagnostic location permission granted=$granted")
        markAskedBefore()
        refreshPermissionState()
        if (granted) startListening()
    }

    // A satellite fix can take from a few seconds to several minutes outdoors, longer indoors —
    // without this the screen can time out and sleep mid-diagnostic, same rationale as E11's
    // TouchDiagBottomSheet (see its kdoc).
    override fun onStart() {
        super.onStart()
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        refreshPermissionState()
        if (hasLocationPermission()) startListening()
    }

    override fun onStop() {
        stopListening()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                GnssDiagnosticContent(
                    uiState = uiState,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    onOpenSettings = ::openAppSettings,
                )
            }
        }
    }

    @SuppressLint("MissingPermission") // hasLocationPermission() gate above/in onStart guarantees this.
    private fun startListening() {
        locationManager.registerGnssStatusCallback(gnssCallback, null)
    }

    private fun stopListening() {
        locationManager.unregisterGnssStatusCallback(gnssCallback)
    }

    private fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private fun refreshPermissionState() {
        val hasPermission = hasLocationPermission()
        val canRequest = hasPermission ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !askedBefore()
        vm.refreshPermissionState(hasPermission = hasPermission, canRequestPermission = canRequest)
    }

    private fun openAppSettings() {
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
    }

    private fun askedBefore(): Boolean = requireContext()
        .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getBoolean(KEY_ASKED_BEFORE, false)

    private fun markAskedBefore() {
        requireContext()
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED_BEFORE, true)
            .apply()
    }

    companion object {
        const val TAG = "GnssDiagnosticBottomSheet"
        private const val PREFS_NAME = "gnss_diag_prefs"
        private const val KEY_ASKED_BEFORE = "location_permission_asked_before"
    }
}

@Composable
internal fun GnssDiagnosticContent(
    uiState: VMGnssDiagnostic.UiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.gnss_diag_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (!uiState.hasLocationPermission) {
                GnssPermissionPrompt(
                    canRequestPermission = uiState.canRequestPermission,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                )
                return@Column
            }

            val snapshot = uiState.snapshot
            if (snapshot == null) {
                Text(stringResource(R.string.gnss_diag_waiting_for_fix))
                return@Column
            }

            Text(
                stringResource(R.string.gnss_diag_used_in_fix, snapshot.usedInFixCount, snapshot.satellites.size),
                style = MaterialTheme.typography.titleMedium,
            )
            if (snapshot.dualFrequencySatelliteCount > 0) {
                Text(stringResource(R.string.gnss_diag_dual_frequency, snapshot.dualFrequencySatelliteCount))
            }
            snapshot.constellationCounts.entries.sortedByDescending { it.value }.forEach { (constellation, count) ->
                Text(
                    stringResource(R.string.gnss_diag_constellation_row, constellation, count),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Column(
                modifier = Modifier
                    .height(280.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                snapshot.satellites.sortedByDescending { it.cn0DbHz }.forEach { sat ->
                    Text(
                        stringResource(
                            R.string.gnss_diag_satellite_row,
                            sat.constellation,
                            sat.svid,
                            sat.cn0DbHz,
                            if (sat.usedInFix) stringResource(R.string.yes) else stringResource(R.string.no),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GnssPermissionPrompt(
    canRequestPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Box(modifier = Modifier.size(6.dp).background(color = Color(0xFFFFA726), shape = CircleShape))
                Text(
                    text = stringResource(R.string.gnss_diag_permission_rationale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                if (canRequestPermission) {
                    Button(onClick = onRequestPermission) {
                        Text(stringResource(R.string.network_grant_permission_button))
                    }
                } else {
                    OutlinedButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.network_open_settings_button))
                    }
                }
            }
        }
    }
}
