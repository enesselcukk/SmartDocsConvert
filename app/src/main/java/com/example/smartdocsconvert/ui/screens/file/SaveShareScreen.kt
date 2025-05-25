package com.example.smartdocsconvert.ui.screens.file

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.smartdocsconvert.R
import java.io.FileInputStream

@SuppressLint("NewApi")
@Composable
fun SaveShareScreen(
    onBackClick: () -> Unit,
    onFinish: () -> Unit,
    optimizedFiles: List<File>
) {
    val primaryColor = Color(0xFF4361EE)
    val surfaceColor = Color(0xFF1E1E1E)
    val cardColor = Color(0xFF242424)
    
    var selectedSaveLocation by remember { mutableStateOf<Uri?>(null) }
    var selectedLocationDisplayName by remember { mutableStateOf("") }
    var processingFiles by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Snackbar state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Function to save files
    fun saveFiles(sourceFiles: List<File>, destinationUri: Uri) {
        try {
            val destinationDirectory = DocumentFile.fromTreeUri(context, destinationUri)
                ?: throw Exception("Hedef klasör açılamadı")

            sourceFiles.forEach { sourceFile ->
                try {
                    // Create a new file in the destination directory
                    val mimeType = when (sourceFile.extension.lowercase()) {
                        "pdf" -> "application/pdf"
                        "doc", "docx" -> "application/msword"
                        "xls", "xlsx" -> "application/vnd.ms-excel"
                        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                        else -> "application/octet-stream"
                    }

                    val newFile = destinationDirectory.createFile(
                        mimeType,
                        sourceFile.name
                    ) ?: throw Exception("Dosya oluşturulamadı: ${sourceFile.name}")

                    // Copy the file content
                    context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            val buffer = ByteArray(8192)
                            var length: Int
                            while (inputStream.read(buffer).also { length = it } > 0) {
                                outputStream.write(buffer, 0, length)
                            }
                            outputStream.flush()
                        }
                    } ?: throw Exception("Dosya yazılamadı: ${sourceFile.name}")

                } catch (e: Exception) {
                    throw Exception("Dosya işlenirken hata oluştu (${sourceFile.name}): ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Dosyalar kaydedilirken hata oluştu: ${e.message}")
        }
    }

    // Directory picker launcher
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        try {
            uri?.let { selectedUri ->
                // Take persistable URI permission
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(selectedUri, takeFlags)

                selectedSaveLocation = selectedUri

                val docUri = DocumentsContract.buildDocumentUriUsingTree(
                    selectedUri,
                    DocumentsContract.getTreeDocumentId(selectedUri)
                )
                
                context.contentResolver.query(docUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            selectedLocationDisplayName = cursor.getString(displayNameIndex)
                        } else {
                            selectedLocationDisplayName = "Seçilen Klasör"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Klasör seçiminde hata oluştu: ${e.localizedMessage}"
                )
            }
        }
    }
    
    Scaffold(
        containerColor = Color(0xFF121212),
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            ModernTopAppBar(
                backgroundColor = surfaceColor,
                primaryColor = primaryColor,
                onBackClick = onBackClick,
                currentStep = 3
            )
        },
        bottomBar = {
            ImprovedBottomButtons(
                onFinishClick = {
                    if (selectedSaveLocation == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Lütfen kayıt konumu seçin")
                        }
                        return@ImprovedBottomButtons
                    }
                    
                    processingFiles = true
                    coroutineScope.launch {
                        try {
                            selectedSaveLocation?.let { uri ->
                                saveFiles(optimizedFiles, uri)
                            }
                            processingFiles = false
                            showSuccessDialog = true
                        } catch (e: Exception) {
                            processingFiles = false
                            snackbarHostState.showSnackbar(e.message ?: "Dosya kaydedilirken hata oluştu")
                        }
                    }
                },
                primaryColor = primaryColor,
                backgroundColor = surfaceColor,
                enabled = !processingFiles
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Özet Kart
                SummaryCard(
                    files = optimizedFiles,
                    cardColor = cardColor,
                    primaryColor = primaryColor
                )
            }
            
            item {
                // Kaydetme Seçenekleri
                SaveOptionsCard(
                    selectedLocation = selectedLocationDisplayName,
                    hasSelectedLocation = selectedSaveLocation != null,
                    onLocationPickerClick = { 
                        directoryPickerLauncher.launch(null)
                    },
                    cardColor = cardColor,
                    primaryColor = primaryColor
                )
            }
            
            item {
                ShareOptionsCard(
                    cardColor = cardColor,
                    primaryColor = primaryColor,
                    onShareClick = { /* TODO: Implement share logic */ }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // İşlem göstergesi
        if (processingFiles) {
            ProcessingOverlay(
                primaryColor = primaryColor
            )
        }

        if (showSuccessDialog) {
            SuccessDialog(
                onDismiss = { showSuccessDialog = false },
                onFinish = onFinish,
                primaryColor = primaryColor
            )
        }
    }
}

@Composable
private fun SummaryCard(
    files: List<File>,
    cardColor: Color,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "İşlem Özeti",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    icon = painterResource(id = R.drawable.ic_file),
                    title = "Toplam Dosya",
                    value = files.size.toString(),
                    primaryColor = primaryColor
                )
                
                SummaryItem(
                    icon = painterResource(id = R.drawable.ic_save),
                    title = "Toplam Boyut",
                    value = formatTotalSize(files),
                    primaryColor = primaryColor
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: Painter,
    title: String,
    value: String,
    primaryColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SaveOptionsCard(
    selectedLocation: String,
    hasSelectedLocation: Boolean,
    onLocationPickerClick: () -> Unit,
    cardColor: Color,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Kaydetme Seçenekleri",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Konum seçici
            OutlinedButton(
                onClick = onLocationPickerClick,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, if (!hasSelectedLocation) Color.White.copy(alpha = 0.3f) else primaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder),
                    contentDescription = null,
                    tint = if (!hasSelectedLocation) Color.White.copy(alpha = 0.7f) else primaryColor
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (!hasSelectedLocation) "Kayıt Konumu Seç" else "Seçilen Konum",
                    color = if (!hasSelectedLocation) Color.White.copy(alpha = 0.7f) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasSelectedLocation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedLocation,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShareOptionsCard(
    cardColor: Color,
    primaryColor: Color,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Paylaşma Seçenekleri",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text("Dosyaları Paylaş")
            }
        }
    }
}

@Composable
private fun ProcessingOverlay(
    primaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Dosyalar Kaydediliyor...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Lütfen bekleyin",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ImprovedBottomButtons(
    onFinishClick: () -> Unit,
    primaryColor: Color,
    backgroundColor: Color,
    enabled: Boolean
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
            .padding(16.dp)
    ) {
        Button(
            onClick = onFinishClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                disabledContainerColor = primaryColor.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(28.dp),
            enabled = enabled
        ) {
            Text(
                text = "İşlemi Tamamla",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatTotalSize(files: List<File>): String {
    val totalSize = files.sumOf { it.length() }
    return when {
        totalSize < 1024 -> "$totalSize B"
        totalSize < 1024 * 1024 -> "${totalSize / 1024} KB"
        totalSize < 1024 * 1024 * 1024 -> "${totalSize / (1024 * 1024)} MB"
        else -> "${totalSize / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
private fun ModernTopAppBar(
    backgroundColor: Color,
    primaryColor: Color,
    onBackClick: () -> Unit,
    currentStep: Int
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
                text = "Save & Share",
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
                currentStep = currentStep,
                totalSteps = 3
            )
        }
    }
}

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

@Composable
private fun SuccessDialog(
    onDismiss: () -> Unit,
    onFinish: () -> Unit,
    primaryColor: Color
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "İşlem Başarılı!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Dosyalarınız başarıyla kaydedildi.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        onDismiss()
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tamam")
                }
            }
        }
    }
} 