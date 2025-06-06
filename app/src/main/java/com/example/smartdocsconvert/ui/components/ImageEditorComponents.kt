package com.example.smartdocsconvert.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.ui.theme.extendedColors
import com.example.smartdocsconvert.data.model.ImageFilterState
import com.example.smartdocsconvert.ui.viewmodel.ImageFilterViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Main editor composable with image display and controls
 */
@Composable
fun MainImageEditor(
    uiState: ImageFilterState,
    transformableState: TransformableState,
    onImageChanged: (Int) -> Unit,
    viewModel: ImageFilterViewModel? = null
) {
    val cameraDistance = with(LocalDensity.current) { 8.dp.toPx() } * 10

    val brightness = uiState.brightnessValues.getOrNull(uiState.currentImageIndex) ?: 1f
    val contrast = uiState.contrastValues.getOrNull(uiState.currentImageIndex) ?: 1f
    val filter = uiState.selectedFilters.getOrNull(uiState.currentImageIndex) ?: "Original"
    val colorMatrix = createColorMatrix(brightness, contrast, filter)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.rotationY > 90f && uiState.currentImageIndex > 0) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(uiState.processedImageUris[uiState.currentImageIndex - 1])
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = uiState.scale
                            scaleY = uiState.scale
                            translationX = uiState.offsetX
                            translationY = uiState.offsetY
                            rotationZ = uiState.rotationAngles.getOrElse(uiState.currentImageIndex - 1) { 0f }
                            rotationY = -180 + uiState.rotationY
                            this.cameraDistance = cameraDistance
                        }
                        .transformable(state = transformableState),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                )
            }

            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(uiState.processedImageUris[uiState.currentImageIndex])
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = uiState.scale
                        scaleY = uiState.scale
                        translationX = uiState.offsetX
                        translationY = uiState.offsetY
                        rotationZ = uiState.rotationAngles[uiState.currentImageIndex]
                        rotationY = uiState.rotationY
                        this.cameraDistance = cameraDistance
                    }
                    .transformable(state = transformableState),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )
        }

        NavigationOverlay(
            currentIndex = uiState.currentImageIndex,
            totalImages = uiState.processedImageUris.size,
            showShapesOverlay = uiState.activeFeature == "shapes",
            onPreviousClick = { 
                if (uiState.currentImageIndex > 0 && uiState.processedImageUris.isNotEmpty()) {
                    onImageChanged(uiState.currentImageIndex - 1)
                }
            },
            onNextClick = { 
                if (uiState.currentImageIndex < uiState.processedImageUris.size - 1 && uiState.processedImageUris.isNotEmpty()) {
                    onImageChanged(uiState.currentImageIndex + 1)
                }
            },
            viewModel = viewModel
        )
    }
}

/**
 * Navigation controls overlaid on the image
 */
@Composable
fun NavigationOverlay(
    currentIndex: Int,
    totalImages: Int,
    showShapesOverlay: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    viewModel: ImageFilterViewModel? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (currentIndex > 0) {
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (currentIndex < totalImages - 1) {
            IconButton(
                onClick = onNextClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_next),
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (showShapesOverlay && viewModel != null) {
            ShapesOverlay(
                viewModel = viewModel,
                initialShapes = viewModel.getShapesForCurrentImage()
            )
        }

        Text(
            text = "${currentIndex + 1}/$totalImages",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 14.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/**
 * Composable for feature-specific controls
 */
@Composable
fun AnimatedFeatureControls(
    uiState: ImageFilterState,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onFilterSelected: (String) -> Unit
) {
    AnimatedVisibility(visible = uiState.activeFeature == "light") {
        BrightnessContrastControls(
            brightness = uiState.brightnessValues.getOrNull(uiState.currentImageIndex) ?: 1f,
            contrast = uiState.contrastValues.getOrNull(uiState.currentImageIndex) ?: 1f,
            onBrightnessChange = onBrightnessChange,
            onContrastChange = onContrastChange
        )
    }

    AnimatedVisibility(visible = uiState.activeFeature == "filter") {
        FilterSelector(
            filters = listOf(
                "Original", "Clarendon", "Gingham", 
                "Moon", "Lark", "Reyes", "Juno"
            ),
            selectedFilter = uiState.selectedFilters.getOrNull(uiState.currentImageIndex) ?: "Original",
            onFilterSelected = onFilterSelected
        )
    }
}

/**
 * Composable for brightness and contrast controls
 */
@Composable
fun BrightnessContrastControls(
    brightness: Float,
    contrast: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Light",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Slider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.5f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
            Text(
                text = String.format("%.1f", (brightness - 1f) * 100),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contrast",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Slider(
                    value = contrast,
                    onValueChange = onContrastChange,
                    valueRange = 0.5f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
            Text(
                text = "${((contrast - 1f) * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FilterSelector(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy((-30).dp),
        contentPadding = PaddingValues(horizontal = 40.dp)
    ) {
        items(filters) { filter ->
            val rotation = remember { 
                Animatable(if (filters.indexOf(filter) % 2 == 0) -15f else 15f) 
            }
            val scale = remember { Animatable(0.85f) }
            val offsetY = remember { 
                Animatable(if (filters.indexOf(filter) % 2 == 0) 20f else -20f) 
            }

            LaunchedEffect(selectedFilter) {
                if (filter == selectedFilter) {
                    launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        rotation.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                } else {
                    launch {
                        scale.animateTo(
                            targetValue = 0.85f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        rotation.animateTo(
                            targetValue = if (filters.indexOf(filter) % 2 == 0) -15f else 15f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        offsetY.animateTo(
                            targetValue = if (filters.indexOf(filter) % 2 == 0) 20f else -20f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
            }

            FilterThumbnailItem(
                filter = filter,
                isSelected = filter == selectedFilter,
                scale = scale.value,
                rotation = rotation.value,
                offsetY = offsetY.value,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
fun FilterThumbnailItem(
    filter: String,
    isSelected: Boolean,
    scale: Float,
    rotation: Float,
    offsetY: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                translationY = offsetY
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
        )
        FilterPreviewImage(
            filter = filter,
            isSelected = isSelected
        )
    }
}

@Composable
fun FilterPreviewImage(
    filter: String,
    isSelected: Boolean
) {
    val viewModel = viewModel<ImageFilterViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentImageUri = remember(uiState.processedImageUris, uiState.currentImageIndex) {
        if (uiState.processedImageUris.isNotEmpty() && 
            uiState.currentImageIndex < uiState.processedImageUris.size) {
            uiState.processedImageUris[uiState.currentImageIndex]
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.extendedColors.accentTeal else Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .background(Color.Gray.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (currentImageUri != null) {
            val colorMatrix = remember(filter) {
                createColorMatrix(1.0f, 1.0f, filter)
            }

            var isLoading by remember { mutableStateOf(true) }
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(currentImageUri)
                    .size(128, 128)
                    .placeholder(R.drawable.ic_image)
                    .crossfade(true)
                    .memoryCacheKey("filter_preview_${filter}_${currentImageUri.hashCode()}")
                    .build()
            )

            isLoading = painter.state is AsyncImagePainter.State.Loading

            Image(
                painter = painter,
                contentDescription = filter,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_image),
                contentDescription = filter,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
            
            Text(
                text = filter,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 48.dp)
            )
        }
    }
}

/**
 * Bottom navigation for the filter screen
 */
@Composable
fun FilterBottomNavigation(
    activeFeature: String?,
    onFeatureClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.extendedColors.filterSurface,
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bottom navigation items
        val navItems = listOf(
            Triple("Auto", R.drawable.ic_auto, "auto"),
            Triple("Filter", R.drawable.ic_filters, "filter"),
            Triple("Light", R.drawable.ic_adjustments, "light"),
            Triple("Crop", R.drawable.ic_crop, "crop"),
            Triple("Shapes", R.drawable.ic_shapes, "shapes"),
            Triple("Rotate", R.drawable.ic_rotate, "rotate")
        )
        
        navItems.forEach { (text, icon, feature) ->
            FilterNavItem(
                text = text,
                icon = icon,
                isSelected = activeFeature == feature,
                onClick = { onFeatureClick(feature) }
            )
        }
    }
}

@Composable
fun FilterNavItem(
    text: String,
    icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = if (isSelected) MaterialTheme.extendedColors.selectedItem else MaterialTheme.extendedColors.filterText,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.extendedColors.selectedItem else MaterialTheme.extendedColors.filterText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.graphicsLayer {
                alpha = if (isSelected) 1f else 0.7f
            }
        )
    }
}

/**
 * Create a transformable state for image manipulation
 */
@Composable
fun rememberImageTransformableState(
    onTransform: (scale: Float, offsetX: Float, offsetY: Float) -> Unit
): TransformableState {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    return rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offsetX += offsetChange.x
        offsetY += offsetChange.y
        onTransform(scale, offsetX, offsetY)
    }
}

/**
 * Create a color matrix based on brightness, contrast, and filter
 */
fun createColorMatrix(
    brightness: Float,
    contrast: Float,
    filter: String,
): ColorMatrix {
    val scale = contrast
    val translate = (-0.5f * scale + 0.5f) * 255f

    val values = when (filter) {
        "Original" -> floatArrayOf(
            brightness * scale, 0f, 0f, 0f, translate,
            0f, brightness * scale, 0f, 0f, translate,
            0f, 0f, brightness * scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        
        "Clarendon" -> {
            val warmth = 1.1f
            val filterBrightness = 1.1f * brightness
            val adjustedScale = contrast * 1.2f
            val adjustedTranslate = (-0.5f * adjustedScale + 0.5f) * 255f
            
            floatArrayOf(
                (1.2f * warmth) * adjustedScale * filterBrightness, 0f, 0f, 0f, adjustedTranslate,
                0f, 1.1f * adjustedScale * filterBrightness, 0f, 0f, adjustedTranslate,
                0f, 0f, adjustedScale * filterBrightness, 0f, adjustedTranslate + 5f,
                0f, 0f, 0f, 1f, 0f
            )
        }
        
        "Gingham" -> {
            val adjustedScale = contrast * 0.9f
            val adjustedTranslate = (-0.5f * adjustedScale + 0.5f) * 255f
            
            floatArrayOf(
                brightness * adjustedScale * 0.8f, 0.2f, 0f, 0f, adjustedTranslate + 5f,
                0.2f, brightness * adjustedScale * 0.9f, 0f, 0f, adjustedTranslate,
                0f, 0f, brightness * adjustedScale * 1.1f, 0f, adjustedTranslate - 5f,
                0f, 0f, 0f, 1f, 0f
            )
        }
        
        else -> floatArrayOf(
            brightness * scale, 0f, 0f, 0f, translate,
            0f, brightness * scale, 0f, 0f, translate,
            0f, 0f, brightness * scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    }

    return ColorMatrix(values)
}

suspend fun animateImageTransition(
    currentIndex: Int,
    newIndex: Int,
    onUpdateRotationY: (Float) -> Unit,
    onImageSelected: (Int) -> Unit
) {
    val direction = if (newIndex > currentIndex) 1 else -1

    animate(
        initialValue = 0f,
        targetValue = 90f * direction,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    ) { value, _ -> 
        onUpdateRotationY(value)
    }

    onImageSelected(newIndex)
    onUpdateRotationY(-90f * direction)

    animate(
        initialValue = -90f * direction,
        targetValue = 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    ) { value, _ -> 
        onUpdateRotationY(value)
    }
}

/**
 * Function to auto enhance image
 */
fun autoEnhanceImage(
    context: Context,
    uiState: ImageFilterState,
    viewModel: ImageFilterViewModel
) {
    try {
        if (uiState.processedImageUris.isEmpty() || uiState.currentImageIndex >= uiState.processedImageUris.size) {
            Toast.makeText(
                context,
                "No valid image to enhance",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        val bitmap =
            context.contentResolver.openInputStream(
                uiState.processedImageUris[uiState.currentImageIndex]
            )?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

        bitmap?.let { originalBitmap ->
            var totalBrightness = 0L
            var pixelCount = 0
            val sampleStep = 4

            for (x in 0 until originalBitmap.width step sampleStep) {
                for (y in 0 until originalBitmap.height step sampleStep) {
                    val pixel = originalBitmap.getPixel(x, y)
                    val red = android.graphics.Color.red(pixel)
                    val green = android.graphics.Color.green(pixel)
                    val blue = android.graphics.Color.blue(pixel)
                    totalBrightness += (red + green + blue) / 3
                    pixelCount++
                }
            }

            if (pixelCount == 0) {
                return
            }

            val averageBrightness = totalBrightness.toFloat() / (pixelCount * 255f)

            viewModel.setBrightness(
                when {
                    averageBrightness < 0.35f -> 1.4f
                    averageBrightness < 0.45f -> 1.3f
                    averageBrightness > 0.65f -> 0.8f
                    averageBrightness > 0.55f -> 0.9f
                    else -> 1.1f
                }
            )

            viewModel.setContrast(
                when {
                    averageBrightness < 0.35f -> 1.3f
                    averageBrightness < 0.45f -> 1.2f
                    averageBrightness > 0.65f -> 1.15f
                    averageBrightness > 0.55f -> 1.1f
                    else -> 1.15f
                }
            )

            viewModel.setFilter(
                when {
                    averageBrightness < 0.4f -> "Clarendon"
                    averageBrightness > 0.6f -> "Lark"
                    else -> "Reyes"
                }
            )
            viewModel.setFilterIntensity(0.4f)
            originalBitmap.recycle()
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Otomatik iyileştirme sırasında hata oluştu: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
} 