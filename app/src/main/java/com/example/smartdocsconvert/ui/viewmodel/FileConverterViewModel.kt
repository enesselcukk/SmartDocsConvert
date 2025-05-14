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

    // UI durumunu tutan veri sınıfı
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

    // Desteklenen dosya türleri
    val supportedExtensions = listOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt")
    
    // Dosya yükleme işi için referans
    private var fileLoadingJob: Job? = null
    
    // Cache mekanizması
    private val fileCache = mutableMapOf<String, List<File>>()
    private val allFilesCache = mutableListOf<File>()

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
        // Önceki yükleme işi varsa iptal et
        fileLoadingJob?.cancel()
        
        // Eğer cache'de dosyalar varsa hemen göster
        if (allFilesCache.isNotEmpty()) {
            _uiState.update { it.copy(files = allFilesCache.toList(), isLoading = false) }
        }
        
        // Yeni yükleme işi başlat
        fileLoadingJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, lastError = null) }
                
                // Tüm desteklenen dosya türlerini yükle
                val allFiles = getAllSupportedFiles(context)
                
                // Cache güncelle
                allFilesCache.clear()
                allFilesCache.addAll(allFiles)
                
                // UI güncelle
                _uiState.update { it.copy(files = allFiles, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(lastError = e.message, isLoading = false) }
                android.util.Log.e("FileConverter", "Dosya yükleme hatası", e)
            }
        }
    }

    // Cache'i temizle ve dosyaları yeniden yükle 
    fun forceRefreshFiles(context: Context) {
        // Cache'i temizle
        allFilesCache.clear()
        fileCache.clear()
        
        // Log ile bildir
        android.util.Log.d("FileSearch", "Cache temizlendi, dosyalar yeniden yükleniyor...")
        
        // Yeni dosyaları yükle
        refreshFiles(context)
    }


    // Tüm desteklenen dosya türlerini yükle
    private suspend fun getAllSupportedFiles(context: Context): List<File> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<File>()
        val uniquePaths = HashSet<String>()
        
        // Logla başlangıç
        android.util.Log.d("FileSearch", "Dosya arama başlatılıyor...")
        
        // Tüm desteklenen uzantılar için dosyaları yükle
        for (extension in supportedExtensions) {
            // Önce cache kontrol et
            val cachedFiles = fileCache[extension]
            if (cachedFiles != null) {
                // Sadece benzersiz dosyaları ekle
                cachedFiles.forEach { file ->
                    if (uniquePaths.add(file.absolutePath)) {
                        allFiles.add(file)
                    }
                }
                android.util.Log.d("FileSearch", "$extension: cache'den ${cachedFiles.size} dosya alındı")
            } else {
                // Cache'de yoksa yükle 
                try {
                    val files = getFilesByExtension(context, extension)
                    fileCache[extension] = files
                    
                    // Sadece benzersiz dosyaları ekle
                    files.forEach { file ->
                        if (uniquePaths.add(file.absolutePath)) {
                            allFiles.add(file)
                        }
                    }
                    android.util.Log.d("FileSearch", "$extension: ${files.size} dosya bulundu")
                } catch (e: Exception) {
                    android.util.Log.e("FileSearch", "$extension dosyalarını yükleme hatası", e)
                }
            }
        }
        
        // Tarih sırasına göre sırala (en yeni en önce)
        allFiles.sortByDescending { it.lastModified() }
        
        // Toplam bulunan dosya sayısını logla
        android.util.Log.d("FileSearch", "Toplam ${allFiles.size} dosya bulundu")
        
        // Sonuçları limitle (performans için)
        val maxFiles = 1000
        if (allFiles.size > maxFiles) {
            android.util.Log.d("FileSearch", "Maksimum dosya limiti: $maxFiles, tümü: ${allFiles.size}")
            allFiles.subList(0, maxFiles)
        } else {
            allFiles
        }
    }
    
    // Belirli bir uzantıya sahip dosyaları bul
    private suspend fun getFilesByExtension(context: Context, extension: String): List<File> = withContext(Dispatchers.IO) {
        val files = mutableListOf<File>()
        val uniquePaths = HashSet<String>()

        android.util.Log.d("FileSearch", "$extension uzantılı dosyaları arama başlıyor")

        // MediaStore ile arama (API düzeyine göre uygun sorgu kullan)
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
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "txt" -> "text/plain"
                else -> null
            }

            // Daha geniş bir arama için sorguyu genişlet
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
            var mediaStoreFileCount = 0
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

                android.util.Log.d("FileSearch", "MediaStore sorgusu: ${cursor.count} sonuç döndürdü")

                while (cursor.moveToNext()) {
                    try {
                        val path = cursor.getString(pathColumn)
                        val name = cursor.getString(nameColumn)
                        
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
                                    android.util.Log.w("FileSearch", "Dosya okunamıyor: $path")
                                }
                            } else {
                                android.util.Log.w("FileSearch", "Dosya mevcut değil: $path")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FileSearch", "Dosya işleme hatası", e)
                    }
                }
            }
            android.util.Log.d("FileSearch", "MediaStore'dan $mediaStoreFileCount $extension dosyası eklendi")
        } catch (e: Exception) {
            android.util.Log.e("FileSearch", "MediaStore sorgu hatası", e)
        }

        // Android 10+ için ContentResolver kullanarak alternatif sorgu yapma
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
                    android.util.Log.d("FileSearch", "Android 10+ için ContentResolver sorgusu yapılıyor")
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
                        android.util.Log.d("FileSearch", "ContentResolver sorgusu: ${cursor.count} sonuç")
                        
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                        
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                                id
                            )
                            
                            // Dosyaya erişmeye çalış
                            try {
                                contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                                    // FileDescriptor'dan gerçek dosyaya erişmek için çözüm yolu
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
                                android.util.Log.e("FileSearch", "ContentResolver dosya erişim hatası: $name", e)
                            }
                        }
                    }
                    android.util.Log.d("FileSearch", "ContentResolver'dan $contentResolverFileCount $extension dosyası eklendi")
                }
            } catch (e: Exception) {
                android.util.Log.e("FileSearch", "ContentResolver sorgu hatası", e)
            }
        }

        // Özel dizinleri tara (izinler kontrol edilerek)
        val specialDirs = mutableListOf<File>().apply {
            // Daha genel hedef dizinler
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            
            // Daha fazla ortak dizin ekle
            add(Environment.getExternalStorageDirectory())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.getExternalFilesDirs(null).forEach { add(it) }
            }
            
            // Android 10+ için uyumlu dizinler
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(File(Environment.getExternalStorageDirectory(), "Download"))
                add(File(Environment.getExternalStorageDirectory(), "Documents"))
                // Diğer yaygın klasörler
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
                    // Özel dizinlerde daha derinden arama yap
                    searchInDirectory(directory, extension, files, uniquePaths, maxDepth = 3)
                    val afterCount = files.size
                    specialDirFileCount += (afterCount - beforeCount)
                    android.util.Log.d("FileSearch", "Dizin taraması: ${directory.absolutePath}, bulunan: ${afterCount - beforeCount}")
                }
            } catch (e: SecurityException) {
                android.util.Log.e("FileSearch", "Özel dizin erişim hatası: ${directory.absolutePath}", e)
            }
        }
        android.util.Log.d("FileSearch", "Özel dizinlerden $specialDirFileCount $extension dosyası eklendi")

        // Uygulamanın kendi dizinlerini de tara (her zaman erişilebilir)
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
                android.util.Log.d("FileSearch", "App dizini: ${directory.absolutePath}, bulunan: ${afterCount - beforeCount}")
            } catch (e: Exception) {
                android.util.Log.e("FileSearch", "Dizin erişim hatası: ${directory.absolutePath}", e)
            }
        }
        android.util.Log.d("FileSearch", "Uygulama dizinlerinden $appDirFileCount $extension dosyası eklendi")

        android.util.Log.d("FileSearch", "Toplam ${files.size} $extension dosyası bulundu")
        files
    }

    // Dizin içinde dosya arama (derinlik kontrolü ile)
    private fun searchInDirectory(
        directory: File,
        extension: String, 
        files: MutableList<File>, 
        uniquePaths: HashSet<String>,
        maxDepth: Int = 3,  // Derinliği artırıyoruz
        currentDepth: Int = 0
    ) {
        if (!directory.exists() || !directory.isDirectory || currentDepth > maxDepth) {
            return
        }

        try {
            directory.listFiles()?.forEach { file ->
                try {
                    when {
                        file.isFile && file.name.lowercase().endsWith(".$extension") -> {
                            val absolutePath = file.absolutePath
                            if (uniquePaths.add(absolutePath)) {
                                // Okuma iznini kontrol etmeden önce dosyayı ekle, ancak log tut
                                files.add(file)
                                if (!file.canRead()) {
                                    android.util.Log.w("FileSearch", "Dosya eklendi ama okuma izni yok: $absolutePath")
                                }
                            }
                        }
                        file.isDirectory && currentDepth < maxDepth -> {
                            // Alt dizine geçerken derinliği artır
                            searchInDirectory(file, extension, files, uniquePaths, maxDepth, currentDepth + 1)
                        }
                    }
                } catch (e: SecurityException) {
                    // Tek bir dosya hatasını atla ve devam et
                    android.util.Log.w("FileSearch", "Dosya erişim hatası: ${file.absolutePath}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FileSearch", "Dizin listeleme hatası: ${directory.absolutePath}", e)
        }
    }
    
    // ViewModel yok edildiğinde kaynakları temizle
    override fun onCleared() {
        super.onCleared()
        fileLoadingJob?.cancel()
        fileCache.clear()
        allFilesCache.clear()
    }

    // URI'den gerçek dosya yolunu çözme yardımcı metodu
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
                    android.util.Log.e("FileSearch", "URI yol çözümleme hatası", e)
                } finally {
                    cursor?.close()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FileSearch", "getPath hatası", e)
        }
        return null
    }
}