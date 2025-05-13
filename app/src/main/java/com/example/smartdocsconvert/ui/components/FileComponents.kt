package com.example.smartdocsconvert.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.window.DialogProperties
import com.example.smartdocsconvert.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dosya tipi için chip bileşeni
 */
@Composable
fun FileTypeChip(
    type: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color = Color(0xFFFF4444)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
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

/**
 * Dosya listesi öğesi
 */
@Composable
fun FileItem(
    file: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    formatFileSize: (Long) -> String,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFFFF4444)
) {
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

/**
 * Yükleme animasyonu
 */
@Composable
fun LoadingAnimation(
    primaryColor: Color = Color(0xFFFF4444)
) {
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
                    .scale(pulseSize),
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

/**
 * Dosya bulunamadı durum ekranı
 */
@Composable
fun EmptyFileState(
    fileType: String,
    onAddFileClick: () -> Unit,
    primaryColor: Color = Color(0xFFFF4444)
) {
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
    
    // File icon and color based on type
    val fileColor = when (fileType) {
        "PDF" -> Color(0xFFFF4444)
        "DOC", "DOCX" -> Color(0xFF4285F4)
        "PPT", "PPTX" -> Color(0xFFFF9800)
        "XLS", "XLSX" -> Color(0xFF0F9D58)
        else -> Color(0xFF9E9E9E)
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
                    painter = painterResource(id = R.drawable.ic_file),
                    contentDescription = null,
                    tint = fileColor,
                    modifier = Modifier.size(60.dp)
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
        
        // Action button
        Button(
            onClick = onAddFileClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = fileColor
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Add $fileType File",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
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

/**
 * İzin verilmedi durum ekranı
 */
@Composable
fun NoPermissionState(
    onRequestPermission: () -> Unit,
    primaryColor: Color = Color(0xFFFF4444)
) {
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

/**
 * İzin reddedildi iletişim kutusu
 */
@Composable
fun PermissionDeniedDialog(
    onGoToSettings: () -> Unit,
    onClose: () -> Unit,
    primaryColor: Color = Color(0xFFFF4444),
    accentColor: Color = Color(0xFF4ECDC4),
    darkSurface: Color = Color(0xFF2A0B0B)
) {
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

    AlertDialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        title = {
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
                    text = "Permission Required",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                // Warning box with animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            color = primaryColor.copy(alpha = 0.1f)
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
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier
                                .size(28.dp)
                                .scale(warningScale)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "You need to grant storage permissions in app settings to continue",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGoToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                ),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "GO TO SETTINGS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
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
        },
        containerColor = darkSurface,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f),
        modifier = Modifier
            .graphicsLayer { 
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            },
    )
} 