package com.galaxyjoy.cpuinfo.feat.p2pcompare

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class DeviceComparePayloadTest {

    @Test
    fun `encode then decode round-trips exactly`() {
        val payload = DeviceComparePayload.create("Pixel 7 Pro", 12_000_000_000L, 256_000_000_000L)

        val decoded = DeviceComparePayload.decode(DeviceComparePayload.encode(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `decode trims surrounding whitespace from a pasted code`() {
        val payload = DeviceComparePayload.create("Pixel 7 Pro", 12_000_000_000L, 256_000_000_000L)
        val code = "  \n${DeviceComparePayload.encode(payload)}\n  "

        assertEquals(payload, DeviceComparePayload.decode(code))
    }

    @Test
    fun `decode returns null for empty string`() {
        assertNull(DeviceComparePayload.decode(""))
    }

    @Test
    fun `decode returns null for garbage text`() {
        assertNull(DeviceComparePayload.decode("not json at all"))
    }

    @Test
    fun `decode returns null for valid JSON missing required fields`() {
        assertNull(DeviceComparePayload.decode("""{"version":1}"""))
    }

    @Test
    fun `decode returns null when deviceModel is blank`() {
        assertNull(DeviceComparePayload.decode("""{"version":1,"deviceModel":"","ramBytes":100,"storageBytes":100}"""))
    }

    @Test
    fun `decode returns null when ramBytes is zero or negative`() {
        assertNull(DeviceComparePayload.decode("""{"version":1,"deviceModel":"X","ramBytes":0,"storageBytes":100}"""))
        assertNull(DeviceComparePayload.decode("""{"version":1,"deviceModel":"X","ramBytes":-5,"storageBytes":100}"""))
    }

    @Test
    fun `decode returns null when storageBytes is zero or negative`() {
        assertNull(DeviceComparePayload.decode("""{"version":1,"deviceModel":"X","ramBytes":100,"storageBytes":0}"""))
    }

    @Test
    fun `decode accepts a well-formed hand-written JSON code`() {
        val decoded = DeviceComparePayload.decode(
            """{"version":1,"deviceModel":"Galaxy S24 Ultra","ramBytes":12000000000,"storageBytes":256000000000}""",
        )

        assertEquals(DeviceComparePayload(1, "Galaxy S24 Ultra", 12_000_000_000L, 256_000_000_000L), decoded)
    }
}
