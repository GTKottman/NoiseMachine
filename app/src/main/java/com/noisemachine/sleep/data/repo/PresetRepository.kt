package com.noisemachine.sleep.data.repo

import com.noisemachine.sleep.data.db.PresetDao
import com.noisemachine.sleep.data.db.PresetEntity
import com.noisemachine.sleep.model.MixCodec
import com.noisemachine.sleep.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PresetRepository(
    private val presetDao: PresetDao
) {
    fun observePresets(): Flow<List<Preset>> {
        return presetDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (presetDao.count() > 0) return@withContext
        val defaults = DefaultPresets.build().map { it.toEntity() }
        presetDao.upsertAll(defaults)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        presetDao.setFavorite(id, favorite)
    }

    suspend fun markUsed(id: Long) = withContext(Dispatchers.IO) {
        presetDao.markUsed(id, System.currentTimeMillis())
    }

    suspend fun getPreset(id: Long): Preset? = withContext(Dispatchers.IO) {
        presetDao.getById(id)?.toDomain()
    }
}

private fun PresetEntity.toDomain(): Preset {
    return Preset(
        id = id,
        name = name,
        description = description,
        mix = MixCodec.decode(mixEncoded),
        favorite = favorite,
        lastUsedEpochMs = lastUsedEpochMs
    )
}

private fun Preset.toEntity(): PresetEntity {
    return PresetEntity(
        id = id,
        name = name,
        description = description,
        mixEncoded = MixCodec.encode(mix),
        favorite = favorite,
        lastUsedEpochMs = lastUsedEpochMs
    )
}
