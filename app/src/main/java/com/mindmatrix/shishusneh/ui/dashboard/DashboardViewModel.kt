package com.mindmatrix.shishusneh.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.mindmatrix.shishusneh.data.local.BabyProfile
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.data.local.ShishuSnehDatabase
import com.mindmatrix.shishusneh.data.model.BMIResult
import com.mindmatrix.shishusneh.data.model.GrowthSummary
import com.mindmatrix.shishusneh.data.repository.BabyProfileRepository
import com.mindmatrix.shishusneh.data.repository.GrowthRepository
import com.mindmatrix.shishusneh.util.UnitSystem
import kotlinx.coroutines.launch

/**
 * DashboardViewModel — Brain of the Dashboard Screen
 *
 * Provides:
 * - Baby profile info (name, DOB)
 * - Latest measurement data
 * - BMI calculation result
 * - Growth summary statistics
 * - Recent records for the mini chart
 * - Unit system toggle (Metric / Imperial)
 *
 * ┌─────────────────────────────────────┐
 * │       DashboardViewModel            │
 * │                                     │
 * │  babyProfile  ← BabyProfileRepo    │
 * │  recentRecords ← GrowthRepo        │
 * │  bmiData      ← computed           │
 * │  growthSummary ← computed           │
 * │  unitSystem   ← SharedPreferences  │
 * └─────────────────────────────────────┘
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // =====================================================
    // SETUP
    // =====================================================

    private val babyProfileRepo: BabyProfileRepository
    private val growthRepo: GrowthRepository
    private val prefs = application.getSharedPreferences("shishu_sneh_prefs", Context.MODE_PRIVATE)
    private val profilePrefs = com.mindmatrix.shishusneh.util.ProfilePreferences(application)

    init {
        val db = ShishuSnehDatabase.getDatabase(application)
        babyProfileRepo = BabyProfileRepository(db.babyProfileDao())
        growthRepo = GrowthRepository(db.growthRecordDao())
    }

    // =====================================================
    // LIVEDATA
    // =====================================================

    /** All baby profiles */
    val allProfiles: LiveData<List<BabyProfile>> = babyProfileRepo.allProfiles

    private val _activeProfileId = MutableLiveData<Int>().apply {
        value = profilePrefs.activeProfileId
    }

    /** Baby profile from database (reacts to profile switches) */
    val babyProfile: LiveData<BabyProfile?> = _activeProfileId.switchMap { id ->
        if (id > 0) {
            babyProfileRepo.getProfileById(id)
        } else {
            babyProfileRepo.babyProfile
        }
    }

    /** Unit system preference */
    private val _unitSystem = MutableLiveData(loadUnitSystem())
    val unitSystem: LiveData<UnitSystem> = _unitSystem

    /** BMI calculation result */
    private val _bmiResult = MutableLiveData<BMIResult?>()
    val bmiResult: LiveData<BMIResult?> = _bmiResult

    /** Growth summary statistics */
    private val _growthSummary = MutableLiveData<GrowthSummary>()
    val growthSummary: LiveData<GrowthSummary> = _growthSummary

    /** Latest measurement */
    private val _latestRecord = MutableLiveData<GrowthRecord?>()
    val latestRecord: LiveData<GrowthRecord?> = _latestRecord

    /** Phase 3: Upcoming vaccines for the dashboard card */
    private val _upcomingVaccines = MutableLiveData<List<com.mindmatrix.shishusneh.data.model.VaccineWithStatus>>()
    val upcomingVaccines: LiveData<List<com.mindmatrix.shishusneh.data.model.VaccineWithStatus>> = _upcomingVaccines

    /** Phase 3: Overdue count */
    private val _overdueCount = MutableLiveData<Int>(0)
    val overdueCount: LiveData<Int> = _overdueCount


    // =====================================================
    // DATA LOADING
    // =====================================================

    /**
     * Get recent records for the mini chart (LiveData).
     */
    fun getRecentRecords(profileId: Int): LiveData<List<GrowthRecord>> {
        return growthRepo.getRecentRecords(profileId)
    }

    /**
     * Switch to a different baby profile.
     */
    fun switchProfile(id: Int) {
        if (id != _activeProfileId.value) {
            profilePrefs.activeProfileId = id
            _activeProfileId.value = id
        }
    }

    /**
     * Load all dashboard data for a given profile.
     * Called when the dashboard opens or when data changes.
     */
    fun loadDashboardData(profileId: Int) {
        viewModelScope.launch {
            try {
                // Load latest record
                val dao = ShishuSnehDatabase.getDatabase(getApplication()).growthRecordDao()
                val latest = dao.getLatestRecord(profileId)
                _latestRecord.postValue(latest)

                // Calculate BMI from latest record
                if (latest != null) {
                    val bmi = BMIResult.calculate(latest.weightKg, latest.heightCm)
                    _bmiResult.postValue(bmi)
                } else {
                    _bmiResult.postValue(null)
                }

                // Load growth summary
                val summary = growthRepo.getGrowthSummary(profileId)
                _growthSummary.postValue(summary)

                // Load Phase 3 Vaccine Data
                val vaccineRepo = com.mindmatrix.shishusneh.data.repository.VaccineRepository(
                    ShishuSnehDatabase.getDatabase(getApplication()).vaccineDao(),
                    ShishuSnehDatabase.getDatabase(getApplication()).vaccinationRecordDao()
                )
                val profileDao = ShishuSnehDatabase.getDatabase(getApplication()).babyProfileDao()
                val profile = profileDao.getProfileByIdDirect(profileId) ?: profileDao.getProfileDirect()
                if (profile != null) {
                    val nextVaccines = vaccineRepo.getUpcomingVaccines(profileId, profile.dateOfBirth, limit = 3)
                    _upcomingVaccines.postValue(nextVaccines)
                    
                    val overdue = vaccineRepo.getOverdueCount(profileId, profile.dateOfBirth)
                    _overdueCount.postValue(overdue)
                }

            } catch (e: Exception) {
                // Silently handle — dashboard will show empty states
                _latestRecord.postValue(null)
                _bmiResult.postValue(null)
                _growthSummary.postValue(GrowthSummary())
                // vaccine lists stay empty/null
            }
        }
    }

    // =====================================================
    // UNIT SYSTEM
    // =====================================================

    /**
     * Toggle between Metric and Imperial units.
     * Saves the preference to SharedPreferences.
     */
    fun toggleUnitSystem() {
        val current = _unitSystem.value ?: UnitSystem.METRIC
        val newSystem = if (current == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
        _unitSystem.value = newSystem
        saveUnitSystem(newSystem)
    }

    /**
     * Load unit system preference from SharedPreferences.
     */
    private fun loadUnitSystem(): UnitSystem {
        val name = prefs.getString("unit_system", UnitSystem.METRIC.name) ?: UnitSystem.METRIC.name
        return try {
            UnitSystem.valueOf(name)
        } catch (e: Exception) {
            UnitSystem.METRIC
        }
    }

    /**
     * Save unit system preference to SharedPreferences.
     */
    private fun saveUnitSystem(system: UnitSystem) {
        prefs.edit().putString("unit_system", system.name).apply()
    }

    /**
     * Get records for CSV export (suspend function).
     */
    suspend fun getRecordsForExport(profileId: Int): List<GrowthRecord> {
        return growthRepo.getAllRecordsDirect(profileId)
    }

    // =====================================================
    // JSON BACKUP & RESTORE (Phase 4)
    // =====================================================

    /**
     * Creates a full JSON backup of all profiles and records, then triggers Android share intent.
     */
    fun exportFullBackup(context: Context) {
        viewModelScope.launch {
            try {
                val db = ShishuSnehDatabase.getDatabase(context)
                val profiles = db.babyProfileDao().getAllProfilesDirect()
                
                val growthRecords = mutableListOf<GrowthRecord>()
                val vaxRecords = mutableListOf<com.mindmatrix.shishusneh.data.local.VaccinationRecord>()
                
                profiles.forEach { p ->
                    growthRecords.addAll(db.growthRecordDao().getAllRecordsDirect(p.id))
                    vaxRecords.addAll(db.vaccinationRecordDao().getAllRecordsDirect(p.id))
                }
                
                val backupData = com.mindmatrix.shishusneh.util.JsonBackupManager.BackupData(
                    profiles, growthRecords, vaxRecords
                )
                
                val uri = com.mindmatrix.shishusneh.util.JsonBackupManager.createBackupFile(context, backupData)
                
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Shishu Sneh Full Backup")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Save Backup"))
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Parses a JSON backup file and restores it to the database using REPLACE strategy.
     */
    fun restoreBackup(context: Context, uri: android.net.Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = com.mindmatrix.shishusneh.util.JsonBackupManager.parseBackup(context, uri)
                val db = ShishuSnehDatabase.getDatabase(context)
                
                // Insert all parsed data (using REPLACE on conflict)
                backupData.profiles.forEach { db.babyProfileDao().insertProfile(it) }
                backupData.growthRecords.forEach { db.growthRecordDao().insertRecord(it) }
                backupData.vaccinationRecords.forEach { db.vaccinationRecordDao().insertRecord(it) }
                
                // Reload dashboard data
                _activeProfileId.value?.let { loadDashboardData(it) }
                
                // Notify UI
                onSuccess()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Restore failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =====================================================
    // DARK MODE (Phase 4)
    // =====================================================

    fun toggleDarkMode() {
        val current = prefs.getBoolean("dark_mode", false)
        val newMode = !current
        prefs.edit().putBoolean("dark_mode", newMode).apply()
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (newMode) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
