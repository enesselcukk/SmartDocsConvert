package com.example.smartdocsconvert.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    data class FileConverterUiState(
        val files: List<File> = emptyList(),
        val selectedFiles: Set<File> = emptySet(),
        val isLoading: Boolean = false,
        val hasPermission: Boolean = false,
        val showPermissionDialog: Boolean = false,
        val permissionRequestCount: Int = 0,
        val lastError: String? = null
    )

    private val _uiState = MutableStateFlow(FileConverterUiState())
    val uiState: StateFlow<FileConverterUiState> = _uiState.asStateFlow()

    private val supportedExtensions = listOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt")
    
    private var fileLoadingJob: Job? = null
    
    private val fileCache = mutableMapOf<String, List<File>>()
    private val allFilesCache = mutableListOf<File>()

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

    fun dismissPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    fun toggleFileSelection(file: File) {
        val currentSelected = _uiState.value.selectedFiles
        val newSelected = if (currentSelected.contains(file)) {
            currentSelected - file
        } else {
            currentSelected + file
        }

        _uiState.update { it.copy(selectedFiles = newSelected) }
    }

    fun refreshFiles(context: Context) {
        fileLoadingJob?.cancel()
        
        if (allFilesCache.isNotEmpty()) {
            _uiState.update { it.copy(files = allFilesCache.toList(), isLoading = false) }
        }
        
        fileLoadingJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, lastError = null) }
                
                val allFiles = getAllSupportedFiles(context)
                
                allFilesCache.clear()
                allFilesCache.addAll(allFiles)
                
                _uiState.update { it.copy(files = allFiles, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(lastError = e.message, isLoading = false) }
               Log.e("FileConverter", "Dosya yükleme hatası", e)
            }
        }
    }

    fun forceRefreshFiles(context: Context) {
        allFilesCache.clear()
        fileCache.clear()
        
       Log.d("FileSearch", "Cache temizlendi, dosyalar yeniden yükleniyor...")
        
        refreshFiles(context)
    }


    private suspend fun getAllSupportedFiles(context: Context): List<File> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<File>()
        val uniquePaths = HashSet<String>()
        
       Log.d("FileSearch", "Dosya arama başlatılıyor...")
        
        for (extension in supportedExtensions) {
            val cachedFiles = fileCache[extension]
            if (cachedFiles != null) {
                cachedFiles.forEach { file ->
                    if (uniquePaths.add(file.absolutePath)) {
                        allFiles.add(file)
                    }
                }
               Log.d("FileSearch", "$extension: cache'den ${cachedFiles.size} dosya alındı")
            } else {
                try {
                    val files = getFilesByExtension(context, extension)
                    fileCache[extension] = files
                    
                    files.forEach { file ->
                        if (uniquePaths.add(file.absolutePath)) {
                            allFiles.add(file)
                        }
                    }
                   Log.d("FileSearch", "$extension: ${files.size} dosya bulundu")
                } catch (e: Exception) {
                   Log.e("FileSearch", "$extension dosyalarını yükleme hatası", e)
                }
            }
        }
        
        allFiles.sortByDescending { it.lastModified() }
        
       Log.d("FileSearch", "Toplam ${allFiles.size} dosya bulundu")
        
        val maxFiles = 1000
        if (allFiles.size > maxFiles) {
           Log.d("FileSearch", "Maksimum dosya limiti: $maxFiles, tümü: ${allFiles.size}")
            allFiles.subList(0, maxFiles)
        } else {
            allFiles
        }
    }
    
    private suspend fun getFilesByExtension(context: Context, extension: String): List<File> = withContext(Dispatchers.IO) {
        val files = mutableListOf<File>()
        val uniquePaths = HashSet<String>()

       Log.d("FileSearch", "$extension uzantılı dosyaları arama başlıyor")

        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

            val mimeType = when (extension) {
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
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

            val queryUri = MediaStore.Files.getContentUri("external")
            var mediaStoreFileCount = 0
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

               Log.d("FileSearch", "MediaStore sorgusu: ${cursor.count} sonuç döndürdü")

                while (cursor.moveToNext()) {
                    try {
                        val path = cursor.getString(pathColumn)
                        
                        if (path != null) {
                            val file = File(path)
                            if (file.exists() && file.isFile) {
                                if (file.canRead()) {
                                    val absolutePath = file.absolutePath
                                    if (uniquePaths.add(absolutePath)) {
                                        files.add(file)
                                        mediaStoreFileCount++
                                    }
                                } else {
                                   Log.w("FileSearch", "Dosya okunamıyor: $path")
                                }
                            } else {
                               Log.w("FileSearch", "Dosya mevcut değil: $path")
                            }
                        }
                    } catch (e: Exception) {
                       Log.e("FileSearch", "Dosya işleme hatası", e)
                    }
                }
            }
           Log.d("FileSearch", "MediaStore'dan $mediaStoreFileCount $extension dosyası eklendi")
        } catch (e: Exception) {
           Log.e("FileSearch", "MediaStore sorgu hatası", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = when (extension) {
                    "pdf" -> "application/pdf"
                    "doc" -> "application/msword"
                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "ppt" -> "application/vnd.ms-powerpoint" 
                    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    "xls" -> "application/vnd.ms-excel"
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "txt" -> "text/plain"
                    else -> ""
                }
                
                if (mimeType.isNotEmpty()) {
                   Log.d("FileSearch", "Android 10+ için ContentResolver sorgusu yapılıyor")
                    var contentResolverFileCount = 0
                    
                    val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    val selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ?"
                    val selectionArgs = arrayOf(mimeType)
                    
                    contentResolver.query(
                        collection,
                        null,
                        selection,
                        selectionArgs,
                        MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
                    )?.use { cursor ->
                       Log.d("FileSearch", "ContentResolver sorgusu: ${cursor.count} sonuç")
                        
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                        
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                                id
                            )
                            
                            try {
                                contentResolver.openFileDescriptor(contentUri, "r")?.use {
                                    val filePath = getPath(context, contentUri)
                                    if (filePath != null) {
                                        val file = File(filePath)
                                        if (file.exists() && file.isFile && file.canRead()) {
                                            if (uniquePaths.add(file.absolutePath)) {
                                                files.add(file)
                                                contentResolverFileCount++
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                               Log.e("FileSearch", "ContentResolver dosya erişim hatası: $name", e)
                            }
                        }
                    }
                   Log.d("FileSearch", "ContentResolver'dan $contentResolverFileCount $extension dosyası eklendi")
                }
            } catch (e: Exception) {
               Log.e("FileSearch", "ContentResolver sorgu hatası", e)
            }
        }

        val specialDirs = mutableListOf<File>().apply {
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            
            add(Environment.getExternalStorageDirectory())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.getExternalFilesDirs(null).forEach { add(it) }
            }
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(File(Environment.getExternalStorageDirectory(), "Download"))
                add(File(Environment.getExternalStorageDirectory(), "Documents"))
                add(File(Environment.getExternalStorageDirectory(), "PDF"))
                add(File(Environment.getExternalStorageDirectory(), "PDFs"))
                add(File(Environment.getExternalStorageDirectory(), "Document"))
                add(File(Environment.getExternalStorageDirectory(), "Downloads"))
            }
        }

        var specialDirFileCount = 0
        specialDirs.forEach { directory ->
            try {
                if (directory.exists() && directory.isDirectory) {
                    val beforeCount = files.size
                    searchInDirectory(directory, extension, files, uniquePaths, maxDepth = 3)
                    val afterCount = files.size
                    specialDirFileCount += (afterCount - beforeCount)
                   Log.d("FileSearch", "Dizin taraması: ${directory.absolutePath}, bulunan: ${afterCount - beforeCount}")
                }
            } catch (e: SecurityException) {
               Log.e("FileSearch", "Özel dizin erişim hatası: ${directory.absolutePath}", e)
            }
        }
       Log.d("FileSearch", "Özel dizinlerden $specialDirFileCount $extension dosyası eklendi")

        val appDirs = mutableListOf<File>().apply {
            add(context.filesDir)
            add(context.cacheDir)
            context.getExternalFilesDir(null)?.let { add(it) }
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { add(it) }
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { add(it) }
        }

        var appDirFileCount = 0
        appDirs.forEach { directory ->
            try {
                val beforeCount = files.size
                searchInDirectory(directory, extension, files, uniquePaths, maxDepth = 5)
                val afterCount = files.size
                appDirFileCount += (afterCount - beforeCount)
               Log.d("FileSearch", "App dizini: ${directory.absolutePath}, bulunan: ${afterCount - beforeCount}")
            } catch (e: Exception) {
               Log.e("FileSearch", "Dizin erişim hatası: ${directory.absolutePath}", e)
            }
        }
       Log.d("FileSearch", "Uygulama dizinlerinden $appDirFileCount $extension dosyası eklendi")

       Log.d("FileSearch", "Toplam ${files.size} $extension dosyası bulundu")
        files
    }

    private fun searchInDirectory(
        directory: File,
        extension: String, 
        files: MutableList<File>, 
        uniquePaths: HashSet<String>,
        maxDepth: Int = 3,
        currentDepth: Int = 0
    ) {
        if (directory.exists().not() || !directory.isDirectory || currentDepth > maxDepth) {
            return
        }

        try {
            directory.listFiles()?.forEach { file ->
                try {
                    when {
                        file.isFile && file.name.lowercase().endsWith(".$extension") -> {
                            val absolutePath = file.absolutePath
                            if (uniquePaths.add(absolutePath)) {
                                files.add(file)
                                if (!file.canRead()) {
                                   Log.w("FileSearch", "Dosya eklendi ama okuma izni yok: $absolutePath")
                                }
                            }
                        }
                        file.isDirectory && currentDepth < maxDepth -> {
                            searchInDirectory(file, extension, files, uniquePaths, maxDepth, currentDepth + 1)
                        }
                    }
                } catch (e: SecurityException) {
                   Log.w("FileSearch", "Dosya erişim hatası: ${file.absolutePath}")
                }
            }
        } catch (e: Exception) {
           Log.e("FileSearch", "Dizin listeleme hatası: ${directory.absolutePath}", e)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        fileLoadingJob?.cancel()
        fileCache.clear()
        allFilesCache.clear()
    }

    @SuppressLint("Range")
    private fun getPath(context: Context, uri: Uri): String? {
        try {
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(uri, projection, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                    }
                } catch (e: Exception) {
                   Log.e("FileSearch", "URI yol çözümleme hatası", e)
                } finally {
                    cursor?.close()
                }
            }
        } catch (e: Exception) {
           Log.e("FileSearch", "getPath hatası", e)
        }
        return null
    }
}
