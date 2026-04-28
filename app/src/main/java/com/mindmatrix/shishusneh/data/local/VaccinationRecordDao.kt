package com.mindmatrix.shishusneh.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * VaccinationRecordDao — Data Access Object for Vaccination Records
 *
 * Manages records of which vaccines have been administered.
 */
@Dao
interface VaccinationRecordDao {

    /**
     * INSERT a vaccination record (mark vaccine as given).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: VaccinationRecord): Long

    /**
     * DELETE a vaccination record (undo "mark as given").
     */
    @Delete
    suspend fun deleteRecord(record: VaccinationRecord)

    /**
     * DELETE record by vaccine ID and profile ID (undo by reference).
     */
    @Query("DELETE FROM vaccination_records WHERE vaccineId = :vaccineId AND babyProfileId = :profileId")
    suspend fun deleteByVaccineAndProfile(vaccineId: Int, profileId: Int)

    /**
     * GET record for a specific vaccine and baby.
     * Returns null if not yet given.
     */
    @Query("SELECT * FROM vaccination_records WHERE vaccineId = :vaccineId AND babyProfileId = :profileId LIMIT 1")
    suspend fun getRecordForVaccine(vaccineId: Int, profileId: Int): VaccinationRecord?

    /**
     * GET all vaccination records for a baby (LiveData).
     */
    @Query("SELECT * FROM vaccination_records WHERE babyProfileId = :profileId ORDER BY dateGiven DESC")
    fun getAllRecords(profileId: Int): LiveData<List<VaccinationRecord>>

    /**
     * GET all vaccination records as a plain list.
     * Used by the WorkManager worker.
     */
    @Query("SELECT * FROM vaccination_records WHERE babyProfileId = :profileId")
    suspend fun getAllRecordsDirect(profileId: Int): List<VaccinationRecord>

    /**
     * GET list of vaccine IDs that have been administered.
     * Used for quickly checking which vaccines are "done".
     */
    @Query("SELECT vaccineId FROM vaccination_records WHERE babyProfileId = :profileId")
    suspend fun getGivenVaccineIds(profileId: Int): List<Int>

    /**
     * GET count of given vaccines (for progress display).
     */
    @Query("SELECT COUNT(*) FROM vaccination_records WHERE babyProfileId = :profileId")
    suspend fun getGivenCount(profileId: Int): Int

    /**
     * GET count of given vaccines as LiveData (for reactive progress bar).
     */
    @Query("SELECT COUNT(*) FROM vaccination_records WHERE babyProfileId = :profileId")
    fun getGivenCountLive(profileId: Int): LiveData<Int>

    @Query("DELETE FROM vaccination_records WHERE babyProfileId = :profileId")
    suspend fun deleteRecordsByProfileId(profileId: Int)
}
