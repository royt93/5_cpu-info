package com.galaxyjoy.cpuinfo.feat.benchreminder

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
import com.galaxyjoy.cpuinfo.feat.infor.base.AdtInfoContainerState
import com.galaxyjoy.cpuinfo.feat.lastbenchwidget.LastBenchWidgetProvider

/**
 * U30 — same "decide-and-post pulled out of the Worker so it's callable with just a real
 * `Context`" shape as [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertNotifier], for the same
 * real-device-testable reason. Own channel (not [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertNotifier.CHANNEL_ID])
 * since this is a different topic (benchmark upkeep, not device health) — lets a user mute one
 * without muting the other via system notification settings.
 */
object BenchReminderNotifier {

    const val CHANNEL_ID = "bench_reminder_channel"
    const val NOTIFICATION_ID = 2003

    fun maybeNotify(context: Context, lastBenchTimestampMs: Long?, nowMs: Long) {
        if (!BenchReminderLogic.shouldRemind(lastBenchTimestampMs, nowMs)) return
        postNotification(context)
    }

    /** Same runtime-permission guard as [com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertNotifier.postNotification]
     * — requested only from the Settings toggle, never assumed. */
    @SuppressLint("MissingPermission")
    private fun postNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // U21's tap target (a specific ViewPager2 tab) — ALL_BENCH_POS lands on the "run all 4"
        // screen since this reminder isn't about any 1 benchmark type in particular.
        val intent = Intent(context, ActHost::class.java).apply {
            putExtra(LastBenchWidgetProvider.EXTRA_OPEN_BENCH_TAB, AdtInfoContainerState.ALL_BENCH_POS)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.bench_reminder_notification_title))
            .setContentText(context.getString(R.string.bench_reminder_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
