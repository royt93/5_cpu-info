package com.galaxyjoy.cpuinfo.feat.sensortruth

import android.content.Context
import android.hardware.SensorManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E02 — real-device tier: registers real `SensorEventListener`s against this device's REAL
 * accelerometer/gyroscope, same "runner classes proven for real on-device" precedent as the other
 * `*BenchmarkRunner`s. Not asserting a specific verdict — real hardware timing is inherently
 * device-dependent; what matters is that it completes cleanly with real event data.
 */
@RunWith(AndroidJUnit4::class)
class SensorTruthRunnerInstrumentedTest {

    @Test
    fun run_onRealDevice_measuresBothSensorsWithoutCrashing() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val runner = SensorTruthRunner(sensorManager)
        val states = mutableListOf<SensorTruthRunner.State>()

        runner.run { states += it }

        val finished = states.filterIsInstance<SensorTruthRunner.State.Finished>().single()
        assertEquals(SensorTruthRunner.AUDITED_SENSOR_TYPES.size, finished.result.audits.size)
        finished.result.audits.forEach { audit ->
            android.util.Log.i(
                "SensorTruthTest",
                "sensorType=${audit.sensorType} advertisedHz=${audit.advertisedHz} actualHz=${audit.actualHz} " +
                    "jitterMs=${audit.jitterMs} eventCount=${audit.eventCount} verdict=${SensorTruthBenchmark.evaluate(audit)}",
            )
        }
        // Accelerometer exists on every real Android phone, so at least this one audit should have
        // captured real events — proves the listener/timestamp wiring actually works end to end.
        assertTrue(finished.result.audits.any { it.eventCount > 0 })
    }
}
