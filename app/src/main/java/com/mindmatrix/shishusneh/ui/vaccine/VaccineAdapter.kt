package com.mindmatrix.shishusneh.ui.vaccine

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.model.VaccineStatus
import com.mindmatrix.shishusneh.data.model.VaccineWithStatus
import com.mindmatrix.shishusneh.databinding.ItemVaccineBinding
import com.mindmatrix.shishusneh.databinding.ItemVaccineSectionHeaderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VaccineAdapter — Sectioned RecyclerView Adapter
 *
 * Supports two view types:
 * 1. SECTION_HEADER (e.g., "6 WEEKS")
 * 2. VACCINE_ITEM (e.g., OPV-1, Pentavalent-1)
 *
 * It flattens the grouped data into a single 1D list for the RecyclerView.
 */
class VaccineAdapter(
    private val onItemClick: (VaccineWithStatus) -> Unit
) : ListAdapter<VaccineAdapter.ListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        private val DiffCallback = object : DiffUtil.ItemCallback<ListItem>() {
            override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                if (oldItem is ListItem.Header && newItem is ListItem.Header) {
                    return oldItem.ageLabel == newItem.ageLabel
                }
                if (oldItem is ListItem.VaccineItem && newItem is ListItem.VaccineItem) {
                    return oldItem.vaccineWithStatus.vaccine.id == newItem.vaccineWithStatus.vaccine.id
                }
                return false
            }

            override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    /**
     * Submit the raw list of vaccines. This function handles the grouping
     * and insertion of section headers.
     */
    fun submitVaccinesList(vaccines: List<VaccineWithStatus>) {
        val flattenedList = mutableListOf<ListItem>()
        var currentLabel = ""

        for (item in vaccines) {
            val label = item.vaccine.ageLabel
            // If the label changes, insert a header first
            if (label != currentLabel) {
                flattenedList.add(ListItem.Header(label))
                currentLabel = label
            }
            // Then add the vaccine item
            flattenedList.add(ListItem.VaccineItem(item))
        }

        submitList(flattenedList)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.VaccineItem -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemVaccineSectionHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemVaccineBinding.inflate(inflater, parent, false)
                VaccineViewHolder(binding, onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.VaccineItem -> (holder as VaccineViewHolder).bind(item)
        }
    }

    // =====================================================
    // VIEW HOLDERS
    // =====================================================

    class HeaderViewHolder(private val binding: ItemVaccineSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: ListItem.Header) {
            binding.textAgeLabel.text = header.ageLabel
        }
    }

    class VaccineViewHolder(
        private val binding: ItemVaccineBinding,
        private val onItemClick: (VaccineWithStatus) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(item: ListItem.VaccineItem) {
            val data = item.vaccineWithStatus
            val context = binding.root.context

            // Root click
            binding.root.setOnClickListener { onItemClick(data) }

            // Title
            binding.textVaccineName.text = data.vaccine.name

            // Status, Colors, Icon, Date string
            val colorRes: Int
            val bgRes: Int
            val iconRes: Int
            val badgeTextRes: Int
            val dateText: String

            when (data.status) {
                VaccineStatus.GIVEN -> {
                    colorRes = R.color.vaccine_given
                    bgRes = R.color.vaccine_given_bg
                    iconRes = R.drawable.ic_check_circle
                    badgeTextRes = R.string.status_given
                    
                    val givenStr = data.dateGiven?.let { dateFormatter.format(Date(it)) } ?: ""
                    dateText = context.getString(R.string.vaccine_given_on, givenStr)
                }
                VaccineStatus.DUE -> {
                    colorRes = R.color.vaccine_due
                    bgRes = R.color.vaccine_due_bg
                    iconRes = R.drawable.ic_warning
                    badgeTextRes = R.string.status_due
                    
                    val dueStr = dateFormatter.format(Date(data.dueDate))
                    dateText = context.getString(R.string.vaccine_due_on, dueStr)
                }
                VaccineStatus.OVERDUE -> {
                    colorRes = R.color.vaccine_overdue
                    bgRes = R.color.vaccine_overdue_bg
                    iconRes = R.drawable.ic_error
                    badgeTextRes = R.string.status_overdue
                    
                    val dueStr = dateFormatter.format(Date(data.dueDate))
                    val daysOverdue = Math.abs(data.daysUntilDue ?: 0)
                    dateText = context.getString(R.string.vaccine_overdue_days, daysOverdue) + " • Due $dueStr"
                }
                VaccineStatus.UPCOMING -> {
                    colorRes = R.color.vaccine_upcoming
                    bgRes = R.color.vaccine_upcoming_bg
                    iconRes = R.drawable.ic_schedule
                    badgeTextRes = R.string.status_upcoming
                    
                    val dueStr = dateFormatter.format(Date(data.dueDate))
                    dateText = context.getString(R.string.vaccine_due_on, dueStr)
                }
            }

            // Apply colors
            val color = ContextCompat.getColor(context, colorRes)
            val bgColor = ContextCompat.getColor(context, bgRes)

            binding.iconStatus.setImageResource(iconRes)
            binding.iconStatus.imageTintList = ColorStateList.valueOf(color)

            binding.badgeStatus.text = context.getString(badgeTextRes)
            binding.badgeStatus.setTextColor(color)

            // Create curved background for badge
            val badgeBg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f * context.resources.displayMetrics.density // 16dp radius
            }
            binding.badgeStatus.background = badgeBg

            binding.textDueDate.text = dateText
            
            // Dim upcoming vaccines slightly
            binding.root.alpha = if (data.status == VaccineStatus.UPCOMING) 0.7f else 1.0f
        }
    }

    // =====================================================
    // SEALED CLASS FOR LIST ITEMS
    // =====================================================

    /**
     * Represents the two possible row types in our RecyclerView.
     */
    sealed class ListItem {
        data class Header(val ageLabel: String) : ListItem()
        data class VaccineItem(val vaccineWithStatus: VaccineWithStatus) : ListItem()
    }
}
