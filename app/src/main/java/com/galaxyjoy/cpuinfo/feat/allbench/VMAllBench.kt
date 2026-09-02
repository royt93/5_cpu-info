package com.galaxyjoy.cpuinfo.feat.allbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmarkRunner
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmarkRunner
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmarkRunner
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleTestRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * U17 — orchestrates the 4 existing benchmarks (Throttle/Storage/RAM/GPU) sequentially, reusing
 * each runner exactly as-is (no shared "AllBenchRunner" class — the sequencing itself is thin
 * enough to live directly here rather than behind another layer of indirection). Throttle/Storage/
 * RAM all expose a suspend `run(onState)` callback that completes when the phase is done; GPU is
 * the odd one out (its workload only runs while a real `GLSurfaceView` is attached in
 * [AllBenchScreen], driven from the GL thread — see [GpuBenchmarkRunner]'s own doc), so it's
 * awaited via `state.first { ... }` instead once [AllBenchScreen] mounts the surface for
 * [UiState.Running] with [Step.GPU].
 *
 * If any step aborts (overheat/insufficient memory/user stop), the whole suite stops there rather
 * than continuing on a device that already signaled it shouldn't keep running.
 */
@HiltViewModel
class VMAllBench @Inject constructor(
    private val throttleRunner: ThrottleTestRunner,
    private val storageRunner: StorageBenchmarkRunner,
    private val ramRunner: RamBenchmarkRunner,
    private val gpuRunner: GpuBenchmarkRunner,
) : ViewModel() {

    enum class Step { THROTTLE, STORAGE, RAM, GPU }

    data class Results(
        val throttle: ThrottleFingerprint.Result,
        val storage: StorageBenchmark.Result,
        val ram: RamBenchmark.Result,
        val gpu: GpuBenchmark.Result,
    )

    sealed interface UiState {
        data object Idle : UiState
        data class Running(val step: Step) : UiState
        data class Done(val results: Results) : UiState
        data class Aborted(val step: Step) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var job: Job? = null

    fun newGpuRenderer() = gpuRunner.newRenderer()

    fun onStartClicked() {
        if (job?.isActive == true) return
        job = viewModelScope.launch { runSuite() }
    }

    fun onStopClicked() {
        throttleRunner.requestStop()
        storageRunner.requestStop()
        ramRunner.requestStop()
        gpuRunner.requestStop()
    }

    fun onDoneClicked() {
        _uiState.value = UiState.Idle
    }

    private suspend fun runSuite() {
        _uiState.value = UiState.Running(Step.THROTTLE)
        var throttleResult: ThrottleFingerprint.Result? = null
        throttleRunner.run { state ->
            if (state is ThrottleTestRunner.State.Finished) throttleResult = state.result
        }
        val throttle = throttleResult
        if (throttle == null || throttle.aborted) return abort(Step.THROTTLE)

        _uiState.value = UiState.Running(Step.STORAGE)
        var storageResult: StorageBenchmark.Result? = null
        var storageAborted = false
        storageRunner.run { state ->
            when (state) {
                is StorageBenchmarkRunner.State.Finished -> storageResult = state.result
                StorageBenchmarkRunner.State.Aborted -> storageAborted = true
                else -> {}
            }
        }
        val storage = storageResult
        if (storageAborted || storage == null) return abort(Step.STORAGE)

        _uiState.value = UiState.Running(Step.RAM)
        var ramResult: RamBenchmark.Result? = null
        var ramAborted = false
        ramRunner.run { state ->
            when (state) {
                is RamBenchmarkRunner.State.Finished -> ramResult = state.result
                is RamBenchmarkRunner.State.Aborted -> ramAborted = true
                else -> {}
            }
        }
        val ram = ramResult
        if (ramAborted || ram == null) return abort(Step.RAM)

        _uiState.value = UiState.Running(Step.GPU)
        gpuRunner.start()
        val gpuFinal = gpuRunner.state.first {
            it is GpuBenchmarkRunner.State.Finished || it is GpuBenchmarkRunner.State.Aborted
        }
        val gpu = (gpuFinal as? GpuBenchmarkRunner.State.Finished)?.result
        if (gpu == null) return abort(Step.GPU)

        _uiState.value = UiState.Done(Results(throttle, storage, ram, gpu))
    }

    private fun abort(step: Step) {
        _uiState.value = UiState.Aborted(step)
    }

    override fun onCleared() {
        onStopClicked()
        super.onCleared()
    }
}
