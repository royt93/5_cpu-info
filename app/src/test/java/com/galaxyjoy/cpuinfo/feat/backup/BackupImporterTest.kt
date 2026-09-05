package com.galaxyjoy.cpuinfo.feat.backup

import android.content.Context
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.setting.FrmSettings
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.Prefs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/** [BackupImporter.apply]'s orchestration only — [BackupImporter.readFrom] does real file I/O and
 * is covered by [BackupInstrumentedTest] instead, same split as this codebase's other
 * Notifier/Logic-style classes. */
class BackupImporterTest {

    private val context: Context = mockk(relaxed = true)
    private val prefs: Prefs = mockk(relaxed = true)
    private val throttlePrefs: ThrottleResultPrefs = mockk(relaxed = true)
    private val storagePrefs: StorageBenchResultPrefs = mockk(relaxed = true)
    private val ramPrefs: RamBenchResultPrefs = mockk(relaxed = true)
    private val gpuPrefs: GpuBenchResultPrefs = mockk(relaxed = true)
    private lateinit var importer: BackupImporter

    @Before
    fun setUp() {
        importer = BackupImporter(context, prefs, throttlePrefs, storagePrefs, ramPrefs, gpuPrefs, DispatchersProvider())
    }

    @Test
    fun `apply replaces only the histories present in the bundle`() {
        val throttleEntry = ThrottleResultPrefs.SavedResult(1L, 2400, 2200, 12, 34, 4_000_000L)
        val bundle = BackupBundle(version = 1, throttleHistory = listOf(throttleEntry))

        importer.apply(bundle)

        verify(exactly = 1) { throttlePrefs.replaceHistory(listOf(throttleEntry)) }
        verify(exactly = 0) { storagePrefs.replaceHistory(any()) }
        verify(exactly = 0) { ramPrefs.replaceHistory(any()) }
        verify(exactly = 0) { gpuPrefs.replaceHistory(any()) }
    }

    @Test
    fun `apply skips the replace entirely when a history field is null, leaving existing data untouched`() {
        val bundle = BackupBundle(version = 1)

        importer.apply(bundle)

        verify(exactly = 0) { throttlePrefs.replaceHistory(any()) }
    }

    @Test
    fun `apply still replaces with an explicit empty list when the bundle says so`() {
        val bundle = BackupBundle(version = 1, throttleHistory = emptyList())

        importer.apply(bundle)

        verify(exactly = 1) { throttlePrefs.replaceHistory(emptyList()) }
    }

    @Test
    fun `apply writes temperature unit and theme when present`() {
        val bundle = BackupBundle(version = 1, temperatureUnit = "1", theme = "dark")

        importer.apply(bundle)

        verify(exactly = 1) { prefs.insert(FrmSettings.KEY_TEMPERATURE_UNIT, "1") }
        verify(exactly = 1) { prefs.insert(FrmSettings.KEY_THEME_CONFIG, "dark") }
    }

    @Test
    fun `apply does not touch temperature unit or theme prefs when absent from the bundle`() {
        val bundle = BackupBundle(version = 1)

        importer.apply(bundle)

        verify(exactly = 0) { prefs.insert(FrmSettings.KEY_TEMPERATURE_UNIT, any()) }
        verify(exactly = 0) { prefs.insert(FrmSettings.KEY_THEME_CONFIG, any()) }
    }
}
