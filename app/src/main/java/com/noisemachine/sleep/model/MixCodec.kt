package com.noisemachine.sleep.model

object MixCodec {
    fun encode(mixState: MixState): String {
        val layerPart = mixState.layers.joinToString(";") { layer ->
            listOf(
                layer.id.toString(),
                layer.type.name,
                layer.volume.toString(),
                layer.filterCutoff.toString(),
                layer.modulationRateHz.toString(),
                layer.enabled.toString()
            ).joinToString(",")
        }
        return "${mixState.masterVolume}|$layerPart"
    }

    fun decode(raw: String): MixState {
        if (raw.isBlank()) return MixState.empty
        val chunks = raw.split("|", limit = 2)
        val master = chunks.firstOrNull()?.toFloatOrNull() ?: 0.7f
        val layersChunk = chunks.getOrNull(1).orEmpty()
        if (layersChunk.isBlank()) return MixState(layers = emptyList(), masterVolume = master)
        val layers = layersChunk.split(";")
            .mapNotNull { row ->
                val cols = row.split(",")
                if (cols.size != 6) return@mapNotNull null
                SoundLayer(
                    id = cols[0].toIntOrNull() ?: return@mapNotNull null,
                    type = runCatching { LayerType.valueOf(cols[1]) }.getOrNull() ?: return@mapNotNull null,
                    volume = cols[2].toFloatOrNull() ?: 0.5f,
                    filterCutoff = cols[3].toFloatOrNull() ?: 0.5f,
                    modulationRateHz = cols[4].toFloatOrNull() ?: 0.2f,
                    enabled = cols[5].toBooleanStrictOrNull() ?: true
                )
            }
            .take(5)
        return MixState(layers = layers, masterVolume = master)
    }
}
