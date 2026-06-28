package com.noisemachine.sleep.data.repo

import com.noisemachine.sleep.model.LayerType
import com.noisemachine.sleep.model.MixState
import com.noisemachine.sleep.model.Preset
import com.noisemachine.sleep.model.SoundLayer

object DefaultPresets {
    fun build(): List<Preset> = listOf(
        preset(1, "Dark Forest", "Wind and rain bed", LayerType.WIND, LayerType.RAIN),
        preset(2, "Cabin Rain", "Heavy rain with warm hum", LayerType.RAIN, LayerType.HUM),
        preset(3, "Quiet Creek", "Soft texture and low stream", LayerType.TEXTURE, LayerType.WIND),
        preset(4, "Night Hum", "Mechanical blanket for city noise", LayerType.HUM, LayerType.BROWN_NOISE),
        preset(5, "Soft Brown", "Stable low-bias sleep base", LayerType.BROWN_NOISE),
        preset(6, "Forest Canopy", "Gentle leaves and distant rain", LayerType.WIND, LayerType.RAIN, LayerType.TEXTURE),
        preset(7, "Deep Shelter", "Subtle hum plus tonal pad", LayerType.HUM, LayerType.TONAL_PAD),
        preset(8, "Rain Tent", "Bright rain with muffled low end", LayerType.RAIN, LayerType.WHITE_NOISE),
        preset(9, "Hollow Wind", "Wide wind field for immersion", LayerType.WIND),
        preset(10, "Machine Calm", "Reliable mask for apartment noise", LayerType.HUM, LayerType.WHITE_NOISE),
        preset(11, "Warm Drift", "Pad-forward drift with textures", LayerType.TONAL_PAD, LayerType.TEXTURE),
        preset(12, "Deep Sleep", "Brown base with soft rain and pad", LayerType.BROWN_NOISE, LayerType.RAIN, LayerType.TONAL_PAD)
    )

    private fun preset(
        id: Long,
        name: String,
        description: String,
        vararg types: LayerType
    ): Preset {
        val layers = types.take(5).mapIndexed { index, type ->
            SoundLayer(
                id = index + 1,
                type = type,
                volume = 0.35f + (index * 0.1f),
                filterCutoff = 0.5f,
                modulationRateHz = 0.05f + (index * 0.04f),
                enabled = true
            )
        }
        return Preset(
            id = id,
            name = name,
            description = description,
            mix = MixState(layers = layers, masterVolume = 0.72f),
            favorite = false
        )
    }
}
