package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.benchreminder.BenchReminderNotifier
import javax.inject.Inject

/** U30 — creates the benchmark-reminder notification channel at app start, same idempotent
 * unconditional-creation reasoning as [HealthAlertNotificationInitializer]. */
class BenchReminderNotificationInitializer @Inject constructor() : AppInitializer {

    override fun init(application: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = application.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            BenchReminderNotifier.CHANNEL_ID,
            application.getString(R.string.bench_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }
}
