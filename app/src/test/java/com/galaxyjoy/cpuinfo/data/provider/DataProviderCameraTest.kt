package com.galaxyjoy.cpuinfo.data.provider

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range
import android.util.Size
import android.util.SizeF
import com.galaxyjoy.cpuinfo.domain.model.CameraFacing
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression coverage for B30: an OEM HAL quirk throwing while reading one camera's
 * characteristics used to abort the whole population (crashing the Camera tab). Each camera
 * must be isolated so the rest of the list still populates.
 *
 * NOTE on mocking style: under the JVM unit-test stub android.jar (no Robolectric — see
 * CLAUDE.md), every `CameraCharacteristics.SOME_KEY` constant evaluates to `null` (their real
 * constructors never run). That makes every `ch.get(key)` call look identical to MockK — it
 * cannot distinguish stubs by key. Tests that need more than one distinct field either stub
 * `get(any())` with a single answer (fine when only one field matters) or `returnsMany` a list
 * in the exact call order `buildLensData` reads fields (see the happy-path test below).
 */
class DataProviderCameraTest {

    private val cameraManager: CameraManager = mockk()
    private val provider = DataProviderCamera(cameraManager)

    @Test
    fun `getCameraData falls back to empty list when cameraIdList throws`() {
        every { cameraManager.cameraIdList } throws RuntimeException("no camera service")

        val data = provider.getCameraData()

        assertEquals(0, data.totalCameras)
        assertTrue(data.lenses.isEmpty())
    }

    @Test
    fun `getCameraData returns empty lenses for an empty camera list`() {
        every { cameraManager.cameraIdList } returns emptyArray()

        val data = provider.getCameraData()

        assertEquals(0, data.totalCameras)
        assertTrue(data.lenses.isEmpty())
    }

    @Test
    fun `getCameraData happy path reads full characteristics for a single camera`() {
        val map: StreamConfigurationMap = mockk {
            every { getOutputSizes(ImageFormat.JPEG) } returns arrayOf(mockSize(4000, 3000))
            every { highSpeedVideoSizes } returns arrayOf(mockSize(1920, 1080))
            every { getHighSpeedVideoFpsRangesFor(any()) } returns arrayOf(mockRange(240))
        }
        // Call order must match buildLensData: facing, focal lengths, sensor size, pixel array,
        // stream map, capabilities, OIS modes, scene modes.
        val ch: CameraCharacteristics = mockk {
            every { get(any<CameraCharacteristics.Key<Any>>()) } returnsMany listOf(
                CameraCharacteristics.LENS_FACING_BACK,
                floatArrayOf(4.2f),
                mockSizeF(5.6f, 4.2f),
                mockSize(4032, 3024),
                map,
                intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
                intArrayOf(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON),
                intArrayOf(CameraCharacteristics.CONTROL_SCENE_MODE_HDR),
            )
        }
        every { cameraManager.cameraIdList } returns arrayOf("0")
        every { cameraManager.getCameraCharacteristics("0") } returns ch

        val data = provider.getCameraData()

        assertEquals(1, data.totalCameras)
        val lens = data.lenses.single()
        assertEquals("0", lens.id)
        assertEquals(CameraFacing.BACK, lens.facing)
        assertEquals(listOf(4.2f), lens.focalLengthsMm)
        assertEquals(5.6f, lens.sensorWidthMm)
        assertEquals(4032, lens.pixelArrayWidth)
        assertEquals(4000, lens.maxJpegWidth)
        assertEquals(1920, lens.maxSlowMotionWidth)
        assertEquals(240, lens.maxSlowMotionFps)
        assertEquals(true, lens.hasRawCapture)
        assertEquals(false, lens.hasManualSensor)
        assertEquals(true, lens.hasOis)
        assertEquals(true, lens.hasHdrSceneMode)
    }

    @Test
    fun `getCameraData isolates a camera whose getCameraCharacteristics throws`() {
        val goodCh: CameraCharacteristics = mockk { every { get(any<CameraCharacteristics.Key<Any>>()) } returns null }
        every { cameraManager.cameraIdList } returns arrayOf("0", "1")
        every { cameraManager.getCameraCharacteristics("0") } throws RuntimeException("HAL error")
        every { cameraManager.getCameraCharacteristics("1") } returns goodCh

        val data = provider.getCameraData()

        assertEquals(2, data.totalCameras)
        assertEquals("1", data.lenses.single().id)
    }

    @Test
    fun `getCameraData isolates a camera whose characteristics read throws mid-way (OEM HAL quirk)`() {
        val badCh: CameraCharacteristics = mockk {
            every { get(any<CameraCharacteristics.Key<Any>>()) } throws AssertionError("OEM HAL quirk")
        }
        val goodCh: CameraCharacteristics = mockk { every { get(any<CameraCharacteristics.Key<Any>>()) } returns null }
        every { cameraManager.cameraIdList } returns arrayOf("0", "1")
        every { cameraManager.getCameraCharacteristics("0") } returns badCh
        every { cameraManager.getCameraCharacteristics("1") } returns goodCh

        // Must not throw — this is the actual regression being guarded against.
        val data = provider.getCameraData()

        assertEquals(2, data.totalCameras)
        assertEquals("1", data.lenses.single().id)
    }

    @Test
    fun `getCameraData nulls out all capability flags when REQUEST_AVAILABLE_CAPABILITIES is missing`() {
        // Preserves an original quirk: OIS/HDR/multi-lens come from unrelated characteristics
        // keys but were gated behind REQUEST_AVAILABLE_CAPABILITIES in the old code too — a
        // single `null` answer for every get() call reproduces "nothing present" faithfully.
        val ch: CameraCharacteristics = mockk { every { get(any<CameraCharacteristics.Key<Any>>()) } returns null }
        every { cameraManager.cameraIdList } returns arrayOf("0")
        every { cameraManager.getCameraCharacteristics("0") } returns ch

        val lens = provider.getCameraData().lenses.single()

        assertEquals(CameraFacing.UNKNOWN, lens.facing)
        assertNull(lens.hasRawCapture)
        assertNull(lens.hasManualSensor)
        assertNull(lens.hasBurstCapture)
        assertNull(lens.hasOis)
        assertNull(lens.hasHdrSceneMode)
        assertNull(lens.hasMultiLens)
    }

    private fun mockSize(w: Int, h: Int): Size = mockk {
        every { width } returns w
        every { height } returns h
    }

    private fun mockSizeF(w: Float, h: Float): SizeF = mockk {
        every { width } returns w
        every { height } returns h
    }

    private fun mockRange(upper: Int): Range<Int> = mockk {
        every { getUpper() } returns upper
    }
}
