package com.mindmatrix.shishusneh.data.repository

import androidx.lifecycle.LiveData
import com.mindmatrix.shishusneh.data.local.BabyProfile
import com.mindmatrix.shishusneh.data.local.BabyProfileDao

/**
 * BabyProfileRepository — The Data "Middleman"
 *
 * WHY DO WE NEED THIS?
 * The ViewModel shouldn't talk directly to the database.
 * The Repository sits in between:
 *
 * ┌──────────┐     ┌──────────────┐     ┌────────────┐
 * │ ViewModel│ ──→ │  Repository  │ ──→ │  Room DAO  │
 * │ (UI logic)│    │  (data logic)│     │ (database)  │
 * └──────────┘     └──────────────┘     └────────────┘
 *
 * BENEFITS:
 * 1. Clean separation — ViewModel doesn't know about databases
 * 2. Single source of truth — all data goes through ONE place
 * 3. Easy to extend — later we can add server sync here
 *    without changing the ViewModel at all!
 *
 * @param babyProfileDao The DAO to perform database operations
 */
class BabyProfileRepository(private val babyProfileDao: BabyProfileDao) {

    /**
     * LiveData of the baby profile.
     *
     * Any UI component observing this will automatically update
     * whenever the profile changes in the database.
     * No manual refresh needed — it's like a live feed!
     */
    val babyProfile: LiveData<BabyProfile?> = babyProfileDao.getProfile()

    /**
     * Save a new baby profile to the database.
     *
     * 'suspend' = runs on a background thread
     * This prevents the UI from freezing during the save.
     *
     * @param profile The BabyProfile to save
     * @return The row ID of the saved profile
     */
    suspend fun insertProfile(profile: BabyProfile): Long {
        return babyProfileDao.insertProfile(profile)
    }

    /**
     * Update an existing baby profile.
     *
     * Room matches by primary key (id).
     *
     * @param profile The updated BabyProfile
     */
    suspend fun updateProfile(profile: BabyProfile) {
        babyProfileDao.updateProfile(profile)
    }

    /**
     * Check if a baby profile already exists.
     *
     * Used to decide whether to show onboarding or skip to main screen.
     *
     * @return true if a profile exists, false otherwise
     */
    suspend fun hasProfile(): Boolean {
        return babyProfileDao.getProfileDirect() != null
    }

    // =====================================================
    // PHASE 4 — MULTI-BABY SUPPORT
    // =====================================================

    /**
     * Get all baby profiles as LiveData.
     * Used by the profile switcher in the Dashboard toolbar.
     */
    val allProfiles: LiveData<List<BabyProfile>> = babyProfileDao.getAllProfiles()

    /**
     * Get a specific profile by its ID (LiveData).
     */
    fun getProfileById(profileId: Int): LiveData<BabyProfile?> {
        return babyProfileDao.getProfileById(profileId)
    }

    /**
     * Get all profiles as a plain list (for JSON backup).
     */
    suspend fun getAllProfilesDirect(): List<BabyProfile> {
        return babyProfileDao.getAllProfilesDirect()
    }

    /**
     * Delete a specific baby profile.
     */
    suspend fun deleteProfile(profile: BabyProfile) {
        babyProfileDao.deleteProfile(profile)
    }
}
