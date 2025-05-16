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
import kotlinx.coroutines.delay

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

                        if (uiState.processedImageUris.isNotEmpty() && uiState.activeFeature == null) {
                            AnimatedSaveButton(
                                onClick = { viewModel.toggleDownloadOptions() },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }

            DownloadOptionsDialog(
                visible = uiState.showDownloadOptions,
                onDismiss = { viewModel.hideDownloadOptions() },
                onSaveAsImage = { viewModel.prepareImageDownload() },
                onSaveAsPdf = { viewModel.preparePdfDownload() }
            )

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
