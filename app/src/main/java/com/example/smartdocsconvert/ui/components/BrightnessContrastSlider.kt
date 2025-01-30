package com.example.smartdocsconvert.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun BrightnessContrastSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    iconResId: Int,
    contentDescription: String,
    valueRange: ClosedFloatingPointRange<Float> = 0.5f..1.5f
) {
    val thumbRadius = 8.dp
    val trackWidth = 2.dp
    val trackLength = 180.dp
    val activeColor = Color(0xFF2196F3)
    val inactiveColor = Color.Gray.copy(alpha = 0.5f)
    val trackBackgroundColor = Color.Gray.copy(alpha = 0.2f)

    Column(
        modifier = modifier
            .width(32.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackLength)
                .background(trackBackgroundColor)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            val trackHeightPx = trackLength.toPx()
                            val newValue = value - (dragAmount / trackHeightPx) * (valueRange.endInclusive - valueRange.start)
                            val clampedValue = newValue.coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(clampedValue)
                            change.consume()
                        }
                    }
            ) {
                // Calculate progress position
                val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                val progressHeight = size.height * (1f - progress)

                // Draw active track
                drawLine(
                    color = activeColor,
                    start = Offset(size.width / 2, progressHeight),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = trackWidth.toPx(),
                    cap = StrokeCap.Round
                )

                // Draw thumb
                drawCircle(
                    color = activeColor,
                    radius = thumbRadius.toPx(),
                    center = Offset(size.width / 2, progressHeight)
                )
            }
        }
    }
} 