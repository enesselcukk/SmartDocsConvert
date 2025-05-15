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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartdocsconvert.ui.components.AnimatedFeatureControls
import com.example.smartdocsconvert.ui.components.FilterBottomNavigation
import com.example.smartdocsconvert.ui.components.ImageCropEditor
import com.example.smartdocsconvert.ui.components.MainImageEditor
import com.example.smartdocsconvert.ui.components.animateImageTransition
import com.example.smartdocsconvert.ui.components.autoEnhanceImage
import com.example.smartdocsconvert.ui.components.rememberImageTransformableState
import com.example.smartdocsconvert.ui.theme.FilterColors
import com.example.smartdocsconvert.ui.viewmodel.ImageFilterViewModel
import com.example.smartdocsconvert.ui.components.DownloadOptionsDialog
import com.example.smartdocsconvert.ui.components.DownloadAnimation
import com.example.smartdocsconvert.ui.components.AnimatedSaveButton
import com.example.smartdocsconvert.ui.components.DownloadConfirmationDialog

@SuppressLint("RememberReturnType")
@Composable
fun FilterScreen(
    navigateUp: () -> Unit,
    imageUris: List<Uri>,
    viewModel: ImageFilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initialize ViewModel with image URIs
    LaunchedEffect(imageUris) {
        viewModel.initializeWithImages(imageUris)
    }

    // Animation states
    val bottomBarHeight = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

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

    // Image transition states
    val transformableState = rememberImageTransformableState(
        onTransform = { scale, offsetX, offsetY ->
            viewModel.updateTransformState(scale, offsetX, offsetY)
        }
    )

    // Handle toast messages
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Scaffold(
        topBar = {
            FilterTopAppBar(
                onBackClick = {
                    scope.launch {
                        // Exit animation
                        contentAlpha.animateTo(0f, tween(300))
                        bottomBarHeight.animateTo(0f, tween(300))
                        navigateUp()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FilterColors.darkBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                    }
            ) {
                // Main image area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Show appropriate editor view based on active feature
                    when (uiState.activeFeature) {
                        "crop" -> {
                            // Make sure we have valid image URIs before displaying the crop editor
                            if (uiState.processedImageUris.isNotEmpty() && uiState.currentImageIndex < uiState.processedImageUris.size) {
                                // Set default crop rect if needed
                                val defaultCropRect = Rect(0.1f, 0.1f, 0.9f, 0.9f)
                                val currentCropRect = if (uiState.cropRect == Rect(0f, 0f, 1f, 1f)) {
                                    // If using the full default rect, use a more reasonable one instead
                                    defaultCropRect
                                } else {
                                    uiState.cropRect
                                }
                                
                                ImageCropEditor(
                                    imageUri = uiState.processedImageUris[uiState.currentImageIndex],
                                    cropRect = currentCropRect,
                                    onCropRectChange = { viewModel.updateCropRect(it) },
                                    onApplyCrop = { viewModel.applyAndSaveCrop() },
                                    onCancelCrop = { viewModel.updateActiveFeature(null) },
                                    uiState = uiState
                                )
                            } else {
                                // Handle error case - show a message or fallback UI
                                Text(
                                    text = "No image available to crop",
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        else -> {
                            // Make sure we have valid image URIs before displaying the main editor
                            if (uiState.processedImageUris.isNotEmpty()) {
                                MainImageEditor(
                                    uiState = uiState,
                                    transformableState = transformableState,
                                    onImageChanged = { index ->
                                        scope.launch {
                                            animateImageTransition(
                                                currentIndex = uiState.currentImageIndex,
                                                newIndex = index,
                                                onUpdateRotationY = viewModel::updateRotationY,
                                                onImageSelected = viewModel::onImageSelected
                                            )
                                        }
                                    },
                                    viewModel = viewModel
                                )
                            } else {
                                // Handle error case - show a message or fallback UI
                                Text(
                                    text = "No images available to edit",
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    // Loading indicator
                    if (uiState.isLoading || uiState.isImageLoading) {
                        LoadingOverlay()
                    }
                    
                    // Download animation
                    DownloadAnimation(visible = uiState.showDownloadAnimation)
                }

                // Feature-specific controls
                if (uiState.processedImageUris.isNotEmpty() && uiState.currentImageIndex < uiState.processedImageUris.size) {
                    AnimatedFeatureControls(
                        uiState = uiState,
                        onBrightnessChange = viewModel::setBrightness,
                        onContrastChange = viewModel::setContrast,
                        onFilterSelected = viewModel::setFilter
                    )
                }

                // Bottom navigation with animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = (1f - bottomBarHeight.value) * 100f
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        FilterBottomNavigation(
                            activeFeature = uiState.activeFeature,
                            onFeatureClick = { feature ->
                                // Only process feature clicks if we have valid images
                                if (uiState.processedImageUris.isNotEmpty()) {
                                    when (feature) {
                                        "auto" -> {
                                            if (uiState.activeFeature == "auto") {
                                                viewModel.resetImageAdjustments()
                                                viewModel.updateActiveFeature(null)
                                            } else {
                                                viewModel.updateActiveFeature("auto")
                                                scope.launch {
                                                    autoEnhanceImage(context, uiState, viewModel)
                                                }
                                            }
                                        }
                                        "rotate" -> viewModel.rotateImage()
                                        else -> viewModel.updateActiveFeature(
                                            if (uiState.activeFeature == feature) null else feature
                                        )
                                    }
                                } else {
                                    // Show a toast if no images available
                                    Toast.makeText(context, "No images available to edit", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        
                        // Only show the download button if we have images and no feature is currently active
                        if (uiState.processedImageUris.isNotEmpty() && uiState.activeFeature == null) {
                            AnimatedSaveButton(
                                onClick = { viewModel.toggleDownloadOptions() },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
            
            // Download options dialog
            DownloadOptionsDialog(
                visible = uiState.showDownloadOptions,
                onDismiss = { viewModel.hideDownloadOptions() },
                onSaveAsImage = { viewModel.prepareImageDownload() },
                onSaveAsPdf = { viewModel.preparePdfDownload() }
            )
            
            // Download confirmation dialog
            DownloadConfirmationDialog(
                visible = uiState.showDownloadConfirmation,
                filename = uiState.pendingDownloadFilename ?: "",
                downloadType = uiState.pendingDownloadType,
                onConfirm = { viewModel.confirmDownload() },
                onCancel = { viewModel.cancelDownload() },
                onFilenameChanged = { viewModel.updateDownloadFilename(it) },
                onDownloadAllChanged = { viewModel.updateDownloadAllImages(it) },
                downloadAll = uiState.downloadAllImages
            )
        }
    }
}

/**
 * Top app bar for the filter screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterTopAppBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "Filtreler",
                color = FilterColors.textColor,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = FilterColors.surfaceColor,
            titleContentColor = FilterColors.textColor
        ),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = FilterColors.textColor
                )
            }
        }
    )
}

/**
 * Loading overlay for image processing operations
 */
@Composable
private fun LoadingOverlay() {
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
