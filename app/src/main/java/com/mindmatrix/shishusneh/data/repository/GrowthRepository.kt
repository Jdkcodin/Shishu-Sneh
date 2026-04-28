package com.mindmatrix.shishusneh.data.repository

import androidx.lifecycle.LiveData
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.data.local.GrowthRecordDao
import com.mindmatrix.shishusneh.data.model.BMIResult
import com.mindmatrix.shishusneh.data.model.GrowthSummary
import java.util.concurrent.TimeUnit

/**
 * GrowthRepository — Data Access + Business Logic for Growth Tracking
 *
 * Sits between ViewModels and the GrowthRecordDao.
 * Handles raw data access AND computed statistics (BMI, growth summaries).
 *
 * ┌──────────────┐     ┌──────────────────┐     ┌────────────────┐
 * │  ViewModels  │ ──→ │ GrowthRepository │ ──→ │ GrowthRecordDao│
 * │  (UI logic)  │     │  (data + stats)  │     │  (database)    │
 * └──────────────┘     └──────────────────┘     └────────────────┘
 *
 * @param growthRecordDao The DAO for growth_records table operations
 */
class GrowthRepository(private val growthRecordDao: GrowthRecordDao) {

    // =====================================================
    // DATA ACCESS — Proxy to DAO
    // =====================================================

    /**
     * Get all records for a baby (LiveData — auto-updates UI).
     * Used by Growth Chart and Measurement History screens.
     */
    fun getAllRecords(profileId: Int): LiveData<List<GrowthRecord>> {
        return growthRecordDao.getAllRecords(profileId)
    }

    /**
     * Get most recent N records (LiveData — for dashboard mini-chart).
     */
    fun getRecentRecords(profileId: Int, limit: Int = 6): LiveData<List<GrowthRecord>> {
        return growthRecordDao.getRecentRecords(profileId, limit)
    }

    /**
     * Save a new growth measurement.
     *
     * @return Row ID of inserted record
     */
    suspend fun insertRecord(record: GrowthRecord): Long {
        return growthRecordDao.insertRecord(record)
    }

    /**
     * Delete a specific record (swipe-to-delete).
     */
    suspend fun deleteRecord(record: GrowthRecord) {
        growthRecordDao.deleteRecord(record)
    }

    /**
     * Get all records as a plain list (for CSV export).
     */
    suspend fun getAllRecordsDirect(profileId: Int): List<GrowthRecord> {
        return growthRecordDao.getAllRecordsDirect(profileId)
    }

    /**
     * Get records within a date range.
     */
    suspend fun getRecordsByDateRange(
        profileId: Int,
        startDate: Long,
        endDate: Long
    ): List<GrowthRecord> {
        return growthRecordDao.getRecordsByDateRange(profileId, startDate, endDate)
    }

    // =====================================================
    // BUSINESS LOGIC — Computed Statistics
    // =====================================================

    /**
     * Calculate BMI from weight and height.
     *
     * @return BMIResult with value, category, and color — or null if invalid
     */
    fun calculateBMI(weightKg: Float, heightCm: Float): BMIResult? {
        return BMIResult.calculate(weightKg, heightCm)
    }

    /**
     * Compute growth summary statistics.
     *
     * This does several queries to build a complete picture:
     * 1. Latest measurement → current weight & height
     * 2. Oldest measurement → "since birth" comparisons
     * 3. Records from last 30 days → monthly weight change
     * 4. Total record count
     *
     * @param profileId The baby profile to compute stats for
     * @return GrowthSummary with all computed statistics
     */
    suspend fun getGrowthSummary(profileId: Int): GrowthSummary {
        val latest = growthRecordDao.getLatestRecord(profileId)
        val oldest = growthRecordDao.getOldestRecord(profileId)
        val totalCount = growthRecordDao.getRecordCount(profileId)

        if (latest == null || totalCount == 0) {
            return GrowthSummary(totalRecords = 0)
        }

        // Calculate days since last measurement
        val daysSinceLast = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - latest.date
        ).toInt()

        // Calculate weight change in the last 30 days
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        val recordsLastMonth = growthRecordDao.getRecordsByDateRange(
            profileId, thirtyDaysAgo, System.currentTimeMillis()
        )
        val weightChangeLastMonth = if (recordsLastMonth.size >= 2) {
            val oldestInMonth = recordsLastMonth.first()
            val newestInMonth = recordsLastMonth.last()
            newestInMonth.weightKg - oldestInMonth.weightKg
        } else {
            null
        }

        // Calculate growth since birth (since first measurement)
        val heightChangeSinceBirth = if (oldest != null && oldest.id != latest.id) {
            latest.heightCm - oldest.heightCm
        } else {
            null
        }

        val weightChangeSinceBirth = if (oldest != null && oldest.id != latest.id) {
            latest.weightKg - oldest.weightKg
        } else {
            null
        }

        return GrowthSummary(
            weightChangeLastMonth = weightChangeLastMonth,
            heightChangeSinceBirth = heightChangeSinceBirth,
            totalRecords = totalCount,
            daysSinceLastRecord = daysSinceLast,
            currentWeight = latest.weightKg,
            currentHeight = latest.heightCm,
            birthWeight = oldest?.weightKg,
            birthHeight = oldest?.heightCm,
            weightChangeSinceBirth = weightChangeSinceBirth
        )
    }
}
