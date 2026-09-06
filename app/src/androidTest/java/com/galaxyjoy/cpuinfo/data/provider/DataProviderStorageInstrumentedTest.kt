package com.galaxyjoy.cpuinfo.data.provider

import android.content.Context
import android.os.storage.StorageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** E10 — real `StorageManager`/`/proc/mounts` behavior can't be faithfully mocked (real fs types,
 * real permission enforcement), so this only asserts the real calls complete without crashing and
 * produce internally-consistent output, same style as [DataProviderBatteryInstrumentedTest]. */
@RunWith(AndroidJUnit4::class)
class DataProviderStorageInstrumentedTest {

    @Test
    fun realDeviceVolumes_doNotCrashAndHaveConsistentTotals() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storageManager = context.getSystemService(StorageManager::class.java)
        val provider = DataProviderStorage(context, storageManager)

        val internal = provider.getInternalVolume()
        assertTrue(internal.totalBytes > 0)
        assertTrue(internal.usedBytes in 0..internal.totalBytes)

        val extraVolumes = provider.getExtraVolumes(provider.getCoveredPaths())
        extraVolumes.forEach { volume ->
            assertTrue(volume.totalBytes > 0)
            assertTrue(volume.usedBytes in 0..volume.totalBytes)
        }
    }
}
