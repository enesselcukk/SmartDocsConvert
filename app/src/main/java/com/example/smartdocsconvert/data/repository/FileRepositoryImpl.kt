package com.example.smartdocsconvert.data.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.smartdocsconvert.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class FileRepositoryImpl @Inject constructor() : FileRepository {
    
    private val supportedExtensions = listOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt")
    private val fileCache = mutableMapOf<String, List<File>>()
    private val allFilesCache = mutableListOf<File>()
    
    override suspend fun getAllSupportedFiles(context: Context): List<File> {
        if (allFilesCache.isNotEmpty()) {
            return allFilesCache.toList()
        }
        
        return refreshFilesInternal(context)
    }
    
    override suspend fun forceRefreshFiles(context: Context): List<File> {
        fileCache.clear()
        allFilesCache.clear()
        return refreshFilesInternal(context)
    }

    override suspend fun renameFile(file: File, newName: String): Result<File> = runCatching {
        val parentPath = file.parentFile?.absolutePath ?: throw IllegalStateException("Parent path not found")
        val newFile = File("$parentPath/$newName")

        if (file.renameTo(newFile)) {
            newFile
        } else {
            throw IllegalStateException("Failed to rename file")
        }
    }

    override suspend fun optimizeFile(file: File, quality: Int, compress: Boolean): Result<File> = runCatching {
        file
    }

    private suspend fun refreshFilesInternal(context: Context): List<File> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<File>()
        val uniquePaths = HashSet<String>()
        
        for (extension in supportedExtensions) {
            val cachedFiles = fileCache[extension]
            if (cachedFiles != null) {
                cachedFiles.forEach { file ->
                    if (uniquePaths.add(file.absolutePath)) {
                        allFiles.add(file)
                    }
                }
            } else {
                try {
                    val files = getFilesByExtension(context, extension)
                    fileCache[extension] = files
                    
                    files.forEach { file ->
                        if (uniquePaths.add(file.absolutePath)) {
                            allFiles.add(file)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FileRepository", "Error loading $extension files", e)
                }
            }
        }
        
        allFiles.sortByDescending { it.lastModified() }

        val maxFiles = 1000
        val resultFiles = if (allFiles.size > maxFiles) {
            allFiles.subList(0, maxFiles)
        } else {
            allFiles
        }
        
        allFilesCache.clear()
        allFilesCache.addAll(resultFiles)
        
        resultFiles
    }
    
    private suspend fun getFilesByExtension(context: Context, extension: String): List<File> = withContext(Dispatchers.IO) {
        val files = mutableListOf<File>()
        val uniquePaths = HashSet<String>()

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
                                    Log.w("FileRepository", "File not readable: $path")
                                }
                            } else {
                                Log.w("FileRepository", "File doesn't exist: $path")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FileRepository", "Error processing file", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "MediaStore query error", e)
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
                                Log.e("FileRepository", "ContentResolver file access error: $name", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FileRepository", "ContentResolver query error", e)
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
                }
            } catch (e: SecurityException) {
                Log.e("FileRepository", "Special directory access error: ${directory.absolutePath}", e)
            }
        }

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
            } catch (e: Exception) {
                Log.e("FileRepository", "Directory access error: ${directory.absolutePath}", e)
            }
        }
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
                                if (file.canRead().not()) {
                                    Log.w("FileRepository", "File added but not readable: $absolutePath")
                                }
                            }
                        }
                        file.isDirectory && currentDepth < maxDepth -> {
                            searchInDirectory(file, extension, files, uniquePaths, maxDepth, currentDepth + 1)
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w("FileRepository", "File access error: ${file.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Directory listing error: ${directory.absolutePath}", e)
        }
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
                    Log.e("FileRepository", "URI path resolution error", e)
                } finally {
                    cursor?.close()
                }
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "getPath error", e)
        }
        return null
    }

} 