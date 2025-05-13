
package com.example.smartdocsconvert.ui.screens.file

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.smartdocsconvert.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConvertFileScreen(
    onBackClick: () -> Unit,
    onNextClick: (List<File>) -> Unit
) {
    // Theme colors
    val primaryColor = Color(0xFFFF4444)
    val primaryVariant = Color(0xFFFF6B6B)
    val darkBackground = Color(0xFF1A1A1A)
    val darkSurface = Color(0xFF2A0B0B)
    val cardColor = Color(0xFF2D1414)
    val accentColor = Color(0xFF4ECDC4)
    
    // App state
    var selectedType by remember { mutableStateOf("DOC") }
    val fileTypes = listOf("DOC", "DOCX", "PDF", "PPT", "PPTX", "XLS", "XLSX", "TXT")
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(checkPermissions(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionRequestCount by remember { mutableStateOf(0) }
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )
    
    val mainGradient = Brush.linearGradient(
        colors = listOf(
            darkBackground,
            Color(0xFF2A1010),
            Color(0xFF2A0B0B),
            darkBackground
        ),
        start = Offset(
            x = cos(Math.toRadians(gradientAngle.toDouble())).toFloat() * 1000f,
            y = sin(Math.toRadians(gradientAngle.toDouble())).toFloat() * 1000f
        ),
        end = Offset(
            x = cos(Math.toRadians((gradientAngle + 180f).toDouble())).toFloat() * 1000f,
            y = sin(Math.toRadians((gradientAngle + 180f).toDouble())).toFloat() * 1000f
        )
    )

    // İzin isteme launcher'ı
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermission = allGranted
        
        if (!allGranted && permissionRequestCount > 0) {
            // İzinler reddedildiyse ve en az bir kere izin istedik, dialoga göster
            showPermissionDialog = true
        }
        
        permissionRequestCount++
    }

    // İzinleri kontrol et ve uygulama ilk başladığında iste
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            requestStoragePermissions(permissionLauncher)
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        PermissionDeniedDialog(
            onGoToSettings = {
                openAppSettings(context)
                showPermissionDialog = false
            },
            onClose = {
                showPermissionDialog = false
            }
        )
    }

    // Dosya türü değiştiğinde dosyaları yükle
    LaunchedEffect(selectedType, hasPermission) {
        if (hasPermission) {
            isLoading = true
            try {
                files = getFilesByType(context, selectedType)
            } finally {
                isLoading = false
            }
        }
    }

    // Dosya seçici için launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: "document_${System.currentTimeMillis()}.${selectedType.lowercase()}"

                    val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                    
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destinationFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    // Dosya uzantısına göre seçili türü güncelle
                    val extension = fileName.substringAfterLast('.', "").uppercase()
                    if (fileTypes.contains(extension)) {
                        selectedType = extension
                    }
                    
                    // Dosya listesini güncelle
                    files = getFilesByType(context, selectedType)
                } catch (e: Exception) {
                    android.util.Log.e("FileSearch", "Dosya kopyalama hatası", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mainGradient)
    ) {
        // Decorative elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .graphicsLayer {
                    alpha = 0.1f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor, Color.Transparent),
                        radius = 300f,
                        center = Offset.Unspecified
                    ),
                    shape = CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .graphicsLayer {
                    alpha = 0.15f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor, Color.Transparent),
                        radius = 200f,
                        center = Offset.Unspecified
                    ),
                    shape = CircleShape
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Animated Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                darkSurface,
                                darkSurface.copy(alpha = 0.9f)
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
                    )
            ) {
                // Back button and title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.05f)
                                    ),
                                    center = Offset.Unspecified,
                                    radius = 40f
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Select document",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            shadowElevation = 4f
                        }
                    )
                    IconButton(
                        onClick = { 
                            if (hasPermission) {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        files = getFilesByType(context, selectedType)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                requestStoragePermissions(permissionLauncher)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.05f)
                                    ),
                                    center = Offset.Unspecified,
                                    radius = 40f
                                )
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder),
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Progress dots with lines - enhanced
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isActive = index == 0
                        val dotScale by animateFloatAsState(
                            targetValue = if (isActive) 1.1f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = ""
                        )
                        
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(dotScale)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                primaryColor,
                                                primaryColor.copy(alpha = 0.7f)
                                            ),
                                            center = Offset.Unspecified,
                                            radius = 30f
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(dotScale)
                                    .clip(CircleShape)
                                    .background(
                                        color = Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )
                        }
                        if (index < 2) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.4f),
                                                Color.White.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Selected Files Count with enhanced styling
            AnimatedVisibility(
                visible = selectedFiles.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = primaryColor.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.5f),
                                primaryColor.copy(alpha = 0.2f)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "${selectedFiles.size} files selected",
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // File Types with enhanced styling
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(fileTypes) { type ->
                        val isSelected = type == selectedType
                        EnhancedFileTypeChip(
                            type = type,
                            isSelected = isSelected,
                            onClick = { 
                                selectedType = type
                                selectedFiles = emptySet() // Reset selections when type changes
                            }
                        )
                    }
                }
            }

            // File List with Animations
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    !hasPermission -> {
                        EnhancedNoPermissionState(
                            onRequestPermission = {
                                requestStoragePermissions(permissionLauncher)
                            }
                        )
                    }
                    isLoading -> {
                        EnhancedLoadingAnimation()
                    }
                    files.isEmpty() -> {
                        EnhancedEmptyStateForFileType(
                            fileType = selectedType,
                            onAddFileClick = {
                                if (hasPermission) {
                                    // Launch file picker specific to the file type
                                    when (selectedType) {
                                        "PDF" -> filePicker.launch("application/pdf")
                                        "DOC", "DOCX" -> filePicker.launch("application/msword")
                                        "PPT", "PPTX" -> filePicker.launch("application/vnd.ms-powerpoint")
                                        "XLS", "XLSX" -> filePicker.launch("application/vnd.ms-excel")
                                        "TXT" -> filePicker.launch("text/plain")
                                        else -> filePicker.launch("*/*")
                                    }
                                } else {
                                    requestStoragePermissions(permissionLauncher)
                                }
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = files,
                                key = { it.absolutePath }
                            ) { file ->
                                EnhancedFileItem(
                                    file = file,
                                    isSelected = selectedFiles.contains(file),
                                    onClick = {
                                        selectedFiles = if (selectedFiles.contains(file)) {
                                            selectedFiles - file
                                        } else {
                                            selectedFiles + file
                                        }
                                    },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                }

                // Bottom Buttons Row - enhanced
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Add File Button
                        Button(
                            onClick = {
                                if (hasPermission) {
                                    filePicker.launch("*/*")
                                } else {
                                    requestStoragePermissions(permissionLauncher)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = darkSurface
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .graphicsLayer {
                                    shadowElevation = 8f
                                },
                            border = BorderStroke(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.2f), 
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = "Add file",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add file",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Next Button (Only visible when files are selected)
                        AnimatedVisibility(
                            visible = selectedFiles.isNotEmpty(),
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { onNextClick(selectedFiles.toList()) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .graphicsLayer {
                                        shadowElevation = 8f
                                    }
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
                                        painter = painterResource(id = R.drawable.ic_next),
                                        contentDescription = "Next",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
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
    val primaryColor = Color(0xFFFF4444)
    val accentColor = Color(0xFF4ECDC4)
    val darkSurface = Color(0xFF2A0B0B)
    
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
                    this.shadowElevation = 16f
                },
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF331515),
                                darkSurface
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top title with logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_file),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "PDF CONVERTER",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Warning box with animation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF3A1515),
                                        Color(0xFF2A1010)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.3f),
                                        primaryColor.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.2f),
                                                primaryColor.copy(alpha = 0.05f)
                                            ),
                                            center = Offset.Unspecified,
                                            radius = 40f
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .scale(warningScale)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = "It seems like you have denied our permission request, however you can grant it again in app settings",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Settings button
                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "GO TO APP SETTINGS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Close button
                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "CLOSE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


private suspend fun getFilesByType(context: Context, type: String): List<File> = withContext(Dispatchers.IO) {
    val files = mutableListOf<File>()
    val uniquePaths = HashSet<String>() // Tekrar eden dosyaları engellemek için
    val extension = type.lowercase()
    
    android.util.Log.d("FileSearch", "Başlangıç: $extension uzantılı dosyalar aranıyor")

    // MediaStore kullanarak harici dosyaları ara
    try {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        // MIME type ve uzantı bazlı arama
        val mimeType = when (extension) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "txt" -> "text/plain"
            else -> null
        }

        val selection = if (mimeType != null) {
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        } else {
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        }

        val selectionArgs = if (mimeType != null) {
            arrayOf(mimeType, "%.$extension")
        } else {
            arrayOf("%.$extension")
        }

        // External storage query
        val queryUri = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC" // En son değiştirilenler önce
        )?.use { cursor ->
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            android.util.Log.d("FileSearch", "MediaStore cursor count: ${cursor.count}")
            
            while (cursor.moveToNext()) {
                val path = cursor.getString(pathColumn)
                val file = File(path)
                if (file.exists() && file.isFile) {
                    val absolutePath = file.absolutePath
                    if (uniquePaths.add(absolutePath)) {
                        android.util.Log.d("FileSearch", "MediaStore'dan dosya bulundu: $absolutePath")
                        files.add(file)
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("FileSearch", "MediaStore sorgu hatası", e)
    }

    // Özel dizinleri tara
    val specialDirs = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        File(Environment.getExternalStorageDirectory(), "Download"),
        File(Environment.getExternalStorageDirectory(), "Documents")
    )

    specialDirs.forEach { directory ->
        try {
            if (directory.exists() && directory.isDirectory) {
                android.util.Log.d("FileSearch", "Özel dizin taranıyor: ${directory.absolutePath}")
                searchInDirectory(directory, extension, files, uniquePaths)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("FileSearch", "Özel dizin erişim hatası: ${directory.absolutePath}", e)
        }
    }

    // Uygulamanın kendi dizinlerini de tara
    val directories = mutableListOf<File>().apply {
        add(context.filesDir)
        add(context.cacheDir)
        context.getExternalFilesDir(null)?.let { add(it) }
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { add(it) }
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { add(it) }
    }

    android.util.Log.d("FileSearch", "Uygulama dizinleri taranıyor. Dizin sayısı: ${directories.size}")
    
    directories.forEach { directory ->
        try {
            android.util.Log.d("FileSearch", "Dizin taranıyor: ${directory.absolutePath}")
            searchInDirectory(directory, extension, files, uniquePaths)
        } catch (e: SecurityException) {
            android.util.Log.e("FileSearch", "Dizin erişim hatası: ${directory.absolutePath}", e)
        }
    }

    android.util.Log.d("FileSearch", "Tüm arama tamamlandı. Toplam bulunan dosya sayısı: ${files.size}")
    files.forEach { file ->
        android.util.Log.d("FileSearch", "Final liste - Dosya: ${file.absolutePath}")
    }

    files
}

private fun searchInDirectory(directory: File, extension: String, files: MutableList<File>, uniquePaths: HashSet<String>) {
    if (!directory.exists() || !directory.isDirectory) {
        android.util.Log.d("FileSearch", "Dizin mevcut değil veya dizin değil: ${directory.absolutePath}")
        return
    }

    directory.listFiles()?.forEach { file ->
        try {
            when {
                file.isFile && file.name.lowercase().endsWith(".$extension") -> {
                    val absolutePath = file.absolutePath
                    if (uniquePaths.add(absolutePath)) { // Eğer dosya yolu daha önce eklenmemişse
                        android.util.Log.d("FileSearch", "Dizin taramasında dosya bulundu: $absolutePath")
                        files.add(file)
                    } else {
                        android.util.Log.d("FileSearch", "Tekrar eden dosya atlandı: $absolutePath")
                    }
                }
                file.isDirectory -> {
                    android.util.Log.d("FileSearch", "Alt dizine giriliyor: ${file.absolutePath}")
                    searchInDirectory(file, extension, files, uniquePaths)
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.e("FileSearch", "Dosya erişim hatası: ${file.absolutePath}", e)
        }
    }
}

private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> String.format("%d Bytes", size)
    }
}

private fun checkPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

// Helper function to open app settings
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

// Helper function to request storage permissions
private fun requestStoragePermissions(permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    permissionLauncher.launch(permissions)
}

@Composable
private fun EnhancedFileTypeChip(
    type: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = Color(0xFFFF4444)
    val interactionSource = remember { MutableInteractionSource() }
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    Card(
        modifier = Modifier
            .width(100.dp)
            .scale(scale)
            .graphicsLayer {
                shadowElevation = if (isSelected) 4f else 0f
            },
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.5f)
                    )
                )
            )
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color(0xFF2A1010).copy(alpha = 0.7f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = type,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.7f),
                                    primaryColor
                                )
                            ),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun EnhancedFileItem(
    file: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFFFF4444)
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    val fileExtension = file.extension.uppercase()
    val fileIcon = when (fileExtension) {
        "PDF" -> R.drawable.ic_file // Replace with actual PDF icon
        "DOC", "DOCX" -> R.drawable.ic_file // Replace with actual Word icon
        "XLS", "XLSX" -> R.drawable.ic_file // Replace with actual Excel icon
        "PPT", "PPTX" -> R.drawable.ic_file // Replace with actual PowerPoint icon
        else -> R.drawable.ic_file
    }
    
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isSelected) 6f else 2f
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color(0xFF2A1010)
        ),
        border = if (isSelected) {
            BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.5f)
                    )
                )
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .clickable(
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
                    }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon in a circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                fileColor.copy(alpha = 0.15f),
                                fileColor.copy(alpha = 0.05f)
                            ),
                            center = Offset.Unspecified,
                            radius = 40f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = fileIcon),
                    contentDescription = "File type",
                    tint = fileColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // File details
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
                Text(
                    text = "${formatFileSize(file.length())} • ${fileExtension}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.8f),
                                    primaryColor.copy(alpha = 0.6f)
                                ),
                                center = Offset.Unspecified,
                                radius = 40f
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Select",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedLoadingAnimation() {
    val primaryColor = Color(0xFFFF4444)
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ), 
        label = ""
    )
    
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseSize)
                    .graphicsLayer {
                        shadowElevation = 8f
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = primaryColor,
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
                
                CircularProgressIndicator(
                    color = primaryColor.copy(alpha = 0.3f),
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer { rotationZ = -rotation * 0.7f }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Loading files...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EnhancedEmptyStateForFileType(
    fileType: String,
    onAddFileClick: () -> Unit
) {
    val primaryColor = Color(0xFFFF4444)
    val infiniteTransition = rememberInfiniteTransition(label = "")
    
    // Animation for the icon pulsing
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ), 
        label = ""
    )
    
    // Animation for the shimmer effect
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500)
        ),
        label = ""
    )
    
    // Edge glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.1f)
        ),
        start = Offset(shimmer * 1000f, 0f),
        end = Offset((shimmer - 1) * 1000f, 0f)
    )
    
    // File icon and color based on type
    val (icon, fileColor) = when (fileType) {
        "PDF" -> Pair(R.drawable.ic_file, Color(0xFFFF4444))
        "DOC", "DOCX" -> Pair(R.drawable.ic_file, Color(0xFF4285F4))
        "PPT", "PPTX" -> Pair(R.drawable.ic_file, Color(0xFFFF9800))
        "XLS", "XLSX" -> Pair(R.drawable.ic_file, Color(0xFF0F9D58))
        else -> Pair(R.drawable.ic_file, Color(0xFF9E9E9E))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top icon with glow effect
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            fileColor.copy(alpha = 0.08f * glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 140f
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                fileColor.copy(alpha = 0.15f),
                                fileColor.copy(alpha = 0.05f)
                            ),
                            center = Offset.Unspecified,
                            radius = 100f
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = fileColor,
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer {
                            shadowElevation = 8f
                        }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main message
        Text(
            text = "No $fileType Files Found",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Detailed message
        Text(
            text = "We couldn't find any $fileType files on your device.\nYou can add files using the button below.",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Action card with shimmer effect
        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onAddFileClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A1010)
            ),
            border = BorderStroke(
                width = 1.dp,
                brush = shimmerBrush
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    tint = fileColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Add $fileType File",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Suggestion to try another format
        Text(
            text = "Or try selecting another document format",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EnhancedNoPermissionState(
    onRequestPermission: () -> Unit
) {
    val primaryColor = Color(0xFFFF4444)
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock icon with animation
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 80f
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_folder),
                contentDescription = null,
                tint = Color.White.copy(alpha = pulseAlpha),
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Storage Access Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "We need permission to access your files for conversion.\nYour files remain private and secure.",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
                .graphicsLayer {
                    shadowElevation = 8f
                }
        ) {
            Text(
                text = "Grant Permission",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
} 