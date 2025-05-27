package com.example.smartdocsconvert.ui.screens.image

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.ui.components.*
import com.example.smartdocsconvert.ui.viewmodel.ImageFilterViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun SelectPermissionsScreen(
    selectedImageNavigator:(String) -> Unit,
    navigateUp: () -> Unit,
    viewModel: ImageFilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Animation states for background elements
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Primary circle animation
    val primaryCircleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primaryScale"
    )
    
    val primaryCircleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primaryAlpha"
    )

    // Secondary circle animation
    val secondaryCircleScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondaryScale"
    )
    
    val secondaryCircleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondaryAlpha"
    )

    // Gradient rotation animation
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "gradientRotation"
    )
    
    // Galeri seçici
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Seçilen resimleri ImageEditorScreen'e yönlendir
            val encodedUris = uris.map { uri ->
                URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
            }
            val urisString = encodedUris.joinToString(",")
            selectedImageNavigator(urisString)
        }
    }
    
    // Kamera seçici
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.tempCameraUri?.let { uri ->
                // Çekilen fotoğrafı ImageEditorScreen'e yönlendir
                val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                selectedImageNavigator(encodedUri)
            }
        }
    }
    
    // Kamera izni kontrolü için
    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            // İzin verildiyse kamerayı aç
            viewModel.createTempCameraUri(context)?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            // İzin reddedildiyse kullanıcıya bildir
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Kamera kullanabilmek için izin gerekiyor")
            }
        }
    }
    
    // Show toast messages
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearToastMessage()
            }
        }
    }
    
    BackHandler {
        navigateUp()
    }
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        content = {
                            Text(text = data.visuals.message)
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Animated background elements
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .offset(x = (-100).dp, y = (-100).dp)
                    .graphicsLayer {
                        scaleX = primaryCircleScale
                        scaleY = primaryCircleScale
                        alpha = primaryCircleAlpha
                        rotationZ = gradientRotation
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 50.dp, y = 50.dp)
                    .graphicsLayer {
                        scaleX = secondaryCircleScale
                        scaleY = secondaryCircleScale
                        alpha = secondaryCircleAlpha
                        rotationZ = -gradientRotation
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00BCD4),
                                Color(0xFF00BCD4).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            if (uiState.processedImageUris.isEmpty()) {
                // Henüz görüntü seçilmedi, boş durumu göster
                EmptyImagesState(
                    onSelectFromGallery = {
                        galleryLauncher.launch("image/*")
                    },
                    onTakePhoto = {
                        // Kamera iznini kontrol et
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                // İzin varsa kamerayı aç
                                viewModel.createTempCameraUri(context)?.let { uri ->
                                    cameraLauncher.launch(uri)
                                }
                            }
                            else -> {
                                // İzin yoksa iste
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                )
            } else {
                // Normal görüntü işleme ekranı
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Top bar
                        TopActionsBar(
                            onSaveClick = { viewModel.saveProcessedImage() },
                            onBackClick = { navigateUp() },
                            isSaving = uiState.isSaving
                        )
                        
                        // Main content
                        if (uiState.activeFeature == "crop") {
                            // Crop view
                            ImageCropView(
                                imageUri = uiState.processedImageUris[uiState.currentImageIndex],
                                cropRect = uiState.cropRect,
                                onCropRectChange = { viewModel.updateCropRect(it) },
                                onApplyCrop = { viewModel.applyAndSaveCrop() },
                                onCancelCrop = { viewModel.updateActiveFeature(null) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Normal view
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                // Main image display
                                val displayUri = if (
                                    uiState.croppedImageUris.size > uiState.currentImageIndex &&
                                    uiState.croppedImageUris[uiState.currentImageIndex] != null
                                ) {
                                    uiState.croppedImageUris[uiState.currentImageIndex]!!
                                } else {
                                    uiState.processedImageUris[uiState.currentImageIndex]
                                }
                                
                                ImagePager(
                                    currentImageUri = displayUri,
                                    rotationAngle = uiState.rotationAngles[uiState.currentImageIndex],
                                    scale = uiState.scale,
                                    offsetX = uiState.offsetX,
                                    offsetY = uiState.offsetY,
                                    rotationY = uiState.rotationY
                                )
                                
                                // Loading indicator
                                if (uiState.isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White)
                                    }
                                }
                            }
                        }
                        
                        // Thumbnails row
                        if (uiState.processedImageUris.size > 1) {
                            ImageThumbnails(
                                imageUris = uiState.processedImageUris,
                                currentIndex = uiState.currentImageIndex,
                                onImageSelected = { viewModel.onImageSelected(it) }
                            )
                        }
                        
                        // Feature UI based on active feature
                        AnimatedVisibility(
                            visible = uiState.activeFeature == "adjust",
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
                        ) {
                            ImageAdjustmentControls(
                                brightness = uiState.brightnessValues[uiState.currentImageIndex],
                                contrast = uiState.contrastValues[uiState.currentImageIndex],
                                onBrightnessChange = { viewModel.setBrightness(it) },
                                onContrastChange = { viewModel.setContrast(it) },
                                onResetClick = { viewModel.resetImageAdjustments() },
                                onAutoEnhanceClick = { viewModel.autoEnhanceImage() }
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = uiState.activeFeature == "filter",
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
                        ) {
                            FilterOptionsView(
                                selectedFilter = uiState.selectedFilters[uiState.currentImageIndex],
                                filterIntensity = uiState.filterIntensityValues[uiState.currentImageIndex],
                                onFilterSelected = { viewModel.setFilter(it) },
                                onIntensityChange = { viewModel.setFilterIntensity(it) }
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = uiState.activeFeature == "rotate",
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
                        ) {
                            ImageControlActions(
                                onRotateClick = { viewModel.rotateImage() }
                            )
                        }
                        
                        // Bottom navigation
                        BottomNavigationBar(
                            activeFeature = uiState.activeFeature,
                            onFeatureClick = { feature ->
                                if (uiState.activeFeature == feature) {
                                    viewModel.updateActiveFeature(null)
                                } else {
                                    viewModel.updateActiveFeature(feature)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyImagesState(
    onSelectFromGallery: () -> Unit,
    onTakePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Image Filtering",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Select an image from gallery or take a new photo with camera to edit your images",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onSelectFromGallery,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Select from Gallery", 
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Take Photo", 
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
