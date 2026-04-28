package com.mindmatrix.shishusneh.ui.growth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.data.local.ShishuSnehDatabase
import com.mindmatrix.shishusneh.data.repository.GrowthRepository
import com.mindmatrix.shishusneh.util.UnitConverter
import kotlinx.coroutines.launch

/**
 * GrowthViewModel — Shared ViewModel for Growth Features
 *
 * Used by: AddMeasurementActivity, GrowthChartActivity, MeasurementHistoryActivity
 *
 * Manages:
 * - Saving new measurements (with validation)
 * - Providing all records as LiveData (for chart and history)
 * - Deleting records (swipe-to-delete)
 * - Providing records for CSV export
 *
 * ┌────────────────────────┐
 * │   GrowthViewModel      │
 * │                        │
 * │  saveMeasurement()     │ ← AddMeasurementActivity
 * │  allRecords            │ ← GrowthChartActivity
 * │  deleteMeasurement()   │ ← MeasurementHistoryActivity
 * │  getRecordsForExport() │ ← CSV Export
 * └────────────────────────┘
 */
class GrowthViewModel(application: Application) : AndroidViewModel(application) {

    // =====================================================
    // SETUP
    // =====================================================

    private val repository: GrowthRepository

    init {
        val dao = ShishuSnehDatabase.getDatabase(application).growthRecordDao()
        repository = GrowthRepository(dao)
    }

    // =====================================================
    // LIVEDATA
    // =====================================================

    /** Signal that a measurement was saved successfully */
    private val _measurementSaved = MutableLiveData<Boolean>()
    val measurementSaved: LiveData<Boolean> = _measurementSaved

    /** Error messages for the UI */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // =====================================================
    // DATA ACCESS
    // =====================================================

    /**
     * Get all records for a baby profile (LiveData for auto-updating UI).
     * Used by: Growth Chart, Measurement History
     */
    fun getAllRecords(profileId: Int): LiveData<List<GrowthRecord>> {
        return repository.getAllRecords(profileId)
    }

    /**
     * Get recent records for dashboard mini-chart.
     */
    fun getRecentRecords(profileId: Int, limit: Int = 6): LiveData<List<GrowthRecord>> {
        return repository.getRecentRecords(profileId, limit)
    }

    // =====================================================
    // SAVE MEASUREMENT
    // =====================================================

    /**
     * Validate and save a new growth measurement.
     *
     * Validation rules:
     * 1. Date must be selected (not 0L)
     * 2. Weight must be a positive number
     * 3. Height must be a positive number
     * 4. Head circumference (if provided) must be positive
     *
     * @param profileId The baby profile ID
     * @param date Measurement date in epoch ms
     * @param weightInput Weight value as entered by user
     * @param heightInput Height value as entered by user
     * @param headCircInput Head circumference as entered (nullable)
     * @param notes Optional notes
     * @param isMetric Whether inputs are in metric (true) or imperial (false)
     */
    fun saveMeasurement(
        profileId: Int,
        date: Long,
        weightInput: Float,
        heightInput: Float,
        headCircInput: Float?,
        notes: String,
        isMetric: Boolean,
        recordId: Int = 0
    ) {
        // ── Validation ──
        if (date == 0L) {
            _errorMessage.value = "Please select a measurement date"
            return
        }
        if (weightInput <= 0f) {
            _errorMessage.value = "Weight must be a positive number"
            return
        }
        if (heightInput <= 0f) {
            _errorMessage.value = "Height must be a positive number"
            return
        }
        if (headCircInput != null && headCircInput <= 0f) {
            _errorMessage.value = "Head circumference must be a positive number"
            return
        }

        // ── Convert to metric for storage ──
        val weightKg = UnitConverter.toMetricWeight(weightInput, isMetric)
        val heightCm = UnitConverter.toMetricHeight(heightInput, isMetric)
        val headCircCm = headCircInput?.let { UnitConverter.toMetricHeight(it, isMetric) }

        // ── Save to database ──
        viewModelScope.launch {
            try {
                val record = GrowthRecord(
                    id = recordId,
                    babyProfileId = profileId,
                    date = date,
                    weightKg = weightKg,
                    heightCm = heightCm,
                    headCircumferenceCm = headCircCm,
                    notes = notes.trim()
                )
                repository.insertRecord(record)
                _measurementSaved.postValue(true)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to save: ${e.message}")
            }
        }
    }

    // =====================================================
    // DELETE MEASUREMENT
    // =====================================================

    /**
     * Delete a measurement (swipe-to-delete in History).
     *
     * @param record The record to delete
     */
    fun deleteMeasurement(record: GrowthRecord) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(record)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to delete: ${e.message}")
            }
        }
    }

    /**
     * Re-insert a measurement (undo delete).
     *
     * @param record The record to re-insert
     */
    fun undoDelete(record: GrowthRecord) {
        viewModelScope.launch {
            try {
                repository.insertRecord(record)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to undo: ${e.message}")
            }
        }
    }

    // =====================================================
    // EXPORT
    // =====================================================

    /**
     * Get all records as a plain list for CSV export.
     * Must be called from a coroutine scope.
     */
    suspend fun getRecordsForExport(profileId: Int): List<GrowthRecord> {
        return repository.getAllRecordsDirect(profileId)
    }

    // =====================================================
    // UTILITY
    // =====================================================

    /** Clear the error message after the UI has shown it */
    fun clearError() {
        _errorMessage.value = null
    }

    /** Reset the saved flag */
    fun resetSavedFlag() {
        _measurementSaved.value = false
    }
}
