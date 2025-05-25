package com.example.smartdocsconvert.ui.components


import android.net.Uri
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ImageCropView(
    imageUri: Uri,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    onApplyCrop: () -> Unit,
    onCancelCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    var imageSize by remember { mutableStateOf(IntSize(0, 0)) }
    var isDragging by remember { mutableStateOf(false) }
    var dragCorner by remember { mutableStateOf<String?>(null) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    
    val cornerSize = with(density) { 20.dp.toPx() }
    val minCropSize = with(density) { 50.dp.toPx() }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { imageSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragStart = offset
                            
                            val topLeft = Offset(
                                cropRect.left * imageSize.width, 
                                cropRect.top * imageSize.height
                            )
                            val topRight = Offset(
                                cropRect.right * imageSize.width, 
                                cropRect.top * imageSize.height
                            )
                            val bottomLeft = Offset(
                                cropRect.left * imageSize.width, 
                                cropRect.bottom * imageSize.height
                            )
                            val bottomRight = Offset(
                                cropRect.right * imageSize.width, 
                                cropRect.bottom * imageSize.height
                            )
                            
                            dragCorner = when {
                                (offset - topLeft).getDistance() < cornerSize -> "topLeft"
                                (offset - topRight).getDistance() < cornerSize -> "topRight"
                                (offset - bottomLeft).getDistance() < cornerSize -> "bottomLeft"
                                (offset - bottomRight).getDistance() < cornerSize -> "bottomRight"
                                cropRect.contains(Offset(
                                    offset.x / imageSize.width,
                                    offset.y / imageSize.height
                                )) -> "move"
                                else -> null
                            }
                        },
                        onDragEnd = { 
                            isDragging = false
                            dragCorner = null
                        },
                        onDragCancel = { 
                            isDragging = false
                            dragCorner = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            
                            if (imageSize.width <= 0 || imageSize.height <= 0) return@detectDragGestures

                            val normalizedDragX = dragAmount.x / imageSize.width
                            val normalizedDragY = dragAmount.y / imageSize.height

                            val newRect = when (dragCorner) {
                                "topLeft" -> {
                                    cropRect.copy(
                                        left = (cropRect.left + normalizedDragX).coerceIn(
                                            0f,
                                            cropRect.right - minCropSize / imageSize.width
                                        ),
                                        top = (cropRect.top + normalizedDragY).coerceIn(
                                            0f,
                                            cropRect.bottom - minCropSize / imageSize.height
                                        )
                                    )
                                }

                                "topRight" -> {
                                    cropRect.copy(
                                        right = (cropRect.right + normalizedDragX).coerceIn(
                                            cropRect.left + minCropSize / imageSize.width,
                                            1f
                                        ),
                                        top = (cropRect.top + normalizedDragY).coerceIn(
                                            0f,
                                            cropRect.bottom - minCropSize / imageSize.height
                                        )
                                    )
                                }

                                "bottomLeft" -> {
                                    cropRect.copy(
                                        left = (cropRect.left + normalizedDragX).coerceIn(
                                            0f,
                                            cropRect.right - minCropSize / imageSize.width
                                        ),
                                        bottom = (cropRect.bottom + normalizedDragY).coerceIn(
                                            cropRect.top + minCropSize / imageSize.height, 1f
                                        )
                                    )
                                }

                                "bottomRight" -> {
                                    cropRect.copy(
                                        right = (cropRect.right + normalizedDragX).coerceIn(
                                            cropRect.left + minCropSize / imageSize.width,
                                            1f
                                        ),
                                        bottom = (cropRect.bottom + normalizedDragY).coerceIn(
                                            cropRect.top + minCropSize / imageSize.height, 1f
                                        )
                                    )
                                }

                                "move" -> {
                                    val rectWidth = cropRect.right - cropRect.left
                                    val rectHeight = cropRect.bottom - cropRect.top

                                    val newLeft = (cropRect.left + normalizedDragX).coerceIn(
                                        0f,
                                        1f - rectWidth
                                    )
                                    val newTop = (cropRect.top + normalizedDragY).coerceIn(
                                        0f,
                                        1f - rectHeight
                                    )

                                    Rect(
                                        left = newLeft,
                                        top = newTop,
                                        right = newLeft + rectWidth,
                                        bottom = newTop + rectHeight
                                    )
                                }

                                else -> cropRect
                            }
                            
                            onCropRectChange(newRect)
                        }
                    )
                }
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Crop image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        
                        // Draw semi-transparent overlay
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                        )
                        
                        // Draw transparent crop window
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(
                                cropRect.left * size.width,
                                cropRect.top * size.height
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                (cropRect.right - cropRect.left) * size.width,
                                (cropRect.bottom - cropRect.top) * size.height
                            )
                        )
                        
                        // Draw crop window border
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(
                                cropRect.left * size.width,
                                cropRect.top * size.height
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                (cropRect.right - cropRect.left) * size.width,
                                (cropRect.bottom - cropRect.top) * size.height
                            ),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        
                        // Draw corners
                        val cornerRadius = 10.dp.toPx()
                        
                        // Top-left corner
                        drawCircle(
                            color = Color.White,
                            radius = cornerRadius,
                            center = Offset(
                                cropRect.left * size.width,
                                cropRect.top * size.height
                            )
                        )
                        
                        // Top-right corner
                        drawCircle(
                            color = Color.White,
                            radius = cornerRadius,
                            center = Offset(
                                cropRect.right * size.width,
                                cropRect.top * size.height
                            )
                        )
                        
                        // Bottom-left corner
                        drawCircle(
                            color = Color.White,
                            radius = cornerRadius,
                            center = Offset(
                                cropRect.left * size.width,
                                cropRect.bottom * size.height
                            )
                        )
                        
                        // Bottom-right corner
                        drawCircle(
                            color = Color.White,
                            radius = cornerRadius,
                            center = Offset(
                                cropRect.right * size.width,
                                cropRect.bottom * size.height
                            )
                        )
                    }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCancelCrop,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                )
            ) {
                Text("İptal")
            }
            
            Button(
                onClick = onApplyCrop,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Uygula")
            }
        }
    }
}