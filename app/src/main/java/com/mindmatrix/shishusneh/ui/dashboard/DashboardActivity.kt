package com.mindmatrix.shishusneh.ui.dashboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.data.model.BMIResult
import com.mindmatrix.shishusneh.data.model.GrowthSummary
import com.mindmatrix.shishusneh.databinding.ActivityDashboardBinding
import com.mindmatrix.shishusneh.ui.growth.AddMeasurementActivity
import com.mindmatrix.shishusneh.ui.growth.GrowthChartActivity
import com.mindmatrix.shishusneh.ui.growth.MeasurementHistoryActivity
import com.mindmatrix.shishusneh.ui.vaccine.VaccineScheduleActivity
import com.mindmatrix.shishusneh.util.CsvExporter
import com.mindmatrix.shishusneh.util.UnitConverter
import com.mindmatrix.shishusneh.util.UnitSystem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * DashboardActivity — Main Screen (Phase 2)
 *
 * Replaces the Phase 1 placeholder MainActivity.
 *
 * Shows:
 * - Baby profile card (name, age)
 * - Latest measurement with BMI
 * - Growth summary statistics
 * - Mini growth chart
 * - Action buttons (History, Export CSV)
 * - FAB to add new measurement
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    private var profileId: Int = 1
    private var babyName: String = ""
    private var isMetric: Boolean = true

    // Launcher for AddMeasurementActivity — refreshes dashboard on return
    private val addMeasurementLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Reload dashboard data when a measurement is added
            viewModel.loadDashboardData(profileId)
        }
    }

    // Launcher for History — refreshes dashboard on return (in case records were deleted)
    private val historyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.loadDashboardData(profileId)
    }

    // =====================================================
    // LIFECYCLE
    // =====================================================

    private val backupPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.restoreBackup(this, it) {
                Toast.makeText(this, getString(R.string.msg_backup_success), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data every time dashboard becomes visible
        if (profileId > 0) {
            viewModel.loadDashboardData(profileId)
        }
    }

    // =====================================================
    // SETUP
    // =====================================================

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_toggle_units -> {
                    viewModel.toggleUnitSystem()
                    true
                }
                R.id.menu_profiles -> {
                    val intent = Intent(this, com.mindmatrix.shishusneh.ui.profile.ManageProfilesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_dark_mode -> {
                    viewModel.toggleDarkMode()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        // Profile Card opens Manage Profiles Activity
        binding.cardProfile.setOnClickListener {
            val intent = Intent(this, com.mindmatrix.shishusneh.ui.profile.ManageProfilesActivity::class.java)
            startActivity(intent)
        }

        // FAB → Add Measurement
        binding.fabAddMeasurement.setOnClickListener {
            val intent = Intent(this, AddMeasurementActivity::class.java).apply {
                putExtra("PROFILE_ID", profileId)
                putExtra("IS_METRIC", isMetric)
            }
            addMeasurementLauncher.launch(intent)
        }

        // View Full Chart
        binding.buttonViewFullChart.setOnClickListener {
            val intent = Intent(this, GrowthChartActivity::class.java).apply {
                putExtra("PROFILE_ID", profileId)
                putExtra("IS_METRIC", isMetric)
            }
            startActivity(intent)
        }

        // View History
        binding.cardViewHistory.setOnClickListener {
            val intent = Intent(this, MeasurementHistoryActivity::class.java).apply {
                putExtra("PROFILE_ID", profileId)
                putExtra("IS_METRIC", isMetric)
                putExtra("BABY_NAME", babyName)
            }
            historyLauncher.launch(intent)
        }

        // Vaccine Schedule (via Card button)
        binding.buttonViewSchedule.setOnClickListener {
            val intent = Intent(this, VaccineScheduleActivity::class.java)
            startActivity(intent)
        }

        // Vaccine Schedule (via bottom action button)
        binding.cardVaccines.setOnClickListener {
            val intent = Intent(this, VaccineScheduleActivity::class.java)
            startActivity(intent)
        }

        // Export Data / Backup
        binding.cardExportCsv.setOnClickListener {
            showExportOptionsDialog()
        }
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf(
            getString(R.string.export_option_csv),
            getString(R.string.export_option_json),
            getString(R.string.import_option_json)
        )
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_export_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportCsv()
                    1 -> viewModel.exportFullBackup(this)
                    2 -> backupPickerLauncher.launch("application/json")
                }
            }
            .show()
    }

    // =====================================================
    // OBSERVE VIEWMODEL
    // =====================================================

    private fun observeViewModel() {
        // ── Baby Profile ──
        viewModel.babyProfile.observe(this) { profile ->
            profile?.let {
                profileId = it.id
                babyName = it.babyName

                binding.textBabyName.text = getString(R.string.dashboard_greeting, it.babyName)

                // Calculate and display age
                val ageMs = System.currentTimeMillis() - it.dateOfBirth
                val ageDays = TimeUnit.MILLISECONDS.toDays(ageMs).toInt()
                val ageMonths = ageDays / 30

                val ageText = if (ageMonths > 0) {
                    getString(R.string.dashboard_age_format, ageMonths)
                } else {
                    getString(R.string.dashboard_age_days_format, ageDays)
                }

                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val dobStr = formatter.format(Date(it.dateOfBirth))
                binding.textBabyAge.text = "$ageText • Born $dobStr"

                // Load dashboard data
                viewModel.loadDashboardData(profileId)

                // Observe recent records for mini chart
                viewModel.getRecentRecords(profileId).observe(this) { records ->
                    updateMiniChart(records)
                }
            }
        }

        // ── Unit System ──
        viewModel.unitSystem.observe(this) { system ->
            isMetric = system.isMetric

            // Update menu title
            val menuTitle = if (isMetric) {
                getString(R.string.menu_imperial)
            } else {
                getString(R.string.menu_metric)
            }
            binding.toolbar.menu.findItem(R.id.menu_toggle_units)?.title = menuTitle

            // Refresh display with new units
            viewModel.latestRecord.value?.let { updateMeasurementCard(it) }
            viewModel.growthSummary.value?.let { updateGrowthSummary(it) }
            viewModel.bmiResult.value?.let { updateBMI(it) }
        }

        // ── Latest Measurement ──
        viewModel.latestRecord.observe(this) { record ->
            if (record != null) {
                updateMeasurementCard(record)
                binding.layoutMeasurementValues.visibility = View.VISIBLE
                binding.textNoMeasurements.visibility = View.GONE
            } else {
                binding.layoutMeasurementValues.visibility = View.GONE
                binding.textNoMeasurements.visibility = View.VISIBLE
                binding.layoutBmi.visibility = View.GONE
            }
        }

        // ── BMI ──
        viewModel.bmiResult.observe(this) { bmi ->
            if (bmi != null) {
                updateBMI(bmi)
                binding.layoutBmi.visibility = View.VISIBLE
            } else {
                binding.layoutBmi.visibility = View.GONE
            }
        }

        // ── Growth Summary ──
        viewModel.growthSummary.observe(this) { summary ->
            updateGrowthSummary(summary)
        }

        // ── Upcoming Vaccines (Phase 3) ──
        viewModel.upcomingVaccines.observe(this) { vaccines ->
            if (vaccines != null) {
                updateUpcomingVaccines(vaccines)
            }
        }

        viewModel.overdueCount.observe(this) { count ->
            if (count > 0) {
                binding.badgeOverdue.visibility = View.VISIBLE
                binding.badgeOverdue.text = getString(R.string.dashboard_overdue_count, count)
                
                // Create curved background for badge
                val badgeBg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(ContextCompat.getColor(this@DashboardActivity, R.color.vaccine_overdue_bg))
                    cornerRadius = 16f * resources.displayMetrics.density
                }
                binding.badgeOverdue.background = badgeBg
            } else {
                binding.badgeOverdue.visibility = View.GONE
            }
        }
    }

    // =====================================================
    // UI UPDATES
    // =====================================================

    private fun updateMeasurementCard(record: GrowthRecord) {
        binding.textWeight.text = UnitConverter.formatWeight(record.weightKg, isMetric)
        binding.textHeight.text = UnitConverter.formatHeight(record.heightCm, isMetric)

        if (record.headCircumferenceCm != null) {
            binding.layoutHeadValue.visibility = View.VISIBLE
            binding.textHeadCirc.text = UnitConverter.formatHeadCircumference(
                record.headCircumferenceCm, isMetric
            )
        } else {
            binding.layoutHeadValue.visibility = View.GONE
        }

        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.textRecordedDate.text = getString(
            R.string.label_recorded_on, formatter.format(Date(record.date))
        )
    }

    private fun updateBMI(bmi: BMIResult) {
        binding.textBmiValue.text = String.format("%.1f", bmi.value)
        binding.textBmiValue.setTextColor(ContextCompat.getColor(this, bmi.colorResId))
        binding.textBmiCategory.text = bmi.category
        binding.textBmiCategory.setTextColor(ContextCompat.getColor(this, bmi.colorResId))

        // Remove the incorrect background drawable on BMI layout
        binding.layoutBmi.background = null
    }

    private fun updateGrowthSummary(summary: GrowthSummary) {
        if (summary.totalRecords < 2) {
            binding.cardGrowthSummary.visibility = View.GONE
            return
        }

        binding.cardGrowthSummary.visibility = View.VISIBLE

        // Weight change this month
        summary.weightChangeLastMonth?.let {
            binding.textWeightChangeMonth.visibility = View.VISIBLE
            val changeStr = UnitConverter.formatWeightChange(it, isMetric)
            binding.textWeightChangeMonth.text = getString(R.string.summary_weight_change_month, changeStr)
            val icon = if (it >= 0) R.drawable.ic_growth_up else R.drawable.ic_growth_down
            binding.textWeightChangeMonth.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        } ?: run {
            binding.textWeightChangeMonth.visibility = View.GONE
        }

        // Height change since birth
        summary.heightChangeSinceBirth?.let {
            binding.textHeightChangeBirth.visibility = View.VISIBLE
            val changeStr = UnitConverter.formatHeightChange(it, isMetric)
            binding.textHeightChangeBirth.text = getString(R.string.summary_height_since_birth, changeStr)
            binding.textHeightChangeBirth.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_growth_up, 0, 0, 0
            )
        } ?: run {
            binding.textHeightChangeBirth.visibility = View.GONE
        }

        // Weight change since birth
        summary.weightChangeSinceBirth?.let {
            binding.textWeightChangeBirth.visibility = View.VISIBLE
            val changeStr = UnitConverter.formatWeightChange(it, isMetric)
            binding.textWeightChangeBirth.text = getString(R.string.summary_weight_since_birth, changeStr)
            val icon = if (it >= 0) R.drawable.ic_growth_up else R.drawable.ic_growth_down
            binding.textWeightChangeBirth.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        } ?: run {
            binding.textWeightChangeBirth.visibility = View.GONE
        }

        // Total records
        val recordsText = getString(R.string.summary_total_records, summary.totalRecords)
        val lastRecordedText = summary.daysSinceLastRecord?.let { days ->
            if (days == 0) getString(R.string.summary_last_recorded_today)
            else getString(R.string.summary_last_recorded, days)
        } ?: ""
        binding.textTotalRecords.text = "$recordsText • $lastRecordedText"
    }

    // =====================================================
    // UPCOMING VACCINES (PHASE 3)
    // =====================================================

    private fun updateUpcomingVaccines(vaccines: List<com.mindmatrix.shishusneh.data.model.VaccineWithStatus>) {
        binding.cardUpcomingVaccines.visibility = View.VISIBLE
        binding.layoutVaccineList.removeAllViews()

        if (vaccines.isEmpty()) {
            binding.textNoUpcomingVaccines.visibility = View.VISIBLE
            binding.layoutVaccineList.visibility = View.GONE
            return
        }

        binding.textNoUpcomingVaccines.visibility = View.GONE
        binding.layoutVaccineList.visibility = View.VISIBLE

        val inflater = layoutInflater
        val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())

        for (item in vaccines) {
            // Inflate a simple layout for each vaccine row directly using code or a layout if we had one.
            // For dashboard, we'll create text views programmatically to keep it simple.
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            // Status Icon
            val icon = android.widget.ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (20 * resources.displayMetrics.density).toInt(),
                    (20 * resources.displayMetrics.density).toInt()
                ).apply { marginEnd = (12 * resources.displayMetrics.density).toInt() }
            }

            // Text
            val textName = android.widget.TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                text = item.vaccine.name
            }

            val textDue = android.widget.TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                textSize = 12f
            }

            val dueStr = dateFormatter.format(Date(item.dueDate))

            when (item.status) {
                com.mindmatrix.shishusneh.data.model.VaccineStatus.DUE -> {
                    icon.setImageResource(R.drawable.ic_warning)
                    icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.vaccine_due))
                    textDue.text = "Due $dueStr"
                    textDue.setTextColor(ContextCompat.getColor(this, R.color.vaccine_due))
                }
                com.mindmatrix.shishusneh.data.model.VaccineStatus.OVERDUE -> {
                    icon.setImageResource(R.drawable.ic_error)
                    icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.vaccine_overdue))
                    textDue.text = "Overdue"
                    textDue.setTextColor(ContextCompat.getColor(this, R.color.vaccine_overdue))
                }
                else -> {
                    icon.setImageResource(R.drawable.ic_schedule)
                    icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.vaccine_upcoming))
                    textDue.text = dueStr
                    textDue.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }

            row.addView(icon)
            row.addView(textName)
            row.addView(textDue)
            binding.layoutVaccineList.addView(row)
        }
    }

    // =====================================================
    // MINI CHART
    // =====================================================

    private fun updateMiniChart(records: List<GrowthRecord>) {
        if (records.size < 2) {
            binding.cardGrowthTrend.visibility = View.GONE
            return
        }

        binding.cardGrowthTrend.visibility = View.VISIBLE

        val chart = binding.miniChart

        // Create weight entries
        val weightEntries = records.mapIndexed { index, record ->
            val value = if (isMetric) record.weightKg else UnitConverter.kgToLbs(record.weightKg)
            Entry(index.toFloat(), value)
        }

        val weightDataSet = LineDataSet(weightEntries, "Weight").apply {
            color = ContextCompat.getColor(this@DashboardActivity, R.color.chart_weight_line)
            setCircleColor(ContextCompat.getColor(this@DashboardActivity, R.color.chart_weight_line))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@DashboardActivity, R.color.chart_weight_line)
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.data = LineData(weightDataSet)

        // Style the mini chart (minimal)
        val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(this@DashboardActivity, R.color.chart_label)
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val idx = value.toInt()
                        return if (idx >= 0 && idx < records.size) {
                            dateFormatter.format(Date(records[idx].date))
                        } else ""
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@DashboardActivity, R.color.chart_grid)
                textColor = ContextCompat.getColor(this@DashboardActivity, R.color.chart_label)
                textSize = 10f
            }

            axisRight.isEnabled = false

            animateX(800)
            invalidate()
        }
    }

    // =====================================================
    // CSV EXPORT
    // =====================================================

    private fun exportCsv() {
        lifecycleScope.launch {
            try {
                val records = viewModel.getRecordsForExport(profileId)
                if (records.isEmpty()) {
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.export_no_data),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                CsvExporter.exportAndShare(
                    context = this@DashboardActivity,
                    records = records,
                    babyName = babyName,
                    isMetric = isMetric
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@DashboardActivity,
                    "Export failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
