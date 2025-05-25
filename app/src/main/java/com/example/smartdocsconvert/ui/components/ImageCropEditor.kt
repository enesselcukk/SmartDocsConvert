package com.example.smartdocsconvert.ui.components

import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.smartdocsconvert.data.model.ImageFilterState


@Composable
fun ImageCropEditor(
    imageUri: Uri,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    onApplyCrop: () -> Unit,
    onCancelCrop: () -> Unit,
    uiState: ImageFilterState
) {
    var imageBounds by remember { mutableStateOf<Rect?>(null) }
    var viewSize by remember { mutableStateOf<IntSize?>(null) }
    var imageLoaded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                if (options.outWidth > 0 && options.outHeight > 0) {
                    context.contentResolver.openInputStream(imageUri)?.use { exifStream ->
                        val exif = ExifInterface(exifStream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        
                        val originalWidth = options.outWidth
                        val originalHeight = options.outHeight

                        val needToSwap = orientation == ExifInterface.ORIENTATION_ROTATE_90 || 
                                        orientation == ExifInterface.ORIENTATION_ROTATE_270
                        
                        val finalWidth = if (needToSwap) originalHeight else originalWidth
                        val finalHeight = if (needToSwap) originalWidth else originalHeight

                        uiState.imageSize = IntSize(finalWidth, finalHeight)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageCropEditor", "Error loading image dimensions: ${e.message}", e)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    viewSize = size
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                val colorMatrix = createColorMatrix(
                    brightness = uiState.brightnessValues.getOrNull(uiState.currentImageIndex) ?: 1f,
                    contrast = uiState.contrastValues.getOrNull(uiState.currentImageIndex) ?: 1f,
                    filter = uiState.selectedFilters.getOrNull(uiState.currentImageIndex) ?: "Original"
                )

                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(imageUri)
                        .build()
                )

                LaunchedEffect(painter.state, viewSize) {
                    if (painter.state is AsyncImagePainter.State.Success && viewSize != null) {
                        val state = painter.state as AsyncImagePainter.State.Success
                        val intrinsicSize = state.painter.intrinsicSize

                        if (uiState.imageSize.width <= 0 || uiState.imageSize.height <= 0) {
                            uiState.imageSize = IntSize(intrinsicSize.width.toInt(), intrinsicSize.height.toInt())
                        }
                        
                        val vSize = viewSize!!

                        val imageAspectRatio = intrinsicSize.width / intrinsicSize.height
                        val viewAspectRatio = vSize.width.toFloat() / vSize.height

                        val (width, height) = if (imageAspectRatio > viewAspectRatio) {
                            vSize.width.toFloat() to (vSize.width / imageAspectRatio)
                        } else {
                            (vSize.height * imageAspectRatio) to vSize.height.toFloat()
                        }

                        val left = (vSize.width - width) / 2
                        val top = (vSize.height - height) / 2

                        val newBounds = Rect(left, top, left + width, top + height)
                        imageBounds = newBounds
                        imageLoaded = true
                    }
                }

                Image(
                    painter = painter,
                    contentDescription = "Image to crop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                )

                if (imageLoaded && imageBounds != null) {
                    CropPointsOverlay(
                        modifier = Modifier.fillMaxSize(),
                        imageBounds = imageBounds!!,
                        initialRect = cropRect,
                        onPointsSelected = { points ->
                            if (points.size == 4) {
                                val bounds = imageBounds!!

                                val clampedPoints = points.map { point ->
                                    Offset(
                                        x = point.x.coerceIn(bounds.left, bounds.right),
                                        y = point.y.coerceIn(bounds.top, bounds.bottom)
                                    )
                                }

                                val relativeRect = Rect(
                                    left = ((clampedPoints[0].x - bounds.left) / bounds.width)
                                        .coerceIn(0f, 1f),
                                    top = ((clampedPoints[0].y - bounds.top) / bounds.height)
                                        .coerceIn(0f, 1f),
                                    right = ((clampedPoints[2].x - bounds.left) / bounds.width)
                                        .coerceIn(0f, 1f),
                                    bottom = ((clampedPoints[2].y - bounds.top) / bounds.height)
                                        .coerceIn(0f, 1f)
                                )

                                if (relativeRect.width > 0.05f && relativeRect.height > 0.05f) {
                                    onCropRectChange(relativeRect)
                                }
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCancelCrop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("İptal")
            }
            
            Button(
                onClick = {
                    if (imageLoaded && imageBounds != null) {
                        onApplyCrop()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text("Kırp")
            }
        }
    }
}

private fun Float.coerceIn(min: Float, max: Float): Float {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
} 