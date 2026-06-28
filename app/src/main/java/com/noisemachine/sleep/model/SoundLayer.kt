package com.noisemachine.sleep.model

data class SoundLayer(
    val id: Int,
    val type: LayerType,
    val volume: Float,
    val filterCutoff: Float,
    val modulationRateHz: Float,
    val enabled: Boolean = true
)
