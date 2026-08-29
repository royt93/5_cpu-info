package com.galaxyjoy.cpuinfo.feat.app

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppPermissionCatalogTest {

    @Test
    fun `camera and location are dangerous`() {
        assertTrue(AppPermissionCatalog.isDangerous("android.permission.CAMERA"))
        assertTrue(AppPermissionCatalog.isDangerous("android.permission.ACCESS_FINE_LOCATION"))
    }

    @Test
    fun `internet and vibrate are not dangerous`() {
        assertFalse(AppPermissionCatalog.isDangerous("android.permission.INTERNET"))
        assertFalse(AppPermissionCatalog.isDangerous("android.permission.VIBRATE"))
    }

    @Test
    fun `location permissions map to the LOCATION category`() {
        assertEquals(
            AppPermissionCatalog.Category.LOCATION,
            AppPermissionCatalog.categoryFor("android.permission.ACCESS_FINE_LOCATION"),
        )
        assertEquals(
            AppPermissionCatalog.Category.LOCATION,
            AppPermissionCatalog.categoryFor("android.permission.ACCESS_BACKGROUND_LOCATION"),
        )
    }

    @Test
    fun `unknown permission falls back to OTHER category`() {
        assertEquals(
            AppPermissionCatalog.Category.OTHER,
            AppPermissionCatalog.categoryFor("com.example.CUSTOM_PERMISSION"),
        )
    }

    @Test
    fun `shortLabel converts SNAKE_CASE suffix to Title Case`() {
        assertEquals(
            "Access Fine Location",
            AppPermissionCatalog.shortLabel("android.permission.ACCESS_FINE_LOCATION"),
        )
        assertEquals(
            "Camera",
            AppPermissionCatalog.shortLabel("android.permission.CAMERA"),
        )
    }
}
