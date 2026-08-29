package com.galaxyjoy.cpuinfo.feat.fleet

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class FleetSpecCatalogTest {

    @Test
    fun `matches Samsung model prefix regardless of regional suffix letter`() {
        assertEquals("Samsung Galaxy S24 Ultra", FleetSpecCatalog.findMatch("SM-S928B")?.displayName)
        assertEquals("Samsung Galaxy S24 Ultra", FleetSpecCatalog.findMatch("SM-S928U")?.displayName)
        assertEquals("Samsung Galaxy S24 Ultra", FleetSpecCatalog.findMatch("SM-S928N")?.displayName)
    }

    @Test
    fun `matches Pixel model by exact marketing name`() {
        assertEquals("Google Pixel 8 Pro", FleetSpecCatalog.findMatch("Pixel 8 Pro")?.displayName)
    }

    @Test
    fun `does not confuse similarly-prefixed different models`() {
        // S23 Ultra (SM-S918) must not match the S24 Ultra (SM-S928) entry or vice versa.
        assertEquals("Samsung Galaxy S23 Ultra", FleetSpecCatalog.findMatch("SM-S918B")?.displayName)
    }

    @Test
    fun `unrecognized model returns null`() {
        assertNull(FleetSpecCatalog.findMatch("Unknown Device 9000"))
    }

    @Test
    fun `match is case-insensitive`() {
        assertEquals("Google Pixel 7 Pro", FleetSpecCatalog.findMatch("pixel 7 pro")?.displayName)
    }
}
