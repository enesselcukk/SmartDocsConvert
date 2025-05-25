package com.example.smartdocsconvert.ui.screens.file

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartdocsconvert.R
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartdocsconvert.domain.model.EditOptimizeEvent
import com.example.smartdocsconvert.domain.model.EditOptimizeState
import com.example.smartdocsconvert.ui.viewmodel.EditOptimizeViewModel

@Composable
fun EditOptimizeScreen(
    onBackClick: () -> Unit,
    onNextClick: (List<File>) -> Unit,
    selectedFiles: List<File>,
    viewModel: EditOptimizeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Initialize files if needed
    LaunchedEffect(selectedFiles) {
        viewModel.initializeFiles(selectedFiles)
    }

    EditOptimizeContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = { onBackClick() },
        onNextClick = { 
            onNextClick(state.files)
        }
    )
}

@Composable
private fun EditOptimizeContent(
    state: EditOptimizeState,
    onEvent: (EditOptimizeEvent) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val primaryColor = Color(0xFF4361EE)
    val accentColor = Color(0xFF3DDAD7)
    val darkBackground = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val cardColor = Color(0xFF242424)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        // Decorative elements
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-120).dp, y = (-180).dp)
                .graphicsLayer {
                    alpha = 0.07f
                }
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
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor, Color.Transparent),
                        radius = 300f,
                        center = Offset.Unspecified
                    ),
                    shape = CircleShape
                )
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            snackbarHost = { 
                SnackbarHost(
                    hostState = remember { SnackbarHostState() },
                    modifier = Modifier.padding(16.dp),
                    snackbar = { data ->
                        Snackbar(
                            modifier = Modifier
                                .padding(16.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFF5A5A).copy(alpha = 0.3f),
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
            },
            bottomBar = {
                if (state.hasFiles) {
                    ImprovedBottomButtons(
                        onNextClick = onNextClick,
                        primaryColor = primaryColor,
                        backgroundColor = surfaceColor
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ModernTopAppBar(
                    backgroundColor = surfaceColor,
                    primaryColor = primaryColor,
                    onBackClick = onBackClick
                )

                if (state.hasFiles) {
                    FileEditorContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        state = state,
                        onEvent = onEvent,
                        primaryColor = primaryColor,
                        cardColor = cardColor
                    )
                } else {
                    EmptyStateContent(
                        primaryColor = primaryColor,
                        onBackClick = onBackClick
                    )
                }
            }
        }

        // Rename dialog
        if (state.dialog.isVisible) {
            RenameFileDialog(
                currentName = state.dialog.fileName,
                onNameChange = { onEvent(EditOptimizeEvent.UpdateFileName(it)) },
                onConfirm = { onEvent(EditOptimizeEvent.Dialog.Confirm) },
                onDismiss = { onEvent(EditOptimizeEvent.Dialog.Hide) },
                primaryColor = primaryColor
            )
        }

        // Loading indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = primaryColor
                )
            }
        }

        // Error snackbar
        state.error?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar
            }
        }
    }
}

/**
 * Modern top app bar with progress tracker for step 2
 */
@Composable
fun ModernTopAppBar(
    backgroundColor: Color,
    primaryColor: Color,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor.copy(alpha = 0.90f)
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                spotColor = primaryColor.copy(alpha = 0.15f)
            )
    ) {
        // Top section with back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Title
            Text(
                text = "Edit & Optimize",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Progress tracker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ModernProgressTracker(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = 0.98f
                    },
                primaryColor = primaryColor,
                currentStep = 2,
                totalSteps = 3
            )
        }
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
                                .height(2.dp)
                        ) {
                            // Base line
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(1.dp))
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
                            
                            // Animated glow effect for completed lines
                            if (isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = glowAlpha
                                        }
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    primaryColor.copy(alpha = 0f),
                                                    primaryColor.copy(alpha = 0.5f),
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
        }

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
 * File editor content with preview and optimization options
 */
@Composable
private fun FileEditorContent(
    modifier: Modifier,
    state: EditOptimizeState,
    onEvent: (EditOptimizeEvent) -> Unit,
    primaryColor: Color,
    cardColor: Color
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.hasFiles && state.currentFile != null) {
            item {
                FilePreviewCard(
                    file = state.currentFile!!,
                    primaryColor = primaryColor,
                    cardColor = cardColor
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OptimizationOptions(
                    qualityLevel = state.qualityLevel,
                    onQualityChange = { onEvent(EditOptimizeEvent.UpdateQualityLevel(it)) },
                    compressionEnabled = state.compressionEnabled,
                    onCompressionToggle = { onEvent(EditOptimizeEvent.UpdateCompression(it)) },
                    onRenameClick = { onEvent(EditOptimizeEvent.Dialog.Show) },
                    primaryColor = primaryColor,
                    cardColor = cardColor
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            if (state.hasMultipleFiles) {
                item {
                    FileListSection(
                        files = state.files,
                        currentFileIndex = state.currentFileIndex,
                        onFileSelect = { onEvent(EditOptimizeEvent.SelectFile(it)) },
                        primaryColor = primaryColor,
                        cardColor = cardColor
                    )
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun FileListSection(
    files: List<File>,
    currentFileIndex: Int,
    onFileSelect: (Int) -> Unit,
    primaryColor: Color,
    cardColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Tüm Dosyalar (${files.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                files.forEachIndexed { index, file ->
                    FileListItem(
                        file = file,
                        isSelected = index == currentFileIndex,
                        onClick = { onFileSelect(index) },
                        primaryColor = primaryColor
                    )
                    
                    if (index < files.size - 1) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * File preview card
 */
@Composable
private fun FilePreviewCard(
    file: File,
    primaryColor: Color,
    cardColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = primaryColor.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Dosya tipi simgesi
            val fileIcon = when (file.extension.lowercase()) {
                "pdf" -> R.drawable.ic_pdf
                "doc", "docx" -> R.drawable.ic_doc
                "xls", "xlsx" -> R.drawable.ic_xls
                "ppt", "pptx" -> R.drawable.ic_ppt
                else -> R.drawable.ic_file
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = fileIcon),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = file.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Boyut: ${formatFileSize(file.length())}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Format file size to human-readable format
 */
private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

/**
 * File list item
 */
@Composable
private fun FileListItem(
    file: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) primaryColor.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dosya tipi simgesi
        val fileIcon = when (file.extension.lowercase()) {
            "pdf" -> R.drawable.ic_pdf
            "doc", "docx" -> R.drawable.ic_doc
            "xls", "xlsx" -> R.drawable.ic_xls
            "ppt", "pptx" -> R.drawable.ic_ppt
            else -> R.drawable.ic_file
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) primaryColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = fileIcon),
                contentDescription = null,
                tint = if (isSelected) primaryColor else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatFileSize(file.length()),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Optimization options card
 */
@Composable
private fun OptimizationOptions(
    qualityLevel: Int,
    onQualityChange: (Int) -> Unit,
    compressionEnabled: Boolean,
    onCompressionToggle: (Boolean) -> Unit,
    onRenameClick: () -> Unit,
    primaryColor: Color,
    cardColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Dosya Optimizasyonu",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Kalite ayarları
            Text(
                text = "Kalite: $qualityLevel%",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = qualityLevel.toFloat(),
                onValueChange = { onQualityChange(it.toInt()) },
                valueRange = 10f..100f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sıkıştırma seçeneği
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCompressionToggle(!compressionEnabled) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dosyayı Sıkıştır",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Switch(
                    checked = compressionEnabled,
                    onCheckedChange = { onCompressionToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryColor,
                        checkedTrackColor = primaryColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Dosya adını değiştir buton
            Button(
                onClick = onRenameClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Dosya Adını Değiştir",
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Rename file dialog
 */
@Composable
private fun RenameFileDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Dosya Adını Değiştir",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dosya Adı") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = primaryColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("İptal")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

/**
 * Bottom buttons navigation
 */
@Composable
private fun ImprovedBottomButtons(
    onNextClick: () -> Unit,
    primaryColor: Color,
    backgroundColor: Color
) {
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
                bottom = 16.dp
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

@Composable
private fun EmptyStateContent(
    primaryColor: Color,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Düzenlenecek dosya bulunamadı",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Lütfen geri dönüp dosya seçin",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Geri Dön")
            }
        }
    }
} 