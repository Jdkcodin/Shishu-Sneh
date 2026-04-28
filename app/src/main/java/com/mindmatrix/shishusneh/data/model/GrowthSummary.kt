package com.mindmatrix.shishusneh.data.model

/**
 * GrowthSummary — Aggregated Growth Statistics
 *
 * Contains computed statistics shown on the Dashboard's Growth Summary Card.
 * These values are calculated from GrowthRecord entries, NOT stored in the database.
 *
 * Example display:
 * ┌─────────────────────────────────────┐
 * │  📈 Growth Summary                  │
 * │  ↑ Weight: +0.5 kg (last month)    │
 * │  ↑ Height: +2.0 cm (since birth)   │
 * │  📝 12 measurements recorded        │
 * │  🕐 Last recorded: 3 days ago      │
 * └─────────────────────────────────────┘
 */
data class GrowthSummary(
    // Weight change in kg over the last 30 days
    // Positive = gained weight, Negative = lost weight, null = not enough data
    val weightChangeLastMonth: Float? = null,

    // Height change in cm since the very first recorded measurement
    // Always positive (babies grow!), null = not enough data
    val heightChangeSinceBirth: Float? = null,

    // Total number of measurements recorded
    val totalRecords: Int = 0,

    // Days since the most recent measurement was recorded
    // null = no records yet
    val daysSinceLastRecord: Int? = null,

    // Current (latest) weight in kg
    val currentWeight: Float? = null,

    // Current (latest) height in cm
    val currentHeight: Float? = null,

    // First recorded weight in kg (for "since birth" comparisons)
    val birthWeight: Float? = null,

    // First recorded height in cm (for "since birth" comparisons)
    val birthHeight: Float? = null,

    // Weight change since birth in kg
    val weightChangeSinceBirth: Float? = null
)
