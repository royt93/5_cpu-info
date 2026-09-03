package com.galaxyjoy.cpuinfo.feat.p2pcompare

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VMP2PCompare @Inject constructor(
    payloadProvider: P2PComparePayloadProvider,
) : ViewModel() {

    sealed interface UiState {
        data class Export(val pastedCode: String = "", val parseError: Boolean = false) : UiState
        data class Result(val comparison: DeviceCompareEvaluator.Result) : UiState
    }

    val localPayload: DeviceComparePayload = payloadProvider.buildLocalPayload()

    private val _uiState = MutableStateFlow<UiState>(UiState.Export())
    val uiState = _uiState.asStateFlow()

    fun onPastedCodeChanged(code: String) {
        _uiState.value = UiState.Export(pastedCode = code)
    }

    fun onCompareClicked() {
        val state = _uiState.value as? UiState.Export ?: return
        val remote = DeviceComparePayload.decode(state.pastedCode)
        if (remote == null) {
            _uiState.value = state.copy(parseError = true)
            return
        }
        _uiState.value = UiState.Result(DeviceCompareEvaluator.compare(localPayload, remote))
    }

    fun onBackClicked() {
        _uiState.value = UiState.Export()
    }
}
