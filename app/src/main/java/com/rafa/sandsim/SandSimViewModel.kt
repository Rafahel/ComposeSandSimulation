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
import java.util.Random
import kotlin.math.round
import kotlin.math.roundToInt

data class PixelData(
    val y: Int,
    val x: Int,
    val hasReachFinalPosition: Boolean = false,
    val pixelColor: Color = Color.Green
) {
    val position: Offset = Offset(x = x.toFloat(), y = y.toFloat())
}

data class LineData(
    val lineStartPosition: Offset, val lineEndPosition: Offset
)

data class CanvasState(
    val canvasSizeX: Float = 0f,
    val canvasSizeY: Float = 0f,
    val pixelSize: Float = 10f,
    val pixels: List<List<Int>> = List(canvasSizeY.roundToInt()) {
        List(canvasSizeX.roundToInt()) { 1 }
    },
    var dragOffsetX: Float = 0f,
    val dragOffsetY: Float = 0f,
    val currentFps: Float = 0f
) {
    fun getPositionWithDrawablePixels(): List<Pair<Int, Int>> {
        val positionsGreaterThanZero = mutableListOf<Pair<Int, Int>>()

        for (y in pixels.indices) {
            for (x in pixels[y].indices) {
                if (pixels[y][x] > 0) {
                    positionsGreaterThanZero.add(
                        Pair(
                            (x * pixelSize).toInt(),
                            (y * pixelSize).toInt()
                        )
                    )
                }
            }
        }

        return positionsGreaterThanZero
    }
}


class SandSimViewModel : ViewModel() {
    private val _state = MutableStateFlow(CanvasState())
    val state = _state.asStateFlow()

    fun initializePixels(canvasSizeX: Float, canvasSizeY: Float) {
        if (getState().canvasSizeX == 0f) {
            val newState = CanvasState(
                canvasSizeX = (canvasSizeX / getState().pixelSize),
                canvasSizeY = (canvasSizeY / getState().pixelSize)
            )
            updateState(newState)
        }
    }

    fun updateDragValues(offsetX: Float, offsetY: Float) {
        val currentState = getState()
        val newOffsetX = currentState.dragOffsetX + offsetX
        val newOffsetY = currentState.dragOffsetY + offsetY
        if (newOffsetX > currentState.canvasSizeX * currentState.pixelSize || newOffsetX < 0) return
        if (newOffsetY > currentState.canvasSizeY * currentState.pixelSize || newOffsetY < 0) return
        val newState = currentState.copy(dragOffsetX = newOffsetX, dragOffsetY = newOffsetY)
        updateState(newState)
    }

    private fun updateState(newState: CanvasState) {
        _state.update { newState }
    }

    private fun getState() = _state.value
}