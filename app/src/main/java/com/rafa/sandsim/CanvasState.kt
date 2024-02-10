package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

data class CanvasState(
    val pixels: List<PixelData> = emptyList(),
    val pixelsOnFinalPosition: List<PixelData> = emptyList(),
    val notCollidablePixels: List<PixelData> = emptyList(),
    val size: Size = Size(1f, 1f),
    val aimPosition: Offset = Offset(0f, 0f),
    val pixelSize: Float = 10f,
    val fps: Float = 0f,
    val isSimulationPaused: Boolean = false,
    val isInitialized: Boolean = false,
    var useDebugColors: Boolean = false,

    ) {

    fun getCurrentPixels() = getAllPixels().size
    fun getAllPixels() = pixelsOnFinalPosition + pixels

    fun canCollideWithOtherPixels(newPosition: Offset): Boolean {
        val hasPixelOnTop = hasPixelOnTop(newPosition)
        return !(hasPixelOnTop(newPosition) && hasPixelOnTopLeft(newPosition) && hasPixelOnTopRight(
            newPosition
        ) || hasWallOnTheRight(newPosition) && hasPixelOnTop || hasWallOnTheLeft(newPosition) && hasPixelOnTop)
    }

    fun isBellowAPixelOnFinalPosition(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == pixelPosition.x && it.position.y < pixelPosition.y }


    fun hasPixelOnTopRight(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x + pixelSize) && it.position.y == pixelPosition.y - pixelSize }

    fun hasPixelOnTopLeft(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x - pixelSize) && it.position.y == pixelPosition.y - pixelSize }

    fun hasPixelOnTheLeft(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x - pixelSize) && it.position.y == pixelPosition.y }

    fun hasPixelOnTheRight(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x + pixelSize) && it.position.y == pixelPosition.y }

    fun hasPixelOnTop(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == pixelPosition.x && it.position.y == pixelPosition.y - pixelSize }

    fun getHasPixelOnTheRightBottom(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == pixelPosition.x + pixelSize && it.position.y == pixelPosition.y + pixelSize }

    fun hasPixelOnTheLeftBottom(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == pixelPosition.x - pixelSize && it.position.y == pixelPosition.y + pixelSize }

    fun hasReachedTheGround(pixel: PixelData) =
        (pixel.position.y + pixelSize) >= (size.height - pixelSize)

    fun hasCollisionBellow(pixelPosition: Offset): Boolean {
        return getCollidingPixel(pixelPosition) != null
    }

    fun getCollidingPixel(pixelPosition: Offset): PixelData? {
        return pixelsOnFinalPosition.firstOrNull { it.position.x == pixelPosition.x && it.position.y == pixelPosition.y + pixelSize }
    }

    fun hasWallOnTheRight(position: Offset) = position.x >= size.width

    fun hasWallOnTheLeft(position: Offset) = position.x - pixelSize < 0
}