package com.mindmatrix.shishusneh.ui.growth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.ShishuSnehDatabase
import com.mindmatrix.shishusneh.databinding.ActivityAddMeasurementBinding
import com.mindmatrix.shishusneh.util.UnitConverter
import com.mindmatrix.shishusneh.util.UnitSystem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * AddMeasurementActivity — Record a New Growth Measurement
 *
 * Form screen where parents enter:
 * - Measurement date (via Material DatePicker)
 * - Weight (in current unit system)
 * - Height (in current unit system)
 * - Head circumference (optional)
 * - Notes (optional)
 *
 * FLOW:
 * ┌────────────────────────────────┐
 * │ User taps FAB on Dashboard    │
 * │   ↓                           │
 * │ AddMeasurementActivity opens  │
 * │   ↓                           │
 * │ User fills form, taps Save    │
 * │   ↓                           │
 * │ ViewModel validates & saves   │
 * │   ↓                           │
 * │ Success → finish() → back     │
 * └────────────────────────────────┘
 */
class AddMeasurementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMeasurementBinding
    private val viewModel: GrowthViewModel by viewModels()

    private var selectedDate: Long = 0L
    private var profileId: Int = 1
    private var isMetric: Boolean = true
    private var editingRecordId: Int = 0

    // =====================================================
    // LIFECYCLE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get profile ID and unit system from intent
        profileId = intent.getIntExtra("PROFILE_ID", 1)
        isMetric = intent.getBooleanExtra("IS_METRIC", true)

        // Set up UI based on unit system
        setupUnitLabels()
        setupDatePicker()
        setupSaveButton()
        setupBackButton()
        observeViewModel()
    }

    // =====================================================
    // SETUP
    // =====================================================

    /**
     * Update input hints and suffixes based on the current unit system.
     */
    private fun setupUnitLabels() {
        val weightUnit = UnitConverter.weightUnit(isMetric)
        val heightUnit = UnitConverter.heightUnit(isMetric)

        binding.layoutWeight.suffixText = weightUnit
        binding.editTextWeight.hint = getString(R.string.hint_weight, weightUnit)

        binding.layoutHeight.suffixText = heightUnit
        binding.editTextHeight.hint = getString(R.string.hint_height, heightUnit)

        binding.layoutHeadCirc.suffixText = heightUnit
        binding.editTextHeadCirc.hint = getString(R.string.hint_head_circumference, heightUnit)
    }

    /**
     * Set up the date picker — defaults to today.
     * Only past dates are allowed.
     */
    private fun setupDatePicker() {
        // Default to today
        selectedDate = MaterialDatePicker.todayInUtcMilliseconds()
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        binding.editTextDate.setText(formatter.format(Date(selectedDate)))

        binding.editTextDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_picker_measurement_title))
                .setCalendarConstraints(constraints)
                .setSelection(selectedDate)
                .build()

            datePicker.addOnPositiveButtonClickListener { dateInMillis ->
                selectedDate = dateInMillis
                val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
                binding.editTextDate.setText(dateFormatter.format(Date(dateInMillis)))
            }

            datePicker.show(supportFragmentManager, "MEASUREMENT_DATE_PICKER")
        }
    }

    /**
     * Save button reads all inputs and passes to ViewModel.
     */
    private fun setupSaveButton() {
        binding.buttonSaveMeasurement.setOnClickListener {
            val weightStr = binding.editTextWeight.text.toString()
            val heightStr = binding.editTextHeight.text.toString()
            val headCircStr = binding.editTextHeadCirc.text.toString()
            val notes = binding.editTextNotes.text.toString()

            // Parse weight
            val weight = weightStr.toFloatOrNull()
            if (weight == null || weight <= 0f) {
                binding.layoutWeight.error = getString(R.string.msg_invalid_weight)
                return@setOnClickListener
            } else {
                binding.layoutWeight.error = null
            }

            // Parse height
            val height = heightStr.toFloatOrNull()
            if (height == null || height <= 0f) {
                binding.layoutHeight.error = getString(R.string.msg_invalid_height)
                return@setOnClickListener
            } else {
                binding.layoutHeight.error = null
            }

            // Parse head circumference (optional)
            val headCirc = if (headCircStr.isNotBlank()) headCircStr.toFloatOrNull() else null

            // Save via ViewModel
            viewModel.saveMeasurement(
                profileId = profileId,
                date = selectedDate,
                weightInput = weight,
                heightInput = height,
                headCircInput = headCirc,
                notes = notes,
                isMetric = isMetric,
                recordId = editingRecordId
            )
        }
    }

    private fun populateForEditing(record: com.mindmatrix.shishusneh.data.local.GrowthRecord) {
        editingRecordId = record.id
        selectedDate = record.date
        
        val dateFormatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        dateFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
        binding.editTextDate.setText(dateFormatter.format(java.util.Date(record.date)))

        val displayWeight = if (isMetric) record.weightKg else UnitConverter.kgToLbs(record.weightKg)
        val displayHeight = if (isMetric) record.heightCm else UnitConverter.cmToInches(record.heightCm)
        
        binding.editTextWeight.setText(String.format(java.util.Locale.US, "%.1f", displayWeight))
        binding.editTextHeight.setText(String.format(java.util.Locale.US, "%.1f", displayHeight))
        
        record.headCircumferenceCm?.let {
            val displayHead = if (isMetric) it else UnitConverter.cmToInches(it)
            binding.editTextHeadCirc.setText(String.format(java.util.Locale.US, "%.1f", displayHead))
        } ?: binding.editTextHeadCirc.setText("")
        
        binding.editTextNotes.setText(record.notes ?: "")
        binding.buttonSaveMeasurement.text = "Update Measurement"
    }

    /**
     * Back button → close this screen.
     */
    private fun setupBackButton() {
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }

    /**
     * Observe ViewModel for save success / errors.
     */
    private fun observeViewModel() {
        viewModel.measurementSaved.observe(this) { saved ->
            if (saved) {
                Toast.makeText(this, getString(R.string.msg_measurement_saved), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        val adapter = MeasurementAdapter(isMetric, { record ->
            populateForEditing(record)
        }, { /* No delete from this view */ })
        
        binding.recyclerViewRecent.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.recyclerViewRecent.adapter = adapter

        viewModel.getRecentRecords(profileId, 3).observe(this) { records ->
            if (records.isNotEmpty()) {
                binding.layoutRecentMeasurements.visibility = android.view.View.VISIBLE
                adapter.submitList(records)
            } else {
                binding.layoutRecentMeasurements.visibility = android.view.View.GONE
            }
        }
    }
}
