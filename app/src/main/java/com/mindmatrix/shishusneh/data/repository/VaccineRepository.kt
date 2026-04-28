package com.mindmatrix.shishusneh.data.repository

import com.mindmatrix.shishusneh.data.local.Vaccine
import com.mindmatrix.shishusneh.data.local.VaccinationRecord
import com.mindmatrix.shishusneh.data.local.VaccinationRecordDao
import com.mindmatrix.shishusneh.data.local.VaccineDao
import com.mindmatrix.shishusneh.data.model.VaccineStatus
import com.mindmatrix.shishusneh.data.model.VaccineWithStatus
import java.util.concurrent.TimeUnit

/**
 * VaccineRepository — Business Logic for Vaccination Tracking
 *
 * Combines data from VaccineDao and VaccinationRecordDao to produce
 * the full vaccination schedule with computed statuses.
 *
 * KEY LOGIC:
 * ┌──────────────────────────────────────────────────────────┐
 * │ For each vaccine:                                        │
 * │   dueDate = babyDOB + (vaccine.weeksAfterBirth * 7 days)│
 * │   if given → GIVEN                                       │
 * │   else if dueDate < today → OVERDUE                     │
 * │   else if dueDate < today + 7 days → DUE                │
 * │   else → UPCOMING                                        │
 * └──────────────────────────────────────────────────────────┘
 */
class VaccineRepository(
    private val vaccineDao: VaccineDao,
    private val vaccinationRecordDao: VaccinationRecordDao
) {

    // =====================================================
    // VACCINE SCHEDULE
    // =====================================================

    /**
     * Build the complete vaccination schedule with statuses.
     *
     * @param profileId Baby's profile ID
     * @param dob Baby's date of birth (epoch ms)
     * @return List of VaccineWithStatus sorted by due date
     */
    suspend fun getVaccineSchedule(profileId: Int, dob: Long): List<VaccineWithStatus> {
        val vaccines = vaccineDao.getAllVaccinesDirect()
        val givenRecords = vaccinationRecordDao.getAllRecordsDirect(profileId)

        // Create a map of vaccineId → VaccinationRecord for quick lookup
        val givenMap = givenRecords.associateBy { it.vaccineId }

        val now = System.currentTimeMillis()

        return vaccines.map { vaccine ->
            // Calculate due date
            val dueDate = dob + TimeUnit.DAYS.toMillis(vaccine.weeksAfterBirth * 7L)

            // Check if given
            val record = givenMap[vaccine.id]

            if (record != null) {
                // ✅ Already given
                VaccineWithStatus(
                    vaccine = vaccine,
                    status = VaccineStatus.GIVEN,
                    dueDate = dueDate,
                    dateGiven = record.dateGiven,
                    daysUntilDue = null,
                    notes = record.notes
                )
            } else {
                // Calculate days until due
                val msUntilDue = dueDate - now
                val daysUntilDue = TimeUnit.MILLISECONDS.toDays(msUntilDue).toInt()

                // Determine status
                val status = when {
                    daysUntilDue < 0 -> VaccineStatus.OVERDUE   // 🔴 Past due
                    daysUntilDue <= 7 -> VaccineStatus.DUE       // ⚠️ Due soon
                    else -> VaccineStatus.UPCOMING                // 🔵 Future
                }

                VaccineWithStatus(
                    vaccine = vaccine,
                    status = status,
                    dueDate = dueDate,
                    dateGiven = null,
                    daysUntilDue = daysUntilDue,
                    notes = ""
                )
            }
        }
    }

    /**
     * Get the next N upcoming or overdue vaccines for the dashboard card.
     *
     * @param profileId Baby's profile ID
     * @param dob Baby's date of birth (epoch ms)
     * @param limit Max number of vaccines to return
     * @return List of upcoming/due/overdue vaccines (not given ones)
     */
    suspend fun getUpcomingVaccines(profileId: Int, dob: Long, limit: Int = 3): List<VaccineWithStatus> {
        return getVaccineSchedule(profileId, dob)
            .filter { it.status != VaccineStatus.GIVEN }
            .sortedBy { it.dueDate }
            .take(limit)
    }

    /**
     * Count how many vaccines are overdue.
     */
    suspend fun getOverdueCount(profileId: Int, dob: Long): Int {
        return getVaccineSchedule(profileId, dob)
            .count { it.status == VaccineStatus.OVERDUE }
    }

    // =====================================================
    // MARK AS GIVEN / UNDO
    // =====================================================

    /**
     * Mark a vaccine as given (create a VaccinationRecord).
     */
    suspend fun markAsGiven(
        vaccineId: Int,
        profileId: Int,
        dateGiven: Long,
        notes: String = ""
    ) {
        val record = VaccinationRecord(
            vaccineId = vaccineId,
            babyProfileId = profileId,
            dateGiven = dateGiven,
            notes = notes.trim()
        )
        vaccinationRecordDao.insertRecord(record)
    }

    /**
     * Un-mark a vaccine as given (delete the VaccinationRecord).
     */
    suspend fun unmarkAsGiven(vaccineId: Int, profileId: Int) {
        vaccinationRecordDao.deleteByVaccineAndProfile(vaccineId, profileId)
    }

    // =====================================================
    // COUNTS
    // =====================================================

    /**
     * Get total number of vaccines in the schedule.
     */
    suspend fun getTotalVaccineCount(): Int {
        return vaccineDao.getVaccineCount()
    }

    /**
     * Get the count of given vaccines (for progress bar).
     */
    suspend fun getGivenCount(profileId: Int): Int {
        return vaccinationRecordDao.getGivenCount(profileId)
    }

    // =====================================================
    // DIRECT ACCESS (for WorkManager)
    // =====================================================

    /**
     * Get all vaccines (non-LiveData, for WorkManager worker).
     */
    suspend fun getAllVaccinesDirect(): List<Vaccine> {
        return vaccineDao.getAllVaccinesDirect()
    }

    /**
     * Get all given vaccine IDs (for WorkManager worker).
     */
    suspend fun getGivenVaccineIds(profileId: Int): List<Int> {
        return vaccinationRecordDao.getGivenVaccineIds(profileId)
    }
}
