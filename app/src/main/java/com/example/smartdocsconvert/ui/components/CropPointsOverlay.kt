package com.example.smartdocsconvert.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CropPointsOverlay(
    modifier: Modifier = Modifier,
    imageBounds: Rect,
    onPointsSelected: (List<Offset>) -> Unit
) {
    var points by remember { mutableStateOf(listOf<Offset>()) }
    var selectedPointIndex by remember { mutableIntStateOf(-1) }

    // Initialize corner points
    LaunchedEffect(imageBounds) {
        points = listOf(
            Offset(imageBounds.left, imageBounds.top),
            Offset(imageBounds.right, imageBounds.top),
            Offset(imageBounds.right, imageBounds.bottom),
            Offset(imageBounds.left, imageBounds.bottom)
        )
        onPointsSelected(points)
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Find the nearest point
                            selectedPointIndex = points.indices.minByOrNull { index ->
                                val point = points[index]
                                sqrt(
                                    (point.x - offset.x).pow(2) +
                                    (point.y - offset.y).pow(2)
                                )
                            } ?: -1
                        },
                        onDrag = { change, dragAmount ->
                            if (selectedPointIndex != -1) {
                                val newPoints = points.toMutableList()
                                val currentPoint = points[selectedPointIndex]
                                
                                // Calculate new position while keeping point within image bounds
                                val newX = (currentPoint.x + dragAmount.x)
                                    .coerceIn(imageBounds.left, imageBounds.right)
                                val newY = (currentPoint.y + dragAmount.y)
                                    .coerceIn(imageBounds.top, imageBounds.bottom)
                                
                                newPoints[selectedPointIndex] = Offset(newX, newY)
                                points = newPoints
                                onPointsSelected(points)
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            selectedPointIndex = -1
                        }
                    )
                }
        ) {
            // Draw semi-transparent overlay
            val path = Path().apply {
                if (points.size == 4) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1..3) {
                        lineTo(points[i].x, points[i].y)
                    }
                    close()
                }
            }
            
            // Draw the crop area outline
            drawPath(
                path = path,
                color = Color(0xFF2196F3),
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw corner points
            points.forEach { point ->
                // White fill
                drawCircle(
                    color = Color.White,
                    radius = 20f,
                    center = point
                )
                // Blue border
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 20f,
                    center = point,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
} 