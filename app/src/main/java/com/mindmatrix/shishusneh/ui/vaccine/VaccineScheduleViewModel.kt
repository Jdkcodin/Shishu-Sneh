package com.mindmatrix.shishusneh.ui.vaccine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mindmatrix.shishusneh.data.local.ShishuSnehDatabase
import com.mindmatrix.shishusneh.data.model.VaccineWithStatus
import com.mindmatrix.shishusneh.data.repository.VaccineRepository
import kotlinx.coroutines.launch

/**
 * VaccineScheduleViewModel — Manages UI state for the vaccine schedule
 *
 * Uses AndroidViewModel because we need the Context to get the Database.
 */
class VaccineScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ShishuSnehDatabase.getDatabase(application)
    private val repository = VaccineRepository(db.vaccineDao(), db.vaccinationRecordDao())
    private val profileDao = db.babyProfileDao()

    // Internal mutable state
    private val _vaccineSchedule = MutableLiveData<List<VaccineWithStatus>>()
    private val _givenCount = MutableLiveData<Int>(0)
    private val _totalCount = MutableLiveData<Int>(24) // Base schedule size

    // Public read-only state for UI
    val vaccineSchedule: LiveData<List<VaccineWithStatus>> = _vaccineSchedule
    val givenCount: LiveData<Int> = _givenCount
    val totalCount: LiveData<Int> = _totalCount

    private var currentProfileId: Int = -1
    private var currentDob: Long = 0

    init {
        // Load the baby profile first, then load the schedule
        viewModelScope.launch {
            val profile = profileDao.getProfileDirect()
            if (profile != null) {
                currentProfileId = profile.id
                currentDob = profile.dateOfBirth
                loadSchedule()
                updateCounts()
            }
        }
    }

    /**
     * Load the full computed schedule from the repository.
     */
    private fun loadSchedule() {
        if (currentProfileId == -1) return

        viewModelScope.launch {
            val schedule = repository.getVaccineSchedule(currentProfileId, currentDob)
            _vaccineSchedule.postValue(schedule)
        }
    }

    /**
     * Update the progress bar counts.
     */
    private fun updateCounts() {
        if (currentProfileId == -1) return

        viewModelScope.launch {
            _givenCount.postValue(repository.getGivenCount(currentProfileId))
            _totalCount.postValue(repository.getTotalVaccineCount())
        }
    }

    /**
     * Mark a vaccine as administered.
     * Re-loads the schedule so the UI updates.
     */
    fun markAsGiven(vaccineId: Int, dateGiven: Long, notes: String) {
        if (currentProfileId == -1) return

        viewModelScope.launch {
            repository.markAsGiven(vaccineId, currentProfileId, dateGiven, notes)
            // Trigger UI refresh
            loadSchedule()
            updateCounts()
        }
    }

    /**
     * Undo marking a vaccine as administered.
     * Re-loads the schedule so the UI updates.
     */
    fun unmarkAsGiven(vaccineId: Int) {
        if (currentProfileId == -1) return

        viewModelScope.launch {
            repository.unmarkAsGiven(vaccineId, currentProfileId)
            // Trigger UI refresh
            loadSchedule()
            updateCounts()
        }
    }
}
