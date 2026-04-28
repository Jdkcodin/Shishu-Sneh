package com.mindmatrix.shishusneh.ui.growth

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.databinding.ActivityGrowthChartBinding
import com.mindmatrix.shishusneh.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GrowthChartActivity — Full-Screen Growth Visualization
 *
 * Displays weight and/or height trends using MPAndroidChart LineChart.
 *
 * Features:
 * - Toggle between Weight-only, Height-only, or Both
 * - Smooth cubic bezier curves with fill-under gradient
 * - Tappable data points with custom marker tooltip
 * - Responsive to unit system (metric/imperial)
 * - Empty state for no-data scenario
 */
class GrowthChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGrowthChartBinding
    private val viewModel: GrowthViewModel by viewModels()

    private var profileId: Int = 1
    private var isMetric: Boolean = true
    private var currentRecords: List<GrowthRecord> = emptyList()

    // Chart display modes
    private companion object {
        const val MODE_WEIGHT = 0
        const val MODE_HEIGHT = 1
        const val MODE_BOTH = 2
    }
    private var chartMode = MODE_BOTH

    // =====================================================
    // LIFECYCLE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGrowthChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileId = intent.getIntExtra("PROFILE_ID", 1)
        isMetric = intent.getBooleanExtra("IS_METRIC", true)

        setupBackButton()
        setupToggleGroup()
        observeRecords()
    }

    // =====================================================
    // SETUP
    // =====================================================

    private fun setupBackButton() {
        binding.buttonBack.setOnClickListener { finish() }
    }

    private fun setupToggleGroup() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                chartMode = when (checkedId) {
                    R.id.buttonWeight -> MODE_WEIGHT
                    R.id.buttonHeight -> MODE_HEIGHT
                    R.id.buttonBoth -> MODE_BOTH
                    else -> MODE_BOTH
                }
                updateChart()
            }
        }
    }

    private fun observeRecords() {
        viewModel.getAllRecords(profileId).observe(this) { records ->
            // Records come DESC from DAO; reverse for chart (oldest first)
            currentRecords = records.sortedBy { it.date }

            if (currentRecords.isEmpty()) {
                binding.lineChart.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.lineChart.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                updateChart()
            }
        }
    }

    // =====================================================
    // CHART RENDERING
    // =====================================================

    private fun updateChart() {
        if (currentRecords.isEmpty()) return

        val chart = binding.lineChart
        val dataSets = mutableListOf<LineDataSet>()
        val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())

        // ── Weight Dataset ──
        if (chartMode == MODE_WEIGHT || chartMode == MODE_BOTH) {
            val weightEntries = currentRecords.mapIndexed { index, record ->
                val value = if (isMetric) record.weightKg else UnitConverter.kgToLbs(record.weightKg)
                Entry(index.toFloat(), value)
            }

            val weightLabel = getString(
                R.string.chart_weight_label,
                UnitConverter.weightUnit(isMetric)
            )

            val weightDataSet = LineDataSet(weightEntries, weightLabel).apply {
                color = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_weight_line)
                setCircleColor(ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_weight_line))
                lineWidth = 2.5f
                circleRadius = 5f
                circleHoleRadius = 2.5f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_weight_line)
                fillAlpha = 25
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawHighlightIndicators(true)
                highLightColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_weight_line)
            }
            dataSets.add(weightDataSet)
        }

        // ── Height Dataset ──
        if (chartMode == MODE_HEIGHT || chartMode == MODE_BOTH) {
            val heightEntries = currentRecords.mapIndexed { index, record ->
                val value = if (isMetric) record.heightCm else UnitConverter.cmToInches(record.heightCm)
                Entry(index.toFloat(), value)
            }

            val heightLabel = getString(
                R.string.chart_height_label,
                UnitConverter.heightUnit(isMetric)
            )

            val heightDataSet = LineDataSet(heightEntries, heightLabel).apply {
                color = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_height_line)
                setCircleColor(ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_height_line))
                lineWidth = 2.5f
                circleRadius = 5f
                circleHoleRadius = 2.5f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_height_line)
                fillAlpha = 25
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawHighlightIndicators(true)
                highLightColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_height_line)
            }
            dataSets.add(heightDataSet)
        }

        // ── Configure Chart ──
        chart.data = LineData(dataSets.toList())

        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // Custom marker
            marker = ChartMarkerView(this@GrowthChartActivity, currentRecords, isMetric)

            // X-Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_label)
                textSize = 11f
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val idx = value.toInt()
                        return if (idx in currentRecords.indices) {
                            dateFormatter.format(Date(currentRecords[idx].date))
                        } else ""
                    }
                }
            }

            // Y-Axis (left)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_grid)
                textColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.chart_label)
                textSize = 11f
            }

            // Hide right Y-axis
            axisRight.isEnabled = false

            // Legend
            legend.apply {
                textSize = 12f
                textColor = ContextCompat.getColor(this@GrowthChartActivity, R.color.text_secondary)
            }

            // Animate
            animateX(1000)
            invalidate()
        }
    }
}
