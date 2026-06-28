package com.noisemachine.sleep.model

data class Preset(
    val id: Long,
    val name: String,
    val description: String,
    val mix: MixState,
    val favorite: Boolean,
    val lastUsedEpochMs: Long? = null
)
