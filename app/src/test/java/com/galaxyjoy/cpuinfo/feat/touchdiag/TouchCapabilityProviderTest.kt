package com.galaxyjoy.cpuinfo.feat.touchdiag

import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.MotionEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TouchCapabilityProviderTest {

    private val inputManager: InputManager = mockk()

    private fun rangeOf(min: Float, max: Float): InputDevice.MotionRange {
        val range: InputDevice.MotionRange = mockk()
        every { range.min } returns min
        every { range.max } returns max
        return range
    }

    @Test
    fun `no touchscreen source among input devices returns null`() {
        val keyboard: InputDevice = mockk()
        every { keyboard.supportsSource(InputDevice.SOURCE_TOUCHSCREEN) } returns false
        every { inputManager.inputDeviceIds } returns intArrayOf(1)
        every { inputManager.getInputDevice(1) } returns keyboard

        assertNull(TouchCapabilityProvider(inputManager).getTouchscreenCapabilities())
    }

    @Test
    fun `empty device id list returns null`() {
        every { inputManager.inputDeviceIds } returns intArrayOf()

        assertNull(TouchCapabilityProvider(inputManager).getTouchscreenCapabilities())
    }

    @Test
    fun `getInputDevice returning null for a stale id is skipped`() {
        val touchscreen: InputDevice = mockk()
        every { touchscreen.supportsSource(InputDevice.SOURCE_TOUCHSCREEN) } returns true
        every { touchscreen.name } returns "Real Touchscreen"
        every { touchscreen.getMotionRange(any()) } returns null

        every { inputManager.inputDeviceIds } returns intArrayOf(1, 2)
        every { inputManager.getInputDevice(1) } returns null
        every { inputManager.getInputDevice(2) } returns touchscreen

        val result = TouchCapabilityProvider(inputManager).getTouchscreenCapabilities()

        assertEquals("Real Touchscreen", result?.deviceName)
    }

    @Test
    fun `maps each axis MotionRange into the matching field`() {
        val touchscreen: InputDevice = mockk()
        every { touchscreen.supportsSource(InputDevice.SOURCE_TOUCHSCREEN) } returns true
        every { touchscreen.name } returns "goodix_ts"
        every { touchscreen.getMotionRange(MotionEvent.AXIS_PRESSURE) } returns rangeOf(0f, 1f)
        every { touchscreen.getMotionRange(MotionEvent.AXIS_SIZE) } returns rangeOf(0f, 0.5f)
        every { touchscreen.getMotionRange(MotionEvent.AXIS_TOUCH_MAJOR) } returns rangeOf(0f, 240f)
        every { touchscreen.getMotionRange(MotionEvent.AXIS_ORIENTATION) } returns null

        every { inputManager.inputDeviceIds } returns intArrayOf(5)
        every { inputManager.getInputDevice(5) } returns touchscreen

        val result = TouchCapabilityProvider(inputManager).getTouchscreenCapabilities()

        assertEquals("goodix_ts", result?.deviceName)
        assertEquals(0f..1f, result?.pressureRange)
        assertEquals(0f..0.5f, result?.sizeRange)
        assertEquals(0f..240f, result?.touchMajorRange)
        assertNull(result?.orientationRange)
    }
}
