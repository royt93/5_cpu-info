package com.galaxyjoy.cpuinfo.feat.infor.network

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VMNetworkInfo @Inject constructor(
    private val networkInfoProvider: NetworkInfoProvider,
) : ViewModel() {

    data class UiState(
        val hasWifiDetailPermission: Boolean,
        val canRequestPermission: Boolean,
        val wifi: NetworkInfoProvider.WifiSnapshot,
        val mobile: NetworkInfoProvider.MobileSnapshot,
        val vpn: NetworkInfoProvider.VpnSnapshot,
    )

    private val _uiState = MutableStateFlow(buildState(hasPermission = false, canRequest = true))
    val uiState = _uiState.asStateFlow()

    /**
     * @param canRequestPermission false once the user has denied the permission with "don't ask
     * again" — the UI should offer a Settings deep link instead of re-triggering the system
     * dialog at that point ([androidx.fragment.app.Fragment.shouldShowRequestPermissionRationale]
     * returning false after a denial is how the caller detects this).
     */
    fun refresh(hasPermission: Boolean, canRequestPermission: Boolean) {
        _uiState.value = buildState(hasPermission, canRequestPermission)
    }

    private fun buildState(hasPermission: Boolean, canRequest: Boolean) = UiState(
        hasWifiDetailPermission = hasPermission,
        canRequestPermission = canRequest,
        wifi = networkInfoProvider.wifiSnapshot(hasPermission),
        mobile = networkInfoProvider.mobileSnapshot(),
        vpn = networkInfoProvider.vpnSnapshot(),
    )
}
