package com.galaxyjoy.cpuinfo.feat.benchhistory

import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BenchHistoryExporterTest {

    private val throttlePrefs: ThrottleResultPrefs = mockk()
    private val storagePrefs: StorageBenchResultPrefs = mockk()
    private val ramPrefs: RamBenchResultPrefs = mockk()
    private val gpuPrefs: GpuBenchResultPrefs = mockk()

    private val exporter = BenchHistoryExporter(throttlePrefs, storagePrefs, ramPrefs, gpuPrefs)

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        every { throttlePrefs.getHistory() } returns emptyList()
        every { storagePrefs.getHistory() } returns emptyList()
        every { ramPrefs.getHistory() } returns emptyList()
        every { gpuPrefs.getHistory() } returns emptyList()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `decimal uses a dot regardless of default locale`() {
        for (locale in listOf(Locale.GERMANY, Locale("vi", "VN"), Locale.US)) {
            Locale.setDefault(locale)
            assertEquals("3.1", exporter.decimal(3.14159), "wrong decimal separator with default locale $locale")
        }
    }

    @Test
    fun `timestamp formats consistently regardless of default locale`() {
        val epochMs = 1_725_000_000_000L
        val expected = exporter.timestamp(epochMs)
        for (locale in listOf(Locale.GERMANY, Locale("vi", "VN"))) {
            Locale.setDefault(locale)
            assertEquals(expected, exporter.timestamp(epochMs), "timestamp drifted with default locale $locale")
        }
    }

    @Test
    fun `all 4 sections present with correct headers even with empty history`() {
        val csv = exporter.buildCsv()

        assertTrue(csv.contains("=== CPU Throttle ==="))
        assertTrue(csv.contains("timestamp,peak_freq_mhz,sustained_freq_mhz,throttle_percent,max_temp_c,ops_per_second"))
        assertTrue(csv.contains("=== Storage ==="))
        assertTrue(csv.contains("timestamp,seq_write_mb_s,seq_read_mb_s,random_write_ops_s,random_read_ops_s,hash_mb_s"))
        assertTrue(csv.contains("=== RAM ==="))
        assertTrue(csv.contains("timestamp,write_mb_s,read_mb_s"))
        assertTrue(csv.contains("=== GPU ==="))
        assertTrue(csv.contains("timestamp,avg_fps"))
    }

    @Test
    fun `throttle rows render every field in order`() {
        every { throttlePrefs.getHistory() } returns listOf(
            ThrottleResultPrefs.SavedResult(
                timestampMs = 1_725_000_000_000L, peakFreqMhz = 2800, sustainedFreqMhz = 2600,
                throttlePercent = 7, maxTempC = 35, opsPerSecond = 5_000_000L,
            ),
        )

        val csv = exporter.buildCsv()

        val expectedRow = "${exporter.timestamp(1_725_000_000_000L)},2800,2600,7,35,5000000"
        assertTrue(csv.contains(expectedRow), "csv was:\n$csv")
    }

    @Test
    fun `storage rows render every field with 1-decimal formatting in order`() {
        every { storagePrefs.getHistory() } returns listOf(
            StorageBenchResultPrefs.SavedResult(
                timestampMs = 1_725_000_000_000L, seqWriteMbPerSec = 100.0, seqReadMbPerSec = 200.0,
                randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
            ),
        )

        val csv = exporter.buildCsv()

        val expectedRow = "${exporter.timestamp(1_725_000_000_000L)},100.0,200.0,300.0,400.0,50.0"
        assertTrue(csv.contains(expectedRow), "csv was:\n$csv")
    }

    @Test
    fun `ram rows render every field with 1-decimal formatting in order`() {
        every { ramPrefs.getHistory() } returns listOf(
            RamBenchResultPrefs.SavedResult(timestampMs = 1_725_000_000_000L, writeMbPerSec = 4000.0, readMbPerSec = 5000.0),
        )

        val csv = exporter.buildCsv()

        val expectedRow = "${exporter.timestamp(1_725_000_000_000L)},4000.0,5000.0"
        assertTrue(csv.contains(expectedRow), "csv was:\n$csv")
    }

    @Test
    fun `gpu rows render every field with 1-decimal formatting`() {
        every { gpuPrefs.getHistory() } returns listOf(
            GpuBenchResultPrefs.SavedResult(timestampMs = 1_725_000_000_000L, avgFps = 55.5),
        )

        val csv = exporter.buildCsv()

        val expectedRow = "${exporter.timestamp(1_725_000_000_000L)},55.5"
        assertTrue(csv.contains(expectedRow), "csv was:\n$csv")
    }

    @Test
    fun `multiple entries of the same type all render in order`() {
        every { gpuPrefs.getHistory() } returns listOf(
            GpuBenchResultPrefs.SavedResult(timestampMs = 1L, avgFps = 30.0),
            GpuBenchResultPrefs.SavedResult(timestampMs = 2L, avgFps = 60.0),
        )

        val csv = exporter.buildCsv()

        val firstIndex = csv.indexOf("${exporter.timestamp(1L)},30.0")
        val secondIndex = csv.indexOf("${exporter.timestamp(2L)},60.0")
        assertTrue(firstIndex >= 0 && secondIndex >= 0 && firstIndex < secondIndex, "csv was:\n$csv")
    }
}
