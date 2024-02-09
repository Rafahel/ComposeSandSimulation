package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastDistinctBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Random
import kotlin.math.absoluteValue
import kotlin.math.round

data class PixelData(
    val position: Offset,
    val hasReachFinalPosition: Boolean = false,
    val canCollideWIthOtherPixels: Boolean = true
) {
    val color = if (canCollideWIthOtherPixels && hasReachFinalPosition) {
        Color.Green
    } else if (hasReachFinalPosition) {
        Color.Red
    } else {
        Color.Yellow
    }
}

data class LineData(
    val lineStartPosition: Offset, val lineEndPosition: Offset
)

data class CanvasState(
    val pixels: List<PixelData> = emptyList(),
    val pixelsOnFinalPosition: List<PixelData> = emptyList(),
    val notCollidablePixels: List<PixelData> = emptyList(),
    val size: Size = Size(0f, 0f),
    val aimPosition: Offset = Offset(0f, 0f),
    val pixelSize: Float = 10f,
    val lines: List<LineData> = emptyList(),
    val fps: Float = 0f,
    val isSimulationPaused: Boolean = false
) {
    fun getCurrentPixels() = getAllPixels().size
    fun getAllPixels() = notCollidablePixels + pixelsOnFinalPosition + pixels
}

class SandSimViewModel : ViewModel() {
    private val _state = MutableStateFlow(CanvasState())
    val state = _state.asStateFlow()

    private var updateJob: Job? = null

    private var lastGeneratedPixelTime = Instant.now().toEpochMilli()

    suspend fun update() {
        if (getState().isSimulationPaused) {
            return
        }
        val startTime = Instant.now().toEpochMilli()
        var pixels = getState().pixels.toMutableList()
        val pixelsOnFinalPosition: MutableList<PixelData> =
            getState().pixelsOnFinalPosition.toMutableList()
        if ((Instant.now().toEpochMilli() - lastGeneratedPixelTime) > 75) {
            val shouldDrop = getState().aimPosition.x % getState().pixelSize == 0f
            if (shouldDrop) {
                val pixelSize = getState().pixelSize
                val newPixelsList = generatePixels()

                val newPixel = PixelData(
                    position = Offset(
                        x = getState().aimPosition.x, y = getState().aimPosition.y
                    )
                )

                pixels.add(newPixel)
                lastGeneratedPixelTime = Instant.now().toEpochMilli()
            }
        }

        pixels = pixels.distinct().toMutableList()

        pixels.forEach { pixel ->
            val updatedPixel = updatePixelPosition(pixel)
            pixels[pixels.indexOf(pixel)] = updatedPixel
        }
        updatePixelsOnState(pixelsOnFinalPosition, pixels, startTime)
    }

    private fun generatePixels(): List<PixelData> {
        val possiblePositions = mutableListOf<PixelData>()
        val currentPosition = getState().aimPosition
        var currentXPosition = currentPosition.x
        var currentYPosition = currentPosition.y
        var highestPosition = -1f
        var currentlyFallingPixels = emptyList<PixelData>()

        for (i in 0 until 16) {
            if (getState().pixels.size >= 5) {
                currentlyFallingPixels = getState().pixels.takeLast(5)
                highestPosition =
                    currentlyFallingPixels.map { it.position }.sortedBy { it.y }.first().y
            }
            if (i != 0 && i % 4 == 0) {
                currentYPosition += getState().pixelSize
                currentXPosition = getState().aimPosition.x
            }
            val newPixel = (PixelData(
                position = Offset(
                    currentXPosition, currentYPosition
                )
            ))
            possiblePositions.add(newPixel)

            currentXPosition += getState().pixelSize
        }


        return possiblePositions.shuffled(Random(Instant.now().toEpochMilli())).take(5)
    }

    private fun createPixels(pixelSize: Float, pixelQuantity: Int = 5): List<PixelData> {
        return listOf(
            // Center pixel
            PixelData(
                position = Offset(
                    x = getState().aimPosition.x, y = getState().aimPosition.y
                )
            ),
//            PixelData(
//                position = Offset(
//                    x = getState().aimPosition.x + pixelSize,
//                    y = getState().aimPosition.y
//                )
//            ),
//            // Right * 2 pixel
//            PixelData(
//                position = Offset(
//                    x = getState().aimPosition.x + (pixelSize * 2),
//                    y = getState().aimPosition.y
//                )
//            ),
//            PixelData(
//                position = Offset(
//                    x = getState().aimPosition.x - pixelSize,
//                    y = getState().aimPosition.y
//                )
//            ),
//            PixelData(
//                position = Offset(
//                    x = getState().aimPosition.x - (pixelSize * 2),
//                    y = getState().aimPosition.y
//                )
//            ),
//            PixelData(
//                position = Offset(
//                    x = getState().aimPosition.x, y = getState().aimPosition.y + (pixelSize * 2)
//                )
//            ),
//            PixelData(
//                position = Offset(
//                    x = getState().aimPosition.x, y = getState().aimPosition.y - (pixelSize * 2)
//                )
//            )
        ).shuffled(Random(Instant.now().toEpochMilli())).take(1)


    }

    private fun updatePixelPosition(pixel: PixelData): PixelData {
        return when {
            hasCollisionBellow(pixel.position) -> {


                val nextXPosition = slipSideways(pixel.position)
                if (nextXPosition != null) {
                    pixel.copy(
                        position = Offset(pixel.position.x + nextXPosition, pixel.position.y)
                    )
                } else {
                    pixel.copy(
                        position = Offset(pixel.position.x, pixel.position.y),
                        hasReachFinalPosition = true
                    )
                }
            }

            hasReachedTheGround(pixel) -> {
                pixel.copy(
                    position = Offset(
                        pixel.position.x, getState().size.height - getState().pixelSize
                    ), hasReachFinalPosition = true

                )

            }

            else -> {
                pixel.copy(
                    position = Offset(pixel.position.x, pixel.position.y + getState().pixelSize),
                    hasReachFinalPosition = false

                )

            }
        }
    }

    private fun hasWallOnTheRight(position: Offset) =
        position.x + getState().pixelSize >= getState().size.width

    private fun hasWallOnTheLeft(position: Offset) =
        position.x - getState().pixelSize <= getState().pixelSize

    private fun updatePixelsOnState(
        pixelsOnFinalPosition: MutableList<PixelData>,
        pixels: MutableList<PixelData>,
        startTime: Long
    ) {
        pixelsOnFinalPosition.addAll(pixels.filter { it.hasReachFinalPosition })
        val nonCollidable = pixelsOnFinalPosition.filter { !canCollideWithOtherPixels(it.position) }
        val updatedNonCollidablePixels =
            (getState().notCollidablePixels + nonCollidable.map { it.copy(canCollideWIthOtherPixels = false) }).fastDistinctBy { it.position }
        val updatedPixelsOnLastPosition =
            pixelsOnFinalPosition.fastDistinctBy { it.position }.toMutableList()
                .apply { removeAll(nonCollidable) }
        val updatedPixels =
            pixels.filter { !it.hasReachFinalPosition }.toList().fastDistinctBy { it.position }
        val newState = getState().copy(
            pixels = updatedPixels,
            pixelsOnFinalPosition = updatedPixelsOnLastPosition,
            notCollidablePixels = updatedNonCollidablePixels,
            fps = getCurrentFps(startTime)
        )
        updateState(newState)
    }

    private fun slipSideways(pixelPosition: Offset): Float? {
        val collidingPixel = getCollidingPixel(pixelPosition)!!
        val collidingPixelPosition = collidingPixel.position
        val hasPixelOnTheLeftBottom = hasPixelOnTheLeftBottom(pixelPosition)
        val hasPixelOnTheRightBottom = getHasPixelOnTheRightBottom(pixelPosition)
        val random = Random(System.nanoTime())
        val shouldGoToRandomXPosition =
            pixelPosition.x == collidingPixelPosition.x && !hasPixelOnTheLeftBottom && !hasPixelOnTheRightBottom
        val hasPixelOnBottom = hasCollisionBellow(pixelPosition)
        val hasPixelOnTopRight = hasPixelOnTopRight(pixelPosition)
        val hasWallOnTheRight = hasWallOnTheRight(pixelPosition)
        val hasWallOnTheLeft = hasWallOnTheLeft(pixelPosition)
        val hasPixelOnTheLeft = hasPixelOnTheLeft(pixelPosition)
        val hasPixelOnTheRight = hasPixelOnTheRight(pixelPosition)
        return when {
            hasWallOnTheLeft && hasPixelOnBottom && !hasPixelOnTheRight -> +getState().pixelSize
            hasWallOnTheRight && hasPixelOnBottom && !hasPixelOnTheLeft -> -getState().pixelSize
            hasWallOnTheLeft -> null
            hasWallOnTheRight -> null
            hasPixelOnTopRight -> null
            shouldGoToRandomXPosition -> {
                if (random.nextBoolean()) {
                    getState().pixelSize
                } else {
                    -getState().pixelSize
                }
            }

            hasPixelOnTheRightBottom && hasPixelOnTheLeftBottom -> {
                null
            }

            !hasPixelOnTheLeftBottom -> -getState().pixelSize
            else -> getState().pixelSize
        }
    }

    private fun canCollideWithOtherPixels(newPosition: Offset): Boolean {
        val hasPixelOnTop = hasPixelOnTop(newPosition)
        return !(hasPixelOnTop(newPosition) && hasPixelOnTopLeft(newPosition) && hasPixelOnTopRight(
            newPosition
        ) || hasWallOnTheRight(newPosition) && hasPixelOnTop || hasWallOnTheLeft(newPosition) && hasPixelOnTop)
    }

    private fun hasPixelOnTopRight(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x + getState().pixelSize) && it.position.y == pixelPosition.y - getState().pixelSize }

    private fun hasPixelOnTopLeft(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x - getState().pixelSize) && it.position.y == pixelPosition.y - getState().pixelSize }

    private fun hasPixelOnTheLeft(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x - getState().pixelSize) && it.position.y == pixelPosition.y }

    private fun hasPixelOnTheRight(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x + getState().pixelSize) && it.position.y == pixelPosition.y }

    private fun hasPixelOnTop(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x && it.position.y == pixelPosition.y - getState().pixelSize }

    private fun getHasPixelOnTheRightBottom(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x + getState().pixelSize && it.position.y == pixelPosition.y + getState().pixelSize }

    private fun hasPixelOnTheLeftBottom(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x - getState().pixelSize && it.position.y == pixelPosition.y + getState().pixelSize }

    private fun hasReachedTheGround(pixel: PixelData) =
        (pixel.position.y + getState().pixelSize) >= (getState().size.height - getState().pixelSize)

    private fun getCurrentFps(startTime: Long): Float {
        val frameTimeS = System.currentTimeMillis() - startTime
        val fps = 1 / frameTimeS.toFloat()
        return round(fps * 1000)
    }

    fun initializePixels(size: Size, center: Offset) {
        if (getState().size.width == 0f) {
            val newState = CanvasState(
                size = size, aimPosition = Offset(x = center.x, y = 0f)
            )
            updateState(newState)
        }
    }

    private fun hasCollisionBellow(pixelPosition: Offset): Boolean {
        return getCollidingPixel(pixelPosition) != null
    }

    private fun getCollidingPixel(pixelPosition: Offset): PixelData? {
        val pixelsOnFinal = getState().pixelsOnFinalPosition
        return pixelsOnFinal.firstOrNull { it.position.x == pixelPosition.x && it.position.y == pixelPosition.y + getState().pixelSize }
    }

    //540 646
    fun updateDragValues(offsetX: Float, offsetY: Float) {
        val isDraggingLeft = offsetX < 0
        val isDraggingUp = offsetY < 0
        val currentState = getState()
        val currentXPosition = currentState.aimPosition.x
        val newXPosition =
            if (isDraggingLeft) currentXPosition - getState().pixelSize else currentXPosition + getState().pixelSize
        val newYPosition =
            if (isDraggingUp) currentState.aimPosition.y - getState().pixelSize else currentState.aimPosition.y + getState().pixelSize
        if (newXPosition + getState().pixelSize > currentState.size.width || newXPosition < 0f) return
        if (newYPosition > currentState.size.height * currentState.pixelSize || newYPosition < 0) return
        val newState = currentState.copy(aimPosition = Offset(newXPosition, newYPosition))
        updateState(newState)
    }

    private fun updateState(newState: CanvasState) {
        _state.update { newState }
    }

    private fun getState() = _state.value
    fun resetCanvas() {
        updateJob?.cancel()
        val newState = CanvasState()
        updateState(newState)
    }

    fun pauseSimulation() {
        val newState = getState().copy(isSimulationPaused = !getState().isSimulationPaused)
        updateState(newState)
    }
}