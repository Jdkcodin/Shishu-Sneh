package com.mindmatrix.shishusneh.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ShishuSnehDatabase — The Main Database (Version 3)
 *
 * This is the "heart" of our local data storage.
 * It creates and manages the SQLite database file on the device.
 *
 * VERSION HISTORY:
 * ┌─────────┬──────────────────────────────────────────────┐
 * │ v1      │ baby_profiles table (Phase 1)                │
 * │ v2      │ + growth_records table (Phase 2)             │
 * │ v3      │ + vaccines + vaccination_records (Phase 3)   │
 * └─────────┴──────────────────────────────────────────────┘
 *
 * SINGLETON PATTERN:
 * We only want ONE database instance for the entire app.
 * If two parts of the app created separate databases,
 * they'd have different data — that would be chaos!
 *
 * Think of it like a family register — there's only ONE,
 * and everyone reads/writes to the same one.
 */
@Database(
    entities = [
        BabyProfile::class,         // Table: baby_profiles (Phase 1)
        GrowthRecord::class,        // Table: growth_records (Phase 2)
        Vaccine::class,             // Table: vaccines (Phase 3) — pre-populated
        VaccinationRecord::class    // Table: vaccination_records (Phase 3)
    ],
    version = 4,                    // Schema version 4 (Multi-baby support via DAO changes)
    exportSchema = false            // Don't export schema (simplicity for now)
)
abstract class ShishuSnehDatabase : RoomDatabase() {

    /**
     * Get the BabyProfile DAO.
     * Room generates the actual implementation of this.
     */
    abstract fun babyProfileDao(): BabyProfileDao

    /**
     * Get the GrowthRecord DAO — Phase 2.
     * Room generates the actual implementation of this.
     */
    abstract fun growthRecordDao(): GrowthRecordDao

    /**
     * Get the Vaccine DAO — Phase 3.
     * For reading the pre-populated vaccine schedule.
     */
    abstract fun vaccineDao(): VaccineDao

    /**
     * Get the VaccinationRecord DAO — Phase 3.
     * For tracking which vaccines have been administered.
     */
    abstract fun vaccinationRecordDao(): VaccinationRecordDao

    companion object {
        /*
         * @Volatile = This variable is always read from MAIN MEMORY
         * (not from a thread's local cache).
         *
         * Without @Volatile, Thread A might create the database,
         * but Thread B wouldn't know and would create another one!
         */
        @Volatile
        private var INSTANCE: ShishuSnehDatabase? = null

        /**
         * Migration from Version 1 → Version 2
         *
         * Creates the growth_records table without touching baby_profiles.
         * This preserves any existing baby profile data from Phase 1.
         *
         * IMPORTANT: The SQL must exactly match what Room would generate
         * from the GrowthRecord @Entity class.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `growth_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `babyProfileId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `weightKg` REAL NOT NULL,
                        `heightCm` REAL NOT NULL,
                        `headCircumferenceCm` REAL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from Version 2 → Version 3
         *
         * Creates:
         * 1. `vaccines` table — pre-populated vaccine schedule
         * 2. `vaccination_records` table — tracks given vaccines
         *
         * The vaccines table is populated via the RoomDatabase.Callback
         * below (not in this migration), because the callback runs
         * for both new installs and upgrades.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create vaccines table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vaccines` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `ageLabel` TEXT NOT NULL,
                        `weeksAfterBirth` INTEGER NOT NULL,
                        `category` TEXT NOT NULL
                    )
                """.trimIndent())

                // Create vaccination_records table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vaccination_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vaccineId` INTEGER NOT NULL,
                        `babyProfileId` INTEGER NOT NULL,
                        `dateGiven` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from Version 3 → Version 4
         * Placeholder for any schema changes in v4.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 4 includes DAO query updates for multi-baby support,
                // but no new table schema changes were added.
            }
        }

        /**
         * Callback to pre-populate vaccine data after database creation/migration.
         *
         * This runs AFTER the database is created or migrated.
         * It checks if vaccines are already populated (to avoid duplicates).
         */
        private fun getPrePopulateCallback(db: ShishuSnehDatabase): Callback {
            return object : Callback() {
                override fun onOpen(sqLiteDb: SupportSQLiteDatabase) {
                    super.onOpen(sqLiteDb)
                    // Pre-populate vaccines on a background thread
                    CoroutineScope(Dispatchers.IO).launch {
                        val vaccineDao = db.vaccineDao()
                        val count = vaccineDao.getVaccineCount()
                        if (count == 0) {
                            // Insert all 24 vaccines
                            vaccineDao.insertAll(VaccineData.getAll())
                        }
                    }
                }
            }
        }

        /**
         * Get the database instance. Creates it if it doesn't exist.
         *
         * This uses the "double-checked locking" pattern:
         * 1. First check: Is INSTANCE already created? (fast, no lock)
         * 2. synchronized: Lock to prevent multiple threads creating it
         * 3. Second check (inside synchronized): Another thread might have
         *    created it while we were waiting for the lock
         *
         * @param context Application context (not Activity — survives rotation)
         * @return The single database instance
         */
        fun getDatabase(context: Context): ShishuSnehDatabase {
            // First check — if database exists, return it immediately
            return INSTANCE ?: synchronized(this) {
                // Inside the lock — create the database
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShishuSnehDatabase::class.java,
                    "shishu_sneh_database"  // File name: shishu_sneh_database.db
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)  // Apply migrations
                    .fallbackToDestructiveMigration()               // Safety net for dev builds
                    .build()

                // Add the callback for pre-populating vaccine data
                // We need to register it after building, then re-open to trigger onOpen
                INSTANCE = instance

                // Pre-populate vaccines in a background thread
                CoroutineScope(Dispatchers.IO).launch {
                    val vaccineDao = instance.vaccineDao()
                    val count = vaccineDao.getVaccineCount()
                    if (count == 0) {
                        vaccineDao.insertAll(VaccineData.getAll())
                    }
                }

                instance  // Return the newly created instance
            }
        }
    }
}
