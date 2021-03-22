package com.cooper.wheellog.data

import androidx.room.*

@Dao
interface TripDao {
    @Query("SELECT * FROM tripEntity")
    suspend fun getAll(): List<TripEntity>

    @Query("SELECT * FROM tripEntity WHERE id IN (:tripIds)")
    suspend fun loadAllByIds(tripIds: IntArray): List<TripEntity>

    @Query("SELECT * FROM tripEntity WHERE fileName LIKE :fileName LIMIT 1")
    suspend fun getTripByFileName(fileName: String): TripEntity?

    @Query("SELECT * FROM tripEntity WHERE ecId LIKE :ecId LIMIT 1")
    suspend fun getTripByElectroClubId(ecId: Int): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg tripEntities: TripEntity)

    @Update
    suspend fun update(vararg tripEntities: TripEntity)

    @Delete
    suspend fun delete(vararg tripEntities: TripEntity)
}