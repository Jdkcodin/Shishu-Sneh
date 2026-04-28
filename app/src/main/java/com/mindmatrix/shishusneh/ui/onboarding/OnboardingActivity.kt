package com.mindmatrix.shishusneh.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.mindmatrix.shishusneh.ui.dashboard.DashboardActivity
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.databinding.ActivityOnboardingBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * OnboardingActivity — The First Screen of the App
 *
 * This is where new mothers enter their baby's information.
 * It collects: Baby Name, Date of Birth, Mother's Name, and Gender.
 *
 * FLOW:
 * ┌─────────────────────────────────────────────────┐
 * │ App Opens                                       │
 * │   ↓                                             │
 * │ Has existing profile? ──YES──→ Go to Main Screen│
 * │   ↓ NO                                          │
 * │ Show Onboarding Form                            │
 * │   ↓                                             │
 * │ User fills form → clicks "Start Journey"        │
 * │   ↓                                             │
 * │ ViewModel validates → saves to Room DB          │
 * │   ↓                                             │
 * │ Success → Navigate to Main Screen               │
 * └─────────────────────────────────────────────────┘
 *
 * KEY CONCEPTS USED:
 * - ViewBinding:  Access views without findViewById()
 * - viewModels(): Get ViewModel instance (survives rotation)
 * - LiveData:     Observe data changes from ViewModel
 * - DatePicker:   Material Design date selection dialog
 */
class OnboardingActivity : AppCompatActivity() {

    // =====================================================
    // PROPERTIES
    // =====================================================

    /*
     * ViewBinding — Android generates a "binding" class for each layout XML.
     * Instead of: val button = findViewById<Button>(R.id.myButton)
     * We write:   binding.myButton
     *
     * 'lateinit' means: "I promise to set this before using it"
     * We set it in onCreate() below.
     */
    private lateinit var binding: ActivityOnboardingBinding

    /*
     * ViewModel — created using the 'by viewModels()' delegate.
     * This is the recommended way in modern Android.
     *
     * The system creates it once and returns the SAME instance
     * even after screen rotation.
     */
    private val viewModel: OnboardingViewModel by viewModels()

    /*
     * Store the selected Date of Birth as milliseconds.
     * Default 0L means "no date selected yet".
     */
    private var selectedDateOfBirth: Long = 0L

    // =====================================================
    // LIFECYCLE
    // =====================================================

    /**
     * onCreate() is called when the Activity is first created.
     * This is where we:
     * 1. Set up the layout
     * 2. Check if user already onboarded
     * 3. Set up button click listeners
     * 4. Start observing ViewModel data
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Inflate (create) the layout and set it as the screen content
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user already completed onboarding before
        checkExistingProfile()

        // Set up interactive elements
        setupDatePicker()
        setupSaveButton()

        // Start listening for ViewModel updates
        observeViewModel()
    }

    // =====================================================
    // PRIVATE METHODS
    // =====================================================

    /**
     * If a profile already exists, skip onboarding.
     *
     * This handles the case where:
     * - User completed onboarding yesterday
     * - Opens the app again today
     * - Should go directly to main screen (no need to re-enter info)
     */
    private fun checkExistingProfile() {
        // Phase 4: Allow adding a second baby without skipping onboarding
        if (intent.getBooleanExtra("IS_ADD_MODE", false)) {
            return
        }

        viewModel.checkExistingProfile { hasProfile ->
            if (hasProfile) {
                navigateToMain()
            }
        }
    }

    /**
     * Set up the Date of Birth picker.
     *
     * We use Material DatePicker — a modern, touch-friendly calendar dialog.
     * The DOB field is NOT editable via keyboard (only via DatePicker).
     * This ensures we always get a valid date!
     *
     * CONSTRAINTS:
     * - Only past dates allowed (baby must already be born)
     * - Default selection = today
     */
    private fun setupDatePicker() {
        binding.editTextDob.setOnClickListener {
            // Create constraints: only allow dates up to TODAY (not future dates)
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()

            // Build the DatePicker dialog
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Baby's Date of Birth")
                .setCalendarConstraints(constraints)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Default = today
                .build()

            // When user selects a date and taps "OK"
            datePicker.addOnPositiveButtonClickListener { dateInMillis ->
                // Save the raw milliseconds (for database storage)
                selectedDateOfBirth = dateInMillis

                // Format the date for display: "15 Jan 2025"
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                val formattedDate = formatter.format(Date(dateInMillis))

                // Show the formatted date in the text field
                binding.editTextDob.setText(formattedDate)
            }

            // Show the DatePicker dialog
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    /**
     * Set up the "Start Journey" button.
     *
     * When clicked:
     * 1. Read all form inputs
     * 2. Determine selected gender from RadioGroup
     * 3. Pass everything to ViewModel for validation & saving
     */
    private fun setupSaveButton() {
        binding.buttonStartJourney.setOnClickListener {
            // Read text inputs
            val babyName = binding.editTextBabyName.text.toString()
            val motherName = binding.editTextMotherName.text.toString()

            // Get selected gender from RadioGroup
            // RadioGroup returns the ID of the selected radio button
            val gender = when (binding.radioGroupGender.checkedRadioButtonId) {
                R.id.radioMale -> "Male"
                R.id.radioFemale -> "Female"
                R.id.radioOther -> "Other"
                else -> ""  // No selection
            }

            // Send data to ViewModel (it handles validation and saving)
            viewModel.saveBabyProfile(
                babyName = babyName,
                dateOfBirth = selectedDateOfBirth,
                motherName = motherName,
                gender = gender
            )
        }
    }

    /**
     * Observe ViewModel LiveData for UI updates.
     *
     * LiveData.observe() works like a subscription:
     * "Hey, whenever this data changes, call my lambda function"
     *
     * 'this' (the Activity) is the lifecycle owner — observation
     * automatically stops when the Activity is destroyed.
     */
    private fun observeViewModel() {
        // ── Observe: Profile Saved Successfully ──
        viewModel.profileSaved.observe(this) { saved ->
            if (saved) {
                // Show a friendly toast message
                Toast.makeText(
                    this,
                    "Welcome to Shishu Sneh! 🍼",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate to the main screen
                navigateToMain()
            }
        }

        // ── Observe: Error Messages ──
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                // Show the error message as a toast
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()

                // Clear the error so it doesn't re-show after rotation
                viewModel.clearError()
            }
        }
    }

    /**
     * Navigate to the main screen.
     *
     * Intent = a "message" to Android saying "open this Activity"
     * finish() = close the onboarding screen (can't go back to it)
     */
    private fun navigateToMain() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()  // Remove onboarding from the back stack
    }
}
