package com.galaxyjoy.cpuinfo.feat.healthalert

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.ActHost
import com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProvider
import java.util.Locale

/**
 * U26 — same "decide-and-post pulled out of the Worker so it's callable with just a real
 * `Context`" shape as [HealthAlertNotifier], for the same [HealthAlertNotifierTest]-style
 * real-device testing reason. Reuses [HealthAlertNotifier.CHANNEL_ID] (one channel is enough,
 * this is still "device health" content) but a distinct [NOTIFICATION_ID] so a drop alert and a
 * weekly digest never silently overwrite each other in the notification shade.
 */
object HealthAlertWeeklyDigestNotifier {

    const val NOTIFICATION_ID = 2002

    fun maybeNotify(context: Context, prefs: HealthAlertPrefs, currentScore: Int) {
        val delta = HealthAlertLogic.weeklyDigestDelta(prefs.getWeeklyBaselineScore(), currentScore) ?: return
        postNotification(context, currentScore, delta)
    }

    /** Same runtime-permission guard as [HealthAlertNotifier.postNotification] — requested only
     * from the Settings toggle, never assumed. */
    @SuppressLint("MissingPermission")
    private fun postNotification(context: Context, score: Int, delta: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, ActHost::class.java).apply {
            putExtra(ShieldScoreWidgetProvider.EXTRA_OPEN_SHIELD_SCORE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "%+d" always renders a leading sign, and Locale.US keeps ASCII digits regardless of the
        // device's default locale — same reasoning as SystemInfoExporter.formatTwoDecimals.
        val deltaText = "%+d".format(Locale.US, delta)

        val notification = NotificationCompat.Builder(context, HealthAlertNotifier.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.health_alert_weekly_digest_title))
            .setContentText(context.getString(R.string.health_alert_weekly_digest_body, score, deltaText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
