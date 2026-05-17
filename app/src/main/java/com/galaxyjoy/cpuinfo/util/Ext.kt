@file:JvmName("Extensions")

package com.galaxyjoy.cpuinfo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
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
 * !Warning! It will control only top/left/right insets. Register your own one for bottom ones.
 *
 * Window background is forced to the primary status-bar color: in edge-to-edge mode the system
 * status bar area shows the window background through, so this is what gives the status bar
 * its app-primary tint instead of falling back to surface (which would appear white in light mode).
 *
 * The locale-change recreate flicker is mitigated separately via [LocaleManager.applyWithSnapshot]
 * which sets a bitmap drawable as window background just before triggering recreate.
 */
fun Activity.setupEdgeToEdge(
    @IdRes containerId: Int = android.R.id.content,
) {
    window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.status_bar)))
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(containerId)) { v, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            top = systemInsets.top,
            left = systemInsets.left,
            right = systemInsets.right,
            bottom = 0,
        )
        insets
    }
}
