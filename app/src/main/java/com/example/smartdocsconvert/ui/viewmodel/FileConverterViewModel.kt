package com.example.smartdocsconvert.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


@HiltViewModel
class FileConverterViewModel @Inject constructor() : ViewModel() {

    // UI durumunu tutan veri sınıfı
    data class FileConverterUiState(
        val selectedType: String = "PDF",
        val files: List<File> = emptyList(),
        val selectedFiles: Set<File> = emptySet(),
        val isLoading: Boolean = false,
        val hasPermission: Boolean = false,
        val showPermissionDialog: Boolean = false,
        val permissionRequestCount: Int = 0
    )

    private val _uiState = MutableStateFlow(FileConverterUiState())
    val uiState: StateFlow<FileConverterUiState> = _uiState.asStateFlow()

    // Desteklenen dosya türleri
    val fileTypes = listOf("PDF", "DOC", "DOCX", "PPT", "PPTX", "XLS", "XLSX", "TXT")

    // İzinleri kontrol et
    fun checkPermissions(context: Context) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        _uiState.update { it.copy(hasPermission = hasPermission) }
    }

    // İzin talebi sonuçlarını işle
    fun handlePermissionResult(allGranted: Boolean) {
        val currentState = _uiState.value

        _uiState.update {
            it.copy(
                hasPermission = allGranted,
                showPermissionDialog = !allGranted && currentState.permissionRequestCount > 0,
                permissionRequestCount = currentState.permissionRequestCount + 1
            )
        }
    }

    // İzin diyaloğunu kapat
    fun dismissPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    // Dosya türünü değiştir
    fun setFileType(type: String) {
        _uiState.update {
            it.copy(
                selectedType = type,
                selectedFiles = emptySet() // Tür değiştiğinde seçimleri sıfırla
            )
        }
    }

    // Dosya seç/seçimi kaldır
    fun toggleFileSelection(file: File) {
        val currentSelected = _uiState.value.selectedFiles
        val newSelected = if (currentSelected.contains(file)) {
            currentSelected - file
        } else {
            currentSelected + file
        }

        _uiState.update { it.copy(selectedFiles = newSelected) }
    }

    // Dosyaları yenile
    fun refreshFiles(context: Context) {
        val currentType = _uiState.value.selectedType

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val files = getFilesByType(context, currentType)
                _uiState.update { it.copy(files = files) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Uri'den dosya kopyala
    fun copyFileFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "document_${System.currentTimeMillis()}.${_uiState.value.selectedType.lowercase()}"

                val destinationFile =
                    File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destinationFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                // Dosya uzantısına göre seçili türü güncelle
                val extension = fileName.substringAfterLast('.', "").uppercase()
                if (fileTypes.contains(extension)) {
                    _uiState.update { it.copy(selectedType = extension) }
                }

                // Dosya listesini güncelle
                refreshFiles(context)

            } catch (e: Exception) {
                // Hata durumu işlenebilir
                android.util.Log.e("FileConverter", "Dosya kopyalama hatası", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Tip için MIME tipini döndür
    fun getMimeTypeForFileType(fileType: String): String {
        return when (fileType) {
            "PDF" -> "application/pdf"
            "DOC", "DOCX" -> "application/msword"
            "PPT", "PPTX" -> "application/vnd.ms-powerpoint"
            "XLS", "XLSX" -> "application/vnd.ms-excel"
            "TXT" -> "text/plain"
            else -> "*/*"
        }
    }

    // Dosya boyutunu formatla
    fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> String.format("%d Bytes", size)
        }
    }

    // İzinleri iste
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
    }

    // Dosya arama işlevleri
    private suspend fun getFilesByType(context: Context, type: String): List<File> = withContext(Dispatchers.IO) {
        val files = mutableListOf<File>()
        val uniquePaths = HashSet<String>() // Tekrar eden dosyaları engellemek için
        val extension = type.lowercase()

        // MediaStore ile arama
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

            // MIME type ve uzantı bazlı arama
            val mimeType = when (extension) {
                "pdf" -> "application/pdf"
                "doc", "docx" -> "application/msword"
                "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                "xls", "xlsx" -> "application/vnd.ms-excel"
                "txt" -> "text/plain"
                else -> null
            }

            val selection = if (mimeType != null) {
                "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            } else {
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            }

            val selectionArgs = if (mimeType != null) {
                arrayOf(mimeType, "%.$extension")
            } else {
                arrayOf("%.$extension")
            }

            // External storage query
            val queryUri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(pathColumn)
                    val file = File(path)
                    if (file.exists() && file.isFile) {
                        val absolutePath = file.absolutePath
                        if (uniquePaths.add(absolutePath)) {
                            files.add(file)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FileSearch", "MediaStore sorgu hatası", e)
        }

        // Özel dizinleri tara
        val specialDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File(Environment.getExternalStorageDirectory(), "Download"),
            File(Environment.getExternalStorageDirectory(), "Documents")
        )

        specialDirs.forEach { directory ->
            try {
                if (directory.exists() && directory.isDirectory) {
                    searchInDirectory(directory, extension, files, uniquePaths)
                }
            } catch (e: SecurityException) {
                android.util.Log.e("FileSearch", "Özel dizin erişim hatası: ${directory.absolutePath}", e)
            }
        }

        // Uygulamanın kendi dizinlerini de tara
        val directories = mutableListOf<File>().apply {
            add(context.filesDir)
            add(context.cacheDir)
            context.getExternalFilesDir(null)?.let { add(it) }
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { add(it) }
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { add(it) }
        }

        directories.forEach { directory ->
            try {
                searchInDirectory(directory, extension, files, uniquePaths)
            } catch (e: SecurityException) {
                android.util.Log.e("FileSearch", "Dizin erişim hatası: ${directory.absolutePath}", e)
            }
        }

        files
    }

    private fun searchInDirectory(directory: File, extension: String, files: MutableList<File>, uniquePaths: HashSet<String>) {
        if (!directory.exists() || !directory.isDirectory) {
            return
        }

        directory.listFiles()?.forEach { file ->
            try {
                when {
                    file.isFile && file.name.lowercase().endsWith(".$extension") -> {
                        val absolutePath = file.absolutePath
                        if (uniquePaths.add(absolutePath)) {
                            files.add(file)
                        }
                    }
                    file.isDirectory -> {
                        searchInDirectory(file, extension, files, uniquePaths)
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("FileSearch", "Dosya erişim hatası: ${file.absolutePath}", e)
            }
        }
    }
}