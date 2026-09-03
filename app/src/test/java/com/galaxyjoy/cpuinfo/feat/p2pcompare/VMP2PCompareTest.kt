package com.galaxyjoy.cpuinfo.feat.p2pcompare

import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class VMP2PCompareTest {

    private val localPayload = DeviceComparePayload.create("Local Phone", 8_000_000_000L, 128_000_000_000L)
    private val payloadProvider: P2PComparePayloadProvider = mockk {
        every { buildLocalPayload() } returns localPayload
    }

    private fun newViewModel() = VMP2PCompare(payloadProvider)

    @Test
    fun `starts in Export state with an empty pasted code`() {
        val vm = newViewModel()

        val state = vm.uiState.value as VMP2PCompare.UiState.Export
        assertEquals("", state.pastedCode)
        assertFalse(state.parseError)
    }

    @Test
    fun `onPastedCodeChanged updates the pasted code without setting an error`() {
        val vm = newViewModel()

        vm.onPastedCodeChanged("some text")

        val state = vm.uiState.value as VMP2PCompare.UiState.Export
        assertEquals("some text", state.pastedCode)
        assertFalse(state.parseError)
    }

    @Test
    fun `onCompareClicked with an invalid code sets parseError and stays in Export`() {
        val vm = newViewModel()
        vm.onPastedCodeChanged("garbage")

        vm.onCompareClicked()

        val state = vm.uiState.value as VMP2PCompare.UiState.Export
        assertEquals("garbage", state.pastedCode)
        assertTrue(state.parseError)
    }

    @Test
    fun `onCompareClicked with a valid code transitions to Result comparing local vs remote`() {
        val vm = newViewModel()
        val remotePayload = DeviceComparePayload.create("Remote Phone", 16_000_000_000L, 256_000_000_000L)
        vm.onPastedCodeChanged(DeviceComparePayload.encode(remotePayload))

        vm.onCompareClicked()

        val state = vm.uiState.value as VMP2PCompare.UiState.Result
        assertEquals(localPayload, state.comparison.local)
        assertEquals(remotePayload, state.comparison.remote)
        assertEquals(DeviceCompareEvaluator.Winner.REMOTE, state.comparison.ram.winner)
    }

    @Test
    fun `onBackClicked from Result returns to a fresh Export state`() {
        val vm = newViewModel()
        vm.onPastedCodeChanged(DeviceComparePayload.encode(DeviceComparePayload.create("Remote", 1L, 1L)))
        vm.onCompareClicked()

        vm.onBackClicked()

        val state = vm.uiState.value as VMP2PCompare.UiState.Export
        assertEquals("", state.pastedCode)
        assertFalse(state.parseError)
    }

    @Test
    fun `editing the pasted code after a parse error clears the error`() {
        val vm = newViewModel()
        vm.onPastedCodeChanged("garbage")
        vm.onCompareClicked()
        check((vm.uiState.value as VMP2PCompare.UiState.Export).parseError)

        vm.onPastedCodeChanged("garbage2")

        val state = vm.uiState.value as VMP2PCompare.UiState.Export
        assertEquals("garbage2", state.pastedCode)
        assertFalse(state.parseError)
    }

    @Test
    fun `onCompareClicked while already in Result state is a no-op`() {
        val vm = newViewModel()
        val remotePayload = DeviceComparePayload.create("Remote Phone", 16_000_000_000L, 256_000_000_000L)
        vm.onPastedCodeChanged(DeviceComparePayload.encode(remotePayload))
        vm.onCompareClicked()
        val resultState = vm.uiState.value as VMP2PCompare.UiState.Result

        vm.onCompareClicked()

        assertEquals(resultState, vm.uiState.value)
    }
}
