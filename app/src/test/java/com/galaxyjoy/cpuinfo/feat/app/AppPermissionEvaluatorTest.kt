package com.galaxyjoy.cpuinfo.feat.app

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppPermissionEvaluatorTest {

    @Test
    fun `counts dangerous permissions separately from total`() {
        val requested = listOf(
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "android.permission.ACCESS_FINE_LOCATION",
        )
        val granted = setOf("android.permission.CAMERA")

        val result = AppPermissionEvaluator.evaluate(requested, granted)

        assertEquals(3, result.totalCount)
        assertEquals(2, result.dangerousCount)
    }

    @Test
    fun `granted flag reflects the granted set per entry`() {
        val requested = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")
        val granted = setOf("android.permission.CAMERA")

        val result = AppPermissionEvaluator.evaluate(requested, granted)

        val camera = result.entries.first { it.name == "android.permission.CAMERA" }
        val mic = result.entries.first { it.name == "android.permission.RECORD_AUDIO" }
        assertTrue(camera.isGranted)
        assertFalse(mic.isGranted)
    }

    @Test
    fun `dangerous permissions sort before normal ones`() {
        val requested = listOf(
            "android.permission.INTERNET",
            "android.permission.VIBRATE",
            "android.permission.CAMERA",
        )

        val result = AppPermissionEvaluator.evaluate(requested, emptySet())

        assertTrue(result.entries.first().isDangerous)
        assertEquals("android.permission.CAMERA", result.entries.first().name)
    }

    @Test
    fun `empty requested list produces an empty result`() {
        val result = AppPermissionEvaluator.evaluate(emptyList(), emptySet())

        assertEquals(0, result.totalCount)
        assertEquals(0, result.dangerousCount)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun `entry carries the resolved category and label`() {
        val result = AppPermissionEvaluator.evaluate(
            listOf("android.permission.ACCESS_FINE_LOCATION"),
            emptySet(),
        )

        val entry = result.entries.single()
        assertEquals(AppPermissionCatalog.Category.LOCATION, entry.category)
        assertEquals("Access Fine Location", entry.label)
    }
}
