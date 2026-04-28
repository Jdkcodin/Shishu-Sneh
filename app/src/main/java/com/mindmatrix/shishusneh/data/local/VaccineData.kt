package com.mindmatrix.shishusneh.data.local

/**
 * VaccineData — India's National Immunization Schedule (NIS)
 *
 * Contains the complete list of 24 vaccines pre-populated into the database.
 * This data is inserted once when the database is first created (via Callback).
 *
 * Source: Government of India — Universal Immunization Programme (UIP)
 *
 * Schedule Overview:
 * ┌───────────┬────────────────────────────────────────────────┐
 * │ Birth     │ BCG, OPV-0, Hepatitis B Birth Dose           │
 * │ 6 Weeks   │ OPV-1, Pentavalent-1, Rotavirus-1, fIPV-1,  │
 * │           │ PCV-1                                         │
 * │ 10 Weeks  │ OPV-2, Pentavalent-2, Rotavirus-2            │
 * │ 14 Weeks  │ OPV-3, Pentavalent-3, Rotavirus-3, fIPV-2,  │
 * │           │ PCV-2                                         │
 * │ 9 Months  │ MR-1, JE-1, PCV Booster, Vitamin A          │
 * │ 16 Months │ DPT Booster-1, OPV Booster, MR-2, JE-2     │
 * └───────────┴────────────────────────────────────────────────┘
 */
object VaccineData {

    /**
     * Get the complete list of 24 vaccines.
     * Each vaccine has a fixed ID for referencing in VaccinationRecord.
     */
    fun getAll(): List<Vaccine> = listOf(
        // ── BIRTH ──
        Vaccine(
            id = 1,
            name = "BCG",
            fullName = "Bacillus Calmette-Guérin",
            description = "Protects against Tuberculosis (TB). Given as an injection in the left upper arm.",
            ageLabel = "Birth",
            weeksAfterBirth = 0,
            category = "Birth"
        ),
        Vaccine(
            id = 2,
            name = "OPV-0",
            fullName = "Oral Polio Vaccine — Zero Dose",
            description = "Protects against Poliomyelitis. Given as oral drops at birth.",
            ageLabel = "Birth",
            weeksAfterBirth = 0,
            category = "Birth"
        ),
        Vaccine(
            id = 3,
            name = "Hepatitis B — Birth",
            fullName = "Hepatitis B Vaccine — Birth Dose",
            description = "Protects against Hepatitis B infection. Given within 24 hours of birth.",
            ageLabel = "Birth",
            weeksAfterBirth = 0,
            category = "Birth"
        ),

        // ── 6 WEEKS ──
        Vaccine(
            id = 4,
            name = "OPV-1",
            fullName = "Oral Polio Vaccine — 1st Dose",
            description = "Protects against Poliomyelitis. Given as oral drops.",
            ageLabel = "6 Weeks",
            weeksAfterBirth = 6,
            category = "Primary"
        ),
        Vaccine(
            id = 5,
            name = "Pentavalent-1",
            fullName = "Pentavalent Vaccine — 1st Dose (DPT + Hep B + Hib)",
            description = "Protects against Diphtheria, Pertussis (Whooping Cough), Tetanus, Hepatitis B, and Haemophilus Influenzae type b.",
            ageLabel = "6 Weeks",
            weeksAfterBirth = 6,
            category = "Primary"
        ),
        Vaccine(
            id = 6,
            name = "Rotavirus-1",
            fullName = "Rotavirus Vaccine — 1st Dose",
            description = "Protects against Rotavirus diarrhea. Given as oral drops.",
            ageLabel = "6 Weeks",
            weeksAfterBirth = 6,
            category = "Primary"
        ),
        Vaccine(
            id = 7,
            name = "fIPV-1",
            fullName = "Fractional Inactivated Polio Vaccine — 1st Dose",
            description = "Additional protection against Poliomyelitis. Given as an injection.",
            ageLabel = "6 Weeks",
            weeksAfterBirth = 6,
            category = "Primary"
        ),
        Vaccine(
            id = 8,
            name = "PCV-1",
            fullName = "Pneumococcal Conjugate Vaccine — 1st Dose",
            description = "Protects against Pneumococcal diseases (pneumonia, meningitis).",
            ageLabel = "6 Weeks",
            weeksAfterBirth = 6,
            category = "Primary"
        ),

        // ── 10 WEEKS ──
        Vaccine(
            id = 9,
            name = "OPV-2",
            fullName = "Oral Polio Vaccine — 2nd Dose",
            description = "Protects against Poliomyelitis. Given as oral drops.",
            ageLabel = "10 Weeks",
            weeksAfterBirth = 10,
            category = "Primary"
        ),
        Vaccine(
            id = 10,
            name = "Pentavalent-2",
            fullName = "Pentavalent Vaccine — 2nd Dose (DPT + Hep B + Hib)",
            description = "Protects against Diphtheria, Pertussis, Tetanus, Hepatitis B, and Haemophilus Influenzae type b.",
            ageLabel = "10 Weeks",
            weeksAfterBirth = 10,
            category = "Primary"
        ),
        Vaccine(
            id = 11,
            name = "Rotavirus-2",
            fullName = "Rotavirus Vaccine — 2nd Dose",
            description = "Protects against Rotavirus diarrhea. Given as oral drops.",
            ageLabel = "10 Weeks",
            weeksAfterBirth = 10,
            category = "Primary"
        ),

        // ── 14 WEEKS ──
        Vaccine(
            id = 12,
            name = "OPV-3",
            fullName = "Oral Polio Vaccine — 3rd Dose",
            description = "Protects against Poliomyelitis. Given as oral drops.",
            ageLabel = "14 Weeks",
            weeksAfterBirth = 14,
            category = "Primary"
        ),
        Vaccine(
            id = 13,
            name = "Pentavalent-3",
            fullName = "Pentavalent Vaccine — 3rd Dose (DPT + Hep B + Hib)",
            description = "Protects against Diphtheria, Pertussis, Tetanus, Hepatitis B, and Haemophilus Influenzae type b.",
            ageLabel = "14 Weeks",
            weeksAfterBirth = 14,
            category = "Primary"
        ),
        Vaccine(
            id = 14,
            name = "Rotavirus-3",
            fullName = "Rotavirus Vaccine — 3rd Dose",
            description = "Protects against Rotavirus diarrhea. Given as oral drops.",
            ageLabel = "14 Weeks",
            weeksAfterBirth = 14,
            category = "Primary"
        ),
        Vaccine(
            id = 15,
            name = "fIPV-2",
            fullName = "Fractional Inactivated Polio Vaccine — 2nd Dose",
            description = "Additional protection against Poliomyelitis. Given as an injection.",
            ageLabel = "14 Weeks",
            weeksAfterBirth = 14,
            category = "Primary"
        ),
        Vaccine(
            id = 16,
            name = "PCV-2",
            fullName = "Pneumococcal Conjugate Vaccine — 2nd Dose",
            description = "Protects against Pneumococcal diseases (pneumonia, meningitis).",
            ageLabel = "14 Weeks",
            weeksAfterBirth = 14,
            category = "Primary"
        ),

        // ── 9 MONTHS ──
        Vaccine(
            id = 17,
            name = "MR-1",
            fullName = "Measles-Rubella Vaccine — 1st Dose",
            description = "Protects against Measles and Rubella (German Measles).",
            ageLabel = "9 Months",
            weeksAfterBirth = 39,
            category = "Primary"
        ),
        Vaccine(
            id = 18,
            name = "JE-1",
            fullName = "Japanese Encephalitis Vaccine — 1st Dose",
            description = "Protects against Japanese Encephalitis. Given in endemic areas.",
            ageLabel = "9 Months",
            weeksAfterBirth = 39,
            category = "Primary"
        ),
        Vaccine(
            id = 19,
            name = "PCV Booster",
            fullName = "Pneumococcal Conjugate Vaccine — Booster",
            description = "Booster dose for continued protection against Pneumococcal diseases.",
            ageLabel = "9 Months",
            weeksAfterBirth = 39,
            category = "Booster"
        ),
        Vaccine(
            id = 20,
            name = "Vitamin A — 1st",
            fullName = "Vitamin A Supplementation — 1st Dose",
            description = "Helps prevent Vitamin A deficiency, supports immune system and eye health.",
            ageLabel = "9 Months",
            weeksAfterBirth = 39,
            category = "Primary"
        ),

        // ── 16 MONTHS ──
        Vaccine(
            id = 21,
            name = "DPT Booster-1",
            fullName = "DPT Vaccine — Booster 1st Dose",
            description = "Booster for continued protection against Diphtheria, Pertussis, and Tetanus.",
            ageLabel = "16 Months",
            weeksAfterBirth = 70,
            category = "Booster"
        ),
        Vaccine(
            id = 22,
            name = "OPV Booster",
            fullName = "Oral Polio Vaccine — Booster",
            description = "Booster for continued protection against Poliomyelitis.",
            ageLabel = "16 Months",
            weeksAfterBirth = 70,
            category = "Booster"
        ),
        Vaccine(
            id = 23,
            name = "MR-2",
            fullName = "Measles-Rubella Vaccine — 2nd Dose",
            description = "Second dose for continued protection against Measles and Rubella.",
            ageLabel = "16 Months",
            weeksAfterBirth = 70,
            category = "Booster"
        ),
        Vaccine(
            id = 24,
            name = "JE-2",
            fullName = "Japanese Encephalitis Vaccine — 2nd Dose",
            description = "Second dose for continued protection against Japanese Encephalitis.",
            ageLabel = "16 Months",
            weeksAfterBirth = 70,
            category = "Booster"
        )
    )
}
