package com.galaxyjoy.cpuinfo.feat.touchdiag

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VMTouchDiagTest {

    private fun newVm(capabilities: TouchCapabilities? = null): VMTouchDiag {
        val provider: TouchCapabilityProvider = mockk()
        every { provider.getTouchscreenCapabilities() } returns capabilities
        return VMTouchDiag(provider)
    }

    @Test
    fun `initial state has no active points and no session stats`() {
        val state = newVm().uiState.value

        assertTrue(state.activePoints.isEmpty())
        assertEquals(0, state.maxPointersObserved)
        assertNull(state.sampleRateHz)
    }

    @Test
    fun `initial state carries the provider's capabilities`() {
        val caps = TouchCapabilities("test-ts", 0f..1f, 0f..1f, 0f..1f, 0f..1f)

        assertEquals(caps, newVm(caps).uiState.value.capabilities)
    }

    @Test
    fun `onFrame updates active points and bumps max observed`() {
        val vm = newVm()
        val twoPoints = listOf(TouchPoint(0, 1f, 1f, 0.5f), TouchPoint(1, 2f, 2f, 0.5f))

        vm.onFrame(twoPoints, listOf(0L, 10L))

        assertEquals(twoPoints, vm.uiState.value.activePoints)
        assertEquals(2, vm.uiState.value.maxPointersObserved)
    }

    @Test
    fun `maxPointersObserved never decreases when a later frame has fewer points`() {
        val vm = newVm()
        vm.onFrame(listOf(TouchPoint(0, 0f, 0f, 0f), TouchPoint(1, 0f, 0f, 0f)), listOf(0L, 10L))

        vm.onFrame(listOf(TouchPoint(0, 0f, 0f, 0f)), listOf(20L, 30L))

        assertEquals(2, vm.uiState.value.maxPointersObserved)
        assertEquals(1, vm.uiState.value.activePoints.size)
    }

    @Test
    fun `onFrame with fewer than 2 timestamps keeps the previous sample rate instead of clearing it`() {
        val vm = newVm()
        vm.onFrame(listOf(TouchPoint(0, 0f, 0f, 0f)), listOf(0L, 10L)) // 100Hz
        val rateAfterFirstFrame = vm.uiState.value.sampleRateHz

        vm.onFrame(listOf(TouchPoint(0, 1f, 1f, 0f)), listOf(20L)) // only 1 timestamp this frame

        assertEquals(rateAfterFirstFrame, vm.uiState.value.sampleRateHz)
    }

    @Test
    fun `onTouchEnd clears active points but keeps session stats`() {
        val vm = newVm()
        vm.onFrame(listOf(TouchPoint(0, 0f, 0f, 0f)), listOf(0L, 10L))

        vm.onTouchEnd()

        assertTrue(vm.uiState.value.activePoints.isEmpty())
        assertEquals(1, vm.uiState.value.maxPointersObserved)
    }

    @Test
    fun `onResetSession clears max observed and sample rate but not capabilities`() {
        val caps = TouchCapabilities("test-ts", null, null, null, null)
        val vm = newVm(caps)
        vm.onFrame(listOf(TouchPoint(0, 0f, 0f, 0f), TouchPoint(1, 0f, 0f, 0f)), listOf(0L, 10L))

        vm.onResetSession()

        assertEquals(0, vm.uiState.value.maxPointersObserved)
        assertNull(vm.uiState.value.sampleRateHz)
        assertEquals(caps, vm.uiState.value.capabilities)
    }
}
