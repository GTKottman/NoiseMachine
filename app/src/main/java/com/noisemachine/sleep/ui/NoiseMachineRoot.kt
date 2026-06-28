package com.noisemachine.sleep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noisemachine.sleep.model.LayerType
import com.noisemachine.sleep.model.Preset
import com.noisemachine.sleep.playback.PlaybackState
import kotlin.math.roundToInt

@Composable
fun NoiseMachineRoot(
    uiState: AppUiState,
    playbackState: PlaybackState,
    onEvent: (AppEvent) -> Unit
) {
    if (uiState.sleepMode) {
        SleepModeScreen(uiState, playbackState, onEvent)
        return
    }

    val tabs = listOf(
        MainTab.PLAY to "Play",
        MainTab.MIX to "Mix",
        MainTab.LIBRARY to "Library",
        MainTab.TIMER to "Timer",
        MainTab.SETTINGS to "Settings"
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (tab, label) ->
                    NavigationBarItem(
                        selected = uiState.tab == tab,
                        onClick = { onEvent(AppEvent.SetTab(tab)) },
                        label = { Text(label) },
                        icon = { Text(label.first().toString()) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (uiState.tab) {
                MainTab.PLAY -> PlayScreen(uiState, playbackState, onEvent)
                MainTab.MIX -> MixScreen(uiState, onEvent)
                MainTab.LIBRARY -> LibraryScreen(uiState, onEvent)
                MainTab.TIMER -> TimerScreen(uiState, onEvent)
                MainTab.SETTINGS -> SettingsScreen(uiState, onEvent)
            }
        }
    }
}

@Composable
private fun PlayScreen(
    uiState: AppUiState,
    playbackState: PlaybackState,
    onEvent: (AppEvent) -> Unit
) {
    val selected = uiState.presets.firstOrNull { it.id == uiState.selectedPresetId }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Procedural Sleep Noise",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = { onEvent(AppEvent.TogglePlayback) },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text(if (playbackState.isPlaying) "Pause" else "Play")
        }

        Text(
            text = selected?.let { "${it.name} • ${it.description}" } ?: "Loading presets...",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Master volume: ${(uiState.masterVolume * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = uiState.masterVolume,
            onValueChange = { onEvent(AppEvent.SetMasterVolume(it)) },
            valueRange = 0f..1f
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.presets, key = { it.id }) { preset ->
                PresetChip(
                    preset = preset,
                    selected = preset.id == uiState.selectedPresetId,
                    onSelect = { onEvent(AppEvent.SelectPreset(preset.id)) },
                    onToggleFavorite = { onEvent(AppEvent.ToggleFavorite(preset.id, !preset.favorite)) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AssistChip(
                onClick = { onEvent(AppEvent.StartTimer(uiState.napMinutes)) },
                label = { Text("Nap") }
            )
            AssistChip(
                onClick = { onEvent(AppEvent.StartTimer(uiState.sleepMinutes)) },
                label = { Text("Sleep") }
            )
            AssistChip(
                onClick = { onEvent(AppEvent.StartTimer(uiState.deepSleepMinutes)) },
                label = { Text("Deep Sleep") }
            )
        }

        OutlinedButton(
            onClick = { onEvent(AppEvent.EnterSleepMode) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter Sleep Mode")
        }
    }
}

@Composable
private fun PresetChip(
    preset: Preset,
    selected: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = preset.name, fontWeight = FontWeight.Medium)
            Text(text = preset.description, style = MaterialTheme.typography.labelSmall)
            OutlinedButton(onClick = onToggleFavorite) {
                Text(if (preset.favorite) "Unfavorite" else "Favorite")
            }
        }
    }
}

@Composable
private fun MixScreen(
    uiState: AppUiState,
    onEvent: (AppEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mixer", style = MaterialTheme.typography.headlineSmall)
        Text("Up to 5 layers. Smooth controls for overnight stability.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onEvent(AppEvent.RandomizeMixSafely) }) {
                Text("Safe Randomizer")
            }
            OutlinedButton(onClick = { onEvent(AppEvent.AddLayer(LayerType.WIND)) }) {
                Text("Add Wind")
            }
            OutlinedButton(onClick = { onEvent(AppEvent.AddLayer(LayerType.RAIN)) }) {
                Text("Add Rain")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onEvent(AppEvent.AddLayer(LayerType.BROWN_NOISE)) }) {
                Text("Add Brown")
            }
            OutlinedButton(onClick = { onEvent(AppEvent.AddLayer(LayerType.TONAL_PAD)) }) {
                Text("Add Pad")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.currentMix.layers, key = { it.id }) { layer ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Layer ${layer.id}: ${layer.type.name}")
                        Text("Volume ${(layer.volume * 100).roundToInt()}%")
                        Slider(
                            value = layer.volume,
                            onValueChange = { onEvent(AppEvent.UpdateLayerVolume(layer.id, it)) }
                        )
                        Text("Filter ${(layer.filterCutoff * 100).roundToInt()}%")
                        Slider(
                            value = layer.filterCutoff,
                            onValueChange = { onEvent(AppEvent.UpdateLayerFilter(layer.id, it)) }
                        )
                        Text("Modulation ${(layer.modulationRateHz * 10f).roundToInt() / 10f} Hz")
                        Slider(
                            value = layer.modulationRateHz,
                            valueRange = 0.01f..1f,
                            onValueChange = { onEvent(AppEvent.UpdateLayerModulation(layer.id, it)) }
                        )
                        OutlinedButton(onClick = { onEvent(AppEvent.RemoveLayer(layer.id)) }) {
                            Text("Remove Layer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    uiState: AppUiState,
    onEvent: (AppEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Library", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(uiState.presets, key = { it.id }) { preset ->
            Card(onClick = { onEvent(AppEvent.SelectPreset(preset.id)) }) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(preset.name, fontWeight = FontWeight.Medium)
                        OutlinedButton(
                            onClick = { onEvent(AppEvent.ToggleFavorite(preset.id, !preset.favorite)) }
                        ) {
                            Text(if (preset.favorite) "Saved" else "Save")
                        }
                    }
                    Text(preset.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TimerScreen(
    uiState: AppUiState,
    onEvent: (AppEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sleep Timer", style = MaterialTheme.typography.headlineSmall)
        Text("Fade-ready timer presets for nap and overnight sessions.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onEvent(AppEvent.StartTimer(uiState.napMinutes)) }) {
                Text("Nap ${uiState.napMinutes}m")
            }
            Button(onClick = { onEvent(AppEvent.StartTimer(uiState.sleepMinutes)) }) {
                Text("Sleep ${uiState.sleepMinutes}m")
            }
            Button(onClick = { onEvent(AppEvent.StartTimer(uiState.deepSleepMinutes)) }) {
                Text("Deep ${uiState.deepSleepMinutes}m")
            }
        }
        val remaining = uiState.timerSecondsRemaining
        if (remaining != null) {
            Text("Remaining: ${formatDuration(remaining)}")
            OutlinedButton(onClick = { onEvent(AppEvent.CancelTimer) }) {
                Text("Cancel timer")
            }
        } else {
            Text("No active timer")
        }
    }
}

@Composable
private fun SettingsScreen(
    uiState: AppUiState,
    onEvent: (AppEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Offline only. Reliability first defaults.")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dim controls in Sleep Mode")
            Switch(
                checked = uiState.sleepModeDimEnabled,
                onCheckedChange = { onEvent(AppEvent.SetSleepModeDim(it)) }
            )
        }
        HorizontalDivider()
        Text("Current interruption status:")
        val interruption = if (uiState.timerSecondsRemaining == null) {
            "No active timer"
        } else {
            "Timer running"
        }
        Text(interruption, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SleepModeScreen(
    uiState: AppUiState,
    playbackState: PlaybackState,
    onEvent: (AppEvent) -> Unit
) {
    val alpha = if (uiState.sleepModeDimEnabled) 0.94f else 0.82f
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = alpha))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Sleep Mode", style = MaterialTheme.typography.headlineMedium)
                Text(if (playbackState.isPlaying) "Playing" else "Paused")
                uiState.timerSecondsRemaining?.let {
                    Text("Timer ${formatDuration(it)}")
                }
                Button(onClick = { onEvent(AppEvent.TogglePlayback) }) {
                    Text(if (playbackState.isPlaying) "Pause" else "Play")
                }
                OutlinedButton(onClick = { onEvent(AppEvent.ExitSleepMode) }) {
                    Text("Exit")
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
