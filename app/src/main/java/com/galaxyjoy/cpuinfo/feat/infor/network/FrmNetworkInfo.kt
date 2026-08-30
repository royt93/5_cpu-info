package com.galaxyjoy.cpuinfo.feat.infor.network

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * #5 "Network Info" — Wi-Fi/mobile/VPN status. Wi-Fi SSID/BSSID need a runtime permission (see
 * [NetworkPermissions]) — this app's first-ever runtime permission request; every other field is
 * shown regardless of grant state.
 */
@AndroidEntryPoint
class FrmNetworkInfo : Fragment() {

    private val viewModel: VMNetworkInfo by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Timber.d("Network Wi-Fi detail permission granted=$granted")
        markAskedBefore()
        refreshUiState()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                NetworkInfoScreen(
                    uiState = uiState,
                    onRequestPermission = { permissionLauncher.launch(NetworkPermissions.wifiDetailPermission()) },
                    onOpenSettings = ::openAppSettings,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshUiState()
    }

    override fun onResume() {
        super.onResume()
        // Catches the user granting the permission via app Settings and coming back.
        refreshUiState()
    }

    private fun refreshUiState() {
        val permission = NetworkPermissions.wifiDetailPermission()
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            permission,
        ) == PackageManager.PERMISSION_GRANTED
        // Before the very first request, shouldShowRequestPermissionRationale() returns false
        // (nothing to show a rationale for yet) — indistinguishable from the "denied forever"
        // state it also returns false for. askedBefore (persisted, survives process death)
        // disambiguates: false only means "never asked yet", so it's still safe to request.
        val canRequestPermission = hasPermission ||
            shouldShowRequestPermissionRationale(permission) ||
            !askedBefore()
        viewModel.refresh(hasPermission = hasPermission, canRequestPermission = canRequestPermission)
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
        private const val PREFS_NAME = "network_info_prefs"
        private const val KEY_ASKED_BEFORE = "wifi_permission_asked_before"
    }
}
