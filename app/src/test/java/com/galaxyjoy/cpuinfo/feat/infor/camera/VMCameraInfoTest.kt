package com.galaxyjoy.cpuinfo.feat.infor.camera

import android.content.res.Resources
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for B30: an OEM HAL quirk throwing while reading one camera's
 * characteristics used to abort VMCameraInfo's whole init() (crashing the Camera tab).
 * Each camera must now be isolated so the rest of the list still populates.
 */
class VMCameraInfoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val resources: Resources = mockk(relaxed = true) {
        every { getString(any()) } returns "label"
        every { getString(any(), any()) } returns "label"
    }

    @Test
    fun `a camera throwing while reading characteristics does not abort the rest of the list`() {
        val badCharacteristics: CameraCharacteristics = mockk {
            every { get(CameraCharacteristics.LENS_FACING) } throws AssertionError("OEM HAL quirk")
        }
        val goodCharacteristics: CameraCharacteristics = mockk(relaxed = true) {
            every { get(CameraCharacteristics.LENS_FACING) } returns CameraCharacteristics.LENS_FACING_BACK
        }
        val cameraManager: CameraManager = mockk {
            every { cameraIdList } returns arrayOf("0", "1")
            every { getCameraCharacteristics("0") } returns badCharacteristics
            every { getCameraCharacteristics("1") } returns goodCharacteristics
        }

        // Must not throw — this is the actual regression being guarded against.
        val vm = VMCameraInfo(cameraManager, resources)

        // Camera "0" contributed nothing beyond the header row (it failed), camera "1" still
        // got its facing row added.
        assertTrue(vm.listLiveData.size >= 2, "expected header row + camera 1's facing row")
    }

    @Test
    fun `cameraIdList throwing falls back to an empty list instead of crashing`() {
        val cameraManager: CameraManager = mockk {
            every { cameraIdList } throws RuntimeException("no camera service")
        }

        val vm = VMCameraInfo(cameraManager, resources)

        assertEquals(1, vm.listLiveData.size) // only the "total: 0" header row
    }
}
