package com.galaxyjoy.cpuinfo.feat.backup

import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupBundleTest {

    @Test
    fun `encode then decode round-trips all fields`() {
        val bundle = BackupBundle(
            version = BackupBundle.CURRENT_VERSION,
            throttleHistory = listOf(
                ThrottleResultPrefs.SavedResult(1L, 2400, 2200, 12, 34, 4_000_000L, osBuildFingerprint = "build_a"),
            ),
            storageHistory = emptyList(),
            ramHistory = emptyList(),
            gpuHistory = emptyList(),
            temperatureUnit = "1",
            theme = "dark",
            languageTag = "vi",
        )

        val decoded = BackupBundle.decode(BackupBundle.encode(bundle))

        assertEquals(bundle, decoded)
    }

    @Test
    fun `decode returns null for garbage text`() {
        assertNull(BackupBundle.decode("not json at all"))
    }

    @Test
    fun `decode returns null for empty string`() {
        assertNull(BackupBundle.decode(""))
    }

    @Test
    fun `decode returns null for a future unknown version`() {
        val json = """{"version":999}"""
        assertNull(BackupBundle.decode(json))
    }

    @Test
    fun `decode tolerates a JSON object missing every optional field`() {
        val json = """{"version":1}"""
        val decoded = BackupBundle.decode(json)
        assertNotNull(decoded)
        assertNull(decoded!!.throttleHistory)
        assertNull(decoded.temperatureUnit)
    }
}
