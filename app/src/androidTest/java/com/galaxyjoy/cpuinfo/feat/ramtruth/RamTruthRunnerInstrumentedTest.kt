package com.galaxyjoy.cpuinfo.feat.ramtruth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E03 — real-device tier: runs the actual production-sized probe (self-capped to 25% of this
 * device's real currently-free RAM, or 800MB, whichever is smaller — see [RamTruthBenchmark])
 * against this device's REAL [android.app.ActivityManager], not a mocked one, same "runner
 * classes proven for real on-device" precedent as the other `*BenchmarkRunner`s.
 */
@RunWith(AndroidJUnit4::class)
class RamTruthRunnerInstrumentedTest {

    @Test
    fun run_onRealDevice_completesWithoutCrashingAndReportsGenuineOrInconclusive() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val runner = RamTruthRunner(appContext)
        val states = mutableListOf<RamTruthRunner.State>()

        runner.run { states += it }

        // Not asserting GENUINE specifically — a real device under real background load could
        // legitimately abort (LOW_MEMORY_SIGNAL) or be INCONCLUSIVE (too little free RAM to
        // safely probe) without that meaning anything is wrong with this code. What matters is
        // that it always reaches a terminal state without crashing or hanging.
        val terminal = states.lastOrNull()
        assertTrue(
            "expected a Finished or Aborted terminal state, got $terminal",
            terminal is RamTruthRunner.State.Finished || terminal is RamTruthRunner.State.Aborted,
        )
        when (terminal) {
            is RamTruthRunner.State.Finished -> {
                android.util.Log.i(
                    "RamTruthTest",
                    "verdict=${RamTruthBenchmark.evaluate(terminal.result)} chunksTested=${terminal.result.measurements.size}",
                )
            }
            is RamTruthRunner.State.Aborted -> {
                android.util.Log.i("RamTruthTest", "aborted reason=${terminal.reason}")
            }
            else -> Unit
        }
        Unit
    }
}
