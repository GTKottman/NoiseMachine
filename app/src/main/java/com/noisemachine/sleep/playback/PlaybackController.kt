package com.noisemachine.sleep.playback

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.noisemachine.sleep.model.MixCodec
import com.noisemachine.sleep.model.MixState

class PlaybackController(
    private val appContext: Context
) {
    fun play(mixState: MixState) {
        dispatch(
            action = SleepPlaybackService.ACTION_PLAY,
            mixState = mixState
        )
    }

    fun update(mixState: MixState) {
        dispatch(
            action = SleepPlaybackService.ACTION_UPDATE,
            mixState = mixState
        )
    }

    fun stop() {
        dispatch(action = SleepPlaybackService.ACTION_STOP)
    }

    private fun dispatch(action: String, mixState: MixState? = null) {
        val intent = Intent(appContext, SleepPlaybackService::class.java).apply {
            this.action = action
            if (mixState != null) {
                putExtra(SleepPlaybackService.EXTRA_MIX, MixCodec.encode(mixState))
            }
        }
        ContextCompat.startForegroundService(appContext, intent)
    }
}
