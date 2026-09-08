package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import com.google.android.material.color.DynamicColors
import javax.inject.Inject

/**
 * Applies Material You dynamic color to every Activity's theme (API 31+; no-op below that) —
 * fixes View-system widgets that rely on Material3's default `colorPrimary`/`colorPrimaryContainer`/
 * `colorSecondary*` theme attrs (e.g. `ExtendedFloatingActionButton`, M2-style `OutlinedButton`)
 * instead of a manual runtime override. Compose screens already get dynamic color independently
 * via `dynamicLightColorScheme`/`dynamicDarkColorScheme` in `ui/theme/Theme.kt` — this covers the
 * View-system side of the same migration (Material You widget audit).
 */
class DynamicColorsInitializer @Inject constructor() : AppInitializer {

    override fun init(application: Application) {
        DynamicColors.applyToActivitiesIfAvailable(application)
    }
}
