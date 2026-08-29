package com.galaxyjoy.cpuinfo.feat.infor.gpu

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class GraphicsExtensionParserTest {

    @Test
    fun `null input produces an empty list`() {
        assertTrue(GraphicsExtensionParser.parse(null).isEmpty())
    }

    @Test
    fun `blank input produces an empty list`() {
        assertTrue(GraphicsExtensionParser.parse("   ").isEmpty())
    }

    @Test
    fun `splits on single spaces`() {
        val result = GraphicsExtensionParser.parse("GL_OES_texture_npot GL_EXT_debug_marker")

        assertEquals(listOf("GL_EXT_debug_marker", "GL_OES_texture_npot"), result)
    }

    @Test
    fun `collapses multiple whitespace characters between entries`() {
        val result = GraphicsExtensionParser.parse("GL_OES_texture_npot   GL_EXT_debug_marker\tGL_ARB_sync")

        assertEquals(
            listOf("GL_ARB_sync", "GL_EXT_debug_marker", "GL_OES_texture_npot"),
            result,
        )
    }

    @Test
    fun `removes duplicate entries`() {
        val result = GraphicsExtensionParser.parse("GL_OES_texture_npot GL_OES_texture_npot GL_EXT_debug_marker")

        assertEquals(listOf("GL_EXT_debug_marker", "GL_OES_texture_npot"), result)
    }

    @Test
    fun `result is sorted alphabetically`() {
        val result = GraphicsExtensionParser.parse("GL_Z_last GL_A_first GL_M_middle")

        assertEquals(listOf("GL_A_first", "GL_M_middle", "GL_Z_last"), result)
    }

    @Test
    fun `trims leading and trailing whitespace`() {
        val result = GraphicsExtensionParser.parse("  GL_OES_texture_npot  ")

        assertEquals(listOf("GL_OES_texture_npot"), result)
    }
}
