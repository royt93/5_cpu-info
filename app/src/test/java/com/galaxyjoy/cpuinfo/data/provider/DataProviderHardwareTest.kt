package com.galaxyjoy.cpuinfo.data.provider

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataProviderHardwareTest {

    private val packageManager: PackageManager = mockk()
    private val contentResolver: ContentResolver = mockk()
    private val wifiManager: WifiManager = mockk()
    private val provider = DataProviderHardware(
        packageManager = packageManager,
        contentResolver = contentResolver,
        wifiManager = wifiManager,
        irManager = null,
    )

    private fun stubFeature(feature: String, has: Boolean) {
        every { packageManager.hasSystemFeature(feature) } returns has
    }

    @Test
    fun `wifi sub-features are false when device has no Wi-Fi at all`() {
        stubFeature(PackageManager.FEATURE_WIFI, false)
        stubFeature(PackageManager.FEATURE_BLUETOOTH, false)
        stubFeature(PackageManager.FEATURE_BLUETOOTH_LE, false)
        stubFeature(PackageManager.FEATURE_LOCATION_GPS, false)
        stubFeature(PackageManager.FEATURE_NFC, false)
        stubFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION, false)
        stubFeature(PackageManager.FEATURE_USB_HOST, false)

        val data = provider.getHardwareData()

        assertFalse(data.hasWifi)
        assertFalse(data.hasWifiAware)
        assertFalse(data.hasWifiDirect)
        assertFalse(data.hasWifiPasspoint)
        assertFalse(data.hasWifi5Ghz)
        assertFalse(data.hasWifiP2p)
    }

    @Test
    fun `wifi sub-features reflect PackageManager and WifiManager when Wi-Fi is present`() {
        stubFeature(PackageManager.FEATURE_WIFI, true)
        stubFeature(PackageManager.FEATURE_WIFI_AWARE, true)
        stubFeature(PackageManager.FEATURE_WIFI_DIRECT, false)
        stubFeature(PackageManager.FEATURE_WIFI_PASSPOINT, true)
        stubFeature(PackageManager.FEATURE_BLUETOOTH, false)
        stubFeature(PackageManager.FEATURE_BLUETOOTH_LE, false)
        stubFeature(PackageManager.FEATURE_LOCATION_GPS, false)
        stubFeature(PackageManager.FEATURE_NFC, false)
        stubFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION, false)
        stubFeature(PackageManager.FEATURE_USB_HOST, false)
        every { wifiManager.is5GHzBandSupported } returns true
        every { wifiManager.isP2pSupported } returns false

        val data = provider.getHardwareData()

        assertTrue(data.hasWifi)
        assertTrue(data.hasWifiAware)
        assertFalse(data.hasWifiDirect)
        assertTrue(data.hasWifiPasspoint)
        assertTrue(data.hasWifi5Ghz)
        assertFalse(data.hasWifiP2p)
    }

    @Test
    fun `hasIrEmitter is false when no ConsumerIrManager is available`() {
        stubFeature(PackageManager.FEATURE_WIFI, false)
        stubFeature(PackageManager.FEATURE_BLUETOOTH, false)
        stubFeature(PackageManager.FEATURE_BLUETOOTH_LE, false)
        stubFeature(PackageManager.FEATURE_LOCATION_GPS, false)
        stubFeature(PackageManager.FEATURE_NFC, false)
        stubFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION, false)
        stubFeature(PackageManager.FEATURE_USB_HOST, false)

        assertFalse(provider.getHardwareData().hasIrEmitter)
    }
}
