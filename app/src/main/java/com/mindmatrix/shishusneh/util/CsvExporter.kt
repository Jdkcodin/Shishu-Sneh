package com.mindmatrix.shishusneh.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.data.model.BMIResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CsvExporter — Export Growth Records as CSV
 *
 * Generates a CSV file from growth records and opens the Android Share Sheet
 * so parents can send the data to their pediatrician via email, WhatsApp, etc.
 *
 * CSV FORMAT:
 * ┌──────────────┬────────────┬─────────────┬──────────────────────┬───────┬────────┐
 * │ Date         │ Weight(kg) │ Height(cm)  │ Head Circumference(cm)│ BMI   │ Notes  │
 * ├──────────────┼────────────┼─────────────┼──────────────────────┼───────┼────────┤
 * │ 15 Apr 2025  │ 3.2        │ 49.5        │ 34.0                 │ 13.1  │        │
 * │ 15 May 2025  │ 4.1        │ 52.0        │ 36.5                 │ 15.1  │ fed    │
 * └──────────────┴────────────┴─────────────┴──────────────────────┴───────┴────────┘
 */
object CsvExporter {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    /**
     * Generate CSV content from growth records.
     *
     * @param records List of GrowthRecord objects
     * @param babyName Baby's name (included in header comment)
     * @param isMetric Whether to use metric (kg/cm) or imperial (lbs/in) units
     * @return CSV content as a String
     */
    fun generateCsvContent(
        records: List<GrowthRecord>,
        babyName: String,
        isMetric: Boolean
    ): String {
        val sb = StringBuilder()

        // Header comment with baby name and export date
        sb.appendLine("# Growth Records for $babyName")
        sb.appendLine("# Exported on ${dateFormatter.format(Date())}")
        sb.appendLine()

        // Column headers
        val weightUnit = if (isMetric) "kg" else "lbs"
        val heightUnit = if (isMetric) "cm" else "in"
        sb.appendLine("Date,Weight ($weightUnit),Height ($heightUnit),Head Circumference ($heightUnit),BMI,Notes")

        // Data rows
        for (record in records) {
            val date = dateFormatter.format(Date(record.date))
            val weight = if (isMetric) record.weightKg else UnitConverter.kgToLbs(record.weightKg)
            val height = if (isMetric) record.heightCm else UnitConverter.cmToInches(record.heightCm)
            val headCirc = record.headCircumferenceCm?.let {
                if (isMetric) it else UnitConverter.cmToInches(it)
            }

            // Calculate BMI
            val bmi = BMIResult.calculate(record.weightKg, record.heightCm)

            // Escape notes (in case they contain commas)
            val escapedNotes = "\"${record.notes.replace("\"", "\"\"")}\""

            sb.appendLine(
                "$date," +
                String.format("%.1f", weight) + "," +
                String.format("%.1f", height) + "," +
                (headCirc?.let { String.format("%.1f", it) } ?: "") + "," +
                (bmi?.let { String.format("%.1f", it.value) } ?: "") + "," +
                escapedNotes
            )
        }

        return sb.toString()
    }

    /**
     * Export records as CSV and open the Android Share Sheet.
     *
     * Workflow:
     * 1. Generate CSV content
     * 2. Write to a temporary file in app's cache directory
     * 3. Create a content URI via FileProvider
     * 4. Launch a share Intent (email, WhatsApp, etc.)
     *
     * @param context Activity or Application context
     * @param records List of GrowthRecord objects
     * @param babyName Baby's name (for filename and header)
     * @param isMetric Whether to use metric or imperial units
     */
    fun exportAndShare(
        context: Context,
        records: List<GrowthRecord>,
        babyName: String,
        isMetric: Boolean
    ) {
        // Generate CSV content
        val csvContent = generateCsvContent(records, babyName, isMetric)

        // Create the file in the cache directory (auto-cleaned by the OS)
        val fileName = "shishu_sneh_${babyName.lowercase().replace(" ", "_")}_growth.csv"
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val csvFile = File(exportDir, fileName)
        csvFile.writeText(csvContent)

        // Get a content URI via FileProvider (required for sharing files on Android 7+)
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        // Launch the Share Sheet
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "$babyName - Growth Records")
            putExtra(
                Intent.EXTRA_TEXT,
                "Here are $babyName's growth records from the Shishu Sneh app."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share Growth Records")
        )
    }
}
