package com.mindmatrix.shishusneh

import android.app.Application
import com.mindmatrix.shishusneh.notification.NotificationHelper
import com.mindmatrix.shishusneh.notification.VaccineScheduler

/**
 * ShishuSnehApplication — The App's Starting Point
 *
 * This class is created ONCE when the app starts and lives
 * as long as the app is running.
 *
 * WHY DO WE NEED THIS?
 * - It provides a global Application context
 * - We initialize things that the entire app needs here
 *
 * PHASE 3 ADDITIONS:
 * 1. Create the notification channel for vaccine reminders
 * 2. Schedule daily WorkManager checks for due vaccines
 *
 * Both of these are safe to call multiple times:
 * - Android ignores duplicate notification channels
 * - WorkManager's KEEP policy prevents duplicate workers
 *
 * HOW TO REGISTER:
 * This class is registered in AndroidManifest.xml:
 * android:name=".ShishuSnehApplication"
 */
class ShishuSnehApplication : Application() {

    /**
     * Called when the application is starting.
     * This is the very first code that runs!
     */
    override fun onCreate() {
        super.onCreate()

        // Phase 3: Create notification channel for vaccine reminders
        // Must be created before any notification is sent.
        // Safe to call multiple times — Android ignores duplicates.
        NotificationHelper.createNotificationChannel(this)

        // Phase 3: Schedule daily vaccine reminder checks
        // WorkManager will run VaccineReminderWorker once every 24 hours
        // to check if any vaccines are due soon and send notifications.
        VaccineScheduler.scheduleDailyReminders(this)

        // Phase 4: Restore Dark Mode Preference
        val prefs = getSharedPreferences("shishu_sneh_prefs", android.content.Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
