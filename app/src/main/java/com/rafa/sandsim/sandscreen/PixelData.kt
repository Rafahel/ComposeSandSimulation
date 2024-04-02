package com.rafa.sandsim.sandscreen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.rafa.sandsim.ui.theme.topSandColor

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
            topSandColor
        }
    }
}

