package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class PixelData(
    val position: Offset,
    val hasReachFinalPosition: Boolean = false,
    val canCollideWIthOtherPixels: Boolean = true
) {
    fun getColor(useDebugColor: Boolean = false): Color {
        return if (useDebugColor) {
            Color.Green
        } else if (canCollideWIthOtherPixels && hasReachFinalPosition) {
            Color.Transparent
        } else if (hasReachFinalPosition) {
            Color.Red
        } else {
            Color.Yellow
        }
    }
}

