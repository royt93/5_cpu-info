package com.galaxyjoy.cpuinfo.feat.gnssdiag

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VMGnssDiagnosticTest {

    private val snapshot = GnssStatusMapper.fromSatellites(
        listOf(GnssSatelliteInfo("GPS", 1, 30f, usedInFix = true, carrierFrequencyHz = 1575.42e6f)),
    )

    @Test
    fun `initial state has no permission and no snapshot`() {
        val state = VMGnssDiagnostic().uiState.value

        assertFalse(state.hasLocationPermission)
        assertTrue(state.canRequestPermission)
        assertNull(state.snapshot)
    }

    @Test
    fun `onSatelliteStatusChanged stores the snapshot`() {
        val vm = VMGnssDiagnostic()

        vm.onSatelliteStatusChanged(snapshot)

        assertEquals(snapshot, vm.uiState.value.snapshot)
    }

    @Test
    fun `refreshPermissionState granting permission keeps an existing snapshot`() {
        val vm = VMGnssDiagnostic()
        vm.onSatelliteStatusChanged(snapshot)

        vm.refreshPermissionState(hasPermission = true, canRequestPermission = true)

        assertTrue(vm.uiState.value.hasLocationPermission)
        assertNotNull(vm.uiState.value.snapshot)
    }

    @Test
    fun `refreshPermissionState losing permission clears any stale snapshot`() {
        val vm = VMGnssDiagnostic()
        vm.onSatelliteStatusChanged(snapshot)
        vm.refreshPermissionState(hasPermission = true, canRequestPermission = true)

        vm.refreshPermissionState(hasPermission = false, canRequestPermission = true)

        assertFalse(vm.uiState.value.hasLocationPermission)
        assertNull(vm.uiState.value.snapshot)
    }

    @Test
    fun `refreshPermissionState propagates canRequestPermission`() {
        val vm = VMGnssDiagnostic()

        vm.refreshPermissionState(hasPermission = false, canRequestPermission = false)

        assertFalse(vm.uiState.value.canRequestPermission)
    }
}
