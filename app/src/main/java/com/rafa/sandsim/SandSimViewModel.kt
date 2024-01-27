package com.rafa.sandsim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round

data class PixelData(
    val y: Int,
    val x: Int,
    val hasReachFinalPosition: Boolean = false
) {
    val position: Offset = Offset(x = x.toFloat(), y = y.toFloat())
}

data class LineData(
    val lineStartPosition: Offset,
    val lineEndPosition: Offset
)

data class CanvasState(
    val pixels: List<PixelData> = emptyList(),
    val pixelsOnLastPosition: List<PixelData> = emptyList(),
    var dragOffsetX: Float = 0f,
    val dragOffsetY: Float = 0f,
    val lines: List<LineData> = emptyList()
)

class SandSimViewModel : ViewModel() {
    private val _state = MutableStateFlow(CanvasState())
    val state = _state.asStateFlow()

    private var canvasSizeX: Float = 0f
    private var canvasSizeY: Float = 0f
    val pixelSize = 10f

    private fun update() {
        viewModelScope.launch {
            while (true) {
                val startTime = System.currentTimeMillis()
                val pixels = getState().pixels.toMutableList()
                val newPixelsPosition: MutableList<PixelData> =
                    getState().pixelsOnLastPosition.toMutableList()
                val newPixelsOnFinalPosition: MutableList<PixelData> = mutableListOf()
                val newPixel = PixelData(
                    x = getState().dragOffsetX.toInt(),
                    y = getState().dragOffsetY.toInt()
                )
                newPixelsPosition.add(newPixel)
                pixels.forEach { pixel ->
                    var hasReachFinalPosition = false
                    val newYPosition = when {
                        hasCollisionBellow(pixel) -> {
                            hasReachFinalPosition = true
                            pixel.y
                        }

                        (pixel.y + 1) >= canvasSizeY -> {
                            hasReachFinalPosition = true
                            canvasSizeY
                        }

                        else -> {
                            pixel.y + 1
                        }
                    }
                    val newXPosition = pixel.x
                    val updatedPixel = pixel.copy(
                        x = newXPosition,
                        y = newYPosition.toInt(),
                        hasReachFinalPosition = hasReachFinalPosition
                    )
                    if (hasReachFinalPosition) {
                        newPixelsOnFinalPosition.add(updatedPixel)
                    } else {
                        newPixelsPosition.add(updatedPixel)
                    }
                }

//                getState().dragOffsetX = 0f // Remove this to keep moving x after
                delay(5)
                println(newPixelsPosition.first())
//                transformPixelToLine()
                val newState = getState().copy(
                    pixels = newPixelsPosition.toList(),
                    pixelsOnLastPosition = newPixelsOnFinalPosition.toList()
                )
                updateState(newState)

                val frameTimeS = System.currentTimeMillis() - startTime

                val fps = 1 / frameTimeS.toFloat()
                val roundedFps = round(fps * 1000)

                println(
                    "Total Pixel Quantity: ${newPixelsPosition.size + newPixelsOnFinalPosition.size}FPS: %.2f".format(
                        roundedFps
                    )
                )

            }
        }
    }

    fun setCenterCoordinates(size: Size, center: Offset) {
        if (canvasSizeX == 0f) {
            canvasSizeX = size.width
            canvasSizeY = size.height
            val newState = getState().copy(dragOffsetX = center.x, dragOffsetY = center.y)
            updateState(newState)
            update()
        }
    }

    private fun hasCollisionBellow(pixel: PixelData): Boolean {
        val pixels = getState().pixelsOnLastPosition
        return pixels.any { it.y == (pixel.y + 1) && it.x == pixel.x }
    }

    private fun transformPixelToLine() {
        val state = getState()
        val pixelsOnLastPosition = state.pixelsOnLastPosition
        val newLines = state.lines.toMutableList()
        val ordered = pixelsOnLastPosition.sortedBy { it.x and it.y }.toMutableList()
        val pixelsConverted = mutableListOf<PixelData>()

        for (i in ordered.indices) {
            val currentPixel = ordered[i]
            val fromX = currentPixel.position.x
            var toX = 0f
            for (j in (i + 1) until ordered.size) {
                val next = ordered[j]
                if (currentPixel.position.y == next.position.y) {
                    if (currentPixel.position.x == (next.position.x - 1)) {
                        toX = next.position.x
                        pixelsConverted.add(currentPixel)
                        pixelsConverted.add(next)
                        newLines.add(
                            LineData(
                                lineStartPosition = Offset(
                                    fromX,
                                    currentPixel.position.y
                                ), lineEndPosition = Offset(toX, currentPixel.position.y)
                            )
                        )
                    } else {
                        break
                    }
                }

            }
        }

        ordered.toMutableList().removeAll(pixelsConverted)
        val newState = getState().copy(pixelsOnLastPosition = ordered, lines = newLines)
        updateState(newState)

    }

    fun updateDragValues(offsetX: Float, offsetY: Float) {
        val newOffsetX = if ((getState().dragOffsetX + offsetX) >= canvasSizeX) {
            canvasSizeX
        } else {
            getState().dragOffsetX + offsetX
        }
        val newOffsetY = if ((getState().dragOffsetY + offsetY) >= canvasSizeY) {
            canvasSizeY
        } else {
            getState().dragOffsetY + offsetY
        }
        val newState = getState().copy(dragOffsetX = newOffsetX, dragOffsetY = newOffsetY)
        updateState(newState)
    }

    private fun updateState(newState: CanvasState) {
        _state.update { newState }
    }

    private fun getState() = _state.value
}