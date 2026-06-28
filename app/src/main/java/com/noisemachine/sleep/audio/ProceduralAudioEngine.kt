package com.noisemachine.sleep.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.noisemachine.sleep.model.LayerType
import com.noisemachine.sleep.model.MixState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ProceduralAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var renderJob: Job? = null
    private var track: AudioTrack? = null
    private val random = Random(System.currentTimeMillis())

    @Volatile
    private var mixState: MixState = MixState.empty

    fun start(initialMix: MixState) {
        mixState = initialMix
        if (renderJob?.isActive == true) return

        val sampleRate = 48_000
        val channelMask = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, audioFormat).coerceAtLeast(2048)

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(audioFormat)
                .setChannelMask(channelMask)
                .build(),
            minSize,
            AudioTrack.MODE_STREAM,
            AudioTrack.AUDIO_SESSION_ID_GENERATE
        )

        track = audioTrack
        audioTrack.play()
        renderJob = scope.launch {
            val bufferSize = 1024
            val buffer = ShortArray(bufferSize)
            val phases = mutableMapOf<Int, Double>()
            var currentMaster = mixState.masterVolume.coerceIn(0f, 1f)

            while (isActive) {
                val currentMix = mixState
                val targetMaster = currentMix.masterVolume.coerceIn(0f, 1f)
                currentMaster += (targetMaster - currentMaster) * 0.01f

                repeat(bufferSize) { i ->
                    var sample = 0f
                    currentMix.layers.take(5).forEach { layer ->
                        if (!layer.enabled) return@forEach
                        val raw = generateLayerSample(
                            layer.type,
                            phases,
                            layer.id,
                            sampleRate,
                            layer.modulationRateHz.coerceAtLeast(0.01f)
                        )
                        sample += raw * layer.volume.coerceIn(0f, 1f)
                    }
                    val mixed = (sample * currentMaster).coerceIn(-0.99f, 0.99f)
                    buffer[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
                }
                audioTrack.write(buffer, 0, buffer.size)
            }
        }
    }

    fun updateMix(newMixState: MixState) {
        mixState = newMixState
    }

    fun stop() {
        renderJob?.cancel()
        renderJob = null
        track?.runCatching {
            pause()
            flush()
            stop()
            release()
        }
        track = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun generateLayerSample(
        type: LayerType,
        phases: MutableMap<Int, Double>,
        id: Int,
        sampleRate: Int,
        modulationRateHz: Float
    ): Float {
        val phase = phases[id] ?: 0.0
        val increment = (2.0 * PI * modulationRateHz) / sampleRate.toDouble()
        val nextPhase = (phase + increment) % (2.0 * PI)
        phases[id] = nextPhase

        return when (type) {
            LayerType.WHITE_NOISE -> random.nextFloat() * 2f - 1f
            LayerType.BROWN_NOISE -> ((random.nextFloat() * 2f - 1f) * 0.02f + sin(nextPhase) * 0.2f).coerceIn(-1f, 1f)
            LayerType.WIND -> (sin(nextPhase * 0.5) * 0.4f + (random.nextFloat() * 2f - 1f) * 0.1f)
            LayerType.RAIN -> (random.nextFloat() * 2f - 1f) * (0.3f + (sin(nextPhase).toFloat() + 1f) * 0.1f)
            LayerType.HUM -> sin(nextPhase * 2.0).toFloat() * 0.35f
            LayerType.TONAL_PAD -> (sin(nextPhase).toFloat() + sin(nextPhase * 0.5).toFloat()) * 0.2f
            LayerType.TEXTURE -> (sin(nextPhase * 3.2).toFloat() * 0.25f) + (random.nextFloat() * 2f - 1f) * 0.08f
        }
    }
}
