package com.mindmatrix.shishusneh.ui.growth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MeasurementAdapter — RecyclerView Adapter for Measurement History
 *
 * Uses ListAdapter + DiffUtil for efficient list updates.
 * DiffUtil compares old and new lists and only updates changed items,
 * rather than refreshing the entire list.
 *
 * Each item shows:
 * ┌─────────────────────────────────────┐
 * │ 📅 15 Apr 2025                      │
 * │ ⚖️ 5.2 kg    📏 58 cm    🧠 38 cm │
 * │ 📝 "After morning feed"            │
 * └─────────────────────────────────────┘
 *
 * @param isMetric Whether to display in metric (kg/cm) or imperial (lbs/in)
 */
class MeasurementAdapter(
    private var isMetric: Boolean = true,
    private val onEdit: ((GrowthRecord) -> Unit)? = null,
    private val onDelete: ((GrowthRecord) -> Unit)? = null
) : ListAdapter<GrowthRecord, MeasurementAdapter.MeasurementViewHolder>(DiffCallback) {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    /**
     * DiffUtil callback — tells RecyclerView how to diff two lists.
     * - areItemsTheSame: Compare by ID (same database row?)
     * - areContentsTheSame: Compare all fields (same data?)
     */
    companion object DiffCallback : DiffUtil.ItemCallback<GrowthRecord>() {
        override fun areItemsTheSame(old: GrowthRecord, new: GrowthRecord): Boolean {
            return old.id == new.id
        }

        override fun areContentsTheSame(old: GrowthRecord, new: GrowthRecord): Boolean {
            return old == new
        }
    }

    /**
     * Inflate the item layout and create a ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasurementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_measurement, parent, false)
        return MeasurementViewHolder(view)
    }

    /**
     * Bind data to the ViewHolder for a given position.
     */
    override fun onBindViewHolder(holder: MeasurementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Update the unit system and refresh the list.
     */
    fun updateUnitSystem(metric: Boolean) {
        isMetric = metric
        notifyDataSetChanged()
    }

    /**
     * ViewHolder — Holds references to the views in item_measurement.xml
     */
    inner class MeasurementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val textWeight: TextView = itemView.findViewById(R.id.textWeight)
        private val textHeight: TextView = itemView.findViewById(R.id.textHeight)
        private val layoutHeadCirc: LinearLayout = itemView.findViewById(R.id.layoutHeadCirc)
        private val textHeadCirc: TextView = itemView.findViewById(R.id.textHeadCirc)
        private val textNotes: TextView = itemView.findViewById(R.id.textNotes)

        fun bind(record: GrowthRecord) {
            // Date
            textDate.text = dateFormatter.format(Date(record.date))

            // Weight & Height
            textWeight.text = UnitConverter.formatWeight(record.weightKg, isMetric)
            textHeight.text = UnitConverter.formatHeight(record.heightCm, isMetric)

            // Head circumference (optional)
            if (record.headCircumferenceCm != null) {
                layoutHeadCirc.visibility = View.VISIBLE
                textHeadCirc.text = UnitConverter.formatHeadCircumference(
                    record.headCircumferenceCm, isMetric
                )
            } else {
                layoutHeadCirc.visibility = View.GONE
            }

            // Notes (only show if not empty)
            if (record.notes.isNotBlank()) {
                textNotes.visibility = View.VISIBLE
                textNotes.text = record.notes
            } else {
                textNotes.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onEdit?.invoke(record)
            }
            
            itemView.setOnLongClickListener {
                onDelete?.invoke(record)
                true
            }
        }
    }
}
