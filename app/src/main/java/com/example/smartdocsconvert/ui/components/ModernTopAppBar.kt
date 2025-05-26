package com.example.smartdocsconvert.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartdocsconvert.R

@Composable
fun ModernTopAppBar(
    title: String,
    currentStep: Int,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
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

            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

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
    
    // Adımlar
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

                    EnhancedStepIndicator(
                        number = stepNumber,
                        isActive = isActive,
                        isCompleted = isCompleted,
                        primaryColor = primaryColor
                    )

                    if (index < totalSteps - 1) {
                        Box(
                            modifier = Modifier
                                .width(lineWidth)
                                .height(2.dp)
                        ) {
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

    val activeScale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isActive) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    val checkScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

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
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(checkScale)
                )
            } else {
                Text(
                    text = number.toString(),
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = if (isActive) 18.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(2.dp)
                )

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