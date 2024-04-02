@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

package com.rafa.sandsim.sandscreen

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafa.sandsim.ui.theme.SandSimTheme
import com.rafa.sandsim.ui.theme.backGroundBottom
import java.time.Instant


@Composable
fun SandScreen(viewModel: SandSimViewModel = viewModel()) {
    SandSimTheme {

        val state by viewModel.state.collectAsState()
        val pauseSimulation = remember { viewModel::pauseSimulation }
        val resetCanvas = remember { viewModel::resetCanvas }
        val drawPixelFloor = remember { viewModel::useDebugColors }
        val showDebugInfo = remember { viewModel::showDebugInfo }

        DrawScreen(state = state,
            updateDragValues = {
                viewModel.updateDragValues(it)
            },
            onInitializePixels = { size, position ->
                viewModel.initializePixels(size, position)
            },
            colors = viewModel.sandColors,
            onPause = pauseSimulation,
            onResetCanvas = resetCanvas,
            onUseDebugColors = drawPixelFloor,
            onShowDebugInfo = showDebugInfo,
            onCurrentDrawFinished = {
                viewModel.update()
            },
            updateIsTouching = { viewModel.updateIsTouchingTheScreen(it) })
    }
}


@Composable
fun DrawScreen(
    state: CanvasState,
    updateDragValues: (Offset) -> Unit,
    onInitializePixels: (size: Size, position: Offset) -> Unit,
    colors: List<Color>,
    onResetCanvas: () -> Unit,
    onPause: () -> Unit,
    onUseDebugColors: () -> Unit,
    onShowDebugInfo: () -> Unit,
    onCurrentDrawFinished: (delayUntilNextDraw: Long) -> Unit,
    updateIsTouching: (isTouching: Boolean) -> Unit
) {
    var drawingTime by remember {
        mutableStateOf("")
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors)),
        color = Color.Transparent
    ) {
        Scaffold(topBar = {
            TopAppBar(title = { Text(text = "SandSim") }, actions = {
                IconButton(onClick = onShowDebugInfo) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = "Debug",
                        tint = Color.Green
                    )
                }
            }, colors = TopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = Color.Green,
                actionIconContentColor = Color.Green,
                navigationIconContentColor = Color.Green,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
            )
        }) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = Color.Green)
                Box {
                    DrawCanvas(state = state, updateDragValues = {
                        updateDragValues.invoke(it)
                    }, onInitializePixels = { size, position ->
                        onInitializePixels.invoke(size, position)
                    }, colors = colors, postDrawingTimes = {
                        drawingTime = it
                    }, onCurrentDrawFinished = onCurrentDrawFinished, updateIsTouching = {
                        updateIsTouching.invoke(it)
                    })
                    if (state.showDebugInfo) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                QuantityAndFPS(state = state, drawingTime = drawingTime)
                                Stats(state = state)
                            }
                            Options(onPause = {
                                onPause.invoke()
                            }, onResetCanvas = {
                                onResetCanvas.invoke()
                            }, onUseDebugColors = {
                                onUseDebugColors.invoke()
                            })
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun QuantityAndFPS(modifier: Modifier = Modifier, state: CanvasState, drawingTime: String) {
    Column(modifier = modifier) {
        Text(text = "Pixel Quantity: ${state.getCurrentPixels()}", color = Color.Green)
        Text(text = "Drawing time: $drawingTime", color = Color.Green)
        Text(text = "Update Time: ${state.updateTimeInMs} ms", color = Color.Green)
    }
}

@Composable
private fun Stats(modifier: Modifier = Modifier, state: CanvasState) {
    Column(modifier = modifier) {
        Text(text = "Pixels not on final: ${state.pixels.size}", color = Color.Green)
        Text(text = "Pixels on final ${state.pixelsOnFinalPosition.size}", color = Color.Green)

    }
}

@Composable
private fun Options(onResetCanvas: () -> Unit, onPause: () -> Unit, onUseDebugColors: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
    ) {
        Button(onClick = onResetCanvas) {
            Text(text = "Reset")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = onPause) {
            Text(text = "Pause")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = { onUseDebugColors.invoke() }) {
            Text(text = "Debug Colors")
        }
    }
}

@Composable
private fun DrawCanvas(
    state: CanvasState,
    updateDragValues: (Offset) -> Unit,
    onInitializePixels: (size: Size, position: Offset) -> Unit,
    colors: List<Color>,
    postDrawingTimes: (String) -> Unit,
    onCurrentDrawFinished: (delayUntilNextDraw: Long) -> Unit,
    updateIsTouching: (isTouching: Boolean) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val startDrawingTime = Instant.now().toEpochMilli()
    Canvas(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface, backGroundBottom
                )
            )
        )
        .pointerInput(Unit) {
            detectDragGestures(onDragStart = {
                updateIsTouching.invoke(true)
            }, onDrag = { change, _ ->
                updateDragValues.invoke(change.position)
            }, onDragEnd = {
                updateIsTouching.invoke(false)
            }, onDragCancel = {
                updateIsTouching.invoke(false)
            })
        }
        .pointerInteropFilter { event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    updateDragValues.invoke(Offset(event.x, event.y))
                    updateIsTouching.invoke(true)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    updateIsTouching.invoke(false)
                    true
                }

                else -> {
                    true
                }
            }
        }) {
        onInitializePixels.invoke(this.size, this.center)

        if (state.showDebugInfo) {
            val lineWidth = 40f
            val horizontalLineStart = state.aimPosition.x - lineWidth
            val horizontalLineEnd = horizontalLineStart + lineWidth * 2
            val verticalLineStart = state.aimPosition.y - lineWidth
            val verticalLineEnd = verticalLineStart + lineWidth * 2
            val textPosition =
                Offset(horizontalLineStart - (lineWidth * 2), verticalLineStart - lineWidth)
            drawLine(
                color = Color.Green,
                start = Offset(horizontalLineStart, state.aimPosition.y),
                end = Offset(horizontalLineEnd, state.aimPosition.y),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Green,
                start = Offset(state.aimPosition.x, verticalLineStart),
                end = Offset(state.aimPosition.x, verticalLineEnd),
                strokeWidth = 3f
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "x: ${state.aimPosition.x} y: ${state.aimPosition.y}",
                topLeft = textPosition,
                style = TextStyle(color = Color.Green)
            )
        }


        state.pixelsOnFinalPosition.forEach {
            val newPath = Path()
            newPath.moveTo(it.position.x, it.position.y)
            newPath.lineTo(it.position.x, size.height)
            newPath.close()
            drawPath(
                path = newPath, style = Stroke(state.pixelSize), brush = Brush.verticalGradient(
                    colors = colors, startY = it.position.y, endY = size.height
                )
            )
        }

        state.getAllPixels().forEach {
            drawRect(
                color = it.getColor(state.useDebugColors),
                size = Size(state.pixelSize, state.pixelSize),
                topLeft = it.position
            )
        }
        val endDrawingTime = Instant.now().toEpochMilli()
        postDrawingTimes.invoke("${endDrawingTime - startDrawingTime} ms")
        onCurrentDrawFinished(endDrawingTime - startDrawingTime)
    }
}
