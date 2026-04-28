package com.mindmatrix.shishusneh.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * VaccineDao — Data Access Object for Vaccines
 *
 * Read-only operations for the pre-populated vaccines table.
 * Also includes an insert for the initial pre-population.
 */
@Dao
interface VaccineDao {

    /**
     * GET all vaccines ordered by their scheduled age.
     * Used by the VaccineScheduleActivity.
     */
    @Query("SELECT * FROM vaccines ORDER BY weeksAfterBirth ASC, id ASC")
    fun getAllVaccines(): LiveData<List<Vaccine>>

    /**
     * GET all vaccines as a plain list (non-LiveData).
     * Used by the WorkManager worker for checking due dates.
     */
    @Query("SELECT * FROM vaccines ORDER BY weeksAfterBirth ASC, id ASC")
    suspend fun getAllVaccinesDirect(): List<Vaccine>

    /**
     * GET a single vaccine by ID.
     * Used by the detail bottom sheet.
     */
    @Query("SELECT * FROM vaccines WHERE id = :id")
    suspend fun getVaccineById(id: Int): Vaccine?

    /**
     * INSERT a vaccine (used for pre-population only).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVaccine(vaccine: Vaccine)

    /**
     * INSERT multiple vaccines at once (used for pre-population).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vaccines: List<Vaccine>)

    /**
     * GET count of vaccines in the table.
     * Used to check if pre-population has already happened.
     */
    @Query("SELECT COUNT(*) FROM vaccines")
    suspend fun getVaccineCount(): Int
}
