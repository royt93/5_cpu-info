package com.galaxyjoy.cpuinfo.data.provider

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BatteryReadingTest {
    @Test fun `converts microamps including fractional readings`() {
        assertEquals(1250.123, BatteryReading.currentMa(1_250_123))
        assertEquals(-1250.0, BatteryReading.currentMa(-1_250_000))
    }

    @Test fun `raw values under the OEM already-mA threshold pass through unscaled`() {
        // Confirmed real-device bug (Samsung Galaxy S24 Ultra): some OEMs report already-scaled
        // mA instead of the documented µA, and a genuine µA reading is never this small for an
        // active phone — see BatteryReading.currentMa's doc comment.
        assertEquals(1500.0, BatteryReading.currentMa(1500))
        assertEquals(-1500.0, BatteryReading.currentMa(-1500))
        assertEquals(0.0, BatteryReading.currentMa(0))
    }

    @Test fun `charging magnitude accepts either OEM polarity and zero`() {
        assertEquals(1250.0, BatteryReading.chargingCurrentMa(1_250_000, true))
        assertEquals(1250.0, BatteryReading.chargingCurrentMa(-1_250_000, true))
        assertEquals(0.0, BatteryReading.chargingCurrentMa(0, true))
    }

    @Test fun `sentinels are hidden before absolute value conversion`() {
        for (raw in listOf(Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertNull(BatteryReading.currentMa(raw))
            assertNull(BatteryReading.chargingCurrentMa(raw, true))
        }
    }

    @Test fun `current row is hidden outside active charging regardless of sign`() {
        for (raw in listOf(-1_000_000, 0, 1_000_000)) {
            assertNull(BatteryReading.chargingCurrentMa(raw, false))
        }
    }

    @Test fun `voltage rejects missing zero negative and sentinel values`() {
        for (raw in listOf(-1, 0, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertNull(BatteryReading.voltageMv(raw))
        }
        assertEquals(4200, BatteryReading.voltageMv(4200))
    }
}
