package com.mindmatrix.shishusneh.data.model

import com.mindmatrix.shishusneh.data.local.Vaccine

/**
 * VaccineWithStatus — UI Model Combining Vaccine + Computed Status
 *
 * This is NOT stored in the database. It's computed at runtime by
 * combining Vaccine data with VaccinationRecord data and baby's DOB.
 *
 * STATUSES:
 * ┌─────────────┬───────────────────────────────────────────────┐
 * │ GIVEN    ✅ │ Already administered — has a VaccinationRecord│
 * │ DUE      ⚠️ │ Due within the next 7 days                   │
 * │ OVERDUE  🔴 │ Past due date and not given                  │
 * │ UPCOMING 🔵 │ Not yet due (future date)                    │
 * └─────────────┴───────────────────────────────────────────────┘
 *
 * Example display in the schedule:
 * ┌──────────────────────────────────────┐
 * │ BCG                          ✅ Given │
 * │ Due: 15 Apr 2025 • Given: 15 Apr    │
 * ├──────────────────────────────────────┤
 * │ OPV-1                   ⚠️ Due Soon │
 * │ Due: 27 May 2025 • In 2 days        │
 * ├──────────────────────────────────────┤
 * │ MR-1                    🔵 Upcoming │
 * │ Due: 15 Jan 2026 • In 245 days      │
 * └──────────────────────────────────────┘
 */
data class VaccineWithStatus(
    // The vaccine information
    val vaccine: Vaccine,

    // Computed status
    val status: VaccineStatus,

    // Computed due date: baby's DOB + (weeksAfterBirth * 7 days)
    val dueDate: Long,

    // When the vaccine was given (null if not yet given)
    val dateGiven: Long? = null,

    // Days until due (negative = overdue, positive = upcoming)
    // null if already given
    val daysUntilDue: Int? = null,

    // Notes from the VaccinationRecord (if given)
    val notes: String = ""
)

/**
 * VaccineStatus — The four possible states of a vaccine
 */
enum class VaccineStatus {
    /** ✅ Already administered — has a VaccinationRecord */
    GIVEN,

    /** ⚠️ Due within the next 7 days (but not overdue) */
    DUE,

    /** 🔴 Past due date and not given */
    OVERDUE,

    /** 🔵 Not yet due — scheduled for the future */
    UPCOMING
}
