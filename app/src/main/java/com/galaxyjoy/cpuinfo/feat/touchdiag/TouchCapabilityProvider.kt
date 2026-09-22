package com.galaxyjoy.cpuinfo.feat.touchdiag

import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.MotionEvent
import javax.inject.Inject

/**
 * Static touchscreen capability read via [InputDevice.getMotionRange] — pressure/size/touch-major/
 * orientation axis ranges. There is no public API for "max simultaneous touch points" (verified
 * against `android-37/android.jar` via `javap`: [InputDevice.getMotionRanges] only covers axis
 * ranges, not pointer-count limits) — [VMTouchDiag] tracks the highest pointer count actually
 * observed during a live session instead of claiming a platform maximum.
 */
data class TouchCapabilities(
    val deviceName: String,
    val pressureRange: ClosedFloatingPointRange<Float>?,
    val sizeRange: ClosedFloatingPointRange<Float>?,
    val touchMajorRange: ClosedFloatingPointRange<Float>?,
    val orientationRange: ClosedFloatingPointRange<Float>?,
)

class TouchCapabilityProvider @Inject constructor(private val inputManager: InputManager) {

    fun getTouchscreenCapabilities(): TouchCapabilities? {
        val device = inputManager.inputDeviceIds
            .toList()
            .mapNotNull { inputManager.getInputDevice(it) }
            .firstOrNull { it.supportsSource(InputDevice.SOURCE_TOUCHSCREEN) }
            ?: return null

        return TouchCapabilities(
            deviceName = device.name,
            pressureRange = device.rangeOf(MotionEvent.AXIS_PRESSURE),
            sizeRange = device.rangeOf(MotionEvent.AXIS_SIZE),
            touchMajorRange = device.rangeOf(MotionEvent.AXIS_TOUCH_MAJOR),
            orientationRange = device.rangeOf(MotionEvent.AXIS_ORIENTATION),
        )
    }

    private fun InputDevice.rangeOf(axis: Int): ClosedFloatingPointRange<Float>? =
        getMotionRange(axis)?.let { it.min..it.max }
}
