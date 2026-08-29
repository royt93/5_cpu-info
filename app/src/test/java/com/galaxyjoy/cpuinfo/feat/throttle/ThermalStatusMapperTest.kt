package com.galaxyjoy.cpuinfo.feat.throttle

import com.galaxyjoy.cpuinfo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalStatusMapperTest {

    @Test
    fun `mappingFor NONE is OK severity`() {
        val mapping = ThermalStatusMapper.mappingFor(0)
        assertEquals(R.string.thermal_status_none, mapping.labelRes)
        assertEquals(ThermalStatusMapper.Severity.OK, mapping.severity)
    }

    @Test
    fun `mappingFor LIGHT and MODERATE are WARNING severity`() {
        assertEquals(ThermalStatusMapper.Severity.WARNING, ThermalStatusMapper.mappingFor(1).severity)
        assertEquals(ThermalStatusMapper.Severity.WARNING, ThermalStatusMapper.mappingFor(2).severity)
    }

    @Test
    fun `mappingFor SEVERE through SHUTDOWN are DANGER severity`() {
        for (status in 3..6) {
            assertEquals(ThermalStatusMapper.Severity.DANGER, ThermalStatusMapper.mappingFor(status).severity)
        }
    }

    @Test
    fun `mappingFor unknown status falls back to unknown label with OK severity`() {
        val mapping = ThermalStatusMapper.mappingFor(-1)
        assertEquals(R.string.thermal_status_unknown, mapping.labelRes)
        assertEquals(ThermalStatusMapper.Severity.OK, mapping.severity)

        val outOfRange = ThermalStatusMapper.mappingFor(99)
        assertEquals(R.string.thermal_status_unknown, outOfRange.labelRes)
    }

    @Test
    fun `isThrottling is true only for LIGHT through SHUTDOWN`() {
        assertFalse(ThermalStatusMapper.isThrottling(0))
        assertFalse(ThermalStatusMapper.isThrottling(-1))
        for (status in 1..6) {
            assertTrue(ThermalStatusMapper.isThrottling(status))
        }
        assertFalse(ThermalStatusMapper.isThrottling(7))
    }
}
