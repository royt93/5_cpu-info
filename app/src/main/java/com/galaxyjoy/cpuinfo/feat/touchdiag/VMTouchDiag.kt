package com.galaxyjoy.cpuinfo.feat.touchdiag

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class TouchPoint(val id: Int, val x: Float, val y: Float, val pressure: Float)

data class TouchDiagUiState(
    val capabilities: TouchCapabilities?,
    val activePoints: List<TouchPoint> = emptyList(),
    val maxPointersObserved: Int = 0,
    val sampleRateHz: Double? = null,
)

@HiltViewModel
class VMTouchDiag @Inject constructor(
    capabilityProvider: TouchCapabilityProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TouchDiagUiState(capabilities = capabilityProvider.getTouchscreenCapabilities()),
    )
    val uiState = _uiState.asStateFlow()

    /**
     * Called by the Compose touch grid on every pointer-event frame. [historicalTimestampsMillis]
     * is whatever sample timestamps were available since the last frame (current position plus
     * any `PointerInputChange.historical` entries) — kept as plain `Long` here so this stays
     * testable without a real `MotionEvent`/`PointerInputChange`.
     */
    fun onFrame(points: List<TouchPoint>, historicalTimestampsMillis: List<Long>) {
        val current = _uiState.value
        _uiState.value = current.copy(
            activePoints = points,
            maxPointersObserved = maxOf(current.maxPointersObserved, points.size),
            sampleRateHz = TouchSampleRateCalculator.hzFrom(historicalTimestampsMillis)
                ?: current.sampleRateHz,
        )
    }

    fun onTouchEnd() {
        _uiState.value = _uiState.value.copy(activePoints = emptyList())
    }

    fun onResetSession() {
        val current = _uiState.value
        _uiState.value = current.copy(maxPointersObserved = 0, sampleRateHz = null)
    }
}
