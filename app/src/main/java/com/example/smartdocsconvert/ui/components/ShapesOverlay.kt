package com.example.smartdocsconvert.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.smartdocsconvert.R
import kotlin.math.*
import android.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartdocsconvert.ui.viewmodel.ImageFilterViewModel

// Shape data classes and enums
data class ShapeItem(
    val type: ShapeType,
    val position: Offset = Offset.Zero,
    val size: Float = 100f,
    val color: Color = Color.White,
    val strokeWidth: Float = 4f,
    val text: String = "",
    val rotation: Float = 0f,
    val scale: Float = 1f
)

enum class ShapeType {
    RECTANGLE, CIRCLE, HEART, STAR, ARROW, TEXT, BRUSH
}

data class BrushStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun ShapesOverlay(
    viewModel: ImageFilterViewModel = hiltViewModel(),
    initialShapes: List<ShapeItem> = emptyList(),
    onShapesChanged: (List<ShapeItem>) -> Unit = {}
) {
    var shapes by remember { mutableStateOf(initialShapes) }
    var selectedShape by remember { mutableStateOf<ShapeType?>(null) }
    val selectedColor by remember { mutableStateOf(Color.White) }
    val strokeWidth by remember { mutableFloatStateOf(4f) }
    val isEditing by remember { mutableStateOf(false) }
    val brushStrokes by remember { mutableStateOf<List<BrushStroke>>(emptyList()) }
    val currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val isDragging by remember { mutableStateOf(false) }
    val isResizing by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
    
    // Initialize shapes from ViewModel
    LaunchedEffect(initialShapes) {
        shapes = initialShapes
    }

    // When shapes change, propagate to the caller
    LaunchedEffect(shapes) {
        onShapesChanged(shapes)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Drawing area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (selectedShape != null && !isEditing && !isDragging && !isResizing) {
                            when (selectedShape) {
                                ShapeType.TEXT -> {
                                    showTextInput = true
                                    lastTapPosition = offset
                                }
                                ShapeType.RECTANGLE,
                                ShapeType.CIRCLE,
                                ShapeType.HEART,
                                ShapeType.STAR,
                                ShapeType.ARROW -> {
                                    val newShape = ShapeItem(
                                        type = selectedShape!!,
                                        position = offset,
                                        color = selectedColor,
                                        strokeWidth = strokeWidth
                                    )
                                    shapes = shapes + newShape
                                    viewModel.addShape(newShape)
                                }
                                ShapeType.BRUSH -> {} // Brush is handled separately
                                null -> {} // Handle null case
                            }
                        }
                    }
                }
        ) {
            // Draw existing shapes
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw shapes
                shapes.forEach { shape ->
                    withTransform({
                        translate(shape.position.x, shape.position.y)
                        rotate(shape.rotation, Offset(shape.size / 2f, shape.size / 2f))
                    }) {
                        when (shape.type) {
                            ShapeType.RECTANGLE -> {
                                drawRect(
                                    color = shape.color,
                                    topLeft = Offset.Zero,
                                    size = Size(shape.size, shape.size),
                                    style = Stroke(width = shape.strokeWidth)
                                )
                            }
                            ShapeType.CIRCLE -> {
                                drawCircle(
                                    color = shape.color,
                                    radius = shape.size / 2,
                                    center = Offset(shape.size / 2f, shape.size / 2f),
                                    style = Stroke(width = shape.strokeWidth)
                                )
                            }
                            ShapeType.HEART -> {
                                val path = Path()
                                drawHeart(path, Offset(shape.size / 2f, shape.size / 2f), shape.size)
                                drawPath(
                                    path = path,
                                    color = shape.color,
                                    style = Stroke(width = shape.strokeWidth)
                                )
                            }
                            ShapeType.STAR -> {
                                val path = Path()
                                drawStar(path, Offset(shape.size / 2f, shape.size / 2f), shape.size)
                                drawPath(
                                    path = path,
                                    color = shape.color,
                                    style = Stroke(width = shape.strokeWidth)
                                )
                            }
                            ShapeType.ARROW -> {
                                val path = Path()
                                drawArrow(path, Offset.Zero, shape.size)
                                drawPath(
                                    path = path,
                                    color = shape.color,
                                    style = Stroke(width = shape.strokeWidth)
                                )
                            }
                            ShapeType.TEXT -> {
                                if (shape.text.isNotEmpty()) {
                                    val paint = Paint().apply {
                                        color = shape.color.toArgb()
                                        textSize = shape.size
                                        typeface = android.graphics.Typeface.create(
                                            android.graphics.Typeface.DEFAULT,
                                            android.graphics.Typeface.BOLD
                                        )
                                    }
                                    drawContext.canvas.nativeCanvas.drawText(
                                        shape.text,
                                        0f,
                                        shape.size,
                                        paint
                                    )
                                }
                            }
                            ShapeType.BRUSH -> {
                                // Brush strokes are handled separately
                            }
                        }
                    }
                }
                
                // Draw brush strokes
                brushStrokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        drawPath(
                            path = Path().apply {
                                moveTo(stroke.points.first().x, stroke.points.first().y)
                                stroke.points.drop(1).forEach { point ->
                                    lineTo(point.x, point.y)
                                }
                            },
                            color = stroke.color,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
                
                // Draw current brush stroke
                if (currentStroke.size > 1) {
                    drawPath(
                        path = Path().apply {
                            moveTo(currentStroke.first().x, currentStroke.first().y)
                            currentStroke.drop(1).forEach { point ->
                                lineTo(point.x, point.y)
                            }
                        },
                        color = selectedColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Shape selection toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShapeButton(
                type = ShapeType.RECTANGLE,
                selected = selectedShape == ShapeType.RECTANGLE,
                onClick = { selectedShape = if (selectedShape == ShapeType.RECTANGLE) null else ShapeType.RECTANGLE }
            )
            ShapeButton(
                type = ShapeType.CIRCLE,
                selected = selectedShape == ShapeType.CIRCLE,
                onClick = { selectedShape = if (selectedShape == ShapeType.CIRCLE) null else ShapeType.CIRCLE }
            )
            ShapeButton(
                type = ShapeType.HEART,
                selected = selectedShape == ShapeType.HEART,
                onClick = { selectedShape = if (selectedShape == ShapeType.HEART) null else ShapeType.HEART }
            )
            ShapeButton(
                type = ShapeType.STAR,
                selected = selectedShape == ShapeType.STAR,
                onClick = { selectedShape = if (selectedShape == ShapeType.STAR) null else ShapeType.STAR }
            )
            ShapeButton(
                type = ShapeType.ARROW,
                selected = selectedShape == ShapeType.ARROW,
                onClick = { selectedShape = if (selectedShape == ShapeType.ARROW) null else ShapeType.ARROW }
            )
            ShapeButton(
                type = ShapeType.TEXT,
                selected = selectedShape == ShapeType.TEXT,
                onClick = { selectedShape = if (selectedShape == ShapeType.TEXT) null else ShapeType.TEXT }
            )
        }
    }

    // Text input dialog
    if (showTextInput) {
        TextInputDialog(
            onTextEntered = { inputText ->
                if (inputText.isNotEmpty()) {
                    val newShape = ShapeItem(
                        type = ShapeType.TEXT,
                        position = lastTapPosition,
                        color = selectedColor,
                        text = inputText,
                        size = 40f
                    )
                    shapes = shapes + newShape
                    viewModel.addShape(newShape)
                }
                showTextInput = false
            },
            onDismiss = { showTextInput = false }
        )
    }
}

// Helper functions
private fun drawHeart(path: Path, center: Offset, size: Float) {
    val width = size
    val height = size

    path.moveTo(center.x, center.y + height / 4)
    
    // Left curve
    path.cubicTo(
        center.x - width / 2, center.y - height / 4,
        center.x - width / 2, center.y - height / 2,
        center.x, center.y - height / 4
    )
    
    // Right curve
    path.cubicTo(
        center.x + width / 2, center.y - height / 2,
        center.x + width / 2, center.y - height / 4,
        center.x, center.y + height / 4
    )
}

private fun drawStar(path: Path, center: Offset, size: Float) {
    val outerRadius = size / 2
    val innerRadius = outerRadius * 0.4f
    val points = 5
    
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = Math.PI * i / points
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
}

private fun drawArrow(path: Path, start: Offset, size: Float) {
    val arrowLength = size * 0.8f
    val headLength = size * 0.2f
    val headWidth = size * 0.3f

    // Shaft
    path.moveTo(start.x, start.y)
    path.lineTo(start.x + arrowLength, start.y)

    // Head
    path.moveTo(start.x + arrowLength - headLength, start.y - headWidth)
    path.lineTo(start.x + arrowLength, start.y)
    path.lineTo(start.x + arrowLength - headLength, start.y + headWidth)
}


private operator fun Rect.contains(point: Offset): Boolean {
    return point.x in left..right && point.y >= top && point.y <= bottom
}

@Composable
private fun ShapeButton(
    type: ShapeType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (type) {
        ShapeType.RECTANGLE -> R.drawable.ic_rectangle
        ShapeType.CIRCLE -> R.drawable.ic_circle
        ShapeType.HEART -> R.drawable.ic_heart
        ShapeType.STAR -> R.drawable.ic_star
        ShapeType.ARROW -> R.drawable.ic_arrow
        ShapeType.TEXT -> R.drawable.ic_text
        ShapeType.BRUSH -> R.drawable.ic_edit
    }
    
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFFFFD700) else Color.Transparent)
            .padding(8.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = type.name,
            tint = if (selected) Color.Black else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TextInputDialog(
    onTextEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.DarkGray
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = Color.White)
                    }
                    TextButton(onClick = { onTextEntered(text) }) {
                        Text("Tamam", color = Color.White)
                    }
                }
            }
        }
    }
} 