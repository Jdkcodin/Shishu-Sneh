package com.mindmatrix.shishusneh.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * VaccineScheduler — Manages WorkManager Scheduling
 *
 * Sets up a PeriodicWorkRequest that runs once every 24 hours
 * to check for due vaccines and send notifications.
 *
 * WHY PERIODIC (not one-shot)?
 * - Vaccines can become due on any day
 * - We need to check EVERY day, not just once
 * - WorkManager handles the scheduling automatically
 *
 * WHY ExistingPeriodicWorkPolicy.KEEP?
 * - If the work is already scheduled, don't create a duplicate
 * - This is safe to call from Application.onCreate() every time
 *   the app starts, without creating duplicate workers
 *
 * ┌──────────────────────────────────────────────┐
 * │ App starts                                    │
 * │ ↓                                             │
 * │ VaccineScheduler.scheduleDailyReminders()     │
 * │ ↓                                             │
 * │ WorkManager enqueues PeriodicWorkRequest      │
 * │ (if not already enqueued — KEEP policy)       │
 * │ ↓                                             │
 * │ Every ~24 hours:                              │
 * │   VaccineReminderWorker.doWork() runs         │
 * │   → Checks due vaccines                      │
 * │   → Sends notifications                      │
 * └──────────────────────────────────────────────┘
 */
object VaccineScheduler {

    /**
     * Schedule the daily vaccine reminder check.
     * Safe to call multiple times — duplicates are prevented by KEEP policy.
     *
     * @param context Application context
     */
    fun scheduleDailyReminders(context: Context) {
        // Build the periodic work request
        // - Repeat interval: 24 hours
        // - Flex interval: 2 hours (can run within 2h of the scheduled time)
        val workRequest = PeriodicWorkRequestBuilder<VaccineReminderWorker>(
            24, TimeUnit.HOURS,    // Run every 24 hours
            2, TimeUnit.HOURS      // Flex: can run within 2h window
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)  // Don't run when battery is critically low
                    .build()
            )
            .build()

        // Enqueue the work — KEEP existing if already scheduled
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            VaccineReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancel all scheduled vaccine reminders.
     * Useful if the user doesn't want notifications anymore.
     *
     * @param context Application context
     */
    fun cancelReminders(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(VaccineReminderWorker.WORK_NAME)
    }
}
