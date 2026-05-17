package com.galaxyjoy.cpuinfo.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Single source of truth for in-app language switching.
 *
 * Uses [AppCompatDelegate.setApplicationLocales] (AppCompat 1.6+) which:
 * - On Android 13+ delegates to system per-app language settings
 * - On older Android stores the choice internally and applies on activity recreate
 *
 * Application class doesn't need to do anything special — AppCompat applies the
 * stored locale automatically before activities inflate.
 */
object LocaleManager {

    /** Tag used to represent "follow system default" choice. */
    const val SYSTEM_DEFAULT_TAG = ""

    /**
     * Locales the app provides translations for. Order shown in the picker UI.
     * "Native" name shown so user can identify their language even when the
     * device is currently in a foreign language.
     */
    val SUPPORTED_LOCALES: List<LocaleOption> = listOf(
        LocaleOption(SYSTEM_DEFAULT_TAG, displayKey = DisplayKey.SystemDefault),
        LocaleOption("en", nativeName = "English"),
        LocaleOption("vi", nativeName = "Tiếng Việt"),
        LocaleOption("cs", nativeName = "Čeština"),
        LocaleOption("de", nativeName = "Deutsch"),
        LocaleOption("pl", nativeName = "Polski"),
        LocaleOption("zh-TW", nativeName = "繁體中文"),
    )

    /**
     * @return the BCP-47 tag of the currently-applied app locale, or empty string
     *         if app is following system default.
     */
    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) SYSTEM_DEFAULT_TAG else locales[0]?.toLanguageTag().orEmpty()
    }

    /**
     * Apply the chosen locale. Empty tag clears the override (follow system).
     * AppCompat will recreate active activities to apply the change.
     */
    fun apply(tag: String) {
        val list = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(list)
    }

    /**
     * @param tag BCP-47 tag for a specific locale, or [SYSTEM_DEFAULT_TAG] for follow-system.
     * @param displayKey set for the system-default entry, since its label depends on
     *        the *current* locale (e.g. "System default" / "Mặc định hệ thống") and
     *        must be resolved via Android resources at render time.
     * @param nativeName label rendered as-is, in the language being offered. Null for
     *        system-default since we use [displayKey] instead.
     */
    data class LocaleOption(
        val tag: String,
        val nativeName: String? = null,
        val displayKey: DisplayKey? = null,
    )

    enum class DisplayKey { SystemDefault }
}
