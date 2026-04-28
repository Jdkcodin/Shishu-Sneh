package com.mindmatrix.shishusneh.ui.growth

import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mindmatrix.shishusneh.R
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.databinding.ActivityMeasurementHistoryBinding
import com.mindmatrix.shishusneh.util.CsvExporter
import kotlinx.coroutines.launch

/**
 * MeasurementHistoryActivity — Browse All Past Measurements
 *
 * Features:
 * - RecyclerView with all measurements (newest first)
 * - Swipe-to-delete with undo Snackbar
 * - CSV export button in toolbar
 * - Empty state when no records exist
 */
class MeasurementHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeasurementHistoryBinding
    private val viewModel: GrowthViewModel by viewModels()
    private lateinit var adapter: MeasurementAdapter

    private var profileId: Int = 1
    private var isMetric: Boolean = true
    private var babyName: String = ""

    // =====================================================
    // LIFECYCLE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeasurementHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileId = intent.getIntExtra("PROFILE_ID", 1)
        isMetric = intent.getBooleanExtra("IS_METRIC", true)
        babyName = intent.getStringExtra("BABY_NAME") ?: ""

        setupRecyclerView()
        setupSwipeToDelete()
        setupClickListeners()
        observeRecords()
    }

    // =====================================================
    // SETUP
    // =====================================================

    private fun setupRecyclerView() {
        adapter = MeasurementAdapter(isMetric)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MeasurementHistoryActivity)
            adapter = this@MeasurementHistoryActivity.adapter
        }
    }

    /**
     * Set up swipe-to-delete with a red background and delete icon.
     * Shows an undo Snackbar after deletion.
     */
    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val record = adapter.currentList[position]

                // Delete the record
                viewModel.deleteMeasurement(record)

                // Show undo Snackbar
                Snackbar.make(
                    binding.recyclerView,
                    getString(R.string.msg_record_deleted),
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.action_undo)) {
                    // Re-insert the record
                    viewModel.undoDelete(record)
                }.setActionTextColor(
                    ContextCompat.getColor(this@MeasurementHistoryActivity, R.color.primary_rose)
                ).show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // Draw red background on swipe
                if (dX < 0) {
                    val itemView = viewHolder.itemView
                    val bgColor = ContextCompat.getColor(
                        this@MeasurementHistoryActivity, R.color.swipe_delete_bg
                    )
                    val background = android.graphics.drawable.ColorDrawable(bgColor)
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    // Draw delete icon
                    val deleteIcon = ContextCompat.getDrawable(
                        this@MeasurementHistoryActivity, R.drawable.ic_delete
                    )
                    deleteIcon?.let { icon ->
                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener { finish() }

        binding.buttonExport.setOnClickListener {
            exportCsv()
        }
    }

    // =====================================================
    // OBSERVE
    // =====================================================

    private fun observeRecords() {
        viewModel.getAllRecords(profileId).observe(this) { records ->
            adapter.submitList(records)

            if (records.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
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
                        this@MeasurementHistoryActivity,
                        getString(R.string.export_no_data),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                CsvExporter.exportAndShare(
                    context = this@MeasurementHistoryActivity,
                    records = records,
                    babyName = babyName,
                    isMetric = isMetric
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@MeasurementHistoryActivity,
                    "Export failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
