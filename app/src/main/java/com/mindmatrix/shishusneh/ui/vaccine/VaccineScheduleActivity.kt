package com.mindmatrix.shishusneh.ui.vaccine

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.model.VaccineWithStatus
import com.mindmatrix.shishusneh.databinding.ActivityVaccineScheduleBinding
import kotlin.math.roundToInt

/**
 * VaccineScheduleActivity — Displays the full vaccination schedule
 *
 * Shows the India National Immunization Schedule grouped by age.
 * Includes a progress bar at the top showing how many vaccines are done.
 * Tapping a vaccine opens a bottom sheet to mark it as given.
 */
class VaccineScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVaccineScheduleBinding
    private val viewModel: VaccineScheduleViewModel by viewModels()
    private lateinit var adapter: VaccineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityVaccineScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        // Create adapter and handle row clicks
        adapter = VaccineAdapter { vaccineWithStatus ->
            // Open bottom sheet when a vaccine is tapped
            showVaccineDetailBottomSheet(vaccineWithStatus)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        // Observe schedule changes
        viewModel.vaccineSchedule.observe(this) { schedule ->
            if (schedule != null) {
                adapter.submitVaccinesList(schedule)
            }
        }

        // Observe progress counts
        viewModel.totalCount.observe(this) { total ->
            binding.progressBar.max = total
            updateProgressText()
        }

        viewModel.givenCount.observe(this) { given ->
            binding.progressBar.progress = given
            updateProgressText()
        }
    }

    private fun updateProgressText() {
        val total = viewModel.totalCount.value ?: 0
        val given = viewModel.givenCount.value ?: 0

        if (total > 0) {
            if (given == total) {
                binding.textProgress.text = getString(R.string.vaccine_progress_complete)
            } else {
                binding.textProgress.text = getString(R.string.vaccine_progress_text, given, total)
            }

            val percent = ((given.toFloat() / total.toFloat()) * 100).roundToInt()
            binding.textPercent.text = "$percent%"
        }
    }

    private fun showVaccineDetailBottomSheet(vaccineWithStatus: VaccineWithStatus) {
        val bottomSheet = VaccineDetailBottomSheet.newInstance(vaccineWithStatus)
        
        // Listen for results from the bottom sheet
        bottomSheet.setOnVaccineUpdatedListener(object : VaccineDetailBottomSheet.OnVaccineUpdatedListener {
            override fun onMarkAsGiven(vaccineId: Int, date: Long, notes: String) {
                viewModel.markAsGiven(vaccineId, date, notes)
                Toast.makeText(
                    this@VaccineScheduleActivity,
                    getString(R.string.msg_vaccine_marked_given, vaccineWithStatus.vaccine.name),
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onUnmarkAsGiven(vaccineId: Int) {
                viewModel.unmarkAsGiven(vaccineId)
                Toast.makeText(
                    this@VaccineScheduleActivity,
                    getString(R.string.msg_vaccine_unmarked, vaccineWithStatus.vaccine.name),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        
        bottomSheet.show(supportFragmentManager, VaccineDetailBottomSheet.TAG)
    }
}
