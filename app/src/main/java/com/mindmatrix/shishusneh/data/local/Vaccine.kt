package com.mindmatrix.shishusneh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Vaccine — Pre-defined Vaccine Information
 *
 * This table stores the India National Immunization Schedule (NIS).
 * The data is pre-populated when the database is first created
 * and is NOT user-editable.
 *
 * Each vaccine has a fixed age (in weeks after birth) when it should
 * be administered. The app calculates the actual due date from
 * the baby's DOB + weeksAfterBirth.
 *
 * TABLE STRUCTURE:
 * ┌────┬──────────┬──────────────────────┬─────────────────┬──────────┬───────┬──────────┐
 * │ id │ name     │ fullName             │ description     │ ageLabel │ weeks │ category │
 * ├────┼──────────┼──────────────────────┼─────────────────┼──────────┼───────┼──────────┤
 * │ 1  │ BCG      │ Bacillus Calmette-.. │ Protects aga..  │ Birth    │ 0     │ Birth    │
 * │ 4  │ OPV-1    │ Oral Polio Vaccine.. │ Protects aga..  │ 6 Weeks  │ 6     │ Primary  │
 * └────┴──────────┴──────────────────────┴─────────────────┴──────────┴───────┴──────────┘
 */
@Entity(tableName = "vaccines")
data class Vaccine(

    // Fixed ID (1-24) — NOT auto-generated because these are pre-defined
    @PrimaryKey
    val id: Int,

    // Short name displayed in the list (e.g., "BCG", "OPV-1")
    val name: String,

    // Full medical name (e.g., "Bacillus Calmette-Guérin")
    val fullName: String,

    // What this vaccine protects against
    val description: String,

    // Human-readable age label (e.g., "Birth", "6 Weeks", "9 Months")
    val ageLabel: String,

    // Number of weeks after birth when this vaccine is due
    // Used for computing the actual due date: DOB + (weeks * 7 days)
    val weeksAfterBirth: Int,

    // Category for grouping: "Birth", "Primary", "Booster"
    val category: String
)
