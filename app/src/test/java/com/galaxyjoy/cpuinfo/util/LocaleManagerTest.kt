package com.galaxyjoy.cpuinfo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleManagerTest {

    @Test
    fun `SYSTEM_DEFAULT_TAG is empty string`() {
        assertEquals("", LocaleManager.SYSTEM_DEFAULT_TAG)
    }

    @Test
    fun `SUPPORTED_LOCALES starts with system default option`() {
        val first = LocaleManager.SUPPORTED_LOCALES.first()
        assertEquals(LocaleManager.SYSTEM_DEFAULT_TAG, first.tag)
        assertEquals(LocaleManager.DisplayKey.SystemDefault, first.displayKey)
    }

    @Test
    fun `SUPPORTED_LOCALES contains all required app languages`() {
        val tags = LocaleManager.SUPPORTED_LOCALES.map { it.tag }
        assertTrue("Must support system default", tags.contains(""))
        assertTrue("Must support English", tags.contains("en"))
        assertTrue("Must support Vietnamese", tags.contains("vi"))
        assertTrue("Must support Czech", tags.contains("cs"))
        assertTrue("Must support German", tags.contains("de"))
        assertTrue("Must support Polish", tags.contains("pl"))
        assertTrue("Must support Traditional Chinese", tags.contains("zh-TW"))
    }

    @Test
    fun `SUPPORTED_LOCALES have non-null native names except system default`() {
        LocaleManager.SUPPORTED_LOCALES.forEach { option ->
            if (option.tag.isEmpty()) {
                assertEquals(LocaleManager.DisplayKey.SystemDefault, option.displayKey)
            } else {
                assertNotNull("Native name should not be null for ${option.tag}", option.nativeName)
                assertTrue("Native name should not be empty for ${option.tag}", option.nativeName!!.isNotBlank())
            }
        }
    }

    @Test
    fun `all tags in SUPPORTED_LOCALES are unique`() {
        val tags = LocaleManager.SUPPORTED_LOCALES.map { it.tag }
        assertEquals(tags.size, tags.toSet().size)
    }
}
