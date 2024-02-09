package com.rafa.sandsim

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.rafa.sandsim.ui.theme.SandSimTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = SandSimViewModel()
        setContent {
            SandSimTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
    var startDrawingTime = Instant.now().toEpochMilli()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color.DarkGray)
                .pointerInput(Unit) {

                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.updateDragValues(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            viewModel.initializePixels(this.size, this.center)
            drawCircle(
                color = Color.Green,
                radius = 5f,
                center = Offset(state.aimPosition.x, state.aimPosition.y)
            )


            for (i in 0 until state.getAllPixels().size) {
                drawRect(
                    color = state.getAllPixels()[i].color, // Change this to your desired pixel color
                    size = Size(state.pixelSize, state.pixelSize),
                    topLeft = state.getAllPixels()[i].position // Specify the pixel position with x and y
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Pixel Quantity: ${state.getCurrentPixels()}")
                Text(text = "FPS: ${state.fps}")
            }
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                IconButton(onClick = { /*TODO*/ }) {
//                    Icon(
//                        imageVector = Icons.Filled.KeyboardArrowUp,
//                        contentDescription = "KeyboardArrowUp"
//                    )
//                }
//                Row {
//                    IconButton(onClick = { /*TODO*/ }, interactionSource = interactionSource) {
//                        Icon(
//                            imageVector = Icons.Filled.KeyboardArrowLeft,
//                            contentDescription = "KeyboardArrowLeft"
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(20.dp))
//                    IconButton(onClick = { /*TODO*/ }) {
//                        Icon(
//                            imageVector = Icons.Filled.KeyboardArrowRight,
//                            contentDescription = "KeyboardArrowRight"
//                        )
//                    }
//                }
//                IconButton(onClick = { /*TODO*/ }) {
//                    Icon(
//                        imageVector = Icons.Filled.KeyboardArrowDown,
//                        contentDescription = "KeyboardArrowDown"
//                    )
//                }
//            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = { viewModel.resetCanvas() }) {
                    Text(text = "Reset")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { viewModel.pauseSimulation() }) {
                    Text(text = "Pause")
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