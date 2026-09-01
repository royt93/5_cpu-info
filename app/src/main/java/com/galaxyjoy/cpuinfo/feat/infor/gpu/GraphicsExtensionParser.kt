package com.galaxyjoy.cpuinfo.feat.infor.gpu

/**
 * F08 "Vulkan/GLES Detail" — [FrmGpuInfo]'s list row already dumps the raw
 * `GL_EXTENSIONS` string (space-separated, driver-defined order, sometimes with duplicates) as
 * one unreadable blob. This turns it into a clean, sorted, deduplicated list for the detail
 * sheet.
 */
object GraphicsExtensionParser {

    private val WHITESPACE = Regex("\\s+")

    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.trim()
            .split(WHITESPACE)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
}
