package com.noisemachine.sleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noisemachine.sleep.ui.AppViewModel
import com.noisemachine.sleep.ui.NoiseMachineRoot
import com.noisemachine.sleep.ui.theme.NoiseMachineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoiseMachineTheme {
                val app = LocalContext.current.applicationContext as SleepNoiseApplication
                val factory = remember(app) { AppViewModel.Factory(app) }
                val viewModel = viewModel<AppViewModel>(factory = factory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
                NoiseMachineRoot(
                    uiState = uiState,
                    playbackState = playbackState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}
