package com.noisemachine.sleep.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String,
    val mixEncoded: String,
    val favorite: Boolean,
    val lastUsedEpochMs: Long?
)
