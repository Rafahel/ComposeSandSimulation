package com.rafa.sandsim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rafa.sandsim.ui.theme.SandSimTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = SandSimViewModel()
        setContent {
            SandSimTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    DrawScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun DrawScreen(viewModel: SandSimViewModel) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val startDrawingTime = Instant.now().toEpochMilli()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    viewModel.updateDragValues(change.position)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    viewModel.updateDragValues(it)
                })
            }) {
            viewModel.initializePixels(this.size, this.center)
            drawCircle(
                color = Color.Green,
                radius = 30f,
                center = Offset(state.aimPosition.x, state.aimPosition.y),
                style = Stroke(width = 3f)
            )

            if (state.pixelsOnFinalPosition.isNotEmpty()) {
                val sorted =
                    state.pixelsOnFinalPosition.sortedByDescending { it.position.x.toInt() and it.position.y.toInt() }
                sorted.forEach {
                    val newPath = Path()
                    newPath.moveTo(it.position.x, it.position.y)
                    newPath.lineTo(it.position.x, size.height)
                    newPath.close()
                    drawPath(
                        path = newPath,
                        style = Stroke(state.pixelSize),
                        brush = Brush.verticalGradient(
                            colors = viewModel.sandColors,
                            startY = it.position.y,
                            endY = size.height
                        )
                    )
                }
            }

            for (i in 0 until state.getAllPixels().size) {
                drawRect(
                    color = state.getAllPixels()[i].getColor(state.useDebugColors),
                    size = Size(state.pixelSize, state.pixelSize),
                    topLeft = state.getAllPixels()[i].position
                )
            }
            val endDrawingTime = Instant.now().toEpochMilli()

            println("${endDrawingTime - startDrawingTime} ms")
        }
        LaunchedEffect(key1 = Unit, block = {
            val shouldDelay = true
            coroutineScope.launch {
                while (true) {
                    viewModel.update()
                    if (shouldDelay) delay(1)
                }

            }
        })

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Pixel Quantity: ${state.getCurrentPixels()}")
                Text(text = "FPS: ${state.fps}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = { viewModel.resetCanvas() }) {
                    Text(text = "Reset")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { viewModel.pauseSimulation() }) {
                    Text(text = "Pause")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { viewModel.drawPixelFloor() }) {
                    Text(text = "Debug Colors")
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Pixels not on final: ${state.pixels.size}")
                Text(text = "Pixels on final ${state.pixelsOnFinalPosition.size}")
                Text(text = "Pixels that cant collide ${state.notCollidablePixels.size}")
            }
        }
    }
}

