package com.example.smartdocsconvert.ui.screens.file

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
import android.content.Context
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.smartdocsconvert.ui.components.ModernTopAppBar
import com.example.smartdocsconvert.ui.theme.extendedColors


@Composable
fun SaveShareScreen(
    onBackClick: () -> Unit,
    onFinish: () -> Unit,
    optimizedFiles: List<File>
) {
    val colors = MaterialTheme.extendedColors
    
    var selectedSaveLocation by remember { mutableStateOf<Uri?>(null) }
    var selectedLocationDisplayName by remember { mutableStateOf("") }
    var processingFiles by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun saveFiles(sourceFiles: List<File>, destinationUri: Uri) {
        try {
            val destinationDirectory = DocumentFile.fromTreeUri(context, destinationUri)
                ?: throw Exception("Hedef klasör açılamadı")

            sourceFiles.forEach { sourceFile ->
                try {
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

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        try {
            uri?.let { selectedUri ->
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
                        selectedLocationDisplayName = if (displayNameIndex != -1) {
                            cursor.getString(displayNameIndex)
                        } else {
                            "Seçilen Klasör"
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
        containerColor = colors.darkBackground,
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            ModernTopAppBar(
                title = "Save & Share",
                currentStep = 3,
                backgroundColor = colors.surfaceColor,
                primaryColor = colors.primaryColor,
                onBackClick = onBackClick
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
                primaryColor = colors.primaryColor,
                backgroundColor = colors.surfaceColor,
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
                    cardColor = colors.cardColor,
                    primaryColor = colors.primaryColor
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
                    cardColor = colors.cardColor,
                    primaryColor = colors.primaryColor
                )
            }
            
            item {
                ShareOptionsCard(
                    cardColor = colors.cardColor,
                    primaryColor = colors.primaryColor,
                    files = optimizedFiles
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // İşlem göstergesi
        if (processingFiles) {
            ProcessingOverlay(
                primaryColor = colors.primaryColor
            )
        }

        if (showSuccessDialog) {
            SuccessDialog(
                onDismiss = { showSuccessDialog = false },
                onFinish = onFinish,
                primaryColor = colors.primaryColor
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
    files: List<File>
) {
    val context = LocalContext.current

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
                onClick = {
                    shareFiles(context, files)
                },
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

private fun shareFiles(context: Context, files: List<File>) {
    try {
        val uris = files.map { file ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }

        val intent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }

        val shareIntent = Intent.createChooser(intent, "Dosyaları Paylaş")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Paylaşma hatası: ${e.localizedMessage}",
            Toast.LENGTH_SHORT
        ).show()
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