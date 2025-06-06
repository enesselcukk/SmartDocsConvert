package com.example.smartdocsconvert.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import androidx.navigation.NavController
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.data.model.DocumentModel
import com.example.smartdocsconvert.ui.navigation.Screen
import com.example.smartdocsconvert.ui.theme.extendedColors
import com.example.smartdocsconvert.ui.viewmodel.FileViewModel
import com.example.smartdocsconvert.ui.viewmodel.SortType
import com.example.smartdocsconvert.ui.viewmodel.ViewType
import com.example.smartdocsconvert.di.PermissionHelperEntryPoint
import com.example.smartdocsconvert.util.getUri
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenFile: () -> Unit,
    onOpenGallery: () -> Unit,
    navController: NavController,
    viewModel: FileViewModel = hiltViewModel()
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
    
    val documentsList by viewModel.documentsList.collectAsState()
    val currentSortType by viewModel.currentSortType.collectAsState()
    val currentViewType by viewModel.currentViewType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scope = rememberCoroutineScope()

    var hasStoragePermission by remember { mutableStateOf(permissionHelper.hasStoragePermissions()) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionRequestCount by remember { mutableIntStateOf(0) }

    val filePickerRequested by viewModel.filePickerRequested.collectAsState()
    val galleryPickerRequested by viewModel.galleryPickerRequested.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionRequestCount++
        hasStoragePermission = allGranted

        if (allGranted.not() && permissionRequestCount > 1) {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        hasStoragePermission = permissionHelper.hasStoragePermissions()
    }

    LaunchedEffect(filePickerRequested) {
        if (filePickerRequested) {
            if (hasStoragePermission) {
                onOpenFile()
            } else {
                permissionLauncher.launch(permissionHelper.getRequiredPermissions())
            }
            viewModel.onFilePickerCompleted()
        }
    }

    LaunchedEffect(galleryPickerRequested) {
        if (galleryPickerRequested) {
            if (hasStoragePermission) {
                onOpenGallery()
            } else {
                permissionLauncher.launch(permissionHelper.getRequiredPermissions())
            }
            viewModel.onGalleryPickerCompleted()
        }
    }

    var isVisible by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val mainGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        )
    )
    
    val cardGradient = Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) {
            listOf(
                MaterialTheme.extendedColors.cardRed,
                Color(0xFF331515),
                Color(0xFF3A1111)
            )
        } else {
            listOf(
                MaterialTheme.extendedColors.cardRed,
                MaterialTheme.extendedColors.cardRed.copy(alpha = 0.9f),
                MaterialTheme.extendedColors.cardRed.copy(alpha = 0.8f)
            )
        }
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Text color based on theme
    val textColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF121212)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mainGradient)
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .graphicsLayer {
                    alpha = 0.1f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, Color.Transparent),
                        radius = 300f
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
                        colors = listOf(MaterialTheme.extendedColors.accentTeal, Color.Transparent),
                        radius = 200f
                    ),
                    shape = CircleShape
                )
        )

        // Main content
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(1000)) +
                    slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(1000))
        ) {
            if (!hasStoragePermission) {
                StoragePermissionRequest(
                    onRequestPermission = {
                        permissionLauncher.launch(permissionHelper.getRequiredPermissions())
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                ) {
                    val listState = rememberLazyListState()
                    val isScrolled = remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
                    val scrollOffset = remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

                    AnimatedVisibility(
                        visible = !isScrolled.value,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "PDF",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                shadowElevation = 8f
                                            }
                                    )
                                    
                                    Card(
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.Transparent
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                )
                                            )
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        )
                                                    )
                                                )
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Converter",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val premiumInteraction = remember { MutableInteractionSource() }
                                    var isPremiumPressed by remember { mutableStateOf(false) }
                                    val premiumScale by animateFloatAsState(
                                        targetValue = if (isPremiumPressed) 1.1f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = ""
                                    )

                                    val premiumGlow by infiniteTransition.animateFloat(
                                        initialValue = 0.8f,
                                        targetValue = 1.1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000), 
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = ""
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .scale(premiumScale)
                                            .graphicsLayer {
                                                shadowElevation = 4f
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        MaterialTheme.extendedColors.goldColor.copy(alpha = 0.2f),
                                                        MaterialTheme.extendedColors.surfaceRed.copy(alpha = 0.9f)
                                                    ),
                                                    radius = premiumGlow * 60f
                                                )
                                            )
                                            .clickable(
                                                interactionSource = premiumInteraction,
                                                indication = null
                                            ) {
                                                isPremiumPressed = true
                                                scope.launch {
                                                    delay(150)
                                                    isPremiumPressed = false
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_crown),
                                            contentDescription = "Premium",
                                            tint = MaterialTheme.extendedColors.goldColor,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .scale(premiumGlow)
                                        )
                                    }

                                    val settingsInteraction = remember { MutableInteractionSource() }
                                    var isSettingsPressed by remember { mutableStateOf(false) }
                                    val settingsScale by animateFloatAsState(
                                        targetValue = if (isSettingsPressed) 1.1f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = ""
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .scale(settingsScale)
                                            .graphicsLayer {
                                                shadowElevation = 4f
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.2f),
                                                        MaterialTheme.extendedColors.surfaceRed.copy(alpha = 0.9f)
                                                    )
                                                )
                                            )
                                            .clickable(
                                                interactionSource = settingsInteraction,
                                                indication = null
                                            ) {
                                                isSettingsPressed = true
                                                scope.launch {
                                                    delay(150)
                                                    isSettingsPressed = false
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_settings),
                                            contentDescription = "Settings",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .graphicsLayer {
                                        shadowElevation = 16f
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Transparent
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.3f),
                                            Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardGradient)
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FeaturedActionButton(
                                            icon = R.drawable.ic_file,
                                            text = "Convert file",
                                            description = "Convert documents to any format",
                                            onClick = {
                                                viewModel.openFilePicker()
                                            },
                                            modifier = Modifier.weight(1f),
                                            iconTint = MaterialTheme.colorScheme.primary
                                        )

                                        HorizontalDivider(
                                            modifier = Modifier
                                                .height(100.dp)
                                                .width(1.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0f),
                                                            Color.White.copy(alpha = 0.3f),
                                                            Color.White.copy(alpha = 0f)
                                                        )
                                                    )
                                                )
                                        )
                                        
                                        FeaturedActionButton(
                                            icon = R.drawable.ic_camera,
                                            text = "Photo to PDF",
                                            description = "Convert images to PDF documents",
                                            onClick = {
                                                viewModel.openGalleryPicker()
                                            },
                                            modifier = Modifier.weight(1f),
                                            iconTint = MaterialTheme.extendedColors.accentTeal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = if (isScrolled.value) 8.dp else 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT DOCUMENTS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RefreshButton(
                                isLoading = isLoading,
                                onClick = { viewModel.refreshDocuments() }
                            )

                            SortButton(
                                icon = R.drawable.ic_sort_az,
                                isSelected = currentSortType == SortType.ALPHABETICAL,
                                onClick = { viewModel.changeSortType(SortType.ALPHABETICAL) }
                            )

                            SortButton(
                                icon = if (currentSortType == SortType.DATE) R.drawable.ic_sort_date else R.drawable.ic_sort_size,
                                isSelected = true,
                                onClick = {
                                    if (currentSortType == SortType.DATE) {
                                        viewModel.changeSortType(SortType.SIZE)
                                    } else {
                                        viewModel.changeSortType(SortType.DATE)
                                    }
                                }
                            )

                            SortButton(
                                icon = if (currentViewType == ViewType.LIST) R.drawable.ic_sort_grid else R.drawable.ic_sort_list,
                                isSelected = true,
                                onClick = {
                                    val newViewType = if (currentViewType == ViewType.LIST) ViewType.GRID else ViewType.LIST
                                    Log.d("HomeScreen", "Görünüm değiştiriliyor: $currentViewType -> $newViewType")
                                    viewModel.changeViewType(newViewType)
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        } else if (documentsList.isEmpty()) {
                            val emptyStateAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.7f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = ""
                            )
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .graphicsLayer {
                                        alpha = emptyStateAlpha
                                    }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_empty_file),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .graphicsLayer {
                                            shadowElevation = 4f
                                        },
                                    tint = textColor.copy(alpha = 0.5f)
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Text(
                                    text = "No Recent Documents",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor.copy(alpha = 0.9f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Converted documents will appear here\nfor quick access",
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Button(
                                    onClick = {
                                        viewModel.openFilePicker()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = "Create New",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (currentViewType == ViewType.LIST) {
                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = documentsList,
                                            key = { document -> document.id }
                                        ) { document ->
                                            DocumentItem(
                                                document = document,
                                                onClick = {
                                                    navController.navigate("${Screen.DocumentViewer.route}/${document.id}")
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = documentsList.chunked(2),
                                            key = { documentGroup -> documentGroup.joinToString { it.id } }
                                        ) { documentGroup ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                documentGroup.forEach { document ->
                                                    DocumentGridItem(
                                                        document = document,
                                                        onClick = {
                                                            navController.navigate("${Screen.DocumentViewer.route}/${document.id}")
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (documentGroup.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }

                                val fabScale by animateFloatAsState(
                                    targetValue = if (isScrolled.value) 1.1f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessVeryLow
                                    ),
                                    label = ""
                                )
                                
                                val fabRotation by animateFloatAsState(
                                    targetValue = if (isScrolled.value) 45f else 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = ""
                                )
                                
                                FloatingActionButton(
                                    onClick = { viewModel.openFilePicker() },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(24.dp)
                                        .size(56.dp)
                                        .graphicsLayer {
                                            val scrollFactor = (1f - minOf(1f, scrollOffset.value / 500f)) * 0.2f
                                            scaleX = fabScale + scrollFactor
                                            scaleY = fabScale + scrollFactor
                                            rotationZ = fabRotation
                                            shadowElevation = if (isScrolled.value) 16f else 8f
                                        },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_add),
                                        contentDescription = "Add document",
                                        modifier = Modifier.size(24.dp)
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
private fun FeaturedActionButton(
    icon: Int,
    text: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color(0xFFFF4444)
) {
    val isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    val textColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF121212)

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                iconTint.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    tint = iconTint,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(iconScale)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SortButton(
    icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val buttonScope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier
            .size(40.dp)
            .clickable { 
                buttonScope.launch {
                    onClick()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (!isSelected) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        else
            null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "Sort",
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RefreshButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(40.dp)
            .clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DocumentItem(
    document: DocumentModel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(context.getUri(document.path), "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = document.getDocumentIcon()),
                    contentDescription = "Document Type",
                    modifier = Modifier.size(28.dp),
                    tint = primaryColor
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(document.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = Formatter.formatFileSize(LocalContext.current, document.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DocumentGridItem(
    document: DocumentModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable { 
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(context.getUri(document.path), "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = document.getDocumentIcon()
                    ),
                    contentDescription = "Document Type",
                    modifier = Modifier.size(40.dp),
                    tint = primaryColor
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDate(document.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = Formatter.formatFileSize(LocalContext.current, document.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val diffDays = diff / (24 * 60 * 60 * 1000)
    
    return when {
        diffDays == 0L -> "Today"
        diffDays == 1L -> "Yesterday"
        diffDays < 7 -> "$diffDays days ago"
        else -> {
            SimpleDateFormatter.format(date)
        }
    }
}

private object SimpleDateFormatter {
    private val formatter by lazy { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    fun format(date: Date): String {
        return formatter.format(date)
    }
}

@Composable
private fun StoragePermissionRequest(
    onRequestPermission: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF000000)
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
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
                color = textColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "We need storage permission to access and convert your files.",
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.7f),
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
private fun PermissionDeniedDialog(
    onGoToSettings: () -> Unit,
    onClose: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = Color(0xFFFF5A5A)
    val backgroundColor = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFF8F9FF)
    val textColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF121212)

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

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

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
                                if (isSystemInDarkTheme()) Color(0xFF121212) else Color(0xFFEDF0FF)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (isSystemInDarkTheme()) 
                                    Color.White.copy(alpha = 0.15f)
                                else 
                                    primaryColor.copy(alpha = 0.2f),
                                if (isSystemInDarkTheme())
                                    Color.White.copy(alpha = 0.05f)
                                else
                                    primaryColor.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                    Text(
                        text = "Permission Required",
                        color = textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "We need storage permission to access documents for conversion. You can grant it in app settings.",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(28.dp))

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
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
