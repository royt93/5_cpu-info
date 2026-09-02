package com.galaxyjoy.cpuinfo.appinitializers

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.healthalert.HealthAlertNotifier
import javax.inject.Inject

/** Creates the U19 health-alert notification channel at app start — channel creation is a cheap,
 * idempotent no-op if it already exists, so this runs unconditionally rather than only once the
 * user turns the feature on in Settings (simpler than tracking "has the channel been created
 * yet" separately, and `NotificationManager.createNotificationChannel` doesn't need the
 * `POST_NOTIFICATIONS` runtime permission — only actually posting a notification does). */
class HealthAlertNotificationInitializer @Inject constructor() : AppInitializer {

    override fun init(application: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = application.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            HealthAlertNotifier.CHANNEL_ID,
            application.getString(R.string.health_alert_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }
}
