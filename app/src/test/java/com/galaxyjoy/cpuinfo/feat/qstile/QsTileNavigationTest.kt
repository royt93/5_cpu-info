package com.galaxyjoy.cpuinfo.feat.qstile

import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Test

/**
 * Regression guard for the API-gate in [openAppAndCollapse]: below API 34
 * `TileService.startActivityAndCollapse(PendingIntent)` doesn't exist and throws
 * `NoSuchMethodError` if called — must fall back to the deprecated `Intent` overload.
 * Unit tests run against the JVM stub android.jar, so `Build.VERSION.SDK_INT` is always 0
 * (below API 34), which exercises exactly that fallback branch.
 */
class QsTileNavigationTest {

    @After
    fun tearDown() {
        unmockkConstructor(Intent::class)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `below API 34 collapses using the deprecated Intent overload`() {
        val context: Context = mockk(relaxed = true)
        val tileService: TileService = mockk(relaxed = true)
        every { tileService.applicationContext } returns context
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().addFlags(any()) } answers { self as Intent }

        tileService.openAppAndCollapse()

        verify(exactly = 1) { tileService.startActivityAndCollapse(any<Intent>()) }
    }
}
