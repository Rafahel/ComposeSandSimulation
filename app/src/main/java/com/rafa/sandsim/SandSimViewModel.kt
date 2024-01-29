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
import kotlin.math.round

data class PixelData(
    val position: Offset, val hasReachFinalPosition: Boolean = false
) {
    val color: Color = if (hasReachFinalPosition) Color.Red else Color.Yellow
}

data class LineData(
    val lineStartPosition: Offset, val lineEndPosition: Offset
)

data class CanvasState(
    val pixels: List<PixelData> = emptyList(),
    val pixelsOnFinalPosition: List<PixelData> = emptyList(),
    val size: Size = Size(0f, 0f),
    val aimPosition: Offset = Offset(0f, 0f),
    val pixelSize: Float = 10f,
    val lines: List<LineData> = emptyList(),
    val fps: Float = 0f
) {
    fun getCurrentPixels() = (pixels + pixelsOnFinalPosition).size
}

class SandSimViewModel : ViewModel() {
    private val _state = MutableStateFlow(CanvasState())
    val state = _state.asStateFlow()

    private var updateJob: Job? = null

    private fun update() {
        var lastGeneratedPixelTime = Instant.now().toEpochMilli()
        updateJob = viewModelScope.launch {
            while (true) {
                val startTime = Instant.now().toEpochMilli()
                val pixels = getState().pixels.toMutableList()
                val pixelsOnFinalPosition: MutableList<PixelData> =
                    getState().pixelsOnFinalPosition.toMutableList()
                val pixelsToBeRemoved = mutableListOf<PixelData>()
                if ((Instant.now().toEpochMilli() - lastGeneratedPixelTime) > 75) {
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

                pixels.forEach { pixel ->
                    var hasReachFinalPosition = false

                    val newYPosition = when {
                        hasCollisionBellow(pixel) -> {
                            hasReachFinalPosition = true
                            pixel.position.y
                        }

                        hasReachedTheGround(pixel) -> {
                            hasReachFinalPosition = true
                            getState().size.height - getState().pixelSize
                        }

                        else -> {
                            pixel.position.y + 1
                        }
                    }
                    val newXPosition = pixel.position.x
                    val updatedPixel = pixel.copy(
                        position = Offset(x = newXPosition, y = newYPosition),
                        hasReachFinalPosition = hasReachFinalPosition
                    )
                    pixels[pixels.indexOf(pixel)] = updatedPixel
                }
                pixelsOnFinalPosition.addAll(pixels.filter { it.hasReachFinalPosition }
                    .distinctBy { it.position })
                pixels.removeAll { it.hasReachFinalPosition }

//                getState().dragOffsetX = 0f // Remove this to keep moving x after
                delay(5)
//                transformPixelToLine()
                val newState = getState().copy(
                    pixels = pixels.toList().distinct(),
                    pixelsOnFinalPosition = pixelsOnFinalPosition.toList().distinct(),
                    fps = getCurrentFps(startTime)
                )
                updateState(newState)
            }
        }
    }

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

    private fun hasCollisionBellow(pixel: PixelData): Boolean {
        val pixelsOnFinalPosition = getState().pixelsOnFinalPosition
        val nextPixelYPosition = pixel.position.y + getState().pixelSize
        return pixelsOnFinalPosition.any { it.position.x == pixel.position.x && it.position.y <= nextPixelYPosition }
    }

    //540 646
    fun updateDragValues(offsetX: Float, offsetY: Float) {
        val currentState = getState()
        val newXPosition = currentState.aimPosition.x + offsetX
        val newYPosition = currentState.aimPosition.y + offsetY
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
}