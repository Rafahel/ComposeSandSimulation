package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.Random
import java.util.TreeSet

class SandSimViewModel : ViewModel() {
    private val _state = MutableStateFlow(CanvasState())
    val state = _state.asStateFlow()
    private var lastGeneratedPixelTime = Instant.now().toEpochMilli()
    private var updateJob: Job? = null
    val sandColors = listOf(Color(0xFFFFEC98), Color(0xFFF1C173), Color(0xFFF89E0C))

    fun update() {
        val startTime = Instant.now().toEpochMilli()
        var pixels = getState().pixels.toMutableList()
        if (!getState().isSimulationPaused) {

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
        }
        updatePixelsOnState(pixels, startTime)
    }

    private fun updatePixelsOnState(
        pixels: MutableList<PixelData>,
        startTime: Long
    ) {
        val pixelsOnFinalPosition = getState().pixelsOnFinalPosition
        pixelsOnFinalPosition.addAll(pixels.filter { it.hasReachFinalPosition })
        val nonCollidable =
            pixelsOnFinalPosition.filter { !getState().canCollideWithOtherPixels(it.position) }
        val updatedPixelsOnLastPosition =
            pixelsOnFinalPosition.apply { removeAll(nonCollidable.toSet()) }
                .distinctBy { it.position }

        val updatedPixels =
            pixels.filter { !it.hasReachFinalPosition }.toSet().distinctBy{ it.position }
        val newState = getState().copy(
            pixels = TreeSet(PixelDataComparator).apply { addAll(updatedPixels) },
            pixelsOnFinalPosition = TreeSet(PixelDataComparator).apply {
                addAll(
                    updatedPixelsOnLastPosition
                )
            },
            notCollidablePixels = emptyList(),
            updateTimeInMs = Instant.now().toEpochMilli() - startTime
        )
        updateState(newState)
    }

    private fun spawnPixels(): List<PixelData> {
        val positions = mutableListOf<Offset>().apply {
            // Single top position
            add(
                Offset(
                    getState().aimPosition.x, getState().aimPosition.y - getState().pixelSize * 4
                )
            )
            // Double above middle position
            add(
                Offset(
                    getState().aimPosition.x - getState().pixelSize,
                    getState().aimPosition.y - getState().pixelSize * 3
                )
            )
            add(
                Offset(
                    getState().aimPosition.x, getState().aimPosition.y - getState().pixelSize * 3
                )
            )
            add(
                Offset(
                    getState().aimPosition.x + getState().pixelSize,
                    getState().aimPosition.y - getState().pixelSize * 3
                )
            )
            // Quad middle
            add(
                Offset(
                    getState().aimPosition.x - getState().pixelSize * 2,
                    getState().aimPosition.y - getState().pixelSize * 2
                )
            )
            add(
                Offset(
                    getState().aimPosition.x - getState().pixelSize,
                    getState().aimPosition.y - getState().pixelSize * 2
                )
            )
            add(
                Offset(
                    getState().aimPosition.x, getState().aimPosition.y - getState().pixelSize * 2
                )
            )
            add(
                Offset(
                    getState().aimPosition.x + getState().pixelSize,
                    getState().aimPosition.y - getState().pixelSize * 2
                )
            )
            add(
                Offset(
                    getState().aimPosition.x + getState().pixelSize * 2,
                    getState().aimPosition.y - getState().pixelSize * 2
                )
            )
            // triple bottom
            add(
                Offset(
                    getState().aimPosition.x - getState().pixelSize,
                    getState().aimPosition.y - getState().pixelSize
                )
            )
            add(Offset(getState().aimPosition.x, getState().aimPosition.y - getState().pixelSize))
            add(
                Offset(
                    getState().aimPosition.x + getState().pixelSize,
                    getState().aimPosition.y - getState().pixelSize
                )
            )
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

    fun initializePixels(size: Size, center: Offset) {
        if (!getState().isInitialized) {
            val newState = CanvasState(
                size = size,
                aimPosition = Offset(x = center.x, y = getState().pixelSize * 2),
                isInitialized = true,
                showDebugInfo = getState().showDebugInfo,
                useDebugColors = getState().useDebugColors
            )
            updateState(newState)
        }
    }

    private fun makeDivisibleByPixelSize(number: Float): Float {
        val remainder = number % getState().pixelSize

        if (remainder == 0f) {
            return number
        }

        val adjustment = if (remainder > getState().pixelSize/2) {
            getState().pixelSize - remainder
        } else {
            -remainder
        }

        return number + adjustment
    }

    fun updateDragValues(offset: Offset) {
        val newOffsetX = makeDivisibleByPixelSize(offset.x)
        val newOffsetY = makeDivisibleByPixelSize(offset.y)
        val newStates = getState().copy(aimPosition = Offset(newOffsetX, newOffsetY))
        updateState(newStates)
    }

    private fun updateState(newState: CanvasState) {
        _state.update { newState }
    }

    private fun getState() = _state.value
    fun resetCanvas() {
        updateJob?.cancel()
        val newState = CanvasState(
            useDebugColors = getState().useDebugColors, showDebugInfo = getState().showDebugInfo
        )
        updateState(newState)
    }

    fun pauseSimulation() {
        val newState = getState().copy(isSimulationPaused = !getState().isSimulationPaused)
        updateState(newState)
    }

    fun useDebugColors() {
        val newState = getState().copy(useDebugColors = !getState().useDebugColors)
        updateState(newState)
    }

    fun showDebugInfo() {
        val newState = getState().copy(showDebugInfo = !getState().showDebugInfo)
        updateState(newState)
    }
}