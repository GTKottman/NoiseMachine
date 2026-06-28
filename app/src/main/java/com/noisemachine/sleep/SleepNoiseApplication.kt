package com.noisemachine.sleep

import android.app.Application
import androidx.room.Room
import com.noisemachine.sleep.data.db.AppDatabase
import com.noisemachine.sleep.data.repo.PresetRepository
import com.noisemachine.sleep.data.settings.AppSettingsStore
import com.noisemachine.sleep.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SleepNoiseApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "sleep-noise.db"
        ).build()
    }

    val presetRepository: PresetRepository by lazy {
        PresetRepository(database.presetDao())
    }

    val settingsStore: AppSettingsStore by lazy {
        AppSettingsStore(this)
    }

    val playbackController: PlaybackController by lazy {
        PlaybackController(this)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            presetRepository.ensureSeeded()
        }
    }
}
