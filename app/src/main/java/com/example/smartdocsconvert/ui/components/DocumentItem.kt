package com.example.smartdocsconvert.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.data.model.DocumentModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DocumentItem(
    document: DocumentModel,
    onDocumentClick: (DocumentModel) -> Unit,
    formatFileSize: (Long) -> String,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFFFF4444),
    cardColor: Color = Color(0xFF2D1414)
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            cardColor,
            Color(0xFF331515),
            Color(0xFF3A1111)
        )
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isPressed = true
                scope.launch {
                    delay(100)
                    isPressed = false
                    onDocumentClick(document)
                }
            }
            .graphicsLayer {
                shadowElevation = 8f
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document icon with type badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.1f),
                                primaryColor.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Document type icon based on extension
                Icon(
                    painter = painterResource(
                        id = getIconForDocumentType(document.type)
                    ),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(30.dp)
                )
                
                // Type badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = document.type.take(1),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Document info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Date and size
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(document.createdAt),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = formatFileSize(document.size),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Action buttons
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = "More options",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp)
            )
        }
    }
}

private fun formatDate(date: java.util.Date): String {
    val now = java.util.Date()
    val diff = now.time - date.time
    val diffDays = diff / (24 * 60 * 60 * 1000)
    
    return when {
        diffDays == 0L -> "Today"
        diffDays == 1L -> "Yesterday"
        diffDays < 7 -> "$diffDays days ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(date)
        }
    }
}

private fun getIconForDocumentType(type: String): Int {
    return when (type.lowercase()) {
        "pdf" -> R.drawable.ic_pdf
        "doc", "docx" -> R.drawable.ic_doc
        "xls", "xlsx" -> R.drawable.ic_xls
        "ppt", "pptx" -> R.drawable.ic_ppt
        "txt" -> R.drawable.ic_txt
        "jpg", "jpeg", "png" -> R.drawable.ic_image
        else -> R.drawable.ic_file
    }
} 