package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

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
    fun getAllPixels() = pixels + pixelsOnFinalPosition + notCollidablePixels
}

class SandSimViewModel : ViewModel() {
    private val _state = MutableStateFlow(CanvasState())
    val state = _state.asStateFlow()

    private var updateJob: Job? = null

    private fun update() {
        var lastGeneratedPixelTime = Instant.now().toEpochMilli()
        updateJob = viewModelScope.launch {
            while (true) {
                if (getState().isSimulationPaused) {
                    delay(1000)
                    continue
                }
                val startTime = Instant.now().toEpochMilli()
                val pixels = getState().pixels.toMutableList()
                val pixelsOnFinalPosition: MutableList<PixelData> =
                    getState().pixelsOnFinalPosition.toMutableList()
                if ((Instant.now().toEpochMilli() - lastGeneratedPixelTime) > 75) {
                    val shouldDrop = getState().aimPosition.x % getState().pixelSize == 0f
                    if (shouldDrop) {
                        val newPixel = PixelData(
                            position = Offset(
                                x = getState().aimPosition.x, y = getState().aimPosition.y
                            ),
                        )
                        if (pixels.none { it.position == newPixel.position }) {
                            pixels.add(newPixel)
                        }
                        lastGeneratedPixelTime = Instant.now().toEpochMilli()
                    }
                }

                pixels.forEach { pixel ->
                    var hasReachFinalPosition = false

                    val newPosition = when {
                        hasCollisionBellow(pixel.position) -> {
                            val nextXPosition = slipSideways(pixel.position)
                            if (nextXPosition != null) {
                                Offset(pixel.position.x + nextXPosition, pixel.position.y)

                            } else {
                                hasReachFinalPosition = true
                                Offset(pixel.position.x, pixel.position.y)
                            }
                        }

                        hasReachedTheGround(pixel) -> {
                            hasReachFinalPosition = true
                            Offset(pixel.position.x, getState().size.height - getState().pixelSize)
                        }

                        else -> {
                            Offset(pixel.position.x, pixel.position.y + 1)
                        }
                    }

                    val updatedPixel = pixel.copy(
                        position = newPosition,
                        hasReachFinalPosition = hasReachFinalPosition,
                    )
                    pixels[pixels.indexOf(pixel)] = updatedPixel
                }
                delay(1)
                updatePixelsOnState(pixelsOnFinalPosition, pixels, startTime)
            }
        }
    }

    private fun updatePixelsOnState(
        pixelsOnFinalPosition: MutableList<PixelData>,
        pixels: MutableList<PixelData>,
        startTime: Long
    ) {
        pixelsOnFinalPosition.addAll(pixels.filter { it.hasReachFinalPosition })
        val nonCollidable = pixelsOnFinalPosition.filter { !canCollideWithOtherPixels(it.position) }
        val updatedNonCollidablePixels =
            getState().notCollidablePixels + nonCollidable.map { it.copy(canCollideWIthOtherPixels = false) }
                .distinct()
        val updatedPixelsOnLastPosition =
            pixelsOnFinalPosition.distinct().toMutableList().apply { removeAll(nonCollidable) }
        val updatedPixels = pixels.filter { !it.hasReachFinalPosition }.toList().distinct()
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

        return when {
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
        return !(hasPixelOnTop(newPosition) && hasPixelOnTopLeft(newPosition) && hasPixelOnTopRight(
            newPosition
        ))
    }

    private fun hasPixelOnTopRight(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x + getState().pixelSize) && it.position.y == pixelPosition.y - getState().pixelSize }

    private fun hasPixelOnTopLeft(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == (pixelPosition.x - getState().pixelSize) && it.position.y == pixelPosition.y - getState().pixelSize }

    private fun hasPixelOnTop(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x && it.position.y == pixelPosition.y - getState().pixelSize }

    private fun getHasPixelOnTheRightSide(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x + getState().pixelSize }

    private fun hasPixelOnTheLeftSide(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x - getState().pixelSize }

    private fun getHasPixelOnTheRightBottom(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x + getState().pixelSize && it.position.y == pixelPosition.y + getState().pixelSize }

    private fun hasPixelOnTheLeftBottom(
        pixelPosition: Offset
    ) =
        getState().pixelsOnFinalPosition.any { it.position.x == pixelPosition.x - getState().pixelSize && it.position.y == pixelPosition.y + getState().pixelSize }

    private fun hasReachedTheGround(pixel: PixelData) =
        (pixel.position.y + 1) >= (getState().size.height - getState().pixelSize)

    private fun getCurrentFps(startTime: Long): Float {
        val frameTimeS = System.currentTimeMillis() - startTime
        val fps = 1 / frameTimeS.toFloat()
        return round(fps * 1000)
    }

    fun initializePixels(size: Size, center: Offset) {
        if (getState().size.width == 0f) {
            val newState = CanvasState(
                size = size, aimPosition = center
            )
            updateState(newState)
            update()
        }
    }

    private fun hasCollisionBellow(pixelPosition: Offset): Boolean {
        return getCollidingPixel(pixelPosition) != null
    }

    private fun getCollidingPixel(pixelPosition: Offset) =
        getState().pixelsOnFinalPosition.firstOrNull {
            val nextPixelYPosition = pixelPosition.y + getState().pixelSize
            it.position.y <= nextPixelYPosition && (it.position.x - pixelPosition.x).absoluteValue < getState().pixelSize
        }

    //540 646
    fun updateDragValues(offsetX: Float, offsetY: Float) {
        val isLeftDrag = offsetX < 0
        val xValueToAdd = if (isLeftDrag) (getState().pixelSize * -1f) else getState().pixelSize
        val currentState = getState()
        val newXPosition =
            (currentState.aimPosition.x + xValueToAdd + offsetX).roundToInt().toFloat()
        val newYPosition = currentState.aimPosition.y.roundToInt() + offsetY
        if (newXPosition > currentState.size.width * currentState.pixelSize || newXPosition < 0) return
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