package com.example.smartdocsconvert.ui.screens.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.smartdocsconvert.R
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    documentPath: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val primaryColor = Color(0xFFFF4444)
    val darkBackground = Color(0xFF1A1A1A)

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

    LaunchedEffect(documentPath) {
        try {
            val file = File(documentPath)
            if (file.exists() && file.canRead()) {
                openDocument(context, file)
                onBackClick()
            } else {
                errorMessage = "Belge bulunamadı veya okunamıyor: ${file.name}"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "Belge açılırken hata oluştu: ${e.message}"
            isLoading = false
        }
    }

    BackHandler {
        onBackClick()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mainGradient)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Belge Görüntüleyici") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Geri"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(48.dp)
                    )
                } else if (errorMessage != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_error),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Hata",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = errorMessage ?: "Bilinmeyen bir hata oluştu",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Geri Dön")
                        }
                    }
                }
            }
        }
    }
}

private fun openDocument(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val extension = file.extension.lowercase()
        val mimeType = getMimeType(extension)
        
        Log.d("DocumentViewer", "Belge açılıyor: ${file.name}, MIME türü: $mimeType, URI: $uri")
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        if (extension == "pdf") {
            val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }

            if (pdfIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(pdfIntent)
                return
            }
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=${mimeType.replace("/", " ")} viewer")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (marketIntent.resolveActivity(context.packageManager) != null) {
                Toast.makeText(
                    context,
                    "Bu belgeyi açabilecek bir uygulama bulunamadı. Uygulama mağazasından bir görüntüleyici indirebilirsiniz.",
                    Toast.LENGTH_LONG
                ).show()
                context.startActivity(marketIntent)
            } else {
                Toast.makeText(
                    context,
                    "Bu belgeyi açabilecek bir uygulama bulunamadı",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        Log.e("DocumentViewer", "Belge açma hatası", e)
        Toast.makeText(
            context,
            "Belge açılamadı: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
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
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
} 