package com.noisemachine.sleep.model

data class MixState(
    val layers: List<SoundLayer>,
    val masterVolume: Float = 0.7f
) {
    companion object {
        val empty = MixState(layers = emptyList(), masterVolume = 0.7f)
    }
}
