package com.mindmatrix.shishusneh.ui.vaccine

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.Vaccine
import com.mindmatrix.shishusneh.data.model.VaccineStatus
import com.mindmatrix.shishusneh.data.model.VaccineWithStatus
import com.mindmatrix.shishusneh.databinding.FragmentVaccineDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VaccineDetailBottomSheet — Form to mark vaccines as given
 *
 * Shows detailed info about the vaccine and allows the user to
 * mark it as given with a date picker and optional notes.
 * If already given, it allows them to undo (unmark).
 */
class VaccineDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "VaccineDetailBottomSheet"
        private const val ARG_VACCINE_ID = "arg_vaccine_id"
        private const val ARG_VACCINE_NAME = "arg_vaccine_name"
        private const val ARG_VACCINE_FULL_NAME = "arg_vaccine_full_name"
        private const val ARG_VACCINE_DESC = "arg_vaccine_desc"
        private const val ARG_VACCINE_AGE = "arg_vaccine_age"
        private const val ARG_DUE_DATE = "arg_due_date"
        private const val ARG_DATE_GIVEN = "arg_date_given"
        private const val ARG_NOTES = "arg_notes"
        private const val ARG_STATUS = "arg_status"

        /**
         * Create a new instance passing all required data.
         * We pass data via bundle instead of parsing the object to avoid Parcelable boilerplate.
         */
        fun newInstance(data: VaccineWithStatus): VaccineDetailBottomSheet {
            val fragment = VaccineDetailBottomSheet()
            val args = Bundle().apply {
                putInt(ARG_VACCINE_ID, data.vaccine.id)
                putString(ARG_VACCINE_NAME, data.vaccine.name)
                putString(ARG_VACCINE_FULL_NAME, data.vaccine.fullName)
                putString(ARG_VACCINE_DESC, data.vaccine.description)
                putString(ARG_VACCINE_AGE, data.vaccine.ageLabel)
                putLong(ARG_DUE_DATE, data.dueDate)
                if (data.dateGiven != null) putLong(ARG_DATE_GIVEN, data.dateGiven)
                putString(ARG_NOTES, data.notes)
                putString(ARG_STATUS, data.status.name)
            }
            fragment.arguments = args
            return fragment
        }
    }

    // Listener interface to pass data back to Activity
    interface OnVaccineUpdatedListener {
        fun onMarkAsGiven(vaccineId: Int, date: Long, notes: String)
        fun onUnmarkAsGiven(vaccineId: Int)
    }

    private var _binding: FragmentVaccineDetailBinding? = null
    private val binding get() = _binding!!

    private var listener: OnVaccineUpdatedListener? = null
    private var selectedDateMs: Long = System.currentTimeMillis()
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun setOnVaccineUpdatedListener(listener: OnVaccineUpdatedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVaccineDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: return
        val vaccineId = args.getInt(ARG_VACCINE_ID)
        val status = VaccineStatus.valueOf(args.getString(ARG_STATUS) ?: VaccineStatus.UPCOMING.name)

        setupView(args, status)
        setupDatePicker()
        setupActionButton(vaccineId, status)
    }

    private fun setupView(args: Bundle, status: VaccineStatus) {
        // Set basic texts
        binding.textVaccineName.text = args.getString(ARG_VACCINE_NAME)
        binding.textFullName.text = args.getString(ARG_VACCINE_FULL_NAME)
        binding.textDescription.text = args.getString(ARG_VACCINE_DESC)
        binding.textAgeLabel.text = getString(R.string.vaccine_detail_recommended_age, args.getString(ARG_VACCINE_AGE))

        // Set due date
        val dueDateMs = args.getLong(ARG_DUE_DATE)
        binding.textDueDate.text = getString(R.string.vaccine_detail_due_date, dateFormatter.format(Date(dueDateMs)))

        // Setup status badge
        val colorRes: Int
        val bgRes: Int
        val badgeTextRes: Int

        when (status) {
            VaccineStatus.GIVEN -> {
                colorRes = R.color.vaccine_given
                bgRes = R.color.vaccine_given_bg
                badgeTextRes = R.string.status_given
            }
            VaccineStatus.DUE -> {
                colorRes = R.color.vaccine_due
                bgRes = R.color.vaccine_due_bg
                badgeTextRes = R.string.status_due
            }
            VaccineStatus.OVERDUE -> {
                colorRes = R.color.vaccine_overdue
                bgRes = R.color.vaccine_overdue_bg
                badgeTextRes = R.string.status_overdue
            }
            VaccineStatus.UPCOMING -> {
                colorRes = R.color.vaccine_upcoming
                bgRes = R.color.vaccine_upcoming_bg
                badgeTextRes = R.string.status_upcoming
            }
        }

        val context = requireContext()
        val color = ContextCompat.getColor(context, colorRes)
        val bgColor = ContextCompat.getColor(context, bgRes)

        binding.badgeStatus.text = getString(badgeTextRes)
        binding.badgeStatus.setTextColor(color)

        val badgeBg = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16f * context.resources.displayMetrics.density
        }
        binding.badgeStatus.background = badgeBg

        // UI state based on whether it's already given
        if (status == VaccineStatus.GIVEN) {
            // Already given — show the given date and notes (if any), hide inputs
            binding.layoutInputArea.visibility = View.GONE
            binding.textDateGiven.visibility = View.VISIBLE
            
            val givenMs = args.getLong(ARG_DATE_GIVEN)
            binding.textDateGiven.text = getString(R.string.vaccine_given_on, dateFormatter.format(Date(givenMs)))
            
            val notes = args.getString(ARG_NOTES)
            if (!notes.isNullOrBlank()) {
                binding.textDescription.text = "${args.getString(ARG_VACCINE_DESC)}\n\nNotes: $notes"
            }
        } else {
            // Not given — show inputs, hide given date
            binding.layoutInputArea.visibility = View.VISIBLE
            binding.textDateGiven.visibility = View.GONE
            
            // Default date to today
            binding.inputDate.setText(dateFormatter.format(Date(selectedDateMs)))
        }
    }

    private fun setupDatePicker() {
        binding.inputDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date given")
                .setSelection(selectedDateMs)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDateMs = selection
                binding.inputDate.setText(dateFormatter.format(Date(selectedDateMs)))
            }

            datePicker.show(childFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupActionButton(vaccineId: Int, status: VaccineStatus) {
        if (status == VaccineStatus.GIVEN) {
            binding.buttonAction.text = getString(R.string.button_mark_not_given)
            binding.buttonAction.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.vaccine_overdue))
            
            binding.buttonAction.setOnClickListener {
                listener?.onUnmarkAsGiven(vaccineId)
                dismiss()
            }
        } else {
            binding.buttonAction.text = getString(R.string.button_mark_given)
            binding.buttonAction.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_rose))
            
            binding.buttonAction.setOnClickListener {
                val notes = binding.inputNotes.text?.toString() ?: ""
                listener?.onMarkAsGiven(vaccineId, selectedDateMs, notes)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
