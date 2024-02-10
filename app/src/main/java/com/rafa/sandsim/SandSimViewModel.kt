package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastDistinctBy
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.Random

class SandSimViewModel : ViewModel() {
    private val _state = MutableStateFlow(CanvasState())
    val state = _state.asStateFlow()
    private var lastGeneratedPixelTime = Instant.now().toEpochMilli()
    private var updateJob: Job? = null
    val sandColors = listOf(Color(0xFFFFEC98), Color(0xFFF1C173), Color(0xFFF89E0C))


    fun update() {
        if (getState().isSimulationPaused) {
            return
        }
        val startTime = Instant.now().toEpochMilli()
        var pixels = getState().pixels.toMutableList()
        val pixelsOnFinalPosition: MutableList<PixelData> =
            getState().pixelsOnFinalPosition.toMutableList()
        if ((Instant.now().toEpochMilli() - lastGeneratedPixelTime) > 75) {
            val newPixelsList = spawnPixels()
            if (newPixelsList.none { getState().isBellowAPixelOnFinalPosition(it.position) }) {
                pixels.addAll(newPixelsList)
            }

            lastGeneratedPixelTime = Instant.now().toEpochMilli()
        }

        pixels = pixels.distinct().toMutableList()

        pixels.forEach { pixel ->
            val updatedPixel = updatePixelPosition(pixel)
            pixels[pixels.indexOf(pixel)] = updatedPixel
        }
        pixels.removeAll { getState().isBellowAPixelOnFinalPosition(it.position) }
        updatePixelsOnState(pixelsOnFinalPosition, pixels, startTime)
    }


    private fun spawnPixels(): List<PixelData> {
        val positions = mutableListOf<Offset>().apply {
            // Single top position
            add(Offset(getState().aimPosition.x, getState().aimPosition.y - getState().pixelSize * 4 ))
            // Double above middle position
            add(Offset(getState().aimPosition.x - getState().pixelSize, getState().aimPosition.y - getState().pixelSize * 3 ))
            add(Offset(getState().aimPosition.x, getState().aimPosition.y - getState().pixelSize * 3 ))
            add(Offset(getState().aimPosition.x + getState().pixelSize, getState().aimPosition.y - getState().pixelSize * 3 ))
            // Quad middle
            add(Offset(getState().aimPosition.x - getState().pixelSize * 2, getState().aimPosition.y - getState().pixelSize * 2))
            add(Offset(getState().aimPosition.x - getState().pixelSize, getState().aimPosition.y - getState().pixelSize * 2))
            add(Offset(getState().aimPosition.x, getState().aimPosition.y - getState().pixelSize * 2))
            add(Offset(getState().aimPosition.x + getState().pixelSize, getState().aimPosition.y - getState().pixelSize * 2))
            add(Offset(getState().aimPosition.x + getState().pixelSize * 2, getState().aimPosition.y - getState().pixelSize * 2))
            // triple bottom
            add(Offset(getState().aimPosition.x - getState().pixelSize, getState().aimPosition.y - getState().pixelSize))
            add(Offset(getState().aimPosition.x , getState().aimPosition.y - getState().pixelSize))
            add(Offset(getState().aimPosition.x + getState().pixelSize, getState().aimPosition.y - getState().pixelSize ))
            // SIngle bottom
            add(Offset(getState().aimPosition.x, getState().aimPosition.y))
        }

        val valueToTake = (3..positions.size).random()
        return positions.map { PixelData(position = it) }.take(valueToTake)
    }

    private fun updatePixelPosition(pixel: PixelData): PixelData {
        return when {
            getState().hasCollisionBellow(pixel.position) -> {
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

            getState().hasReachedTheGround(pixel) -> {
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

    private fun updatePixelsOnState(
        pixelsOnFinalPosition: MutableList<PixelData>,
        pixels: MutableList<PixelData>,
        startTime: Long
    ) {
        pixelsOnFinalPosition.addAll(pixels.filter { it.hasReachFinalPosition })
        val nonCollidable =
            pixelsOnFinalPosition.filter { !getState().canCollideWithOtherPixels(it.position) }
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
        val collidingPixel = getState().getCollidingPixel(pixelPosition)!!
        val collidingPixelPosition = collidingPixel.position
        val hasPixelOnTheLeftBottom = getState().hasPixelOnTheLeftBottom(pixelPosition)
        val hasPixelOnTheRightBottom = getState().getHasPixelOnTheRightBottom(pixelPosition)
        val random = Random(System.nanoTime())
        val shouldGoToRandomXPosition =
            pixelPosition.x == collidingPixelPosition.x && !hasPixelOnTheLeftBottom && !hasPixelOnTheRightBottom
        val hasPixelOnBottom = getState().hasCollisionBellow(pixelPosition)
        val hasPixelOnTopRight = getState().hasPixelOnTopRight(pixelPosition)
        val hasWallOnTheRight = getState().hasWallOnTheRight(pixelPosition)
        val hasWallOnTheLeft = getState().hasWallOnTheLeft(pixelPosition)
        val hasPixelOnTheLeft = getState().hasPixelOnTheLeft(pixelPosition)
        val hasPixelOnTheRight = getState().hasPixelOnTheRight(pixelPosition)
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

    private fun getCurrentFps(startTime: Long): Float {
        val frameTimeS = Instant.now().toEpochMilli() - startTime
        val fps = 1000f / frameTimeS
        return fps
    }

    fun initializePixels(size: Size, center: Offset) {
        if (!getState().isInitialized) {
            val newState = CanvasState(
                size = size, aimPosition = Offset(x = center.x, y = 0f), isInitialized = true
            )
            updateState(newState)
        }
    }

    fun makeDivisibleBy10(number: Float): Float {
        val remainder = number % 10

        // Handle zero and multiples of 10 directly
        if (remainder == 0f) {
            return number
        }

        // Handle positive and negative remainders efficiently
        val adjustment = if (remainder > 5) {
            10 - remainder
        } else {
            -remainder
        }

        return number + adjustment
    }

    fun updateDragValues(offset: Offset) {
        val newOffsetX = makeDivisibleBy10(offset.x)
        val newOffsetY = makeDivisibleBy10(offset.y)
        val newStates = getState().copy(aimPosition = Offset(newOffsetX, newOffsetY))
        updateState(newStates)
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

    fun drawPixelFloor() {
        val newState = getState().copy(useDebugColors = !getState().useDebugColors)
        updateState(newState)
    }

}