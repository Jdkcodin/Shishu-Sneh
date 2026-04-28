package com.mindmatrix.shishusneh.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mindmatrix.shishusneh.R

/**
 * NotificationHelper — Manages Android Notifications for Vaccine Reminders
 *
 * WHAT ARE NOTIFICATION CHANNELS?
 * Since Android 8.0 (Oreo), apps must create a "channel" before
 * sending notifications. Think of it as a category:
 *
 * ┌─────────────────────────────────────────────┐
 * │  Shishu Sneh App                            │
 * │  ├── Channel: Vaccine Reminders  [ON/OFF]  │
 * │  │   └── "OPV-1 is due tomorrow!"          │
 * │  └── (future channels for other features)   │
 * └─────────────────────────────────────────────┘
 *
 * Users can independently control each channel in Android Settings.
 *
 * PERMISSION (Android 13+):
 * On Android 13 (API 33), apps need POST_NOTIFICATIONS permission.
 * Without it, notifications silently fail.
 */
object NotificationHelper {

    // Channel ID — unique identifier for vaccine reminders
    const val CHANNEL_ID = "vaccine_reminders"

    // Notification IDs — unique per notification
    // Using vaccine ID as the notification ID so each vaccine gets its own notification
    private const val NOTIFICATION_ID_BASE = 1000

    /**
     * Create the notification channel.
     * Safe to call multiple times — Android ignores duplicates.
     * Should be called once in Application.onCreate().
     */
    fun createNotificationChannel(context: Context) {
        // Channels are only needed on Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val description = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                enableVibration(true)
                enableLights(true)
            }

            // Register the channel with the system
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show a vaccine reminder notification.
     *
     * @param context Application context
     * @param vaccineId Unique vaccine ID (used as notification ID)
     * @param title Notification title
     * @param body Notification body text
     */
    fun showVaccineNotification(
        context: Context,
        vaccineId: Int,
        title: String,
        body: String
    ) {
        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // No permission — can't show notification
                return
            }
        }

        // Create an intent to open VaccineScheduleActivity when tapped
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.putExtra("OPEN_VACCINES", true)

        val pendingIntent = PendingIntent.getActivity(
            context,
            vaccineId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vaccine)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)                    // Dismiss when tapped
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.primary_rose))
            .build()

        // Show the notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID_BASE + vaccineId, notification)
    }

    /**
     * Check if the app has notification permission.
     * Always true on Android 12 and below.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
