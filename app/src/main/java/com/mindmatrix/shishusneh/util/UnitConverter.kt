package com.mindmatrix.shishusneh.util

/**
 * UnitConverter — Metric ↔ Imperial Conversions
 *
 * All data is stored in METRIC (kg/cm) in the database.
 * This utility converts for display when the user selects Imperial mode.
 *
 * CONVERSION FORMULAS:
 * ┌───────────────┬────────────────────────┐
 * │ kg  → lbs    │ multiply by 2.20462    │
 * │ lbs → kg     │ divide by 2.20462      │
 * │ cm  → inches │ divide by 2.54         │
 * │ inches → cm  │ multiply by 2.54       │
 * └───────────────┴────────────────────────┘
 */
object UnitConverter {

    // =====================================================
    // WEIGHT CONVERSIONS
    // =====================================================

    /** Convert kilograms to pounds */
    fun kgToLbs(kg: Float): Float = kg * 2.20462f

    /** Convert pounds to kilograms */
    fun lbsToKg(lbs: Float): Float = lbs / 2.20462f

    // =====================================================
    // HEIGHT CONVERSIONS
    // =====================================================

    /** Convert centimeters to inches */
    fun cmToInches(cm: Float): Float = cm / 2.54f

    /** Convert inches to centimeters */
    fun inchesToCm(inches: Float): Float = inches * 2.54f

    // =====================================================
    // FORMATTED DISPLAY STRINGS
    // =====================================================

    /**
     * Format weight for display with appropriate unit suffix.
     *
     * @param kg Weight in kilograms (always metric from DB)
     * @param isMetric If true, show kg; if false, convert and show lbs
     * @return Formatted string like "5.2 kg" or "11.5 lbs"
     */
    fun formatWeight(kg: Float, isMetric: Boolean): String {
        return if (isMetric) {
            String.format("%.1f kg", kg)
        } else {
            String.format("%.1f lbs", kgToLbs(kg))
        }
    }

    /**
     * Format height for display with appropriate unit suffix.
     *
     * @param cm Height in centimeters (always metric from DB)
     * @param isMetric If true, show cm; if false, convert and show in
     * @return Formatted string like "58.0 cm" or "22.8 in"
     */
    fun formatHeight(cm: Float, isMetric: Boolean): String {
        return if (isMetric) {
            String.format("%.1f cm", cm)
        } else {
            String.format("%.1f in", cmToInches(cm))
        }
    }

    /**
     * Format head circumference for display.
     *
     * @param cm Head circumference in cm (nullable — field is optional)
     * @param isMetric Unit system
     * @return Formatted string or "—" if null
     */
    fun formatHeadCircumference(cm: Float?, isMetric: Boolean): String {
        if (cm == null) return "—"
        return if (isMetric) {
            String.format("%.1f cm", cm)
        } else {
            String.format("%.1f in", cmToInches(cm))
        }
    }

    /**
     * Format a weight change value with + or - prefix.
     *
     * @param changeKg Weight change in kg (positive = gained, negative = lost)
     * @param isMetric Unit system
     * @return Formatted string like "+0.5 kg" or "-0.2 lbs"
     */
    fun formatWeightChange(changeKg: Float, isMetric: Boolean): String {
        val prefix = if (changeKg >= 0) "+" else ""
        return if (isMetric) {
            String.format("%s%.1f kg", prefix, changeKg)
        } else {
            String.format("%s%.1f lbs", prefix, kgToLbs(changeKg))
        }
    }

    /**
     * Format a height change value with + or - prefix.
     *
     * @param changeCm Height change in cm
     * @param isMetric Unit system
     * @return Formatted string like "+2.0 cm" or "+0.8 in"
     */
    fun formatHeightChange(changeCm: Float, isMetric: Boolean): String {
        val prefix = if (changeCm >= 0) "+" else ""
        return if (isMetric) {
            String.format("%s%.1f cm", prefix, changeCm)
        } else {
            String.format("%s%.1f in", prefix, cmToInches(changeCm))
        }
    }

    /**
     * Get the weight unit label.
     */
    fun weightUnit(isMetric: Boolean): String = if (isMetric) "kg" else "lbs"

    /**
     * Get the height unit label.
     */
    fun heightUnit(isMetric: Boolean): String = if (isMetric) "cm" else "in"

    /**
     * Convert a weight value from the user's input unit to metric for storage.
     *
     * @param value The value entered by the user
     * @param isMetric Whether the user is entering in metric
     * @return The value in kilograms
     */
    fun toMetricWeight(value: Float, isMetric: Boolean): Float {
        return if (isMetric) value else lbsToKg(value)
    }

    /**
     * Convert a height value from the user's input unit to metric for storage.
     *
     * @param value The value entered by the user
     * @param isMetric Whether the user is entering in metric
     * @return The value in centimeters
     */
    fun toMetricHeight(value: Float, isMetric: Boolean): Float {
        return if (isMetric) value else inchesToCm(value)
    }
}

/**
 * UnitSystem — Enum for the two supported unit systems.
 *
 * Stored in SharedPreferences as the string name ("METRIC" or "IMPERIAL").
 */
enum class UnitSystem {
    METRIC,
    IMPERIAL;

    val isMetric: Boolean get() = this == METRIC
}
