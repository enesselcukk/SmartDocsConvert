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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CropPointsOverlay(
    modifier: Modifier = Modifier,
    imageBounds: Rect,
    initialRect: Rect = Rect(0f, 0f, 1f, 1f),
    onPointsSelected: (List<Offset>) -> Unit
) {
    var points by remember { mutableStateOf(listOf<Offset>()) }
    var selectedPointIndex by remember { mutableIntStateOf(-1) }
    var isDragging by remember { mutableStateOf(false) }


    fun rectToPoints(rect: Rect, bounds: Rect): List<Offset> {
        val safeRect = Rect(
            left = rect.left.coerceIn(0f, 1f),
            top = rect.top.coerceIn(0f, 1f),
            right = rect.right.coerceIn(0f, 1f),
            bottom = rect.bottom.coerceIn(0f, 1f)
        )
        
        val actualLeft = bounds.left + (safeRect.left * bounds.width)
        val actualTop = bounds.top + (safeRect.top * bounds.height)
        val actualRight = bounds.left + (safeRect.right * bounds.width)
        val actualBottom = bounds.top + (safeRect.bottom * bounds.height)

        return listOf(
            Offset(actualLeft, actualTop),      // Top-left (0)
            Offset(actualRight, actualTop),     // Top-right (1)
            Offset(actualRight, actualBottom),  // Bottom-right (2)
            Offset(actualLeft, actualBottom)    // Bottom-left (3)
        )
    }

    fun pointsToRect(pointsList: List<Offset>, bounds: Rect): Rect {
        if (pointsList.size != 4 || bounds.width <= 0 || bounds.height <= 0) {
            return Rect(0.1f, 0.1f, 0.9f, 0.9f)
        }

        val topLeft = pointsList[0]
        val bottomRight = pointsList[2]

        val left = ((topLeft.x - bounds.left) / bounds.width).coerceIn(0f, 1f)
        val top = ((topLeft.y - bounds.top) / bounds.height).coerceIn(0f, 1f)
        val right = ((bottomRight.x - bounds.left) / bounds.width).coerceIn(0f, 1f)
        val bottom = ((bottomRight.y - bounds.top) / bounds.height).coerceIn(0f, 1f)

        val normalizedRect = Rect(left, top, right, bottom)
        return normalizedRect
    }

    LaunchedEffect(imageBounds, initialRect) {
        try {
            if (imageBounds.width <= 0 || imageBounds.height <= 0) {
                return@LaunchedEffect
            }

            val validRect = if (initialRect.width > 0.05f && initialRect.height > 0.05f) {
                initialRect
            } else {
                Rect(0.1f, 0.1f, 0.9f, 0.9f)
            }
            
            points = rectToPoints(validRect, imageBounds)
            onPointsSelected(points)
        } catch (e: Exception) {
            points = rectToPoints(Rect(0.1f, 0.1f, 0.9f, 0.9f), imageBounds)
            onPointsSelected(points)
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val selectionRadius = 40f // px
                            selectedPointIndex = points.indices.firstOrNull { index ->
                                val point = points[index]
                                val distance = sqrt(
                                    (point.x - offset.x).pow(2) +
                                    (point.y - offset.y).pow(2)
                                )
                                distance < selectionRadius
                            } ?: -1

                            if (selectedPointIndex == -1 && points.size == 4) {
                                if (isPointInside(offset, points)) {
                                    selectedPointIndex = -2
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (selectedPointIndex >= 0 && selectedPointIndex < points.size) {
                                val newPoints = points.toMutableList()
                                val currentPoint = points[selectedPointIndex]
                                val newX = (currentPoint.x + dragAmount.x)
                                    .coerceIn(imageBounds.left, imageBounds.right)
                                val newY = (currentPoint.y + dragAmount.y)
                                    .coerceIn(imageBounds.top, imageBounds.bottom)

                                val minSize = minOf(imageBounds.width, imageBounds.height) * 0.1f
                                
                                when (selectedPointIndex) {
                                    0 -> { // Top-left
                                        if (newX < points[1].x - minSize && newY < points[3].y - minSize) {
                                            newPoints[0] = Offset(newX, newY)
                                        }
                                    }
                                    1 -> { // Top-right
                                        if (newX > points[0].x + minSize && newY < points[2].y - minSize) {
                                            newPoints[1] = Offset(newX, newY)
                                        }
                                    }
                                    2 -> { // Bottom-right
                                        if (newX > points[3].x + minSize && newY > points[1].y + minSize) {
                                            newPoints[2] = Offset(newX, newY)
                                        }
                                    }
                                    3 -> { // Bottom-left
                                        if (newX < points[2].x - minSize && newY > points[0].y + minSize) {
                                            newPoints[3] = Offset(newX, newY)
                                        }
                                    }
                                }

                                if (newPoints != points) {
                                    points = newPoints
                                    onPointsSelected(points)
                                }
                            } else if (selectedPointIndex == -2 && points.size == 4) {
                                val newPoints = mutableListOf<Offset>()
                                val wouldBeValid = points.all { point ->
                                    val newX = point.x + dragAmount.x
                                    val newY = point.y + dragAmount.y
                                    newX in imageBounds.left..imageBounds.right && 
                                    newY in imageBounds.top..imageBounds.bottom
                                }
                                
                                if (wouldBeValid) {
                                    for (point in points) {
                                        newPoints.add(
                                            Offset(
                                                x = point.x + dragAmount.x,
                                                y = point.y + dragAmount.y
                                            )
                                        )
                                    }
                                    
                                    points = newPoints
                                    onPointsSelected(points)
                                }
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            isDragging = false
                            selectedPointIndex = -1
                        }
                    )
                }
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()

                if (points.size == 4) {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[2].x, points[2].y)
                    lineTo(points[3].x, points[3].y)
                    close()
                }
            }

            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.5f)
            )

            if (points.size == 4) {
                val cropPath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[2].x, points[2].y)
                    lineTo(points[3].x, points[3].y)
                    close()
                }
                drawPath(
                    path = cropPath,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 3.dp.toPx())
                )

                val gridColor = Color.White.copy(alpha = 0.7f)

                for (i in 1..2) {
                    val t = i / 3f
                    val y1 = points[0].y * (1 - t) + points[3].y * t
                    val y2 = points[1].y * (1 - t) + points[2].y * t
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(points[0].x, y1),
                        end = Offset(points[1].x, y2),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )
                }

                for (i in 1..2) {
                    val t = i / 3f
                    val x1 = points[0].x * (1 - t) + points[1].x * t
                    val x2 = points[3].x * (1 - t) + points[2].x * t
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(x1, points[0].y),
                        end = Offset(x2, points[3].y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )
                }
            }

            points.forEachIndexed { index, point ->
                val isSelected = index == selectedPointIndex
                val pointRadius = if (isSelected) 18f else 15f

                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = pointRadius + 2f,
                    center = point
                )

                drawCircle(
                    color = Color.White,
                    radius = pointRadius,
                    center = point
                )

                drawCircle(
                    color = if (isSelected || isDragging) Color(0xFF03A9F4) else Color(0xFF2196F3),
                    radius = pointRadius,
                    center = point,
                    style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.dp.toPx())
                )
            }
        }
    }
}

private fun isPointInside(point: Offset, corners: List<Offset>): Boolean {
    if (corners.size != 4) return false

    val minX = corners.minOf { it.x }
    val maxX = corners.maxOf { it.x }
    val minY = corners.minOf { it.y }
    val maxY = corners.maxOf { it.y }
    
    if (point.x < minX || point.x > maxX || point.y < minY || point.y > maxY) {
        return false
    }

    var inside = false
    for (i in corners.indices) {
        val j = (i + 1) % corners.size
        
        val intersect = ((corners[i].y > point.y) != (corners[j].y > point.y)) &&
                (point.x < (corners[j].x - corners[i].x) * (point.y - corners[i].y) / 
                (corners[j].y - corners[i].y) + corners[i].x)
        
        if (intersect) inside = !inside
    }
    
    return inside
} 