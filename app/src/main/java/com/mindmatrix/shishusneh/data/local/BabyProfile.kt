package com.mindmatrix.shishusneh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BabyProfile — The Core Data Class
 *
 * This class represents a baby's profile information.
 * Room uses it to create a database table called "baby_profiles".
 *
 * HOW IT WORKS:
 * ┌──────────────────────────────────────────────────┐
 * │  @Entity = "Create a table from this class"      │
 * │  Each property = One column in the table         │
 * │  @PrimaryKey = Unique identifier for each row    │
 * └──────────────────────────────────────────────────┘
 *
 * EXAMPLE TABLE:
 * ┌────┬──────────┬─────────────┬────────────┬────────┬───────────┐
 * │ id │ babyName │ dateOfBirth │ motherName │ gender │ createdAt │
 * ├────┼──────────┼─────────────┼────────────┼────────┼───────────┤
 * │ 1  │ Aarav    │ 167299200.. │ Priya      │ Male   │ 171456..  │
 * └────┴──────────┴─────────────┴────────────┴────────┴───────────┘
 */
@Entity(tableName = "baby_profiles")
data class BabyProfile(

    // @PrimaryKey = unique identifier for each row
    // autoGenerate = Room assigns IDs automatically (1, 2, 3...)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Baby's name — entered during onboarding
    val babyName: String,

    // Date of Birth stored as milliseconds since Jan 1, 1970 (epoch time)
    // WHY Long instead of Date?
    //   → Room can't store Date objects directly
    //   → We convert: Date → Long (save) and Long → Date (display)
    //   → Example: April 15, 2025 = 1744675200000 milliseconds
    val dateOfBirth: Long,

    // Mother's name — helpful for personalized messages
    val motherName: String = "",

    // Gender — "Male", "Female", or "Other" (REQUIRED)
    val gender: String,

    // When this profile was created (set automatically)
    val createdAt: Long = System.currentTimeMillis()
)
