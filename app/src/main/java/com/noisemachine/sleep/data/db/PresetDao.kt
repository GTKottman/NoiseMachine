package com.noisemachine.sleep.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY CASE WHEN favorite THEN 0 ELSE 1 END, name ASC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PresetEntity)

    @Query("UPDATE presets SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE presets SET lastUsedEpochMs = :timestampMs WHERE id = :id")
    suspend fun markUsed(id: Long, timestampMs: Long)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int
}
