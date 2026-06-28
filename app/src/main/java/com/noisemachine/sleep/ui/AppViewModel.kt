package com.noisemachine.sleep.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noisemachine.sleep.SleepNoiseApplication
import com.noisemachine.sleep.model.LayerType
import com.noisemachine.sleep.model.MixState
import com.noisemachine.sleep.model.Preset
import com.noisemachine.sleep.model.SoundLayer
import com.noisemachine.sleep.playback.PlaybackState
import com.noisemachine.sleep.playback.PlaybackStateStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class MainTab {
    PLAY,
    MIX,
    LIBRARY,
    TIMER,
    SETTINGS
}

data class AppUiState(
    val tab: MainTab = MainTab.PLAY,
    val presets: List<Preset> = emptyList(),
    val selectedPresetId: Long? = null,
    val currentMix: MixState = MixState.empty,
    val timerSecondsRemaining: Long? = null,
    val napMinutes: Int = 25,
    val sleepMinutes: Int = 90,
    val deepSleepMinutes: Int = 480,
    val sleepMode: Boolean = false,
    val masterVolume: Float = 0.72f,
    val sleepModeDimEnabled: Boolean = true
)

sealed interface AppEvent {
    data object TogglePlayback : AppEvent
    data class SetTab(val tab: MainTab) : AppEvent
    data class SelectPreset(val presetId: Long) : AppEvent
    data class ToggleFavorite(val presetId: Long, val favorite: Boolean) : AppEvent
    data class SetMasterVolume(val value: Float) : AppEvent
    data class UpdateLayerVolume(val layerId: Int, val value: Float) : AppEvent
    data class UpdateLayerFilter(val layerId: Int, val value: Float) : AppEvent
    data class UpdateLayerModulation(val layerId: Int, val value: Float) : AppEvent
    data class RemoveLayer(val layerId: Int) : AppEvent
    data class AddLayer(val type: LayerType) : AppEvent
    data object RandomizeMixSafely : AppEvent
    data class StartTimer(val minutes: Int) : AppEvent
    data object CancelTimer : AppEvent
    data object EnterSleepMode : AppEvent
    data object ExitSleepMode : AppEvent
    data class SetSleepModeDim(val enabled: Boolean) : AppEvent
}

class AppViewModel(
    private val app: SleepNoiseApplication
) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    val playbackState: StateFlow<PlaybackState> = PlaybackStateStore.state

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            app.presetRepository.observePresets().collect { presets ->
                _uiState.update { current ->
                    if (current.selectedPresetId == null && presets.isNotEmpty()) {
                        val initial = presets.first()
                        current.copy(
                            presets = presets,
                            selectedPresetId = initial.id,
                            currentMix = initial.mix.copy(masterVolume = current.masterVolume)
                        )
                    } else {
                        current.copy(presets = presets)
                    }
                }
            }
        }
        viewModelScope.launch {
            app.settingsStore.settings.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        masterVolume = settings.masterVolume,
                        napMinutes = settings.napMinutes,
                        sleepMinutes = settings.sleepMinutes,
                        deepSleepMinutes = settings.deepSleepMinutes,
                        sleepModeDimEnabled = settings.sleepModeDimEnabled,
                        currentMix = current.currentMix.copy(masterVolume = settings.masterVolume)
                    )
                }
            }
        }
    }

    fun onEvent(event: AppEvent) {
        when (event) {
            AppEvent.TogglePlayback -> togglePlayback()
            is AppEvent.SetTab -> _uiState.update { it.copy(tab = event.tab) }
            is AppEvent.SelectPreset -> selectPreset(event.presetId)
            is AppEvent.ToggleFavorite -> setFavorite(event.presetId, event.favorite)
            is AppEvent.SetMasterVolume -> setMasterVolume(event.value)
            is AppEvent.UpdateLayerVolume -> updateLayer(event.layerId) { it.copy(volume = event.value) }
            is AppEvent.UpdateLayerFilter -> updateLayer(event.layerId) { it.copy(filterCutoff = event.value) }
            is AppEvent.UpdateLayerModulation -> updateLayer(event.layerId) { it.copy(modulationRateHz = event.value) }
            is AppEvent.RemoveLayer -> removeLayer(event.layerId)
            is AppEvent.AddLayer -> addLayer(event.type)
            AppEvent.RandomizeMixSafely -> randomizeMixSafely()
            is AppEvent.StartTimer -> startTimer(event.minutes)
            AppEvent.CancelTimer -> cancelTimer()
            AppEvent.EnterSleepMode -> _uiState.update { it.copy(sleepMode = true) }
            AppEvent.ExitSleepMode -> _uiState.update { it.copy(sleepMode = false) }
            is AppEvent.SetSleepModeDim -> setSleepModeDim(event.enabled)
        }
    }

    private fun togglePlayback() {
        val currentlyPlaying = playbackState.value.isPlaying
        if (currentlyPlaying) {
            app.playbackController.stop()
            return
        }

        val mix = _uiState.value.currentMix.copy(masterVolume = _uiState.value.masterVolume)
        app.playbackController.play(mix)
        _uiState.value.selectedPresetId?.let { presetId ->
            viewModelScope.launch { app.presetRepository.markUsed(presetId) }
        }
    }

    private fun selectPreset(presetId: Long) {
        val preset = _uiState.value.presets.firstOrNull { it.id == presetId } ?: return
        _uiState.update {
            it.copy(
                selectedPresetId = presetId,
                currentMix = preset.mix.copy(masterVolume = it.masterVolume)
            )
        }
        if (playbackState.value.isPlaying) {
            app.playbackController.update(_uiState.value.currentMix)
        }
        viewModelScope.launch { app.presetRepository.markUsed(presetId) }
    }

    private fun setFavorite(presetId: Long, favorite: Boolean) {
        viewModelScope.launch {
            app.presetRepository.setFavorite(presetId, favorite)
        }
    }

    private fun setMasterVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(
                masterVolume = clamped,
                currentMix = it.currentMix.copy(masterVolume = clamped)
            )
        }
        viewModelScope.launch { app.settingsStore.setMasterVolume(clamped) }
        if (playbackState.value.isPlaying) {
            app.playbackController.update(_uiState.value.currentMix)
        }
    }

    private fun updateLayer(layerId: Int, transform: (SoundLayer) -> SoundLayer) {
        _uiState.update { current ->
            val updatedLayers = current.currentMix.layers.map { layer ->
                if (layer.id == layerId) transform(layer) else layer
            }
            current.copy(currentMix = current.currentMix.copy(layers = updatedLayers))
        }
        if (playbackState.value.isPlaying) {
            app.playbackController.update(_uiState.value.currentMix)
        }
    }

    private fun addLayer(type: LayerType) {
        _uiState.update { current ->
            if (current.currentMix.layers.size >= 5) return@update current
            val nextId = (current.currentMix.layers.maxOfOrNull { it.id } ?: 0) + 1
            val layer = SoundLayer(
                id = nextId,
                type = type,
                volume = 0.45f,
                filterCutoff = 0.5f,
                modulationRateHz = 0.2f
            )
            current.copy(currentMix = current.currentMix.copy(layers = current.currentMix.layers + layer))
        }
        if (playbackState.value.isPlaying) {
            app.playbackController.update(_uiState.value.currentMix)
        }
    }

    private fun removeLayer(layerId: Int) {
        _uiState.update { current ->
            current.copy(currentMix = current.currentMix.copy(layers = current.currentMix.layers.filterNot { it.id == layerId }))
        }
        if (playbackState.value.isPlaying) {
            app.playbackController.update(_uiState.value.currentMix)
        }
    }

    private fun randomizeMixSafely() {
        _uiState.update { current ->
            val updated = current.currentMix.layers.map {
                it.copy(
                    volume = Random.nextDouble(0.2, 0.7).toFloat(),
                    filterCutoff = Random.nextDouble(0.35, 0.85).toFloat(),
                    modulationRateHz = Random.nextDouble(0.05, 0.45).toFloat()
                )
            }
            current.copy(currentMix = current.currentMix.copy(layers = updated))
        }
        if (playbackState.value.isPlaying) {
            app.playbackController.update(_uiState.value.currentMix)
        }
    }

    private fun startTimer(minutes: Int) {
        val bounded = minutes.coerceIn(1, 720)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = bounded * 60L
            _uiState.update { it.copy(timerSecondsRemaining = remaining) }
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                _uiState.update { it.copy(timerSecondsRemaining = remaining) }
            }
            app.playbackController.stop()
            _uiState.update { it.copy(timerSecondsRemaining = null) }
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(timerSecondsRemaining = null) }
    }

    private fun setSleepModeDim(enabled: Boolean) {
        _uiState.update { it.copy(sleepModeDimEnabled = enabled) }
        viewModelScope.launch { app.settingsStore.setSleepModeDimEnabled(enabled) }
    }

    class Factory(
        private val app: SleepNoiseApplication
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}
