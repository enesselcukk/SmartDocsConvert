package com.example.smartdocsconvert.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ImageEditorCanvas(
    modifier: Modifier = Modifier,
    onDrawPath: (List<PathProperties>) -> Unit
) {
    var paths by remember { mutableStateOf(listOf<PathProperties>()) }
    var currentPath by remember { mutableStateOf<PathProperties?>(null) }
    var currentPosition by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = PathProperties(
                            points = mutableListOf(offset),
                            color = Color.Blue,
                            strokeWidth = 5f
                        )
                        currentPosition = offset
                    },
                    onDrag = { change, _ ->
                        val newPoint = change.position
                        currentPath?.points?.add(newPoint)
                        currentPosition = newPoint
                    },
                    onDragEnd = {
                        currentPath?.let {
                            paths = paths + it
                            onDrawPath(paths)
                        }
                        currentPath = null
                        currentPosition = null
                    }
                )
            }
    ) {
        paths.forEach { path ->
            val pathPoints = path.points
            if (pathPoints.size > 1) {
                drawPath(
                    path = Path().apply {
                        moveTo(pathPoints.first().x, pathPoints.first().y)
                        pathPoints.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                    },
                    color = path.color,
                    style = Stroke(width = path.strokeWidth)
                )
            }
        }

        currentPath?.let { path ->
            val pathPoints = path.points
            if (pathPoints.size > 1) {
                drawPath(
                    path = Path().apply {
                        moveTo(pathPoints.first().x, pathPoints.first().y)
                        pathPoints.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                    },
                    color = path.color,
                    style = Stroke(width = path.strokeWidth)
                )
            }
        }
    }
}

data class PathProperties(
    val points: MutableList<Offset>,
    val color: Color,
    val strokeWidth: Float
) 