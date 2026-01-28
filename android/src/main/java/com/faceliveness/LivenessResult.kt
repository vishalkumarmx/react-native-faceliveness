package com.faceliveness

internal data class LivenessResult(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val confidence: Float = 0f,
    val timeMs: Long = 0,
    val hasFace: Boolean = false
)
