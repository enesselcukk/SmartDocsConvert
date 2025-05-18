package com.example.smartdocsconvert.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.ui.viewmodel.FileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    onOpenFile: () -> Unit,
    onOpenGallery: () -> Unit,
    viewModel: FileViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    
    // FileViewModel state observers
    val filePickerRequested by viewModel.filePickerRequested.collectAsState()
    val galleryPickerRequested by viewModel.galleryPickerRequested.collectAsState()
    
    // Handle file picker request
    LaunchedEffect(filePickerRequested) {
        if (filePickerRequested) {
            onOpenFile()
            viewModel.onFilePickerCompleted()
        }
    }
    
    // Handle gallery picker request
    LaunchedEffect(galleryPickerRequested) {
        if (galleryPickerRequested) {
            onOpenGallery()
            viewModel.onGalleryPickerCompleted()
        }
    }
    
    // Theme colors
    val primaryColor = Color(0xFFFF4444)
    val primaryVariant = Color(0xFFFF6B6B)
    val darkBackground = Color(0xFF1A1A1A)
    val darkSurface = Color(0xFF2A0B0B)
    val cardColor = Color(0xFF2D1414)
    val accentColor = Color(0xFF4ECDC4)
    val goldColor = Color(0xFFFFD700)
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    
    // Animated gradients
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
    
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            cardColor,
            Color(0xFF331515),
            Color(0xFF3A1111)
        )
    )

    LaunchedEffect(Unit) {
        isVisible = true
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
                        colors = listOf(accentColor, Color.Transparent),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Top app bar with premium look
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo and app name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PDF",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
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
                                    colors = listOf(primaryColor, primaryVariant)
                                )
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.2f),
                                                primaryVariant.copy(alpha = 0.1f)
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
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Premium button with animation
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
                        
                        // Premium animation
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
                                            goldColor.copy(alpha = 0.2f),
                                            darkSurface.copy(alpha = 0.9f)
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
                                tint = goldColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(premiumGlow)
                            )
                        }
                        
                        // Settings button
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
                                            darkSurface.copy(alpha = 0.9f)
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
                
                // Main action cards with advanced animation
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
                                iconTint = primaryColor
                            )
                            
                            Divider(
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
                                iconTint = accentColor
                            )
                        }
                    }
                }

                // Recent section header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT DOCUMENTS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortButton(icon = R.drawable.ic_sort_az, isSelected = true)
                        SortButton(icon = R.drawable.ic_sort_list, isSelected = false)
                        SortButton(icon = R.drawable.ic_sort_grid, isSelected = false)
                    }
                }

                // Empty state with enhanced animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
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
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = "No Recent Documents",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Converted documents will appear here\nfor quick access",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                viewModel.openFilePicker()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
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
    var isHovered by remember { mutableStateOf(false) }
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
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SortButton(
    icon: Int,
    isSelected: Boolean
) {
    val backgroundColor = if (isSelected) {
        Color(0xFFFF4444).copy(alpha = 0.2f)
    } else {
        Color(0xFF2A0B0B)
    }
    
    val iconColor = if (isSelected) {
        Color(0xFFFF4444)
    } else {
        Color.White.copy(alpha = 0.6f)
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .scale(scale)
            .graphicsLayer {
                shadowElevation = if (isSelected) 4f else 0f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isHovered = !isHovered
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
} 