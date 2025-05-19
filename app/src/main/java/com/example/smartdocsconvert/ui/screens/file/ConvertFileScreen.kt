package com.example.smartdocsconvert.ui.screens.file

import android.app.AlertDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.ui.components.LoadingAnimation
import com.example.smartdocsconvert.ui.viewmodel.FileConverterViewModel
import com.example.smartdocsconvert.util.FileUtils
import com.example.smartdocsconvert.di.PermissionHelperEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.core.content.FileProvider

@Composable
fun ConvertFileScreen(
    onBackClick: () -> Unit,
    onNextClick: (List<File>) -> Unit,
    viewModel: FileConverterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val permissionHelper = remember {
        val appContext = context.applicationContext
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            PermissionHelperEntryPoint::class.java
        )
        hiltEntryPoint.permissionHelper()
    }

    val primaryColor = Color(0xFF4361EE) // Modern blue
    val accentColor = Color(0xFF3DDAD7) // Teal accent
    val darkBackground = Color(0xFF121212) // Deeper dark
    val surfaceColor = Color(0xFF1E1E1E) // Dark surface
    val errorColor = Color(0xFFFF5A5A) // Warning/error
    val cardColor = Color(0xFF242424)
    
    // App state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Permission state (now handled in UI)
    var hasStoragePermission by remember { mutableStateOf(permissionHelper.hasStoragePermissions()) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionRequestCount by remember { mutableStateOf(0) }
    
    // Check permissions on launch
    LaunchedEffect(Unit) {
        hasStoragePermission = permissionHelper.hasStoragePermissions()
    }
    
    // Refresh files when permission granted
    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            viewModel.refreshFiles(context)
        }
    }
    
    // Hata mesajı varsa göster
    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    // İzin isteme launcher'ı
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionRequestCount++
        hasStoragePermission = allGranted
        
        // Show rationale dialog if permissions are denied and this isn't the first request
        if (!allGranted && permissionRequestCount > 1) {
            showPermissionRationale = true
        }
    }
    
    // İzin reddedildi dialoga
    if (showPermissionRationale) {
        PermissionDeniedDialog(
            onGoToSettings = {
                permissionHelper.openAppSettings(context)
                showPermissionRationale = false
            },
            onClose = {
                showPermissionRationale = false
            }
        )
    }

    // Main screen structure
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        // Decorative elements with blur effect for depth
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-120).dp, y = (-180).dp)
                .graphicsLayer {
                    alpha = 0.07f
                }
                .blur(radius = 80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor, Color.Transparent),
                        radius = 350f,
                        center = Offset.Unspecified
                    ),
                    shape = CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .graphicsLayer {
                    alpha = 0.07f
                }
                .blur(radius = 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor, Color.Transparent),
                        radius = 300f,
                        center = Offset.Unspecified
                    ),
                    shape = CircleShape
                )
        )
        
        // Scaffold ile Snackbar hostu ekle
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(16.dp),
                    snackbar = { data ->
                        Snackbar(
                            modifier = Modifier
                                .padding(16.dp)
                                .border(
                                    width = 1.dp,
                                    color = errorColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            containerColor = Color(0xFF422222),
                            contentColor = Color.White,
                            action = {
                                TextButton(onClick = { data.dismiss() }) {
                                    Text(
                                        text = "OK",
                                        color = Color.White
                                    )
                                }
                            }
                        ) {
                            Text(text = data.visuals.message)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // İçerik
                when {
                    !hasStoragePermission -> {
                        StoragePermissionRequest(
                            onRequestPermission = {
                                permissionLauncher.launch(permissionHelper.getRequiredPermissions())
                            }
                        )
                    }
                    uiState.isLoading -> {
                        LoadingContent()
                    }
                    uiState.files.isEmpty() -> {
                        EmptyFilesContent()
                    }
                    else -> {
                        FileListContent(
                            files = uiState.files,
                            selectedFiles = uiState.selectedFiles,
                            onFileSelected = { viewModel.toggleFileSelection(it) }
                        )
                    }
                }
                
                // Bottom action buttons in a fixed position at the bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    ImprovedBottomButtons(
                        selectedFiles = uiState.selectedFiles,
                        onNextClick = { onNextClick(uiState.selectedFiles.toList()) },
                        primaryColor = primaryColor,
                        backgroundColor = surfaceColor
                    )
                }
            }
        }
    }
}

/**
 * Modern top app bar
 */
@Composable
private fun ModernTopAppBar(
    backgroundColor: Color,
    primaryColor: Color,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor.copy(alpha = 0.90f)
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                spotColor = primaryColor.copy(alpha = 0.15f)
            )
    ) {
        // Üst tarafta dekoratif gradient efekti
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(bottomStart = 100.dp, bottomEnd = 100.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Progress tracker etrafında parıltı
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(width = 240.dp, height = 90.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Progress tracker with modern design
        ModernProgressTracker(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .graphicsLayer {
                    alpha = 0.98f
                },
            primaryColor = primaryColor,
            currentStep = 1,
            totalSteps = 3
        )
    }
}

/**
 * Modern progress tracker for multi-step process
 */
@Composable
private fun ModernProgressTracker(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    currentStep: Int,
    totalSteps: Int
) {
    val lineWidth = 70.dp
    
    // Ana container için sıçrama animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val containerScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    // Bağlantı çizgileri için parıltı animasyonu
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    // Gradient ışıltı efekti için pozisyon
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
        ),
        label = ""
    )
    
    // Adım açıklamaları
    val stepDescriptions = listOf("Select Document", "Edit & Optimize", "Save & Share")
    val currentStepDescription = stepDescriptions.getOrNull(currentStep - 1) ?: ""
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = containerScale
                    this.scaleY = containerScale
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = primaryColor.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF242424)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    val stepNumber = index + 1
                    val isActive = stepNumber == currentStep
                    val isCompleted = stepNumber < currentStep
                    
                    // Step indicator
                    EnhancedStepIndicator(
                        number = stepNumber,
                        isActive = isActive,
                        isCompleted = isCompleted,
                        primaryColor = primaryColor
                    )
                    
                    // Connector line between steps
                    if (index < totalSteps - 1) {
                        Box(
                            modifier = Modifier
                                .width(lineWidth)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isCompleted) {
                                            listOf(
                                                primaryColor,
                                                primaryColor.copy(alpha = 0.8f)
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = 0.2f),
                                                Color.White.copy(alpha = 0.1f)
                                            )
                                        }
                                    )
                                )
                        )
                        
                        // Animasyonlu parıltı efekti (tamamlanmış adımlar arasında)
                        if (isCompleted) {
                            Box(
                                modifier = Modifier
                                    .width(lineWidth)
                                    .height(4.dp)
                                    .offset(y = (-4).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0f),
                                                primaryColor.copy(alpha = glowAlpha),
                                                primaryColor.copy(alpha = 0f)
                                            ),
                                            startX = shimmerOffset - 500f,
                                            endX = shimmerOffset + 500f
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        // Adım açıklaması
        AnimatedVisibility(
            visible = currentStepDescription.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = primaryColor.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                primaryColor.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Step konu göstergesi (ikon)
                val stepIcon = when(currentStep) {
                    1 -> R.drawable.ic_file
                    2 -> R.drawable.ic_edit
                    3 -> R.drawable.ic_download
                    else -> R.drawable.ic_file
                }
                
                Icon(
                    painter = painterResource(id = stepIcon),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Adım açıklaması
                Text(
                    text = currentStepDescription,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Enhanced individual step indicator with advanced animations
 */
@Composable
private fun EnhancedStepIndicator(
    number: Int,
    isActive: Boolean,
    isCompleted: Boolean,
    primaryColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    
    // Aktif adım için daha vurgulu animasyon
    val activeScale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    // Seçili adım için parıltı animasyonu
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    // Glow efekti için opaklık animasyonu
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isActive) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    // Tamamlanmış adım için hareketli onay işareti animasyonu
    val checkScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    // Adım içindeki rotasyon
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    val size = if (isActive) 50.dp else 40.dp
    
    Box(
        modifier = Modifier
            .size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dış parıltı efekti
        if (isActive || isCompleted) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isActive) primaryColor.copy(alpha = glowAlpha) else primaryColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        // Ana gösterge dairesi
        Box(
            modifier = Modifier
                .size(size)
                .scale(activeScale)
                .graphicsLayer {
                    rotationZ = rotation
                }
                .clip(CircleShape)
                .background(
                    brush = if (isActive || isCompleted) {
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.8f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.value, size.value)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF333333),
                                Color(0xFF222222)
                            )
                        )
                    }
                )
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    brush = if (isActive || isCompleted) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.8f),
                                primaryColor.copy(alpha = 0.5f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                // Tamamlanmış adım için onay işareti
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(checkScale)
                )
            } else {
                // Aktif veya pasif adım numarası
                Text(
                    text = number.toString(),
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = if (isActive) 18.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(2.dp)
                )
                
                // Aktif adım için dönüşen dairesel ilerleme göstergesi
                if (isActive) {
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = LinearEasing)
                        ),
                        label = ""
                    )
                    
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(size)
                            .graphicsLayer { rotationZ = rotation },
                        color = Color.White.copy(alpha = 0.4f),
                        strokeWidth = 1.dp
                    )
                }
            }
        }
    }
}

/**
 * Selected files info with modern design
 */
@Composable
private fun SelectedFilesInfo(
    selectedCount: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = primaryColor.copy(alpha = 0.08f)
            ),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.4f),
                        primaryColor.copy(alpha = 0.1f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated check indicator
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val iconScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = ""
                )
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(iconScale)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "$selectedCount files selected",
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    
                    if (selectedCount > 0) {
                        Text(
                            text = "Ready to convert",
                            color = primaryColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced file list with animations and improved layout
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedFilesList(
    files: List<File>,
    selectedFiles: Set<File>,
    onFileClick: (File) -> Unit,
    formatFileSize: (Long) -> String,
    primaryColor: Color,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Section heading with file count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Available Files (${files.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            if (selectedFiles.isNotEmpty()) {
                Text(
                    text = "${selectedFiles.size} selected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = primaryColor
                )
            }
        }
        
        // Files list
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(
                items = files,
                key = { it.absolutePath }
            ) { file ->
                ModernFileItem(
                    file = file,
                    isSelected = selectedFiles.contains(file),
                    onClick = { onFileClick(file) },
                    formatFileSize = formatFileSize,
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    primaryColor = primaryColor,
                    cardColor = cardColor
                )
            }
        }
    }
}

/**
 * Modern file item with improved design and animations
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernFileItem(
    file: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    formatFileSize: (Long) -> String,
    modifier: Modifier = Modifier,
    primaryColor: Color,
    cardColor: Color
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 4f else 1f,
        label = ""
    )
    
    // File details
    val fileExtension = file.extension.uppercase()
    val fileIcon = R.drawable.ic_file
    
    val fileColor = when (fileExtension) {
        "PDF" -> Color(0xFFFF4444)
        "DOC", "DOCX" -> Color(0xFF4285F4)
        "XLS", "XLSX" -> Color(0xFF0F9D58)
        "PPT", "PPTX" -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isSelected) primaryColor.copy(alpha = 0.3f) else Color.Black
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) cardColor.copy(alpha = 0.95f) else cardColor.copy(alpha = 0.7f)
        ),
        border = if (isSelected) {
            BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.4f)
                    )
                )
            )
        } else {
            BorderStroke(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.1f)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.1f)),
                    onClick = { 
                        isPressed = true
                        onClick()
                        // Reset the pressed state after a short delay
                        coroutineScope.launch {
                           delay(150)
                            isPressed = false
                        }
                    },
                    onLongClick = {
                        // Dosyayı açmak için "Şununla Aç" seçeneğini göster
                        openDocumentWithChooser(context, file)
                    }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon with modern design
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = fileColor.copy(alpha = 0.12f)
                    )
                    .border(
                        width = 1.dp,
                        color = fileColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = file.getDocumentIcon()),
                    contentDescription = "File type",
                    tint = fileColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // File details with better layout
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = file.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(file.length()),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    Text(
                        text = fileExtension,
                        fontSize = 12.sp,
                        color = fileColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Selection indicator with animation
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Empty circle
                }
            }
        }
    }
}

private fun File.getDocumentIcon(): Int {
    return when (extension.lowercase()) {
        "pdf" -> R.drawable.ic_pdf
        "doc", "docx", "rtf", "odt" -> R.drawable.ic_doc
        "xls", "xlsx", "csv", "ods" -> R.drawable.ic_xls
        "ppt", "pptx", "pps", "odp" -> R.drawable.ic_ppt
        "txt", "md", "log", "json", "xml", "html", "htm", "css", "js" -> R.drawable.ic_txt
        "jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp", "gif", "svg", "ico" -> R.drawable.ic_image
        else -> R.drawable.ic_file
    }
}

private fun openDocumentWithChooser(context: Context, file: File) {
    try {
        // FileProvider URI oluştur
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // Dosya uzantısını al ve MIME türünü belirle
        val extension = file.extension.lowercase()
        val mimeType = getMimeType(extension)

        // VIEW intent oluştur
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Kullanıcıya "Şununla Aç" seçeneğini sunan Intent
        val chooserIntent = Intent.createChooser(
            viewIntent,
            "Dosyayı şununla aç"
        )

        // Intent'i başlat
        if (viewIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooserIntent)
        } else {
            // Uygun bir uygulama bulunamadıysa, kullanıcıya göster ve Play Store'a yönlendir
            showNoAppFoundDialog(context, mimeType)
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Dosya açılamadı: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}


private fun showNoAppFoundDialog(context: Context, mimeType: String) {
    val alertDialog = AlertDialog.Builder(context)
        .setTitle("Uygun Uygulama Bulunamadı")
        .setMessage("Bu dosyayı açabilecek bir uygulama bulunamadı. Play Store'dan uygun bir uygulama yüklemek ister misiniz?")
        .setPositiveButton("Evet") { _, _ ->
            val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=${mimeType.replace("/", " ")} görüntüleyici")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (marketIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(marketIntent)
            } else {
                Toast.makeText(
                    context,
                    "Google Play Store bulunamadı",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        .setNegativeButton("Hayır", null)
        .create()
    
    alertDialog.show()
}


private fun getMimeType(extension: String): String {
    return when (extension) {
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
        "txt" -> "text/plain"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "csv" -> "text/csv"
        "rtf" -> "application/rtf"
        "odt" -> "application/vnd.oasis.opendocument.text"
        "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
        "odp" -> "application/vnd.oasis.opendocument.presentation"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
}


@Composable
private fun ImprovedBottomButtons(
    selectedFiles: Set<File>,
    onNextClick: () -> Unit,
    primaryColor: Color,
    backgroundColor: Color
) {
    // Only show the bottom buttons when files are selected
    AnimatedVisibility(
        visible = selectedFiles.isNotEmpty(),
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
    ) {
        // Sabit alt çubuk
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor 
                        )
                    )
                )
                .padding(
                    start = 16.dp, 
                    end = 16.dp,
                    top = 12.dp,
                    bottom = if (selectedFiles.isNotEmpty()) 16.dp else 12.dp
                )
        ) {
            // Buton etrafına gölge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        spotColor = primaryColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Button(
                    onClick = onNextClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedDialog(
    onGoToSettings: () -> Unit,
    onClose: () -> Unit
) {
    val primaryColor = Color(0xFF4361EE) // Modern blue
    val accentColor = Color(0xFF3DDAD7) // Teal accent
    val backgroundColor = Color(0xFF1E1E1E) // Dark surface
    val errorColor = Color(0xFFFF5A5A) // Warning/error
    
    // Animation for dialog appearance
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = ""
    )
    
    // Warning pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val warningScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    // Set animation to visible
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .graphicsLayer { 
                    this.scaleX = scale
                    this.scaleY = scale
                    this.alpha = alpha
                },
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor,
                                Color(0xFF121212)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top warning icon with glow effect
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        errorColor.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    color = backgroundColor
                                )
                                .border(
                                    width = 1.dp,
                                    color = errorColor.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = errorColor,
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(warningScale)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Dialog title
                    Text(
                        text = "Permission Required",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Dialog message
                    Text(
                        text = "We need storage permission to access documents for conversion. You can grant it in app settings.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Settings button
                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "Open Settings",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Close button
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Not Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoragePermissionRequest(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val primaryColor = Color(0xFF4361EE)
            
            // Animated icon
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = ""
            )
            
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            color = primaryColor.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = primaryColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Storage Permission Required",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "We need storage permission to access and convert your files.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Grant Permission",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val primaryColor = Color(0xFF4361EE)
        LoadingAnimation(primaryColor = primaryColor)
    }
}

@Composable
private fun EmptyFilesContent() {
    val primaryColor = Color(0xFF4361EE)
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated icon
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = ""
            )
            
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            color = primaryColor.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = primaryColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_file),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "No Files Found",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "We couldn't find any document files on your device.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Refresh button
            val viewModel = hiltViewModel<FileConverterViewModel>()
            Button(
                onClick = { 
                    viewModel.forceRefreshFiles(context)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "Refresh",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Refresh",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "You can also import documents from your device.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FileListContent(
    files: List<File>,
    selectedFiles: Set<File>,
    onFileSelected: (File) -> Unit
) {
    val primaryColor = Color(0xFF4361EE)
    val cardColor = Color(0xFF242424)
    
    Column {
        // Modern top app bar with updated design
        ModernTopAppBar(
            backgroundColor = Color(0xFF1E1E1E),
            primaryColor = primaryColor,
            onBackClick = { /* Handle back */ },
            onRefreshClick = { /* Handle refresh */ }
        )
        
        // Selected files info with modern design
        SelectedFilesInfo(
            selectedCount = selectedFiles.size,
            primaryColor = primaryColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        // Kullanıcıya uzun basış ipucu
        AnimatedVisibility(visible = files.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF333333)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "İpucu: Dosyayı seçmek için tıklayın, dosyayı açmak için uzun basın",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                val isSelected = selectedFiles.contains(file)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFileSelected(file) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else cardColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) {
                        BorderStroke(1.dp, primaryColor.copy(alpha = 0.7f))
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // File icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) primaryColor.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val extension = file.extension.lowercase()
                            val iconRes = when (extension) {
                                "pdf" -> R.drawable.ic_pdf
                                "doc", "docx" -> R.drawable.ic_doc
                                "xls", "xlsx" -> R.drawable.ic_xls
                                "ppt", "pptx" -> R.drawable.ic_ppt
                                else -> R.drawable.ic_file
                            }
                            
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = if (isSelected) primaryColor else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = file.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = FileUtils.formatFileSize(file.length()),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                
                                Text(
                                    text = " • ",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                
                                Text(
                                    text = file.extension.uppercase(),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}