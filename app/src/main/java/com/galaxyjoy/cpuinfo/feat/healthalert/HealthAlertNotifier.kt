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

/**
 * The actual decide-and-post step, pulled out of [HealthAlertWorker] so it's callable with a real
 * `Context` (e.g. [androidx.test.platform.app.InstrumentationRegistry]'s target context in
 * [HealthAlertNotifierTest]) without needing a real `WorkerParameters` — that class has no public
 * constructor outside the `androidx.work:work-testing` artifact, which this project doesn't
 * depend on (adding it just for this one file isn't worth a new test-only dependency).
 */
object HealthAlertNotifier {

    const val CHANNEL_ID = "health_alert_channel"
    const val NOTIFICATION_ID = 2001

    fun maybeNotify(context: Context, prefs: HealthAlertPrefs, currentScore: Int, nowMs: Long) {
        if (!HealthAlertLogic.shouldAlert(prefs.getLastScore(), currentScore, prefs.getLastAlertTimestampMs(), nowMs)) {
            return
        }
        postNotification(context, currentScore)
        prefs.saveAlertTimestamp(nowMs)
    }

    /** Guarded by the same runtime check Android itself enforces on API 33+ (the permission is
     * only ever requested from the Settings toggle — see `FrmSettings.wireHealthAlertPref()` —
     * never assumed) — the lint warning this suppresses is a false positive for that reason. */
    @SuppressLint("MissingPermission")
    private fun postNotification(context: Context, score: Int) {
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
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.health_alert_notification_title))
            .setContentText(context.getString(R.string.health_alert_notification_body, score))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
