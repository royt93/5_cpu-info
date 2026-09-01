package com.galaxyjoy.cpuinfo.feat.vipreport

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * [VipDiagnosticReportRepository] depends on `BatteryManager`/`Environment`/DataStore — same class
 * of Android-framework/DataStore dependency `HardwareSnapshotProvider` has, which this codebase
 * also leaves to instrumented rather than JVM unit tests. Uses a throwaway on-device DataStore
 * file (not the app's real one) so this never touches real user prefs.
 */
@RunWith(AndroidJUnit4::class)
class VipDiagnosticReportRepositoryInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: VipDiagnosticReportRepository
    private lateinit var dataStoreFile: File

    @Before
    fun setUp() {
        dataStoreFile = File(appContext.cacheDir, "test_vip_diagnostic_${System.nanoTime()}.preferences_pb")
        val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        repository = VipDiagnosticReportRepository(
            batteryManager = batteryManager,
            batteryStatusProvider = BatteryStatusProvider(appContext),
            dataProviderRam = DataProviderRam(activityManager),
            dataStore = PreferenceDataStoreFactory.create { dataStoreFile },
        )
    }

    @After
    fun tearDown() {
        dataStoreFile.delete()
    }

    @Test
    fun captureSnapshot_returnsPlausibleRealDeviceValues() {
        val snapshot = repository.captureSnapshot()

        assertTrue(
            "RAM available % should be 0..100, was ${snapshot.ramAvailablePercentage}",
            snapshot.ramAvailablePercentage in 0..100,
        )
        assertTrue("storage total should be positive", snapshot.internalStorageTotalBytes > 0)
        assertTrue("storage free should be positive", snapshot.internalStorageFreeBytes > 0)
    }

    @Test
    fun loadHistory_onFreshDataStore_returnsEmptyList() = runBlocking {
        assertTrue(repository.loadHistory().isEmpty())
    }

    @Test
    fun saveSnapshot_thenLoadHistory_roundTripsCorrectly() = runBlocking {
        val snapshot = repository.captureSnapshot()

        repository.saveSnapshot(snapshot)
        val history = repository.loadHistory()

        assertEquals(1, history.size)
        assertEquals(snapshot, history.first())
    }

    @Test
    fun saveSnapshot_twiceSameDay_replacesRatherThanAppends() = runBlocking {
        val first = repository.captureSnapshot()
        repository.saveSnapshot(first)
        val second = first.copy(ramAvailablePercentage = first.ramAvailablePercentage.let { if (it < 50) it + 1 else it - 1 })
        repository.saveSnapshot(second)

        val history = repository.loadHistory()

        assertEquals(1, history.size)
        assertEquals(second.ramAvailablePercentage, history.first().ramAvailablePercentage)
    }

    @Test
    fun saveSnapshot_beyondCap_evictsOldestEntriesFirst() = runBlocking {
        val base = repository.captureSnapshot()
        val dayMillis = 24L * 60 * 60 * 1000
        val totalToSave = VipDiagnosticReportRepository.MAX_HISTORY_ENTRIES + 5

        repeat(totalToSave) { i ->
            repository.saveSnapshot(base.copy(timestampMillis = base.timestampMillis + i * dayMillis))
        }

        val history = repository.loadHistory()

        assertEquals(VipDiagnosticReportRepository.MAX_HISTORY_ENTRIES, history.size)
        // The 5 oldest saves (day offsets 0..4) must have been evicted; the newest 5 (offsets
        // totalToSave-5 .. totalToSave-1) must have survived.
        val survivingOffsets = history.map { (it.timestampMillis - base.timestampMillis) / dayMillis }
        assertTrue(survivingOffsets.none { it < 5 })
        assertTrue((totalToSave - 5 until totalToSave).all { it.toLong() in survivingOffsets })
    }
}
