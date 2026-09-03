package com.k.hosken.navipulse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripLog::class, FuelLog::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun fuelDao(): FuelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Drops the isBusiness column now that trip type classification has been removed.
        // SQLite has no DROP COLUMN (pre-3.35), so rebuild the table without it.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE trip_logs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTimestamp INTEGER NOT NULL,
                        endTimestamp INTEGER NOT NULL,
                        distanceKm REAL NOT NULL,
                        durationMs INTEGER NOT NULL,
                        startAddress TEXT NOT NULL,
                        endAddress TEXT NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO trip_logs_new (id, startTimestamp, endTimestamp, distanceKm, durationMs, startAddress, endAddress, notes)
                    SELECT id, startTimestamp, endTimestamp, distanceKm, durationMs, startAddress, endAddress, notes FROM trip_logs
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE trip_logs")
                db.execSQL("ALTER TABLE trip_logs_new RENAME TO trip_logs")
            }
        }

        // Adds average/max speed columns captured during tracking.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_logs ADD COLUMN avgSpeedKmh REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE trip_logs ADD COLUMN maxSpeedKmh REAL NOT NULL DEFAULT 0.0")
            }
        }

        // Adds the fuel_logs table for the Fuel Up entry screen.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fuel_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dateRefuelled INTEGER NOT NULL,
                        litres REAL NOT NULL,
                        pricePerLitre REAL NOT NULL,
                        totalPrice REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Adds the distance-since-last-fuel-up column used for the fuel economy calculation.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fuel_logs ADD COLUMN distanceKmSinceLastFuelUp REAL NOT NULL DEFAULT 0.0")
            }
        }

        // Adds average/top speed columns for the trips covered by each fuel-up.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fuel_logs ADD COLUMN avgSpeedKmhSinceLastFuelUp REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE fuel_logs ADD COLUMN maxSpeedKmhSinceLastFuelUp REAL NOT NULL DEFAULT 0.0")
            }
        }

        // Adds moving-time tracking so fuel-up reports can average speed only over
        // time spent underway (above the minimum-moving-speed threshold).
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_logs ADD COLUMN movingTimeMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Adds the recorded GPS track so a past trip can be exported/viewed on a map.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_logs ADD COLUMN routePointsCsv TEXT NOT NULL DEFAULT ''")
            }
        }

        // Adds a real creation timestamp for fuel logs so newly added entries always sort
        // to the top of the log, regardless of the (date-only, user-editable) refuel date.
        // Existing rows backfill from dateRefuelled to preserve their current relative order.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fuel_logs ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE fuel_logs SET createdAt = dateRefuelled")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // No fallbackToDestructiveMigration: a future schema bump without a real
                // Migration should crash loudly, not silently wipe every saved trip.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "navipulse_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}