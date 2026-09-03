package com.k.hosken.navipulse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(fuelLog: FuelLog)

    @Query("SELECT * FROM fuel_logs ORDER BY createdAt DESC")
    fun getAllFuelLogs(): Flow<List<FuelLog>>

    @Query("DELETE FROM fuel_logs WHERE id = :id")
    suspend fun deleteFuelLogById(id: Long)
}
