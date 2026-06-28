package com.noisemachine.sleep.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import com.noisemachine.sleep.MainActivity
import com.noisemachine.sleep.R
import com.noisemachine.sleep.audio.ProceduralAudioEngine
import com.noisemachine.sleep.model.MixCodec
import com.noisemachine.sleep.model.MixState

class SleepPlaybackService : Service() {
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var mediaSession: MediaSessionCompat
    private val engine = ProceduralAudioEngine()
    private var currentMix: MixState = MixState.empty

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                if (focusChange <= AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stopPlayback("Audio focus lost")
                }
            }
            .build()
        mediaSession = MediaSessionCompat(this, "SleepPlaybackSession").apply {
            isActive = true
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val mix = MixCodec.decode(intent.getStringExtra(EXTRA_MIX).orEmpty())
                startPlayback(mix)
            }

            ACTION_UPDATE -> {
                val mix = MixCodec.decode(intent.getStringExtra(EXTRA_MIX).orEmpty())
                currentMix = mix
                engine.updateMix(mix)
            }

            ACTION_STOP -> stopPlayback(null)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlayback(null)
        mediaSession.release()
        engine.release()
        super.onDestroy()
    }

    private fun startPlayback(mix: MixState) {
        currentMix = mix
        if (audioManager.requestAudioFocus(audioFocusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            PlaybackStateStore.update(PlaybackState(isPlaying = false, interruptionReason = "Audio focus denied"))
            return
        }
        engine.start(mix)
        startForeground(NOTIFICATION_ID, buildNotification())
        PlaybackStateStore.update(PlaybackState(isPlaying = true))
    }

    private fun stopPlayback(reason: String?) {
        engine.stop()
        runCatching { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
        PlaybackStateStore.update(PlaybackState(isPlaying = false, interruptionReason = reason))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val launchPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            1002,
            Intent(this, SleepPlaybackService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sleep_notification_title))
            .setContentText(getString(R.string.sleep_notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(launchPendingIntent)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "Stop",
                    stopPendingIntent
                )
            )
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sleep Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Persistent playback controls"
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_PLAY = "com.noisemachine.sleep.action.PLAY"
        const val ACTION_UPDATE = "com.noisemachine.sleep.action.UPDATE"
        const val ACTION_STOP = "com.noisemachine.sleep.action.STOP"
        const val EXTRA_MIX = "extra_mix"
        private const val CHANNEL_ID = "sleep_playback_channel"
        private const val NOTIFICATION_ID = 404
    }
}
