package com.galaxyjoy.cpuinfo.feat.qstile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.galaxyjoy.cpuinfo.feat.ActHost

/**
 * Open the app and collapse the QS panel — shared by informational tiles (CPU/RAM/Battery/
 * Network) that don't need their own custom tap handling.
 *
 * `TileService.startActivityAndCollapse(PendingIntent)` only exists from API 34 (per the SDK's
 * `api-versions.xml`, that overload is `since="34"`) — calling it below that throws
 * `NoSuchMethodError`. The deprecated `Intent` overload is what's actually valid pre-34.
 */
@Suppress("DEPRECATION")
fun TileService.openAppAndCollapse() {
    val intent = Intent(applicationContext, ActHost::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pendingIntent)
    } else {
        startActivityAndCollapse(intent)
    }
}
