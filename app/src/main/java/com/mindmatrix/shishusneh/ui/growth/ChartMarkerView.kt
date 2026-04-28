package com.mindmatrix.shishusneh.ui.growth

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ChartMarkerView — Custom Tooltip for Growth Chart
 *
 * When a user taps a data point on the LineChart, this marker
 * pops up showing the date, weight, and height for that measurement.
 *
 * ┌──────────────────┐
 * │ 15 Apr 2025      │
 * │ Weight: 5.2 kg   │
 * │ Height: 58.0 cm  │
 * └──────────────────┘
 *
 * @param context Activity context for inflating the layout
 * @param records The list of GrowthRecord objects (in chart order)
 * @param isMetric Whether to show metric or imperial values
 */
class ChartMarkerView(
    context: Context,
    private val records: List<GrowthRecord>,
    private val isMetric: Boolean
) : MarkerView(context, R.layout.layout_chart_marker) {

    private val textDate: TextView = findViewById(R.id.textMarkerDate)
    private val textWeight: TextView = findViewById(R.id.textMarkerWeight)
    private val textHeight: TextView = findViewById(R.id.textMarkerHeight)

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    /**
     * Called every time the MarkerView is redrawn.
     * Updates the content with the data from the selected entry.
     */
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val index = e?.x?.toInt() ?: 0

        if (index in records.indices) {
            val record = records[index]

            textDate.text = dateFormatter.format(Date(record.date))
            textWeight.text = context.getString(
                R.string.chart_marker_weight,
                UnitConverter.formatWeight(record.weightKg, isMetric)
            )
            textHeight.text = context.getString(
                R.string.chart_marker_height,
                UnitConverter.formatHeight(record.heightCm, isMetric)
            )
        }

        super.refreshContent(e, highlight)
    }

    /**
     * Position the marker so it appears above the data point,
     * centered horizontally.
     */
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}
