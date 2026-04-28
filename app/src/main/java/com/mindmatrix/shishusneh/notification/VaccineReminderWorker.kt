package com.mindmatrix.shishusneh.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.ShishuSnehDatabase
import com.mindmatrix.shishusneh.data.repository.VaccineRepository
import java.util.concurrent.TimeUnit

/**
 * VaccineReminderWorker — Background Worker for Vaccine Notifications
 *
 * This runs periodically (every 24 hours) via WorkManager, even when
 * the app is closed. It's the "alarm clock" that wakes up, checks if
 * any vaccines are due, and sends notifications.
 *
 * HOW IT WORKS:
 * ┌──────────────────────────────────────────────────┐
 * │  WorkManager triggers VaccineReminderWorker      │
 * │  ↓                                               │
 * │  Load baby profile from database                 │
 * │  ↓                                               │
 * │  Get all vaccines + given status                 │
 * │  ↓                                               │
 * │  For each vaccine NOT given:                     │
 * │    Calculate: dueDate = DOB + weeksAfterBirth    │
 * │    If due within 1 day → SEND NOTIFICATION       │
 * │    If overdue ≤ 7 days → SEND NOTIFICATION       │
 * │  ↓                                               │
 * │  Return Result.success()                         │
 * └──────────────────────────────────────────────────┘
 *
 * WHY CoroutineWorker?
 * - Regular Worker runs on a background thread (blocking)
 * - CoroutineWorker runs in a coroutine (non-blocking, cleaner code)
 * - We need coroutines because Room DAO operations are suspend functions
 */
class VaccineReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        // Unique name for the periodic work request
        const val WORK_NAME = "vaccine_reminder_daily"
    }

    /**
     * The main work function — called by WorkManager.
     *
     * Returns:
     * - Result.success() — completed successfully
     * - Result.retry() — failed, try again later
     * - Result.failure() — failed permanently
     */
    override suspend fun doWork(): Result {
        return try {
            val db = ShishuSnehDatabase.getDatabase(applicationContext)
            val vaccineRepo = VaccineRepository(db.vaccineDao(), db.vaccinationRecordDao())

            // Get the baby profile
            val profile = db.babyProfileDao().getProfileDirect()
                ?: return Result.success()  // No profile yet — nothing to check

            val babyName = profile.babyName
            val dob = profile.dateOfBirth

            // Get all vaccines and which ones are given
            val allVaccines = vaccineRepo.getAllVaccinesDirect()
            val givenIds = vaccineRepo.getGivenVaccineIds(profile.id)

            val now = System.currentTimeMillis()

            // Check each vaccine that hasn't been given yet
            for (vaccine in allVaccines) {
                if (vaccine.id in givenIds) continue  // Already given, skip

                // Calculate due date
                val dueDate = dob + TimeUnit.DAYS.toMillis(vaccine.weeksAfterBirth * 7L)
                val msUntilDue = dueDate - now
                val daysUntilDue = TimeUnit.MILLISECONDS.toDays(msUntilDue).toInt()

                // Send notification based on how close the due date is
                when {
                    // Due today
                    daysUntilDue == 0 -> {
                        NotificationHelper.showVaccineNotification(
                            context = applicationContext,
                            vaccineId = vaccine.id,
                            title = applicationContext.getString(R.string.notification_vaccine_title),
                            body = applicationContext.getString(
                                R.string.notification_vaccine_due_today,
                                vaccine.name,
                                babyName
                            )
                        )
                    }

                    // Due tomorrow
                    daysUntilDue == 1 -> {
                        NotificationHelper.showVaccineNotification(
                            context = applicationContext,
                            vaccineId = vaccine.id,
                            title = applicationContext.getString(R.string.notification_vaccine_title),
                            body = applicationContext.getString(
                                R.string.notification_vaccine_due_tomorrow,
                                vaccine.name,
                                babyName
                            )
                        )
                    }

                    // Overdue (up to 7 days)
                    daysUntilDue in -7..-1 -> {
                        NotificationHelper.showVaccineNotification(
                            context = applicationContext,
                            vaccineId = vaccine.id,
                            title = applicationContext.getString(R.string.notification_vaccine_title),
                            body = applicationContext.getString(
                                R.string.notification_vaccine_overdue,
                                vaccine.name,
                                babyName
                            )
                        )
                    }
                }
            }

            Result.success()

        } catch (e: Exception) {
            // If something goes wrong, retry later
            Result.retry()
        }
    }
}
