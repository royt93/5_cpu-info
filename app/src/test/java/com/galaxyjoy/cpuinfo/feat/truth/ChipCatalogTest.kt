package com.galaxyjoy.cpuinfo.feat.truth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChipCatalogTest {

    @Test
    fun `vendorName resolves known ids and falls back for unknown ones`() {
        assertEquals("Qualcomm", ChipCatalog.vendorName(4))
        assertEquals("Samsung", ChipCatalog.vendorName(6))
        assertTrue(ChipCatalog.vendorName(999).startsWith("Unknown"))
    }

    @Test
    fun `uarchName resolves known ids and falls back for unknown ones`() {
        assertEquals("Qualcomm Kryo", ChipCatalog.uarchName(0x00400102))
        assertEquals("Samsung Exynos M5", ChipCatalog.uarchName(0x00600104))
        assertTrue(ChipCatalog.uarchName(0x7FFFFFFF).startsWith("Unknown"))
    }

    @Test
    fun `only Samsung, Apple, HiSilicon are brand-locked`() {
        assertTrue(ChipCatalog.isBrandLocked(6)) // samsung
        assertTrue(ChipCatalog.isBrandLocked(5)) // apple
        assertTrue(ChipCatalog.isBrandLocked(15)) // huawei
        assertFalse(ChipCatalog.isBrandLocked(4)) // qualcomm
        assertFalse(ChipCatalog.isBrandLocked(3)) // arm
        assertFalse(ChipCatalog.isBrandLocked(0)) // unknown
    }

    @Test
    fun `isPlausibleBrand is case-insensitive and substring-tolerant`() {
        assertTrue(ChipCatalog.isPlausibleBrand(6, "Samsung"))
        assertTrue(ChipCatalog.isPlausibleBrand(6, "samsung electronics"))
        assertFalse(ChipCatalog.isPlausibleBrand(6, "xiaomi"))
    }

    @Test
    fun `isPlausibleBrand is always true for non brand-locked vendors`() {
        assertTrue(ChipCatalog.isPlausibleBrand(4, "literally anything"))
        assertTrue(ChipCatalog.isPlausibleBrand(0, ""))
    }
}
