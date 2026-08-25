package com.k.hosken.navipulse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripLog)

    @Update
    suspend fun updateTrip(trip: TripLog)

    @Query("SELECT * FROM trip_logs ORDER BY startTimestamp DESC")
    fun getAllTrips(): Flow<List<TripLog>>

    @Query("SELECT * FROM trip_logs WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: Long): TripLog?

    @Query("DELETE FROM trip_logs WHERE id = :id")
    suspend fun deleteTripById(id: Long)
}