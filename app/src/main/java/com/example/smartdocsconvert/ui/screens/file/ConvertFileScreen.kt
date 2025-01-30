package com.example.smartdocsconvert.ui.screens.file

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smartdocsconvert.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ConvertFileScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf("DOC") }
    val fileTypes = listOf("DOC", "DOCX", "PDF", "PPT", "PPTX", "XLS", "XLSX", "TXT")
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkPermissions(context)) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
    }

    LaunchedEffect(selectedType, hasPermission) {
        if (hasPermission) {
            isLoading = true
            files = withContext(Dispatchers.IO) {
                getFilesByType(context, selectedType)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
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
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    color = Color(0xFF2A0B0B),
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
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
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                IconButton(
                    onClick = { /* TODO: Open folder */ },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder),
                        contentDescription = "Folder",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Progress dots with lines
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 0) Color(0xFFFF4444)
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                    if (index < 2) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        // File type tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(fileTypes) { type ->
                val isSelected = type == selectedType
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { selectedType = type }
                        )
                ) {
                    Text(
                        text = type,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(Color(0xFFFF4444))
                        )
                    }
                }
            }
        }

        // File list or loading state
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                files.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No ${selectedType} Files Found",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            FileItem(file = file)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(file: File) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A0B0B), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "${formatFileSize(file.length())} • ${file.parent}",
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_next),
            contentDescription = "Select",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

private suspend fun getFilesByType(context: Context, type: String): List<File> = withContext(Dispatchers.IO) {
    val files = mutableListOf<File>()
    val extension = type.lowercase()
    
    // MediaStore kullanarak dosyaları ara
    val projection = arrayOf(
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.SIZE
    )

    val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%.$extension")
    
    val queryUri = MediaStore.Files.getContentUri("external")

    try {
        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            
            while (cursor.moveToNext()) {
                val path = cursor.getString(pathColumn)
                val file = File(path)
                if (file.exists() && file.isFile) {
                    files.add(file)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("FileSearch", "MediaStore query error", e)
    }

    // Ek olarak doğrudan dizinlerde de ara
    val directories = mutableListOf<File>().apply {
        add(Environment.getExternalStorageDirectory())
        add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
        context.getExternalFilesDirs(null).forEach { if (it != null) add(it) }
    }

    directories.forEach { directory ->
        try {
            searchInDirectory(directory, extension, files)
        } catch (e: SecurityException) {
            android.util.Log.e("FileSearch", "Directory access error: ${directory.absolutePath}", e)
        }
    }

    // Debug bilgisi
    android.util.Log.d("FileSearch", "Bulunan dosya sayısı: ${files.size}")
    android.util.Log.d("FileSearch", "Aranan uzantı: $extension")
    files.forEach { file ->
        android.util.Log.d("FileSearch", "Bulunan dosya: ${file.absolutePath}")
    }

    files
}

private fun searchInDirectory(directory: File, extension: String, files: MutableList<File>) {
    if (!directory.exists() || !directory.isDirectory) return

    directory.listFiles()?.forEach { file ->
        try {
            when {
                file.isFile && file.name.lowercase().endsWith(".$extension") -> {
                    files.add(file)
                }
                file.isDirectory -> {
                    // Sadece belirli bir derinliğe kadar ara (sonsuz döngüyü önlemek için)
                    if (!file.name.startsWith(".") && !isSystemDirectory(file)) {
                        searchInDirectory(file, extension, files)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Dosya erişim hatası - atla
        }
    }
}

private fun isSystemDirectory(file: File): Boolean {
    val name = file.name.lowercase()
    return name == "android" || name == "data" || name == "obb" || 
           name.startsWith("com.") || name.startsWith("org.") ||
           name.startsWith(".")
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