package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.util.TreeSet

object PixelDataComparator : Comparator<PixelData> {
    override fun compare(p1: PixelData, p2: PixelData): Int {
        val xComparison = p1.position.x.compareTo(p2.position.x)
        return if (xComparison != 0) {
            xComparison
        } else {
            p1.position.y.compareTo(p2.position.y)
        }
    }
}

data class CanvasState(
    val pixels: TreeSet<PixelData> = TreeSet(PixelDataComparator),
    val pixelsOnFinalPosition: TreeSet<PixelData> = TreeSet(PixelDataComparator),
    val notCollidablePixels: List<PixelData> = emptyList(),
    val size: Size = Size(1f, 1f),
    val pixelSize: Float = 10f,
    val aimPosition: Offset = Offset(0f, pixelSize * 10),
    val updateTimeInMs: Long = 0L,
    val isSimulationPaused: Boolean = false,
    val isInitialized: Boolean = false,
    val useDebugColors: Boolean = false,
    val showDebugInfo: Boolean = false,
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
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x + pixelSize) && it.position.y <= pixelPosition.y - pixelSize }

    fun hasPixelOnTopLeft(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x - pixelSize) && it.position.y <= pixelPosition.y - pixelSize }

    fun hasPixelOnTheLeft(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x - pixelSize) && it.position.y <= pixelPosition.y }

    fun hasPixelOnTheRight(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x + pixelSize) && it.position.y <= pixelPosition.y }

    private fun hasPixelOnTop(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == pixelPosition.x && it.position.y <= pixelPosition.y - pixelSize }

    fun getHasPixelOnTheRightBottom(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == pixelPosition.x + pixelSize && it.position.y <= pixelPosition.y + pixelSize }

    fun hasPixelOnTheLeftBottom(
        pixelPosition: Offset
    ) =
        pixelsOnFinalPosition.any { it.position.x == pixelPosition.x - pixelSize && it.position.y <= pixelPosition.y + pixelSize }

    fun hasReachedTheGround(pixel: PixelData) =
        (pixel.position.y + pixelSize) >= (size.height - pixelSize)

    fun hasCollisionBellow(pixelPosition: Offset): Boolean {
        return getCollidingPixel(pixelPosition) != null
    }

    fun getCollidingPixel(pixelPosition: Offset): PixelData? {
        return pixelsOnFinalPosition.firstOrNull { it.position.x == pixelPosition.x && it.position.y <= pixelPosition.y + pixelSize }
    }

    fun hasWallOnTheRight(position: Offset) = position.x >= size.width

    fun hasWallOnTheLeft(position: Offset) = position.x - pixelSize < 0
}