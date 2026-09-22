package com.galaxyjoy.cpuinfo.feat.gnssdiag

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VMGnssDiagnostic @Inject constructor() : ViewModel() {

    data class UiState(
        val hasLocationPermission: Boolean = false,
        val canRequestPermission: Boolean = true,
        val snapshot: GnssStatusSnapshot? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    /** Same disambiguation as [com.galaxyjoy.cpuinfo.feat.infor.network.VMNetworkInfo.refresh]. */
    fun refreshPermissionState(hasPermission: Boolean, canRequestPermission: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = hasPermission,
            canRequestPermission = canRequestPermission,
            // A revoked permission (denied via app Settings mid-session) must drop any stale
            // satellite data rather than keep showing a snapshot we no longer have access to.
            snapshot = if (hasPermission) _uiState.value.snapshot else null,
        )
    }

    fun onSatelliteStatusChanged(snapshot: GnssStatusSnapshot) {
        _uiState.value = _uiState.value.copy(snapshot = snapshot)
    }
}
