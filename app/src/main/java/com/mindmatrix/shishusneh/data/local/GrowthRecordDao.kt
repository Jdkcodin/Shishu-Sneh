package com.mindmatrix.shishusneh.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * GrowthRecordDao — Data Access Object for Growth Records
 *
 * All database operations for the growth_records table.
 * Room generates the SQL implementations from these annotations.
 *
 * OPERATIONS:
 * ┌──────────────────────┬─────────────────────────────────────┐
 * │ insertRecord()       │ Save a new measurement              │
 * │ getAllRecords()       │ LiveData list for chart & history   │
 * │ getRecentRecords()   │ Last N records for dashboard preview│
 * │ getLatestRecord()    │ Most recent for summary card        │
 * │ getOldestRecord()    │ First-ever for "since birth" stats  │
 * │ getRecordsByDateRange│ For CSV export filtering            │
 * │ deleteRecord()       │ Swipe-to-delete in history          │
 * │ getAllRecordsDirect() │ Non-LiveData list for CSV export    │
 * │ getRecordCount()     │ Total number of records             │
 * └──────────────────────┴─────────────────────────────────────┘
 */
@Dao
interface GrowthRecordDao {

    /**
     * INSERT a new growth measurement.
     *
     * @param record The GrowthRecord to save
     * @return The row ID of the newly inserted record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GrowthRecord): Long

    /**
     * GET all records for a baby, ordered by date (newest first).
     *
     * Returns LiveData so the UI auto-updates when records change.
     * Used by: Growth Chart, Measurement History
     *
     * @param profileId The baby profile to get records for
     */
    @Query("SELECT * FROM growth_records WHERE babyProfileId = :profileId ORDER BY date DESC")
    fun getAllRecords(profileId: Int): LiveData<List<GrowthRecord>>

    /**
     * GET the most recent N records for the dashboard mini-chart.
     *
     * Ordered by date ASC so the chart plots left-to-right chronologically.
     *
     * @param profileId The baby profile
     * @param limit How many recent records to fetch (default: 6)
     */
    @Query("""
        SELECT * FROM (
            SELECT * FROM growth_records 
            WHERE babyProfileId = :profileId 
            ORDER BY date DESC 
            LIMIT :limit
        ) ORDER BY date ASC
    """)
    fun getRecentRecords(profileId: Int, limit: Int = 6): LiveData<List<GrowthRecord>>

    /**
     * GET the latest (most recent) record.
     *
     * Used for the dashboard "Latest Measurement" card & BMI calculation.
     * Returns null if no records exist yet.
     *
     * @param profileId The baby profile
     */
    @Query("SELECT * FROM growth_records WHERE babyProfileId = :profileId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecord(profileId: Int): GrowthRecord?

    /**
     * GET the oldest (first-ever) record.
     *
     * Used for "growth since birth" calculations in the summary card.
     *
     * @param profileId The baby profile
     */
    @Query("SELECT * FROM growth_records WHERE babyProfileId = :profileId ORDER BY date ASC LIMIT 1")
    suspend fun getOldestRecord(profileId: Int): GrowthRecord?

    /**
     * GET records within a date range.
     *
     * Used for CSV export with date filtering and monthly growth calculations.
     *
     * @param profileId The baby profile
     * @param startDate Start of range (epoch ms, inclusive)
     * @param endDate End of range (epoch ms, inclusive)
     */
    @Query("""
        SELECT * FROM growth_records 
        WHERE babyProfileId = :profileId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date ASC
    """)
    suspend fun getRecordsByDateRange(profileId: Int, startDate: Long, endDate: Long): List<GrowthRecord>

    /**
     * DELETE a specific record (swipe-to-delete in History screen).
     *
     * Room matches by primary key (id).
     *
     * @param record The GrowthRecord to delete
     */
    @Delete
    suspend fun deleteRecord(record: GrowthRecord)

    /**
     * GET all records without LiveData wrapping.
     *
     * Used for CSV export — we need a one-time snapshot, not a live stream.
     *
     * @param profileId The baby profile
     */
    @Query("SELECT * FROM growth_records WHERE babyProfileId = :profileId ORDER BY date ASC")
    suspend fun getAllRecordsDirect(profileId: Int): List<GrowthRecord>

    /**
     * GET the total number of records for a baby.
     *
     * Used for empty-state checks and summary stats.
     *
     * @param profileId The baby profile
     */
    @Query("SELECT COUNT(*) FROM growth_records WHERE babyProfileId = :profileId")
    suspend fun getRecordCount(profileId: Int): Int

    @Query("DELETE FROM growth_records WHERE babyProfileId = :profileId")
    suspend fun deleteRecordsByProfileId(profileId: Int)
}
