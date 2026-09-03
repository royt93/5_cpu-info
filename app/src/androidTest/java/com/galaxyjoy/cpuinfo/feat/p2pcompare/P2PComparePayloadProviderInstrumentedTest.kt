package com.galaxyjoy.cpuinfo.feat.p2pcompare

import android.app.ActivityManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same "real end-to-end against a real on-device provider" tier as
 * [com.galaxyjoy.cpuinfo.feat.ramwidget.RamWidgetProviderInstrumentedTest]'s
 * `updateWidget_endToEnd_withRealDataProviderRam` test — builds a payload from this device's real
 * RAM/storage, round-trips it through the exact JSON encode/decode a real share+paste would use,
 * then compares it against itself to confirm the whole pipeline (real values -> JSON text code ->
 * parsed back -> compared) works, not just each piece in isolation with synthetic numbers.
 */
@RunWith(AndroidJUnit4::class)
class P2PComparePayloadProviderInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun buildLocalPayload_reflectsRealDeviceModelAndPositiveRamAndStorage() {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val provider = P2PComparePayloadProvider(DataProviderRam(activityManager))

        val payload = provider.buildLocalPayload()

        assertTrue("deviceModel should not be blank", payload.deviceModel.isNotBlank())
        assertTrue("ramBytes should be positive on a real device", payload.ramBytes > 0)
        assertTrue("storageBytes should be positive on a real device", payload.storageBytes > 0)
    }

    @Test
    fun realPayload_survivesShareAndPasteRoundTrip_andComparesAsATieAgainstItself() {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val provider = P2PComparePayloadProvider(DataProviderRam(activityManager))
        val local = provider.buildLocalPayload()

        val shareCode = DeviceComparePayload.encode(local)
        val pastedBack = DeviceComparePayload.decode(shareCode)

        assertEquals(local, pastedBack)

        val comparison = DeviceCompareEvaluator.compare(local, pastedBack!!)
        assertEquals(DeviceCompareEvaluator.Winner.TIE, comparison.ram.winner)
        assertEquals(DeviceCompareEvaluator.Winner.TIE, comparison.storage.winner)
    }
}
