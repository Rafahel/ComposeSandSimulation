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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.viewModelScope
import com.rafa.sandsim.ui.theme.SandSimTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


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

    val context = LocalContext.current

    val interactionSource = remember { MutableInteractionSource() }

    val viewConfiguration = LocalViewConfiguration.current


    LaunchedEffect(interactionSource) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongClick = true
                    Toast.makeText(context, "Long click", Toast.LENGTH_SHORT).show()
                }

                is PressInteraction.Release -> {
                    if (isLongClick.not()) {
                        Toast.makeText(context, "click", Toast.LENGTH_SHORT).show()
                    }

                }

            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Pixel Quantity: ${state.getCurrentPixels()}")
            Text(text = "FPS: ${state.fps}")
        }
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
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            // Left arrow key
                            Key.DirectionLeft -> {
                                true
                            }
                            // Right arrow key
                            Key.DirectionRight -> {
                                true
                            }

                            else -> false
                        }
                    } else {
                        false
                    }
                }
        ) {
            viewModel.initializePixels(this.size, this.center)
            drawCircle(
                color = Color.Green,
                radius = 5f,
                center = Offset(state.aimPosition.x, state.aimPosition.y)
            )

            (state.pixelsOnFinalPosition + state.pixels).forEach { pixel ->
                drawRect(
                    color = pixel.color, // Change this to your desired pixel color
                    size = Size(state.pixelSize, state.pixelSize),
                    topLeft = pixel.position // Specify the pixel position with x and y
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "KeyboardArrowUp"
                    )
                }
                Row {
                    IconButton(onClick = { /*TODO*/ }, interactionSource = interactionSource) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "KeyboardArrowLeft"
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "KeyboardArrowRight"
                        )
                    }
                }
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "KeyboardArrowDown"
                    )
                }
            }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Pixels not on final: ${state.pixels.size}")
                Text(text = "Pixels on final ${state.pixelsOnFinalPosition.size}")
            }
        }
    }
}