package com.example.smartdocsconvert.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smartdocsconvert.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions

@Composable
fun TopActionsBar(
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IconButton(
            onClick = onBackClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Geri",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        FilledIconButton(
            onClick = onSaveClick,
            enabled = !isSaving,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_download),
                    contentDescription = "Kaydet",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ImagePager(
    currentImageUri: Uri,
    rotationAngle: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationY: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = currentImageUri,
            contentDescription = "Document image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    rotationZ = rotationAngle
                    this.rotationY = rotationY
                }
        )
    }
}

@Composable
fun ImageThumbnails(
    imageUris: List<Uri>,
    currentIndex: Int,
    onImageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageUris.forEachIndexed { index, uri ->
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (index == currentIndex) 2.dp else 1.dp,
                        color = if (index == currentIndex) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onImageSelected(index) }
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Thumbnail ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    activeFeature: String?,
    onFeatureClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomNavItem(
            icon = painterResource(id = R.drawable.ic_rotate),
            label = "Döndür",
            isSelected = activeFeature == "rotate",
            onClick = { onFeatureClick("rotate") }
        )
        
        BottomNavItem(
            icon = painterResource(id = R.drawable.ic_crop),
            label = "Kırp",
            isSelected = activeFeature == "crop",
            onClick = { onFeatureClick("crop") }
        )
        
        BottomNavItem(
            icon = painterResource(id = R.drawable.ic_filter),
            label = "Filtre",
            isSelected = activeFeature == "filter",
            onClick = { onFeatureClick("filter") }
        )
        
        BottomNavItem(
            icon = painterResource(id = R.drawable.ic_adjust),
            label = "Ayarla",
            isSelected = activeFeature == "adjust",
            onClick = { onFeatureClick("adjust") }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: Any,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else Color.Transparent
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon as androidx.compose.ui.graphics.painter.Painter,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ImageAdjustmentControls(
    brightness: Float,
    contrast: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onResetClick: () -> Unit,
    onAutoEnhanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Görüntü Ayarları",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(onClick = onResetClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Sıfırla"
                        )
                    }
                    
                    IconButton(onClick = onAutoEnhanceClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "Otomatik İyileştir"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Parlaklık",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = 0.5f..1.5f,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Kontrast",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Slider(
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = 0.5f..1.5f,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun FilterOptionsView(
    selectedFilter: String,
    filterIntensity: Float,
    onFilterSelected: (String) -> Unit,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("Original", "Clarendon", "Moon", "Lark", "Reyes", "Juno", "Gingham")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Filtreler",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filters.forEach { filter ->
                    FilterOption(
                        name = filter,
                        isSelected = filter == selectedFilter,
                        onClick = { onFilterSelected(filter) }
                    )
                }
            }
            
            if (selectedFilter != "Original") {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Filtre Yoğunluğu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Slider(
                    value = filterIntensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FilterOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    when (name) {
                        "Original" -> Color.White
                        "Clarendon" -> Color(0xFFDDA5B6)
                        "Moon" -> Color(0xFFBFBFBF)
                        "Lark" -> Color(0xFFC1DDC7)
                        "Reyes" -> Color(0xFFE6D0B3)
                        "Juno" -> Color(0xFFD4C5AA)
                        "Gingham" -> Color(0xFFBFDDE6)
                        else -> Color.White
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ImageControlActions(
    onRotateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onRotateClick,
            modifier = Modifier
                .size(56.dp)
                .border(1.dp, Color.LightGray, CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rotate),
                contentDescription = "Döndür",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun DownloadOptionsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaveAsImage: () -> Unit,
    onSaveAsPdf: () -> Unit
) {
    val dialogOffset = remember { Animatable(if (visible) 0f else 300f) }
    val dialogAlpha = remember { Animatable(if (visible) 1f else 0f) }
    var isDialogVisible by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) {
            isDialogVisible = true
            dialogOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            dialogAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(300)
            )
        } else {
            dialogOffset.animateTo(
                targetValue = 300f,
                animationSpec = tween(150)
            )
            dialogAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(150)
            )
            isDialogVisible = false
        }
    }

    if (isDialogVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .background(Color.Black.copy(alpha = 0.5f * dialogAlpha.value)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp)
                    .graphicsLayer {
                        alpha = dialogAlpha.value
                        scaleX = 0.8f + (0.2f * dialogAlpha.value)
                        scaleY = 0.8f + (0.2f * dialogAlpha.value)
                        translationY = dialogOffset.value
                    }
                    .clickable(enabled = false) {  },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "İndirme Seçenekleri",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DownloadOptionCard(
                            icon = R.drawable.ic_image,
                            text = "Görüntü",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSaveAsImage()
                            }
                        )

                        DownloadOptionCard(
                            icon = R.drawable.ic_pdf,
                            text = "PDF",
                            color = Color(0xFFE91E63),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSaveAsPdf()
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "İptal",
                            color = Color(0xFF757575),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadOptionCard(
    icon: Int,
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Card(
        modifier = modifier
            .aspectRatio(0.9f)
            .graphicsLayer {
                scaleX = if (isPressed) 0.95f else 1f
                scaleY = if (isPressed) 0.95f else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = text,
                color = Color(0xFF333333),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun DownloadAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.5f) }
    val rotationAnim = remember { Animatable(0f) }
    var isAnimationVisible by remember { mutableStateOf(visible) }
    
    LaunchedEffect(visible) {
        if (visible) {
            isAnimationVisible = true

            alphaAnim.snapTo(0f)
            scaleAnim.snapTo(0.5f)
            rotationAnim.snapTo(0f)

            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(300)
                )
                delay(1200)
                alphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(300)
                )

                isAnimationVisible = false
            }
            
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            
            launch {
                rotationAnim.animateTo(
                    targetValue = 720f,
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        } else {
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(150)
            )
            isAnimationVisible = false
        }
    }
    
    if (isAnimationVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * alphaAnim.value))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        alpha = alphaAnim.value
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = Color(0xFF2196F3),
                                strokeWidth = 4.dp
                            )
                            
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = "Download",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        rotationZ = rotationAnim.value
                                    }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "İndiriliyor",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val buttonSize = 56.dp
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .size(buttonSize)
            .graphicsLayer {
                scaleX = if (isPressed) 0.9f else 1f
                scaleY = if (isPressed) 0.9f else 1f
            }
            .shadow(8.dp, CircleShape)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2196F3),
                        Color(0xFF03A9F4)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_download),
            contentDescription = "Save",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DownloadConfirmationDialog(
    visible: Boolean,
    filename: String,
    downloadType: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onFilenameChanged: (String) -> Unit,
    onDownloadAllChanged: ((Boolean) -> Unit)? = null,
    downloadAll: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dialogOffset = remember { Animatable(if (visible) 0f else 300f) }
    val dialogAlpha = remember { Animatable(if (visible) 1f else 0f) }
    var isDialogVisible by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        isDialogVisible = visible
        if (visible) {
            dialogOffset.animateTo(0f)
            dialogAlpha.animateTo(1f)
        } else {
            dialogAlpha.animateTo(0f)
            dialogOffset.animateTo(300f)
        }
    }
    
    if (isDialogVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { 
                    alpha = dialogAlpha.value
                }
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCancel() }
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.Center)
                    .fillMaxWidth(0.95f)
                    .graphicsLayer {
                        translationY = dialogOffset.value
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp
                ),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val titleText = if (downloadType == "pdf") "PDF İndirme" else "Görüntü İndirme"
                    val iconRes = if (downloadType == "pdf") R.drawable.ic_pdf else R.drawable.ic_image
                    val iconColor = if (downloadType == "pdf") Color(0xFFE91E63) else Color(0xFF4CAF50)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF333333)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEDEDED)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFDDDDDD))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Dosya adı:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF505050)
                            )

                            val fileExtension = remember(filename) {
                                filename.substringAfterLast(".", "")
                            }

                            val baseFilename = remember(filename) {
                                filename.substringBeforeLast(".", "")
                            }

                            var textFieldValue by remember(baseFilename) { mutableStateOf(baseFilename) }
                            val focusRequester = remember { FocusRequester() }

                            LaunchedEffect(Unit) {
                                delay(300)
                                focusRequester.requestFocus()
                            }
                            
                            OutlinedTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue

                                    val sanitizedValue = newValue.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                                    if (sanitizedValue.isNotEmpty()) {
                                        onFilenameChanged("$sanitizedValue.$fileExtension")
                                    } else {
                                        onFilenameChanged("document.$fileExtension")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onConfirm() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = iconColor,
                                    unfocusedBorderColor = Color(0xFF9E9E9E),
                                    cursorColor = iconColor,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                ),
                                leadingIcon = if (textFieldValue.isNotEmpty()) {
                                    {
                                        IconButton(
                                            onClick = {
                                                textFieldValue = ""
                                                onFilenameChanged("document.$fileExtension")
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear text",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                } else null,
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xFFEEEEEE), 
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = ".$fileExtension",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color(0xFF555555)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (onDownloadAllChanged != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { onDownloadAllChanged(!downloadAll) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = downloadAll,
                                onCheckedChange = { onDownloadAllChanged(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = iconColor,
                                    uncheckedColor = Color(0xFF757575)
                                )
                            )

                            val labelText = if (downloadType == "pdf") {
                                "Tüm resimleri PDF olarak birleştir"
                            } else {
                                "Tüm resimleri indir"
                            }
                            
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF505050)
                            )
                        ) {
                            Text(
                                "İptal",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = iconColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "İndir",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
