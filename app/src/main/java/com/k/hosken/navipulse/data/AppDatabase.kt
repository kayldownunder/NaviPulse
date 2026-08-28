package com.k.hosken.navipulse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripLog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // No fallbackToDestructiveMigration: a future schema bump without a real
                // Migration should crash loudly, not silently wipe every saved trip.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "navipulse_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}