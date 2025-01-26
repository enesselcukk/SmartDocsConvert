package com.example.smartdocsconvert.ui.screens.image

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.smartdocsconvert.R
import androidx.compose.foundation.gestures.*
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.example.smartdocsconvert.ui.components.CropPointsOverlay
import androidx.compose.ui.unit.IntSize
import com.example.smartdocsconvert.ui.components.ShapesOverlay
import android.os.Environment
import android.widget.Toast
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.content.FileProvider
import coil.compose.AsyncImagePainter
import kotlin.math.abs


@SuppressLint("RememberReturnType")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageFilterScreen(
    navController: NavController,
    imageUris: List<Uri>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Modern color palette
    val darkBackground = Color(0xFF1E1E2E)  // Koyu arka plan
    val surfaceColor = Color(0xFF313244)    // Yüzey rengi
    val textColor = Color(0xFFCDD6F4)       // Metin rengi
    val selectedColor = Color(0xFFA6E3A1)   // Seçili öğe rengi

    // Animation states
    val bottomBarHeight = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    // Image transition animation state
    var rotationY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current.density
    var cameraDistance by remember { mutableFloatStateOf(8f * density) }

    // Initial animations
    LaunchedEffect(Unit) {
        launch {
            bottomBarHeight.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(500)
            )
        }
    }

    var currentImageIndex by remember { mutableIntStateOf(0) }
    var previousImageIndex by remember { mutableIntStateOf(0) }
    var transitionDirection by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isImageLoading by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageSize by remember { mutableStateOf(IntSize(0, 0)) }
    var rotationAngles by remember { mutableStateOf(List(imageUris.size) { 0f }) }
    var activeFeature by remember { mutableStateOf<String?>(null) }
    var processedImageUris by remember { mutableStateOf(imageUris) }

    // Reset activeFeature when changing images
    LaunchedEffect(currentImageIndex) {
        activeFeature = null
    }

    // Filter and adjustment states for each image
    var brightnessValues by remember { mutableStateOf(List(imageUris.size) { 1f }) }
    var contrastValues by remember { mutableStateOf(List(imageUris.size) { 1f }) }
    var selectedFilters by remember { mutableStateOf(List(imageUris.size) { "Original" }) }
    var filterIntensityValues by remember { mutableStateOf(List(imageUris.size) { 0f }) }

    // Transform states
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Crop states
    var cropRect by remember { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }
    val cropScale by remember { mutableFloatStateOf(1f) }
    val cropOffset by remember { mutableStateOf(Offset.Zero) }

    // Function to get current image values
    fun getCurrentBrightness(): Float = brightnessValues[currentImageIndex]
    fun setCurrentBrightness(value: Float) {
        brightnessValues = brightnessValues.toMutableList().also { it[currentImageIndex] = value }
    }

    fun getCurrentContrast(): Float = contrastValues[currentImageIndex]
    fun setCurrentContrast(value: Float) {
        contrastValues = contrastValues.toMutableList().also { it[currentImageIndex] = value }
    }

    fun getCurrentFilter(): String = selectedFilters[currentImageIndex]
    fun setCurrentFilter(value: String) {
        selectedFilters = selectedFilters.toMutableList().also { it[currentImageIndex] = value }
    }

    fun getCurrentFilterIntensity(): Float = filterIntensityValues[currentImageIndex]
    fun setCurrentFilterIntensity(value: Float) {
        filterIntensityValues =
            filterIntensityValues.toMutableList().also { it[currentImageIndex] = value }
    }

    // Function to reset image adjustments
    fun resetImageAdjustments(index: Int) {
        brightnessValues = brightnessValues.toMutableList().also { it[index] = 1f }
        contrastValues = contrastValues.toMutableList().also { it[index] = 1f }
        selectedFilters = selectedFilters.toMutableList().also { it[index] = "Original" }
        filterIntensityValues = filterIntensityValues.toMutableList().also { it[index] = 0f }
    }

    // Function to animate image transition
    suspend fun animateImageTransition(newIndex: Int) {
        val direction = if (newIndex > currentImageIndex) 1 else -1
        transitionDirection = direction
        previousImageIndex = currentImageIndex

        // Flip out current image
        animate(
            initialValue = rotationY,
            targetValue = 90f * direction,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ) { value, _ -> rotationY = value }
        
        // Update current image index
        currentImageIndex = newIndex
        rotationY = -90f * direction
        
        // Flip in new image
        animate(
            initialValue = rotationY,
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ) { value, _ -> rotationY = value }
    }

    // Function to apply crop and save the image
    suspend fun applyCropAndSave() {
        isLoading = true
        try {
            val bitmap =
                context.contentResolver.openInputStream(processedImageUris[currentImageIndex])
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }

            bitmap?.let { originalBitmap ->
                // Calculate actual crop dimensions based on original image size
                val cropX = (cropRect.left * originalBitmap.width).toInt()
                val cropY = (cropRect.top * originalBitmap.height).toInt()
                val cropWidth = ((cropRect.right - cropRect.left) * originalBitmap.width).toInt()
                val cropHeight = ((cropRect.bottom - cropRect.top) * originalBitmap.height).toInt()

                // Ensure dimensions are valid
                val finalCropX = cropX.coerceIn(0, originalBitmap.width - 1)
                val finalCropY = cropY.coerceIn(0, originalBitmap.height - 1)
                val finalCropWidth = cropWidth.coerceIn(1, originalBitmap.width - finalCropX)
                val finalCropHeight = cropHeight.coerceIn(1, originalBitmap.height - finalCropY)

                // Create cropped bitmap
                val croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    finalCropX,
                    finalCropY,
                    finalCropWidth,
                    finalCropHeight
                )

                // Save the processed image
                val fileName =
                    "processed_image_${currentImageIndex}_${System.currentTimeMillis()}.jpg"
                val outputFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    fileName
                )

                FileOutputStream(outputFile).use { out ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                // Update the URI list with the new processed image
                val newUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    outputFile
                )

                val newUris = processedImageUris.toMutableList()
                newUris[currentImageIndex] = newUri
                processedImageUris = newUris

                // Clean up
                croppedBitmap.recycle()
                originalBitmap.recycle()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Resim işlenirken hata oluştu: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            // Reset crop mode
            activeFeature = null
            isLoading = false
        }
    }

    // Function to auto enhance image
    suspend fun autoEnhanceImage() {
        isLoading = true
        try {
            val bitmap =
                context.contentResolver.openInputStream(processedImageUris[currentImageIndex])
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }

            bitmap?.let { originalBitmap ->
                // Calculate average brightness using sampling
                var totalBrightness = 0L
                var pixelCount = 0
                val sampleStep = 4  // Sample every 4th pixel for faster processing

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

                val averageBrightness = totalBrightness.toFloat() / (pixelCount * 255f)

                // Auto adjust brightness and contrast
                setCurrentBrightness(
                    when {
                        averageBrightness < 0.35f -> 1.4f  // Very dark image
                        averageBrightness < 0.45f -> 1.3f  // Dark image
                        averageBrightness > 0.65f -> 0.8f  // Very bright image
                        averageBrightness > 0.55f -> 0.9f  // Bright image
                        else -> 1.1f  // Normal image
                    }
                )

                setCurrentContrast(
                    when {
                        averageBrightness < 0.35f -> 1.3f  // Increase contrast for very dark images
                        averageBrightness < 0.45f -> 1.2f  // Increase contrast for dark images
                        averageBrightness > 0.65f -> 1.15f // Slight contrast for very bright images
                        averageBrightness > 0.55f -> 1.1f  // Slight contrast for bright images
                        else -> 1.15f  // Normal contrast
                    }
                )

                // Apply color enhancement based on brightness
                setCurrentFilter(
                    when {
                        averageBrightness < 0.4f -> "Clarendon"  // More vibrant for dark images
                        averageBrightness > 0.6f -> "Lark"       // Softer for bright images
                        else -> "Reyes"                          // Balanced for normal images
                    }
                )
                setCurrentFilterIntensity(0.4f)

                // Clean up
                originalBitmap.recycle()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Otomatik iyileştirme sırasında hata oluştu: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            isLoading = false
        }
    }

    // Load the current image
    LaunchedEffect(processedImageUris[currentImageIndex]) {
        isImageLoading = true
        try {
            val bitmap =
                context.contentResolver.openInputStream(processedImageUris[currentImageIndex])
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
            bitmap?.let {
                imageBitmap = it.asImageBitmap()
                imageSize = IntSize(it.width, it.height)
            }
        } finally {
            isImageLoading = false
        }
    }

    val filters = listOf(
        "Original",
        "Clarendon",
        "Gingham",
        "Moon",
        "Lark",
        "Reyes",
        "Juno"
    )

    // Create ColorMatrix for brightness and contrast
    val colorMatrix = remember(
        getCurrentBrightness(),
        getCurrentContrast(),
        getCurrentFilter(),
        getCurrentFilterIntensity()
    ) {
        when (getCurrentFilter()) {
            "Original" -> ColorMatrix().apply {
                // Apply only brightness and contrast
                val scale = getCurrentContrast()
                val translate = (-.5f * scale + .5f) * 255f

                val matrix = floatArrayOf(
                    getCurrentBrightness() * scale, 0f, 0f, 0f, translate,
                    0f, getCurrentBrightness() * scale, 0f, 0f, translate,
                    0f, 0f, getCurrentBrightness() * scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
                matrix.forEachIndexed { index, value -> values[index] = value }
            }

            "Clarendon" -> ColorMatrix().apply {
                // Increased contrast, slightly warm, vibrant colors
                val saturation = 1.3f
                val warmth = 1.1f
                val filterBrightness = 1.1f * getCurrentBrightness()
                val scale = getCurrentContrast() * 1.2f
                val translate = (-.5f * scale + .5f) * 255f

                val matrix = floatArrayOf(
                    (1.2f * warmth) * scale * filterBrightness, 0f, 0f, 0f, translate,
                    0f, 1.1f * scale * filterBrightness, 0f, 0f, translate,
                    0f, 0f, scale * filterBrightness, 0f, translate + 5f,
                    0f, 0f, 0f, 1f, 0f
                )
                matrix.forEachIndexed { index, value -> values[index] = value }
            }

            "Gingham" -> ColorMatrix().apply {
                // Vintage look, slightly desaturated, soft contrast
                val filterBrightness = 1.05f * getCurrentBrightness()
                val scale = getCurrentContrast() * 0.9f
                val translate = (-.5f * scale + .5f) * 255f

                val matrix = floatArrayOf(
                    0.95f * scale * filterBrightness, 0.05f, 0f, 0f, translate + 10f,
                    0.05f, 0.95f * scale * filterBrightness, 0f, 0f, translate + 10f,
                    0f, 0f, 0.9f * scale * filterBrightness, 0f, translate + 5f,
                    0f, 0f, 0f, 1f, 0f
                )
                matrix.forEachIndexed { index, value -> values[index] = value }
            }

            "Moon" -> ColorMatrix().apply {
                // Black and white with enhanced contrast
                val filterBrightness = 1.1f * getCurrentBrightness()
                val scale = getCurrentContrast() * 1.3f
                val translate = (-.5f * scale + .5f) * 255f

                val r = 0.3f
                val g = 0.59f
                val b = 0.11f

                val matrix = floatArrayOf(
                    r * scale * filterBrightness,
                    g * scale * filterBrightness,
                    b * scale * filterBrightness,
                    0f,
                    translate,
                    r * scale * filterBrightness,
                    g * scale * filterBrightness,
                    b * scale * filterBrightness,
                    0f,
                    translate,
                    r * scale * filterBrightness,
                    g * scale * filterBrightness,
                    b * scale * filterBrightness,
                    0f,
                    translate,
                    0f,
                    0f,
                    0f,
                    1f,
                    0f
                )
                matrix.forEachIndexed { index, value -> values[index] = value }
            }

            "Lark" -> ColorMatrix().apply {
                // Brightened exposure, reduced saturation in reds
                val filterBrightness = 1.15f * getCurrentBrightness()
                val scale = getCurrentContrast() * 1.1f
                val translate = (-.5f * scale + .5f) * 255f

                val matrix = floatArrayOf(
                    0.9f * scale * filterBrightness, 0.1f, 0f, 0f, translate + 15f,
                    0.05f, scale * filterBrightness, 0f, 0f, translate + 10f,
                    0f, 0.05f, 1.1f * scale * filterBrightness, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
                matrix.forEachIndexed { index, value -> values[index] = value }
            }

            "Reyes" -> ColorMatrix().apply {
                // Washed out vintage look
                val filterBrightness = 1.2f * getCurrentBrightness()
                val scale = getCurrentContrast() * 0.85f
                val translate = (-.5f * scale + .5f) * 255f

                val matrix = floatArrayOf(
                    0.9f * scale * filterBrightness, 0.1f, 0f, 0f, translate + 20f,
                    0.1f, 0.85f * scale * filterBrightness, 0.05f, 0f, translate + 20f,
                    0f, 0.05f, 0.9f * scale * filterBrightness, 0f, translate + 10f,
                    0f, 0f, 0f, 1f, 0f
                )
                matrix.forEachIndexed { index, value -> values[index] = value }
            }

            "Juno" -> ColorMatrix().apply {
                // Warm tint, enhanced reds
                val filterBrightness = 1.1f * getCurrentBrightness()
                val scale = getCurrentContrast() * 1.15f
                val translate = (-.5f * scale + .5f) * 255f

                val matrix = floatArrayOf(
                    1.3f * scale * filterBrightness, 0f, 0f, 0f, translate + 5f,
                    0.1f, 1.1f * scale * filterBrightness, 0f, 0f, translate,
                    0f, 0f, 0.9f * scale * filterBrightness, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
                matrix.forEachIndexed { index, value -> values[index] = value }
            }

            else -> {
                val baseMatrix = when (getCurrentFilter()) {
                    "Clarendon" -> ColorMatrix().apply {
                        // Increased contrast, slightly warm, vibrant colors
                        val saturation = 1.3f
                        val warmth = 1.1f
                        val filterBrightness = 1.1f * getCurrentBrightness()
                        val scale = getCurrentContrast() * 1.2f
                        val translate = (-.5f * scale + .5f) * 255f

                        val matrix = floatArrayOf(
                            (1.2f * warmth) * scale * filterBrightness, 0f, 0f, 0f, translate,
                            0f, 1.1f * scale * filterBrightness, 0f, 0f, translate,
                            0f, 0f, scale * filterBrightness, 0f, translate + 5f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        matrix.forEachIndexed { index, value -> values[index] = value }
                    }

                    "Gingham" -> ColorMatrix().apply {
                        // Vintage look, slightly desaturated, soft contrast
                        val filterBrightness = 1.05f * getCurrentBrightness()
                        val scale = getCurrentContrast() * 0.9f
                        val translate = (-.5f * scale + .5f) * 255f

                        val matrix = floatArrayOf(
                            0.95f * scale * filterBrightness, 0.05f, 0f, 0f, translate + 10f,
                            0.05f, 0.95f * scale * filterBrightness, 0f, 0f, translate + 10f,
                            0f, 0f, 0.9f * scale * filterBrightness, 0f, translate + 5f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        matrix.forEachIndexed { index, value -> values[index] = value }
                    }

                    "Moon" -> ColorMatrix().apply {
                        // Black and white with enhanced contrast
                        val filterBrightness = 1.1f * getCurrentBrightness()
                        val scale = getCurrentContrast() * 1.3f
                        val translate = (-.5f * scale + .5f) * 255f

                        val r = 0.3f
                        val g = 0.59f
                        val b = 0.11f

                        val matrix = floatArrayOf(
                            r * scale * filterBrightness,
                            g * scale * filterBrightness,
                            b * scale * filterBrightness,
                            0f,
                            translate,
                            r * scale * filterBrightness,
                            g * scale * filterBrightness,
                            b * scale * filterBrightness,
                            0f,
                            translate,
                            r * scale * filterBrightness,
                            g * scale * filterBrightness,
                            b * scale * filterBrightness,
                            0f,
                            translate,
                            0f,
                            0f,
                            0f,
                            1f,
                            0f
                        )
                        matrix.forEachIndexed { index, value -> values[index] = value }
                    }

                    "Lark" -> ColorMatrix().apply {
                        // Brightened exposure, reduced saturation in reds
                        val filterBrightness = 1.15f * getCurrentBrightness()
                        val scale = getCurrentContrast() * 1.1f
                        val translate = (-.5f * scale + .5f) * 255f

                        val matrix = floatArrayOf(
                            0.9f * scale * filterBrightness,
                            0.1f,
                            0f,
                            0f,
                            translate + 15f,
                            0.05f,
                            scale * filterBrightness,
                            0f,
                            0f,
                            translate + 10f,
                            0f,
                            0.05f,
                            1.1f * scale * filterBrightness,
                            0f,
                            translate,
                            0f,
                            0f,
                            0f,
                            1f,
                            0f
                        )

                        matrix.forEachIndexed { index, value -> values[index] = value }
                    }

                    "Reyes" -> ColorMatrix().apply {
                        // Washed out vintage look
                        val filterBrightness = 1.2f * getCurrentBrightness()
                        val scale = getCurrentContrast() * 0.85f
                        val translate = (-.5f * scale + .5f) * 255f

                        val matrix = floatArrayOf(
                            0.9f * scale * filterBrightness,
                            0.1f,
                            0f,
                            0f,
                            translate + 20f,
                            0.1f,
                            0.85f * scale * filterBrightness,
                            0.05f,
                            0f,
                            translate + 20f,
                            0f,
                            0.05f,
                            0.9f * scale * filterBrightness,
                            0f,
                            translate + 10f,
                            0f,
                            0f,
                            0f,
                            1f,
                            0f
                        )

                        matrix.forEachIndexed { index, value -> values[index] = value }
                    }

                    "Juno" -> ColorMatrix().apply {
                        // Warm tint, enhanced reds
                        val filterBrightness = 1.1f * getCurrentBrightness()
                        val scale = getCurrentContrast() * 1.15f
                        val translate = (-.5f * scale + .5f) * 255f

                        val matrix = floatArrayOf(
                            1.3f * scale * filterBrightness,
                            0f,
                            0f,
                            0f,
                            translate + 5f,
                            0.1f,
                            1.1f * scale * filterBrightness,
                            0f,
                            0f,
                            translate,
                            0f,
                            0f,
                            0.9f * scale * filterBrightness,
                            0f,
                            translate,
                            0f,
                            0f,
                            0f,
                            1f,
                            0f
                        )

                        matrix.forEachIndexed { index, value -> values[index] = value }
                    }

                    else -> ColorMatrix()
                }

                // Interpolate between original and filter based on intensity
                ColorMatrix().apply {
                    val originalMatrix = ColorMatrix()
                    for (i in 0..19) {
                        values[i] =
                            originalMatrix.values[i] * (1 - getCurrentFilterIntensity()) + baseMatrix.values[i] * getCurrentFilterIntensity()
                    }
                }
            }
        }
    }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offsetX += offsetChange.x
        offsetY += offsetChange.y
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Filtreler",
                        color = textColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                // Exit animation
                                contentAlpha.animateTo(0f, tween(300))
                                bottomBarHeight.animateTo(0f, tween(300))
                                navController.navigateUp()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                    }
            ) {
                // Main image area with navigation arrows
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp)
                    ) {
                        if (activeFeature == "crop") {
                            ImageCropView(
                                imageUri = processedImageUris[currentImageIndex],
                                cropRect = cropRect,
                                cropScale = cropScale,
                                cropOffset = cropOffset,
                                onCropRectChange = { cropRect = it },
                                colorMatrix = colorMatrix,
                                currentImageIndex = currentImageIndex
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Previous image (for transition)
                                if (abs(rotationY) > 90f && previousImageIndex >= 0 && previousImageIndex < processedImageUris.size) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(LocalContext.current)
                                                .data(processedImageUris[previousImageIndex])
                                                .build()
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                translationX = offsetX
                                                translationY = offsetY
                                                rotationZ = rotationAngles[previousImageIndex]
                                                rotationY = rotationY
                                                cameraDistance = cameraDistance
                                            }
                                            .transformable(state = transformableState),
                                        contentScale = ContentScale.Fit,
                                        colorFilter = ColorFilter.colorMatrix(colorMatrix)
                                    )
                                }

                                // Current image
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(processedImageUris[currentImageIndex])
                                            .build()
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            translationX = offsetX
                                            translationY = offsetY
                                            rotationZ = rotationAngles[currentImageIndex]
                                            rotationY = rotationY
                                            cameraDistance = cameraDistance
                                        }
                                        .transformable(state = transformableState),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                                )
                            }
                        }

                        // Loading indicator for image changes
                        if (isImageLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF2196F3),
                                        modifier = Modifier.size(64.dp),
                                        strokeWidth = 6.dp
                                    )
                                    Text(
                                        text = "Resim iyileştiriliyor...",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    // Navigation arrows and counter
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Left arrow for previous image
                        if (currentImageIndex > 0) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        animateImageTransition(currentImageIndex - 1)
                                    }
                                },
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

                        // Right arrow for next image
                        if (currentImageIndex < processedImageUris.size - 1) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        animateImageTransition(currentImageIndex + 1)
                                    }
                                },
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
                        // Add shapes overlay when active feature is "shapes"
                        this@Column.AnimatedVisibility(visible = activeFeature == "shapes") {
                            ShapesOverlay()
                        }

                        // Image counter
                        Text(
                            text = "${currentImageIndex + 1}/${processedImageUris.size}",
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

                // Content based on selected view
                AnimatedVisibility(visible = activeFeature == "light") {
                    // Adjustment sliders
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp)
                    ) {
                        // Light control
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
                                    value = getCurrentBrightness(),
                                    onValueChange = { setCurrentBrightness(it) },
                                    valueRange = 0.5f..2f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            Text(
                                text = String.format("%.1f", (getCurrentBrightness() - 1f) * 100),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Grain control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grain",
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
                                    value = getCurrentContrast(),
                                    onValueChange = { setCurrentContrast(it) },
                                    valueRange = 0.5f..2f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            Text(
                                text = "${((getCurrentContrast() - 1f) * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                AnimatedVisibility(visible = activeFeature == "filter") {
                    // Filter thumbnails
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy((-30).dp),
                        contentPadding = PaddingValues(horizontal = 40.dp)
                    ) {
                        items(filters) { filter ->
                            val rotation =
                                remember { Animatable(if (filters.indexOf(filter) % 2 == 0) -15f else 15f) }
                            val scale = remember { Animatable(0.85f) }
                            val offsetY =
                                remember { Animatable(if (filters.indexOf(filter) % 2 == 0) 20f else -20f) }

                            LaunchedEffect(getCurrentFilter()) {
                                if (filter == getCurrentFilter()) {
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

                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(140.dp)
                                    .graphicsLayer {
                                        scaleX = scale.value
                                        scaleY = scale.value
                                        rotationZ = rotation.value
                                        translationY = offsetY.value
                                    }
                                    .clickable { setCurrentFilter(filter) }
                            ) {
                                // Shadow effect
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

                                // Main image with border
                                Image(
                                    painter = rememberAsyncImagePainter(processedImageUris[currentImageIndex]),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 2.dp,
                                            color = if (filter == getCurrentFilter()) Color(
                                                0xFF2196F3
                                            ) else Color.White,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = when (filter) {
                                        "Original" -> null
                                        "Clarendon" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                                            val saturation = 1.3f
                                            val warmth = 1.1f
                                            val brightness = 1.1f
                                            val scale = getCurrentContrast() * 1.2f
                                            val translate = (-.5f * scale + .5f) * 255f

                                            val matrix = floatArrayOf(
                                                (1.2f * warmth) * scale * brightness,
                                                0f,
                                                0f,
                                                0f,
                                                translate,
                                                0f,
                                                1.1f * scale * brightness,
                                                0f,
                                                0f,
                                                translate,
                                                0f,
                                                0f,
                                                scale * brightness,
                                                0f,
                                                translate + 5f,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f
                                            )

                                            matrix.forEachIndexed { index, value ->
                                                values[index] = value
                                            }
                                        })

                                        "Gingham" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                                            val filterBrightness = 1.05f * getCurrentBrightness()
                                            val scale = getCurrentContrast() * 0.9f
                                            val translate = (-.5f * scale + .5f) * 255f

                                            val matrix = floatArrayOf(
                                                0.95f * scale * filterBrightness,
                                                0.05f,
                                                0f,
                                                0f,
                                                translate + 10f,
                                                0.05f,
                                                0.95f * scale * filterBrightness,
                                                0f,
                                                0f,
                                                translate + 10f,
                                                0f,
                                                0f,
                                                0.9f * scale * filterBrightness,
                                                0f,
                                                translate + 5f,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f
                                            )

                                            matrix.forEachIndexed { index, value ->
                                                values[index] = value
                                            }
                                        })

                                        "Moon" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                                            val filterBrightness = 1.1f * getCurrentBrightness()
                                            val scale = getCurrentContrast() * 1.3f
                                            val translate = (-.5f * scale + .5f) * 255f

                                            val r = 0.3f
                                            val g = 0.59f
                                            val b = 0.11f

                                            val matrix = floatArrayOf(
                                                r * scale * filterBrightness,
                                                g * scale * filterBrightness,
                                                b * scale * filterBrightness,
                                                0f,
                                                translate,
                                                r * scale * filterBrightness,
                                                g * scale * filterBrightness,
                                                b * scale * filterBrightness,
                                                0f,
                                                translate,
                                                r * scale * filterBrightness,
                                                g * scale * filterBrightness,
                                                b * scale * filterBrightness,
                                                0f,
                                                translate,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f
                                            )

                                            matrix.forEachIndexed { index, value ->
                                                values[index] = value
                                            }
                                        })

                                        "Lark" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                                            val filterBrightness = 1.15f * getCurrentBrightness()
                                            val scale = getCurrentContrast() * 1.1f
                                            val translate = (-.5f * scale + .5f) * 255f

                                            val matrix = floatArrayOf(
                                                0.9f * scale * filterBrightness,
                                                0.1f,
                                                0f,
                                                0f,
                                                translate + 15f,
                                                0.05f,
                                                scale * filterBrightness,
                                                0f,
                                                0f,
                                                translate + 10f,
                                                0f,
                                                0.05f,
                                                1.1f * scale * filterBrightness,
                                                0f,
                                                translate,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f
                                            )

                                            matrix.forEachIndexed { index, value ->
                                                values[index] = value
                                            }
                                        })

                                        "Reyes" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                                            val filterBrightness = 1.2f * getCurrentBrightness()
                                            val scale = getCurrentContrast() * 0.85f
                                            val translate = (-.5f * scale + .5f) * 255f

                                            val matrix = floatArrayOf(
                                                0.9f * scale * filterBrightness,
                                                0.1f,
                                                0f,
                                                0f,
                                                translate + 20f,
                                                0.1f,
                                                0.85f * scale * filterBrightness,
                                                0.05f,
                                                0f,
                                                translate + 20f,
                                                0f,
                                                0.05f,
                                                0.9f * scale * filterBrightness,
                                                0f,
                                                translate + 10f,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f
                                            )

                                            matrix.forEachIndexed { index, value ->
                                                values[index] = value
                                            }
                                        })

                                        "Juno" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                                            val filterBrightness = 1.1f * getCurrentBrightness()
                                            val scale = getCurrentContrast() * 1.15f
                                            val translate = (-.5f * scale + .5f) * 255f

                                            val matrix = floatArrayOf(
                                                1.3f * scale * filterBrightness,
                                                0f,
                                                0f,
                                                0f,
                                                translate + 5f,
                                                0.1f,
                                                1.1f * scale * filterBrightness,
                                                0f,
                                                0f,
                                                translate,
                                                0f,
                                                0f,
                                                0.9f * scale * filterBrightness,
                                                0f,
                                                translate,
                                                0f,
                                                0f,
                                                0f,
                                                1f,
                                                0f
                                            )

                                            matrix.forEachIndexed { index, value ->
                                                values[index] = value
                                            }
                                        })

                                        else -> null
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom navigation with animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = (1f - bottomBarHeight.value) * 100f
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                surfaceColor.copy(alpha = 0.95f),
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bottom navigation items with animations
                        listOf(
                            Triple("Auto", R.drawable.ic_auto, "auto"),
                            Triple("Filter", R.drawable.ic_filters, "filter"),
                            Triple("Light", R.drawable.ic_adjustments, "light"),
                            Triple("Crop", R.drawable.ic_crop, "crop"),
                            Triple("Shapes", R.drawable.ic_shapes, "shapes"),
                            Triple("Rotate", R.drawable.ic_rotate, "rotate")
                        ).forEach { (text, icon, feature) ->
                            val selected = activeFeature == feature
                            val scale = remember { Animatable(1f) }

                            LaunchedEffect(selected) {
                                if (selected) {
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
                                IconButton(
                                    onClick = {
                                        when (feature) {
                                            "auto" -> {
                                                if (activeFeature == "auto") {
                                                    resetImageAdjustments(currentImageIndex)
                                                    activeFeature = null
                                                } else {
                                                    activeFeature = "auto"
                                                    scope.launch {
                                                        autoEnhanceImage()
                                                    }
                                                }
                                            }

                                            "rotate" -> {
                                                val newRotationAngles =
                                                    rotationAngles.toMutableList()
                                                newRotationAngles[currentImageIndex] =
                                                    (newRotationAngles[currentImageIndex] + 90f) % 360f
                                                rotationAngles = newRotationAngles
                                            }

                                            else -> activeFeature =
                                                if (activeFeature == feature) null else feature
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = icon),
                                        contentDescription = text,
                                        tint = if (selected) selectedColor else textColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = text,
                                    color = if (selected) selectedColor else textColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = if (selected) 1f else 0.7f
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageCropView(
    imageUri: Uri,
    cropRect: Rect,
    cropScale: Float,
    cropOffset: Offset,
    onCropRectChange: (Rect) -> Unit,
    colorMatrix: ColorMatrix,
    currentImageIndex: Int
) {
    var imageSize by remember { mutableStateOf<IntSize?>(null) }
    var viewSize by remember { mutableStateOf<IntSize?>(null) }
    var imageBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewSize = size
            },
        contentAlignment = Alignment.Center
    ) {
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .build()
        )

        // Calculate image bounds when the image is loaded
        LaunchedEffect(painter.state) {
            when (val state = painter.state) {
                is AsyncImagePainter.State.Success -> {
                    val intrinsicSize = state.painter.intrinsicSize
                    imageSize = IntSize(intrinsicSize.width.toInt(), intrinsicSize.height.toInt())

                    viewSize?.let { vSize ->
                        // Calculate the actual bounds of the image within the view
                        val imageAspectRatio = intrinsicSize.width / intrinsicSize.height
                        val viewAspectRatio = vSize.width.toFloat() / vSize.height

                        val (width, height) = if (imageAspectRatio > viewAspectRatio) {
                            // Image is wider than view
                            vSize.width.toFloat() to (vSize.width / imageAspectRatio)
                        } else {
                            // Image is taller than view
                            (vSize.height * imageAspectRatio) to vSize.height.toFloat()
                        }

                        val left = (vSize.width - width) / 2
                        val top = (vSize.height - height) / 2

                        imageBounds = Rect(left, top, left + width, top + height)
                    }
                }

                else -> {}
            }
        }

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = cropScale,
                    scaleY = cropScale,
                    translationX = cropOffset.x,
                    translationY = cropOffset.y
                ),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(colorMatrix)
        )

        // Crop overlay with points
        imageBounds?.let { bounds ->
            CropPointsOverlay(
                modifier = Modifier.fillMaxSize(),
                imageBounds = bounds,
                onPointsSelected = { points ->
                    if (points.size == 4) {
                        // Sort points to ensure consistent order: top-left, top-right, bottom-right, bottom-left
                        val sortedPoints = points.sortedWith(compareBy({ it.y }, { it.x }))
                        val topPoints = sortedPoints.take(2).sortedBy { it.x }
                        val bottomPoints = sortedPoints.takeLast(2).sortedBy { it.x }
                        val orderedPoints = topPoints + bottomPoints.reversed()

                        // Convert view coordinates to relative coordinates within the image bounds
                        val relativeRect = Rect(
                            left = ((orderedPoints[0].x - bounds.left) / bounds.width).coerceInRange(
                                0f,
                                1f
                            ),
                            top = ((orderedPoints[0].y - bounds.top) / bounds.height).coerceInRange(
                                0f,
                                1f
                            ),
                            right = ((orderedPoints[2].x - bounds.left) / bounds.width).coerceInRange(
                                0f,
                                1f
                            ),
                            bottom = ((orderedPoints[2].y - bounds.top) / bounds.height).coerceInRange(
                                0f,
                                1f
                            )
                        )
                        onCropRectChange(relativeRect)
                    }
                }
            )
        }
    }
}

private fun Float.coerceInRange(min: Float, max: Float): Float {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}
