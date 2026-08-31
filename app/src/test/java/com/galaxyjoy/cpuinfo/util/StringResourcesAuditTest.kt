package com.galaxyjoy.cpuinfo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

class StringResourcesAuditTest {

    private val projectRoot = File(System.getProperty("user.dir") ?: ".")
    private val resDir = if (File(projectRoot, "src/main/res").exists()) {
        File(projectRoot, "src/main/res")
    } else {
        File(projectRoot, "app/src/main/res")
    }

    private val supportedLocales = listOf(
        "values-vi" to "vi",
        "values-cs" to "cs",
        "values-de" to "de",
        "values-pl" to "pl",
        "values-zh-rTW" to "zh-TW",
    )

    private fun parseStringsXml(file: File): Map<String, StringInfo> {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        val map = mutableMapOf<String, StringInfo>()
        val nodeList = doc.getElementsByTagName("string")
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                val name = element.getAttribute("name")
                val translatable = element.getAttribute("translatable") != "false"
                val text = element.textContent ?: ""
                map[name] = StringInfo(name, text, translatable)
            }
        }
        return map
    }

    data class StringInfo(val name: String, val text: String, val translatable: Boolean)

    @Test
    fun `default values strings_xml exists and has keys`() {
        val defaultStringsFile = File(resDir, "values/strings.xml")
        assertTrue("values/strings.xml must exist", defaultStringsFile.exists())
        val defaultKeys = parseStringsXml(defaultStringsFile)
        assertTrue("values/strings.xml should have over 300 keys", defaultKeys.size > 300)
    }

    @Test
    fun `all supported language directories exist and have zero missing translatable keys`() {
        val defaultStringsFile = File(resDir, "values/strings.xml")
        val defaultKeys = parseStringsXml(defaultStringsFile)
        val translatableDefaults = defaultKeys.filter { it.value.translatable }

        for ((folder, _) in supportedLocales) {
            val file = File(resDir, "$folder/strings.xml")
            assertTrue("File $folder/strings.xml must exist", file.exists())

            val targetKeys = parseStringsXml(file)
            val missingKeys = translatableDefaults.keys.filter { it !in targetKeys }

            assertTrue(
                "Language folder $folder is missing ${missingKeys.size} translatable keys: $missingKeys",
                missingKeys.isEmpty(),
            )
        }
    }

    @Test
    fun `format specifiers match between default and translated strings`() {
        val defaultStringsFile = File(resDir, "values/strings.xml")
        val defaultKeys = parseStringsXml(defaultStringsFile)
        val fmtPattern = Pattern.compile("%(\\d+\\$)?[sdf%]")

        for ((folder, _) in supportedLocales) {
            val file = File(resDir, "$folder/strings.xml")
            val targetKeys = parseStringsXml(file)

            for ((name, defInfo) in defaultKeys) {
                if (!defInfo.translatable) continue
                val targetInfo = targetKeys[name] ?: continue

                val defMatcher = fmtPattern.matcher(defInfo.text)
                var defFmtCount = 0
                while (defMatcher.find()) defFmtCount++

                val targetMatcher = fmtPattern.matcher(targetInfo.text)
                var targetFmtCount = 0
                while (targetMatcher.find()) targetFmtCount++

                assertEquals(
                    "Format specifier count mismatch in $folder for key $name. " +
                        "Default='${defInfo.text}', Translated='${targetInfo.text}'",
                    defFmtCount,
                    targetFmtCount,
                )
            }
        }
    }

    @Test
    fun `locales_config_xml matches supported locales`() {
        val file = File(resDir, "xml/locales_config.xml")
        assertTrue("locales_config.xml must exist", file.exists())

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        val list = doc.getElementsByTagName("locale")
        val declaredLocales = mutableListOf<String>()
        for (i in 0 until list.length) {
            val element = list.item(i) as Element
            declaredLocales.add(element.getAttribute("android:name"))
        }

        assertTrue("locales_config must contain en", declaredLocales.contains("en"))
        for ((_, tag) in supportedLocales) {
            assertTrue("locales_config must contain $tag", declaredLocales.contains(tag))
        }
    }

    @Test
    fun `pref_xml has no hardcoded title strings`() {
        val file = File(resDir, "xml/pref.xml")
        assertTrue("pref.xml must exist", file.exists())
        val content = file.readText()

        val hardcodedPattern = Pattern.compile("""app:title="([A-Za-z][^@"]{4,})"""")
        val matcher = hardcodedPattern.matcher(content)
        val hardcodedTitles = mutableListOf<String>()
        while (matcher.find()) {
            val title = matcher.group(1)
            // exclude empty title
            if (!title.isNullOrBlank()) {
                hardcodedTitles.add(title)
            }
        }

        assertTrue(
            "Found hardcoded titles in pref.xml that should use @string references: $hardcodedTitles",
            hardcodedTitles.isEmpty(),
        )
    }
}
