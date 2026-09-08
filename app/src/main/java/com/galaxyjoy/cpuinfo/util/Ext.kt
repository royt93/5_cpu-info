@file:JvmName("Extensions")

package com.galaxyjoy.cpuinfo.util

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.galaxyjoy.cpuinfo.R
import kotlin.math.roundToLong

/**
 * All basic extensions
 *
 * @author galaxyjoy
 */
fun Float.round1(): Float = try {
    (this * 10.0).roundToLong() / 10.0f
} catch (e: Exception) {
    0.0f
}

@Suppress("unused")
fun Double.round1(): Double = try {
    (this * 10.0).roundToLong() / 10.0
} catch (e: Exception) {
    0.0
}

fun Float.round2(): Float = try {
    (this * 100.0).roundToLong() / 100.0f
} catch (e: Exception) {
    0.0f
}

fun Double.round2(): Double = try {
    (this * 100.0).roundToLong() / 100.0
} catch (e: Exception) {
    0.0
}

inline fun runOnApiBelow(api: Int, f: () -> Unit) {
    if (Build.VERSION.SDK_INT < api) {
        f()
    }
}

inline fun runOnApiAbove(api: Int, f: () -> Unit) {
    if (Build.VERSION.SDK_INT > api) {
        f()
    }
}

inline fun runOnApiBelow(api: Int, f: () -> Unit, otherwise: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT < api) {
        f()
    } else {
        otherwise()
    }
}

inline fun runOnApiAbove(api: Int, f: () -> Unit, otherwise: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT > api) {
        f()
    } else {
        otherwise()
    }
}

/**
 * @return true if used device is tablet
 */
@Suppress("unused")
fun Context.isTablet(): Boolean = this.resources.getBoolean(R.bool.isTablet)

/**
 * In the feature this method should be replaced with PackageManager
 */
@Suppress("DEPRECATION")
fun Activity.uninstallApp(packageName: String) {
    val uri = Uri.fromParts("package", packageName, null)
    val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri)
    startActivity(uninstallIntent)
}

/**
 * The action bar/toolbar color: dynamic (Material You, wallpaper-based) on API31+, same static
 * `@color/primary` fallback below that (already dark-toned in `values-night/colors.xml`) — a
 * plain `Context` is enough, `dynamicLightColorScheme`/`dynamicDarkColorScheme` aren't
 * `@Composable`, they only read system resources (`@android:color/system_accent1_*`).
 *
 * Light theme uses `primary` (M3 tonal spec: saturated/dark-ish tone in light scheme, pairs with
 * white `onPrimary`) — a nice branded pop of color, matches the old static teal look. Dark theme
 * deliberately does NOT use `primary`/`onPrimary` — M3's dynamic DARK scheme's `primary` is a
 * light/PASTEL tone (opposite of light scheme, by design — it's meant for small accents on a dark
 * surface, not a whole toolbar). Using it made the toolbar look like a stray light patch in an
 * otherwise dark UI — reported directly by the user after seeing it on a real device. `primaryContainer`
 * in the dark scheme is the tone actually meant for this ("dark tone, white text" — matches
 * `onPrimaryContainer`), while still being wallpaper-tinted, not a flat static color.
 */
fun Context.resolveActionBarColor(useDarkTheme: Boolean): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (useDarkTheme) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
        (if (useDarkTheme) scheme.primaryContainer else scheme.primary).toArgb()
    } else {
        ContextCompat.getColor(this, R.color.primary)
    }
}

fun Context.isNightMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * The general "accent" color for small UI elements drawn on the app's normal surface/background —
 * section header text, progress bar fill, highlight icons (`R.color.accent`'s old static role
 * throughout `feat/infor` — `AdtInfoItems`, `view_holder_cpu_frequency.xml`'s progressColor,
 * `vi_item_storage.xml`, etc.). Unlike [resolveActionBarColor], this always uses `primary`
 * (never `primaryContainer`) regardless of theme: here `primary` is a FOREGROUND accent against
 * the theme's own neutral surface (light: dark-saturated text on a light row = fine; dark:
 * light/pastel text on a dark row = ALSO fine) — it's only a problem as a large filled
 * BACKGROUND in dark mode (see [resolveActionBarColor] kdoc), which this usage never is.
 */
fun Context.resolveAccentColor(useDarkTheme: Boolean): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (useDarkTheme) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
        scheme.primary.toArgb()
    } else {
        ContextCompat.getColor(this, R.color.accent)
    }
}

/**
 * Text/icon color for content drawn ON an [resolveAccentColor]-filled element (a solid button
 * background, e.g. VIP screen's "Kích hoạt"/"Watch ad" buttons — old static
 * `@color/btn_primary_activate_text`). Always `onPrimary` (matches [resolveAccentColor] always
 * using `primary`, never `primaryContainer`).
 */
fun Context.resolveOnAccentColor(useDarkTheme: Boolean): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (useDarkTheme) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
        scheme.onPrimary.toArgb()
    } else {
        ContextCompat.getColor(this, R.color.btn_primary_activate_text)
    }
}

/**
 * The color for text/icons drawn ON [resolveActionBarColor] (title, tab labels, bottom nav
 * icons, menu action icons) — the counterpart token to whichever role [resolveActionBarColor]
 * picked (`onPrimary` for light's `primary`, `onPrimaryContainer` for dark's `primaryContainer`).
 * See [resolveActionBarColor] kdoc for why dark theme doesn't just use `onPrimary`.
 */
fun Context.resolveActionBarContentColor(useDarkTheme: Boolean): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (useDarkTheme) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
        (if (useDarkTheme) scheme.onPrimaryContainer else scheme.onPrimary).toArgb()
    } else {
        ContextCompat.getColor(this, R.color.onPrimary)
    }
}

/**
 * !Warning! MUST be called before `super.onCreate()`/`setContentView()` (required by
 * `enableEdgeToEdge()` itself regardless of the finding below).
 *
 * Only sets up `decorFitsSystemWindows(false)` + icon appearance. Deliberately does NOT rely on
 * `Window.statusBarColor`/`SystemBarStyle` scrim to actually PAINT the bar — tried that first and
 * it silently did nothing on a real Android 16 device (Samsung S24U): `dumpsys window windows`
 * for the app's window doesn't even list a status bar color field anymore, confirming it's fully
 * dead at this API level, not just deprecated. The real, version-agnostic fix is
 * [applyStatusBarColorToToolbar] — let the toolbar's own opaque background bleed up through the
 * transparent bar instead of asking the system to paint it. The scrim int passed here still
 * matters for API<35 (where it's not dead) and, more importantly, picking `.dark()` vs `.light()`
 * from it is what drives the system icon color via `WindowInsetsControllerCompat` — that part
 * keeps working on every API level. Icon choice is by actual luminance of the resolved dynamic
 * color, not a hardcoded assumption: M3's dynamic dark scheme's `primary` is a light/pastel tone
 * (needs dark icons), the opposite of the old static branded dark primary (`#1C1C1C`, needed
 * light icons) — a fixed `.dark()` would be wrong for half of the dynamic color cases.
 */
fun ComponentActivity.enableEdgeToEdgeMatchingActionBar() {
    val color = resolveActionBarColor(isNightMode())
    val useLightIcons = ColorUtils.calculateLuminance(color) < 0.5
    enableEdgeToEdge(
        statusBarStyle = if (useLightIcons) SystemBarStyle.dark(color) else SystemBarStyle.light(color, color),
        navigationBarStyle = if (useLightIcons) SystemBarStyle.dark(color) else SystemBarStyle.light(color, color),
    )
}

/**
 * !Warning! It will control only top/left/right insets. Register your own one for bottom ones
 * (e.g. a bottom nav bar's own inset listener) — this fn deliberately leaves bottom untouched.
 * Call after `setContentView()` — needs the real content view to attach the listener to.
 *
 * Deliberately does NOT pad `top` — padding the whole content container top pushes the toolbar
 * down, leaving the vacated strip at y=0 showing the plain window background
 * (`android:windowBackground` = `?attr/colorSurface`) instead of the toolbar's own color. Call
 * [applyStatusBarColorToToolbar] separately for that — its padding target is the toolbar itself,
 * so the toolbar's background still starts at y=0 and bleeds up under the transparent status bar.
 *
 * The locale-change recreate flicker is mitigated separately via [LocaleManager.applyWithSnapshot]
 * which sets a bitmap drawable as window background just before triggering recreate.
 */
fun ComponentActivity.applyEdgeToEdgeContentPadding(
    @IdRes containerId: Int = android.R.id.content,
) {
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(containerId)) { v, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            left = systemInsets.left,
            right = systemInsets.right,
        )
        insets
    }
}

/**
 * Sets [this] toolbar's background to [color] (the resolved dynamic-or-static action bar color —
 * XML's `?attr/colorPrimary` is always static, so a dynamic color can only be applied at
 * runtime like this) and pads its top by the status bar inset (added to whatever top padding it
 * already has) instead of moving/clipping it — so the background still starts at y=0 and shows
 * through the transparent status bar, while the actual content (title/icons) sits below the
 * status bar. Use on the app's toolbar rather than on the whole content container — see
 * [applyEdgeToEdgeContentPadding] kdoc for why that doesn't work.
 */
fun android.view.View.applyStatusBarColorToToolbar(color: Int) {
    setBackgroundColor(color)
    val initialPadding = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        v.updatePadding(top = initialPadding + top)
        insets
    }
}

private const val SKELETON_PULSE_DURATION_MS = 800L

/**
 * Starts an infinite alpha pulse on [this] view (a `layout_skeleton_list.xml` overlay) — a
 * hand-rolled placeholder-loading affordance rather than a true diagonal shimmer sweep, to avoid
 * pulling in a new UI library for what most tabs show for well under a second (they read
 * hardware/system data directly, no network) — matches the existing hand-rolled
 * `ObjectAnimator`-based pulse/glow pattern already used for the VIP crown badge
 * ([com.galaxyjoy.cpuinfo.feat.vip.FVipManagement]) rather than introducing a second animation
 * approach. Call [stopSkeletonPulse] once real data arrives.
 */
fun View.startSkeletonPulse() {
    visibility = View.VISIBLE
    alpha = 1f
    tag = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0.4f).apply {
        duration = SKELETON_PULSE_DURATION_MS
        repeatMode = ObjectAnimator.REVERSE
        repeatCount = ObjectAnimator.INFINITE
        start()
    }
}

/** Stops the pulse started by [startSkeletonPulse] and hides the skeleton overlay. */
fun View.stopSkeletonPulse() {
    (tag as? ObjectAnimator)?.cancel()
    tag = null
    alpha = 1f
    visibility = View.GONE
}

/**
 * Shows [this] skeleton overlay (pulsing) until [adapter] reports its first real data, then
 * hides it — generic across every info tab regardless of whether that adapter's data arrives via
 * LiveData/Flow observation or is already present synchronously at call time. Takes the adapter
 * explicitly rather than reading it off the `RecyclerView` — matters for `ConcatAdapter` cases
 * (e.g. FrmCpuInfo's Compose header + list): the *outer* `ConcatAdapter.itemCount` is never 0
 * (the header alone counts as 1), so pass the inner data-bearing adapter (e.g. `AdtCpuInfo`) here,
 * not the `ConcatAdapter` itself, or the skeleton would never show.
 */
fun View.hideSkeletonAfterFirstData(adapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>) {
    if (adapter.itemCount > 0) {
        stopSkeletonPulse()
        return
    }
    startSkeletonPulse()
    val skeleton = this
    adapter.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
        private fun hideIfHasData() {
            if (adapter.itemCount == 0) return
            skeleton.stopSkeletonPulse()
            adapter.unregisterAdapterDataObserver(this)
        }
        override fun onChanged() = hideIfHasData()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = hideIfHasData()
    })
}
