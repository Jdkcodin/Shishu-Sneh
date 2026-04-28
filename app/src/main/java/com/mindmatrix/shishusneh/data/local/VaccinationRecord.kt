package com.mindmatrix.shishusneh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * VaccinationRecord — Tracks Which Vaccines Have Been Given
 *
 * When a parent taps "Mark as Given" on a vaccine, a record is
 * created here. The existence of a record for a given vaccineId
 * means that vaccine has been administered.
 *
 * TABLE STRUCTURE:
 * ┌────┬───────────┬──────────────┬───────────┬────────────────────┬───────────┐
 * │ id │ vaccineId │ babyProfileId│ dateGiven │ notes              │ createdAt │
 * ├────┼───────────┼──────────────┼───────────┼────────────────────┼───────────┤
 * │ 1  │ 1         │ 1            │ 174467..  │ Dr. Sharma, Apollo │ 174467..  │
 * │ 2  │ 2         │ 1            │ 174467..  │                    │ 174467..  │
 * └────┴───────────┴──────────────┴───────────┴────────────────────┴───────────┘
 */
@Entity(tableName = "vaccination_records")
data class VaccinationRecord(

    // Auto-generated unique ID
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Which vaccine was given (FK → vaccines.id)
    val vaccineId: Int,

    // Which baby received it (FK → baby_profiles.id)
    val babyProfileId: Int,

    // When the vaccine was administered (epoch milliseconds)
    val dateGiven: Long,

    // Optional notes (e.g., doctor name, hospital, batch number)
    val notes: String = "",

    // When this record was created in the app
    val createdAt: Long = System.currentTimeMillis()
)
