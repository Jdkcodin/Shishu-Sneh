package com.mindmatrix.shishusneh.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mindmatrix.shishusneh.data.local.BabyProfile
import com.mindmatrix.shishusneh.data.local.ShishuSnehDatabase
import com.mindmatrix.shishusneh.data.repository.BabyProfileRepository
import kotlinx.coroutines.launch

/**
 * EditProfileViewModel — Brain of the Edit Profile Screen (Phase 4)
 *
 * Loads the current baby profile and handles validation + update.
 *
 * FLOW:
 * ┌────────────────────────────────────────────────┐
 * │ loadProfile(id) → fills LiveData               │
 * │ Activity pre-fills form from LiveData           │
 * │ User edits fields → taps "Save Changes"        │
 * │ updateProfile() validates → updates Room DB     │
 * │ profileUpdated LiveData signals success → finish│
 * └────────────────────────────────────────────────┘
 */
class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BabyProfileRepository

    init {
        val dao = ShishuSnehDatabase.getDatabase(application).babyProfileDao()
        repository = BabyProfileRepository(dao)
    }

    // ── LiveData ──
    private val _currentProfile = MutableLiveData<BabyProfile?>()
    val currentProfile: LiveData<BabyProfile?> = _currentProfile

    private val _profileUpdated = MutableLiveData<Boolean>()
    val profileUpdated: LiveData<Boolean> = _profileUpdated

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Load the profile to edit.
     * Called from Activity's onCreate with the profile ID.
     */
    fun loadProfile(profileId: Int) {
        viewModelScope.launch {
            try {
                val dao = ShishuSnehDatabase.getDatabase(getApplication()).babyProfileDao()
                val profile = dao.getProfileByIdDirect(profileId)
                _currentProfile.postValue(profile)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to load profile: ${e.message}")
            }
        }
    }

    /**
     * Validate and update the profile in the database.
     */
    fun updateProfile(
        profileId: Int,
        babyName: String,
        dateOfBirth: Long,
        motherName: String,
        gender: String,
        createdAt: Long
    ) {
        // Validate
        if (babyName.isBlank()) {
            _errorMessage.value = "Please enter baby's name"
            return
        }
        if (dateOfBirth == 0L) {
            _errorMessage.value = "Please select date of birth"
            return
        }
        if (gender.isBlank()) {
            _errorMessage.value = "Please select gender"
            return
        }

        viewModelScope.launch {
            try {
                val updatedProfile = BabyProfile(
                    id = profileId,
                    babyName = babyName.trim(),
                    dateOfBirth = dateOfBirth,
                    motherName = motherName.trim(),
                    gender = gender,
                    createdAt = createdAt
                )
                repository.updateProfile(updatedProfile)
                _profileUpdated.postValue(true)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to save: ${e.message}")
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Delete the active profile and all associated data.
     */
    fun deleteProfileAndData(context: android.content.Context, onComplete: () -> Unit) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch {
            try {
                val db = ShishuSnehDatabase.getDatabase(context)
                db.growthRecordDao().deleteRecordsByProfileId(profile.id)
                db.vaccinationRecordDao().deleteRecordsByProfileId(profile.id)
                db.babyProfileDao().deleteProfile(profile)
                
                // Check if active profile was deleted
                val prefs = com.mindmatrix.shishusneh.util.ProfilePreferences(context)
                if (prefs.activeProfileId == profile.id) {
                    // Find next profile
                    val remainingProfiles = db.babyProfileDao().getAllProfilesDirect()
                    if (remainingProfiles.isNotEmpty()) {
                        prefs.activeProfileId = remainingProfiles.first().id
                    } else {
                        prefs.activeProfileId = 0
                    }
                }
                onComplete()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to delete: ${e.message}")
            }
        }
    }
}
