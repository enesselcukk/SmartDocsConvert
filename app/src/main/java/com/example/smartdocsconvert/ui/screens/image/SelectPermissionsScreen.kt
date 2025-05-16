package com.example.smartdocsconvert.ui.screens.image

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.ui.components.BottomNavigationBar
import com.example.smartdocsconvert.ui.components.FilterOptionsView
import com.example.smartdocsconvert.ui.components.ImageAdjustmentControls
import com.example.smartdocsconvert.ui.components.ImageControlActions
import com.example.smartdocsconvert.ui.components.ImageCropView
import com.example.smartdocsconvert.ui.components.ImagePager
import com.example.smartdocsconvert.ui.components.ImageThumbnails
import com.example.smartdocsconvert.ui.components.TopActionsBar
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
                .padding(paddingValues)
        ) {
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
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Görüntü Filtreleme",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Görüntülerinizi düzenlemek için galeriden görüntü seçin veya kamera ile yeni bir fotoğraf çekin",
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
                text = "Galeriden Seç", 
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
                text = "Fotoğraf Çek", 
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
