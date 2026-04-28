package com.mindmatrix.shishusneh.ui.onboarding

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
 * OnboardingViewModel — Brain of the Onboarding Screen
 *
 * WHY USE A VIEWMODEL?
 * When you rotate your phone, Android DESTROYS and RECREATES the Activity.
 * Without ViewModel, all data (form inputs, state) would be lost!
 *
 * ViewModel survives rotation:
 * ┌──────────┐ rotate ┌──────────┐
 * │ Activity │ ──────→ │ Activity │  (new Activity)
 * │ destroyed│        │ created  │
 * └──────────┘        └──────────┘
 *       │                    │
 *       └──── ViewModel ─────┘  (SAME ViewModel, data preserved!)
 *
 * AndroidViewModel vs ViewModel:
 * - ViewModel: no access to Application context
 * - AndroidViewModel: has Application context (needed for database creation)
 *
 * @param application The Application context (survives Activity lifecycle)
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    // =====================================================
    // SETUP: Database → DAO → Repository chain
    // =====================================================

    private val repository: BabyProfileRepository

    init {
        // Build the data chain:
        // 1. Get the database instance
        // 2. Get the DAO from the database
        // 3. Create the repository with the DAO
        val dao = ShishuSnehDatabase.getDatabase(application).babyProfileDao()
        repository = BabyProfileRepository(dao)
    }

    // =====================================================
    // LIVEDATA — Observable data for the UI
    // =====================================================

    /*
     * MutableLiveData vs LiveData:
     * - MutableLiveData: can be changed (private — only ViewModel can modify)
     * - LiveData: read-only (public — UI can only observe, not change)
     *
     * This pattern prevents the UI from accidentally modifying data.
     * Think of it as: ViewModel = writer, UI = reader
     */

    // Signals that the profile was saved successfully
    private val _profileSaved = MutableLiveData<Boolean>()
    val profileSaved: LiveData<Boolean> = _profileSaved

    // Signals an error message to show to the user
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // =====================================================
    // FUNCTIONS — Business Logic
    // =====================================================

    /**
     * Save the baby profile to the database.
     *
     * This function:
     * 1. Validates all inputs (name, DOB, gender)
     * 2. Creates a BabyProfile object
     * 3. Saves it to the database
     * 4. Notifies the UI when done
     *
     * viewModelScope.launch = starts a COROUTINE (lightweight background task)
     * This prevents the UI from freezing during the save operation.
     *
     * @param babyName   Baby's name from the text field
     * @param dateOfBirth DOB in milliseconds (from DatePicker)
     * @param motherName  Mother's name (optional)
     * @param gender      "Male", "Female", or "Other"
     */
    fun saveBabyProfile(
        babyName: String,
        dateOfBirth: Long,
        motherName: String,
        gender: String
    ) {
        // ── Step 1: Validate inputs ──
        if (babyName.isBlank()) {
            _errorMessage.value = "Please enter baby's name"
            return  // Stop here — don't save invalid data
        }
        if (dateOfBirth == 0L) {
            _errorMessage.value = "Please select date of birth"
            return
        }
        if (gender.isBlank()) {
            _errorMessage.value = "Please select gender"
            return
        }

        // ── Step 2: Save to database ──
        // viewModelScope ensures the coroutine is cancelled if ViewModel is destroyed
        viewModelScope.launch {
            try {
                // Create the profile object
                val profile = BabyProfile(
                    babyName = babyName.trim(),    // Remove extra spaces
                    dateOfBirth = dateOfBirth,
                    motherName = motherName.trim(),
                    gender = gender
                )

                // Save to Room database (returns new row ID)
                val newId = repository.insertProfile(profile)

                // Phase 4: Set as active profile
                com.mindmatrix.shishusneh.util.ProfilePreferences(getApplication()).activeProfileId = newId.toInt()

                // Notify UI: "Save was successful!"
                // postValue is used instead of value because we're on a background thread
                _profileSaved.postValue(true)

            } catch (e: Exception) {
                // Something went wrong — tell the user
                _errorMessage.postValue("Failed to save: ${e.message}")
            }
        }
    }

    /**
     * Check if a profile already exists (to skip onboarding).
     *
     * If the user already completed onboarding before,
     * we should go directly to the main screen.
     *
     * @param onResult Callback with true if profile exists, false otherwise
     */
    fun checkExistingProfile(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = repository.hasProfile()
            onResult(exists)
        }
    }

    /**
     * Clear the error message after the UI has shown it.
     *
     * This prevents the same error from showing again
     * after a screen rotation.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
