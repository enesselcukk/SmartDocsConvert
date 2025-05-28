package com.example.smartdocsconvert.ui.screens.image

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.smartdocsconvert.R
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Rect
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartdocsconvert.ui.components.AnimatedFeatureControls
import com.example.smartdocsconvert.ui.components.FilterBottomNavigation
import com.example.smartdocsconvert.ui.components.ImageCropEditor
import com.example.smartdocsconvert.ui.components.MainImageEditor
import com.example.smartdocsconvert.ui.components.animateImageTransition
import com.example.smartdocsconvert.ui.components.autoEnhanceImage
import com.example.smartdocsconvert.ui.components.rememberImageTransformableState
import com.example.smartdocsconvert.ui.theme.extendedColors
import com.example.smartdocsconvert.ui.viewmodel.ImageFilterViewModel
import com.example.smartdocsconvert.ui.components.DownloadAnimation
import com.example.smartdocsconvert.ui.components.DownloadConfirmationDialog
import kotlinx.coroutines.delay
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

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

    // Animation states for background elements
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Primary gradient animation
    val primaryGradientScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primaryScale"
    )
    
    val primaryGradientAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primaryAlpha"
    )

    // Secondary gradient animation
    val secondaryGradientScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondaryScale"
    )

    // Gradient rotation animation
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "gradientRotation"
    )

    // Animation states for the download button
    var isDownloadExpanded by remember { mutableStateOf(false) }
    val downloadButtonWidth by animateFloatAsState(
        targetValue = if (isDownloadExpanded) 180f else 56f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "width"
    )
    
    val downloadButtonAlpha by animateFloatAsState(
        targetValue = if (isDownloadExpanded) 1f else 0f,
        animationSpec = tween(200),
        label = "alpha"
    )
    
    val downloadIconRotation by animateFloatAsState(
        targetValue = if (isDownloadExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    LaunchedEffect(imageUris) {
        viewModel.initializeWithImages(imageUris)
    }

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            delay(500)
            viewModel.resetNavigateBack()
            navigateUp()
        }
    }

    val bottomBarHeight = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

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


    val transformableState = rememberImageTransformableState(
        onTransform = { scale, offsetX, offsetY ->
            viewModel.updateTransformState(scale, offsetX, offsetY)
        }
    )


    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }


    BackHandler {
        if (uiState.activeFeature != null) {
            viewModel.updateActiveFeature(null)
        } else if (uiState.showDownloadOptions) {
            viewModel.hideDownloadOptions()
        } else if (uiState.showDownloadConfirmation) {
            viewModel.cancelDownload()
        } else {
            navigateUp()
        }
    }

    Scaffold(
        topBar = {
            FilterTopAppBar(
                onBackClick = {
                    navigateUp()
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.extendedColors.filterBackground)
                .padding(paddingValues)
        ) {
            // Animated background elements
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Primary gradient circle
                Box(
                    modifier = Modifier
                        .size(700.dp)
                        .graphicsLayer {
                            scaleX = primaryGradientScale
                            scaleY = primaryGradientScale
                            alpha = primaryGradientAlpha
                            rotationZ = gradientRotation
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Secondary gradient circle
                Box(
                    modifier = Modifier
                        .size(500.dp)
                        .graphicsLayer {
                            scaleX = secondaryGradientScale
                            scaleY = secondaryGradientScale
                            alpha = 0.2f
                            rotationZ = -gradientRotation
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                    }
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState.activeFeature) {
                        "crop" -> {
                            if (uiState.processedImageUris.isNotEmpty() && uiState.currentImageIndex < uiState.processedImageUris.size) {
                                val defaultCropRect = Rect(0.1f, 0.1f, 0.9f, 0.9f)
                                val currentCropRect = if (uiState.cropRect == Rect(0f, 0f, 1f, 1f)) {
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
                                Text(
                                    text = "No image available to crop",
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        else -> {
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
                                Text(
                                    text = "No images available to edit",
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    if (uiState.isLoading || uiState.isImageLoading) {
                        LoadingOverlay()
                    }

                    DownloadAnimation(visible = uiState.showDownloadAnimation)
                }


                if (uiState.processedImageUris.isNotEmpty() && uiState.currentImageIndex < uiState.processedImageUris.size) {
                    AnimatedFeatureControls(
                        uiState = uiState,
                        onBrightnessChange = viewModel::setBrightness,
                        onContrastChange = viewModel::setContrast,
                        onFilterSelected = viewModel::setFilter
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    FilterBottomNavigation(
                        activeFeature = uiState.activeFeature,
                        onFeatureClick = { feature ->
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
                                Toast.makeText(context, "No images available to edit", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Animated Download Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(downloadButtonWidth.dp)
                        .height(56.dp)
                        .graphicsLayer {
                            shadowElevation = 8f
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (isSystemInDarkTheme()) {
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    } else {
                                        listOf(
                                            Color(0xFF2196F3),
                                            Color(0xFF1976D2)
                                        )
                                    }
                                )
                            )
                            .clickable {
                                if (isDownloadExpanded) {
                                    isDownloadExpanded = false
                                } else {
                                    isDownloadExpanded = !isDownloadExpanded
                                }
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Download text
                            AnimatedVisibility(
                                visible = isDownloadExpanded,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Text(
                                    text = "Download",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            // Download icon with rotation animation
                            Icon(
                                painter = painterResource(
                                    id = if (isDownloadExpanded) 
                                        R.drawable.ic_close 
                                    else 
                                        R.drawable.ic_download
                                ),
                                contentDescription = if (isDownloadExpanded) "Close" else "Download",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        rotationZ = downloadIconRotation
                                    }
                            )
                        }
                    }
                }

                // Download options dropdown
                AnimatedVisibility(
                    visible = isDownloadExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier
                        .padding(top = 64.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .graphicsLayer {
                                shadowElevation = 8f
                                alpha = downloadButtonAlpha
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            DownloadOption(
                                icon = R.drawable.ic_image,
                                text = "Save as Image",
                                onClick = {
                                    isDownloadExpanded = false
                                    viewModel.prepareImageDownload()
                                }
                            )
                            
                            DownloadOption(
                                icon = R.drawable.ic_pdf,
                                text = "Save as PDF",
                                onClick = {
                                    isDownloadExpanded = false
                                    viewModel.preparePdfDownload()
                                }
                            )
                        }
                    }
                }
            }

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
                "Filters",
                color = MaterialTheme.extendedColors.filterText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.extendedColors.filterSurface,
            titleContentColor = MaterialTheme.extendedColors.filterText
        ),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.extendedColors.filterText
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
                text = "Enhancing image...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun DownloadOption(
    icon: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
