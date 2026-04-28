package com.mindmatrix.shishusneh.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * BabyProfileDao — Data Access Object
 *
 * This is the "menu" of database operations.
 * You define WHAT you want to do, and Room writes the actual SQL code!
 *
 * HOW IT WORKS:
 * ┌───────────────────────────────────────────────────┐
 * │  @Dao = "This interface talks to the database"    │
 * │  @Insert = "Add new data"                         │
 * │  @Query  = "Read data using SQL"                  │
 * │  @Update = "Change existing data"                 │
 * │  suspend = "Runs on background thread"            │
 * │  LiveData = "UI auto-updates when data changes"   │
 * └───────────────────────────────────────────────────┘
 *
 * IMPORTANT: Room generates the implementation of this interface.
 * You write the interface → Room writes the actual code!
 */
@Dao
interface BabyProfileDao {

    /**
     * INSERT a new baby profile into the database.
     *
     * 'suspend' keyword means this function runs on a BACKGROUND thread.
     * This is important because database operations can be slow,
     * and we don't want to freeze the UI while saving.
     *
     * @param profile The BabyProfile to save
     * @return The row ID of the newly inserted profile
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BabyProfile): Long

    /**
     * GET the baby profile from the database.
     *
     * LiveData is MAGICAL — it automatically notifies the UI when data changes!
     * So if the profile gets updated, the UI refreshes instantly.
     *
     * LIMIT 1 = We only need one profile (this app is for one baby)
     *
     * @return LiveData wrapping the profile (null if no profile exists)
     */
    @Query("SELECT * FROM baby_profiles ORDER BY id ASC LIMIT 1")
    fun getProfile(): LiveData<BabyProfile?>

    /**
     * GET profile without LiveData (for one-time checks).
     *
     * Used to check if onboarding was already completed.
     * Unlike LiveData version, this returns the value directly.
     *
     * @return The profile or null
     */
    @Query("SELECT * FROM baby_profiles ORDER BY id ASC LIMIT 1")
    suspend fun getProfileDirect(): BabyProfile?

    /**
     * UPDATE an existing profile.
     *
     * Room matches the profile by its primary key (id).
     * Only the changed fields get updated.
     *
     * @param profile The updated BabyProfile
     */
    @Update
    suspend fun updateProfile(profile: BabyProfile)

    // =====================================================
    // PHASE 4 — MULTI-BABY SUPPORT
    // =====================================================

    /**
     * GET a profile by its ID (LiveData — for reactive UI).
     *
     * @param profileId The profile ID to look up
     * @return LiveData wrapping the profile (null if not found)
     */
    @Query("SELECT * FROM baby_profiles WHERE id = :profileId")
    fun getProfileById(profileId: Int): LiveData<BabyProfile?>

    /**
     * GET a profile by its ID (direct, for one-time lookups).
     *
     * Used by EditProfileActivity to pre-fill the form.
     *
     * @param profileId The profile ID
     * @return The profile or null
     */
    @Query("SELECT * FROM baby_profiles WHERE id = :profileId")
    suspend fun getProfileByIdDirect(profileId: Int): BabyProfile?

    /**
     * GET ALL baby profiles as LiveData.
     *
     * Used by the Dashboard profile switcher to show all babies.
     *
     * @return LiveData list of all profiles, ordered by creation date
     */
    @Query("SELECT * FROM baby_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): LiveData<List<BabyProfile>>

    /**
     * GET ALL baby profiles as a plain list.
     *
     * Used for JSON backup export.
     *
     * @return List of all profiles
     */
    @Query("SELECT * FROM baby_profiles ORDER BY createdAt ASC")
    suspend fun getAllProfilesDirect(): List<BabyProfile>

    /**
     * DELETE a specific baby profile.
     *
     * Used when a parent wants to remove a baby profile.
     * Associated growth and vaccination records should be
     * cleaned up separately.
     *
     * @param profile The BabyProfile to delete
     */
    @Delete
    suspend fun deleteProfile(profile: BabyProfile)

    /**
     * DELETE all profiles.
     *
     * Useful for testing or if user wants to reset the app.
     * In production, you'd want a confirmation dialog before calling this!
     */
    @Query("DELETE FROM baby_profiles")
    suspend fun deleteAllProfiles()
}
