package com.galaxyjoy.cpuinfo.feat.networktile

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class ServiceNetworkTileTest {

    private val connectivityManager: ConnectivityManager = mockk()
    private val service = ServiceNetworkTile().apply {
        connectivityManager = this@ServiceNetworkTileTest.connectivityManager
    }

    private fun withTransports(vararg transports: Int) {
        val network: Network = mockk()
        val capabilities: NetworkCapabilities = mockk()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasTransport(any()) } answers { transports.contains(firstArg()) }
    }

    @Test
    fun `no active network reports Offline`() {
        every { connectivityManager.activeNetwork } returns null

        assertEquals("Offline" to false, service.connectionState())
    }

    @Test
    fun `wifi transport reports Wi-Fi connected`() {
        withTransports(NetworkCapabilities.TRANSPORT_WIFI)

        assertEquals("Wi-Fi" to true, service.connectionState())
    }

    @Test
    fun `cellular transport reports Mobile data connected`() {
        withTransports(NetworkCapabilities.TRANSPORT_CELLULAR)

        assertEquals("Mobile data" to true, service.connectionState())
    }

    @Test
    fun `ethernet transport reports Ethernet connected`() {
        withTransports(NetworkCapabilities.TRANSPORT_ETHERNET)

        assertEquals("Ethernet" to true, service.connectionState())
    }

    @Test
    fun `capabilities with no recognized transport reports Offline`() {
        withTransports()

        assertEquals("Offline" to false, service.connectionState())
    }
}
