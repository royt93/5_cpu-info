package com.galaxyjoy.cpuinfo.feat.benchresultcard

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.feat.allbench.VMAllBench
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same "render a real bitmap on a real device" verification tier as
 * [com.galaxyjoy.cpuinfo.feat.devicecard.DeviceCardRendererTest] — `android.graphics.*` is stubbed
 * under this project's JVM `unitTests.isReturnDefaultValues = true`, so this can't be a unit test.
 */
@RunWith(AndroidJUnit4::class)
class BenchResultCardRendererTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val sampleResults = VMAllBench.Results(
        throttle = ThrottleFingerprint.Result(
            peakFreqMhz = 2800, sustainedFreqMhz = 2600, throttlePercent = 7, throttled = false,
            startTempC = 30, maxTempC = 35, durationMs = 30_000, aborted = false, abortReason = null,
            opsPerSecond = 5_000_000L,
        ),
        storage = StorageBenchmark.Result(
            seqWriteMbPerSec = 100.0, seqReadMbPerSec = 200.0,
            randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
        ),
        ram = RamBenchmark.Result(writeMbPerSec = 4000.0, readMbPerSec = 5000.0),
        gpu = GpuBenchmark.Result(avgFps = 55.5, frameCount = 300, durationMs = 5000),
    )

    @Test
    fun render_producesBitmapOfTheFixedCardSize() {
        val bitmap = BenchResultCardRenderer.render(appContext, sampleResults)

        assertEquals(BenchResultCardRenderer.CARD_WIDTH_PX, bitmap.width)
        assertEquals(BenchResultCardRenderer.CARD_HEIGHT_PX, bitmap.height)
    }

    @Test
    fun render_actuallyDrawsContent_notABlankCanvas() {
        val bitmap = BenchResultCardRenderer.render(appContext, sampleResults)

        val distinctColors = mutableSetOf<Int>()
        var y = 0
        while (y < bitmap.height && distinctColors.size < 2) {
            var x = 0
            while (x < bitmap.width && distinctColors.size < 2) {
                distinctColors.add(bitmap.getPixel(x, y))
                x += 4
            }
            y += 4
        }
        assertTrue("card should have more than one color drawn on it", distinctColors.size >= 2)
    }

    @Test
    fun render_withZeroAndNegativeEdgeValues_doesNotCrash() {
        val edgeResults = sampleResults.copy(
            throttle = sampleResults.throttle.copy(sustainedFreqMhz = 0L),
            storage = sampleResults.storage.copy(seqWriteMbPerSec = 0.0, seqReadMbPerSec = 0.0),
            ram = sampleResults.ram.copy(writeMbPerSec = 0.0, readMbPerSec = 0.0),
            gpu = sampleResults.gpu.copy(avgFps = 0.0),
        )

        val bitmap = BenchResultCardRenderer.render(appContext, edgeResults)

        assertEquals(BenchResultCardRenderer.CARD_WIDTH_PX, bitmap.width)
        assertEquals(BenchResultCardRenderer.CARD_HEIGHT_PX, bitmap.height)
    }
}
