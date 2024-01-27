package com.rafa.sandsim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.viewModelScope
import com.rafa.sandsim.ui.theme.SandSimTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text(
//            text = "Total Pixel Quantity: ${state.getTotalPixels()}   FPS: %.2f".format(
//                state.currentFps
//            )
//        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.Gray)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.updateDragValues(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            viewModel.initializePixels(size.width, size.height)


            state.getPositionWithDrawablePixels().forEach {
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(it.first.toFloat(), it.second.toFloat()),
                    size = Size(state.pixelSize, state.pixelSize)
                )
            }

            drawCircle(
                color = Color.Red,
                radius = 10f,
                center = Offset(state.dragOffsetX, state.dragOffsetY)
            )

//            (state.pixelsOnLastPosition + state.pixels).forEach { pixel ->
//                drawRect(
//                    color = pixel.pixelColor, // Change this to your desired pixel color
//                    size = Size(viewModel.pixelSize, viewModel.pixelSize),
//                    topLeft = pixel.position // Specify the pixel position with x and y
//                )
//            }
        }

        Button(onClick = {
//            viewModel.reset()
        }) {
            Text(text = "Reset")
        }
    }
}