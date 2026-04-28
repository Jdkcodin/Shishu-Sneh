package com.mindmatrix.shishusneh.data.model

import com.mindmatrix.shishusneh.R

/**
 * BMIResult — Body Mass Index Calculation Result
 *
 * Contains the computed BMI value along with its category and display color.
 * BMI = weight(kg) / height(m)²
 *
 * Note: Standard adult BMI categories are used as a rough guide.
 * For infants, WHO growth percentile charts are more accurate,
 * but BMI gives a quick visual indicator for parents.
 *
 * Display example:
 * ┌──────────────────────────┐
 * │  BMI: 15.4  [Normal] 🟢 │
 * └──────────────────────────┘
 */
data class BMIResult(
    // The calculated BMI value (e.g., 15.4)
    val value: Float,

    // Human-readable category: "Underweight", "Normal", "Overweight"
    val category: String,

    // Color resource ID for the category badge
    // Green = Normal, Orange = Underweight, Red = Overweight
    val colorResId: Int
) {
    companion object {
        /**
         * Calculate BMI and determine category.
         *
         * Formula: BMI = weight(kg) / height(m)²
         * Height must be converted from cm to meters first.
         *
         * For infants (0-2 years), we use simplified thresholds:
         * - Below 14.0 = Underweight
         * - 14.0 to 18.0 = Normal
         * - Above 18.0 = Overweight
         *
         * @param weightKg Weight in kilograms
         * @param heightCm Height in centimeters
         * @return BMIResult with value, category, and color — or null if inputs are invalid
         */
        fun calculate(weightKg: Float, heightCm: Float): BMIResult? {
            if (weightKg <= 0f || heightCm <= 0f) return null

            val heightM = heightCm / 100f
            val bmi = weightKg / (heightM * heightM)

            return when {
                bmi < 14.0f -> BMIResult(
                    value = bmi,
                    category = "Underweight",
                    colorResId = R.color.bmi_underweight
                )
                bmi <= 18.0f -> BMIResult(
                    value = bmi,
                    category = "Normal",
                    colorResId = R.color.bmi_normal
                )
                else -> BMIResult(
                    value = bmi,
                    category = "Overweight",
                    colorResId = R.color.bmi_overweight
                )
            }
        }
    }
}
