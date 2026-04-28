package com.mindmatrix.shishusneh.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.databinding.ActivityEditProfileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * EditProfileActivity — Edit Baby's Profile (Phase 4)
 *
 * Pre-fills the form with existing profile data and allows
 * the parent to update baby name, DOB, mother's name, and gender.
 *
 * FLOW:
 * ┌──────────────────────────────────────────────┐
 * │ Receives PROFILE_ID via Intent extras         │
 * │   ↓                                           │
 * │ ViewModel loads profile → LiveData updates UI │
 * │   ↓                                           │
 * │ User edits fields → taps "Save Changes"       │
 * │   ↓                                           │
 * │ ViewModel validates → updates Room DB          │
 * │   ↓                                           │
 * │ Success → finish() with RESULT_OK             │
 * └──────────────────────────────────────────────┘
 */
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    private var profileId: Int = 0
    private var selectedDateOfBirth: Long = 0L
    private var profileCreatedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileId = intent.getIntExtra("PROFILE_ID", 0)

        setupToolbar()
        setupDatePicker()
        setupButtons()
        observeViewModel()

        // Load the profile data
        viewModel.loadProfile(profileId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDatePicker() {
        binding.editTextDob.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_picker_title))
                .setCalendarConstraints(constraints)
                .setSelection(
                    if (selectedDateOfBirth > 0) selectedDateOfBirth
                    else MaterialDatePicker.todayInUtcMilliseconds()
                )
                .build()

            datePicker.addOnPositiveButtonClickListener { dateInMillis ->
                selectedDateOfBirth = dateInMillis
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                binding.editTextDob.setText(formatter.format(Date(dateInMillis)))
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupButtons() {
        binding.buttonSaveChanges.setOnClickListener {
            val babyName = binding.editTextBabyName.text.toString()
            val motherName = binding.editTextMotherName.text.toString()

            val gender = when (binding.radioGroupGender.checkedRadioButtonId) {
                R.id.radioMale -> "Male"
                R.id.radioFemale -> "Female"
                R.id.radioOther -> "Other"
                else -> ""
            }

            viewModel.updateProfile(
                profileId = profileId,
                babyName = babyName,
                dateOfBirth = selectedDateOfBirth,
                motherName = motherName,
                gender = gender,
                createdAt = profileCreatedAt
            )
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.deleteProfileAndData(this) {
                    Toast.makeText(this, R.string.msg_profile_deleted, Toast.LENGTH_SHORT).show()
                    
                    val prefs = com.mindmatrix.shishusneh.util.ProfilePreferences(this)
                    if (prefs.activeProfileId == 0) {
                        val intent = android.content.Intent(this, com.mindmatrix.shishusneh.ui.onboarding.OnboardingActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    finish()
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun observeViewModel() {
        // ── Pre-fill form from loaded profile ──
        viewModel.currentProfile.observe(this) { profile ->
            profile?.let {
                profileCreatedAt = it.createdAt
                selectedDateOfBirth = it.dateOfBirth

                binding.editTextBabyName.setText(it.babyName)
                binding.editTextMotherName.setText(it.motherName)

                // Format and display DOB
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                binding.editTextDob.setText(formatter.format(Date(it.dateOfBirth)))

                // Set gender radio button
                when (it.gender) {
                    "Male" -> binding.radioMale.isChecked = true
                    "Female" -> binding.radioFemale.isChecked = true
                    "Other" -> binding.radioOther.isChecked = true
                }
            }
        }

        // ── Profile updated successfully ──
        viewModel.profileUpdated.observe(this) { updated ->
            if (updated) {
                Toast.makeText(this, getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }

        // ── Error messages ──
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
}
