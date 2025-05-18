package com.example.smartdocsconvert.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.smartdocsconvert.data.model.DocumentModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun findDownloadedDocuments(): List<DocumentModel> = withContext(Dispatchers.IO) {
        val documentsMap = mutableMapOf<String, DocumentModel>() // Tekrarlanan dosyaları önlemek için

        val downloadDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(null),
            File(context.filesDir, "downloads"),
            File(context.filesDir, "documents")
        )

        val supportedExtensions = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "jpg", "jpeg", "png")

        for (dir in downloadDirs) {
            if (dir?.exists() == true && dir.isDirectory) {
                try {
                    Log.d("DocumentRepository", "Klasör taranıyor: ${dir.absolutePath}")
                    val files = dir.listFiles() ?: continue
                    
                    processFiles(files, supportedExtensions, documentsMap)
                } catch (e: Exception) {
                    Log.e("DocumentRepository", "Klasör okunurken hata: ${dir.absolutePath}", e)
                }
            } else {
                Log.d("DocumentRepository", "Klasör mevcut değil veya boş: ${dir?.absolutePath}")
            }
        }

        val sortedDocuments = documentsMap.values.toList().sortedByDescending { it.createdAt }

        val pdfDocuments = sortedDocuments.filter { it.type.equals("PDF", ignoreCase = true) }
        val otherDocuments = sortedDocuments.filter { !it.type.equals("PDF", ignoreCase = true) }

        return@withContext (pdfDocuments + otherDocuments).take(30)
    }

    private fun processFiles(files: Array<File>, supportedExtensions: List<String>, documentsMap: MutableMap<String, DocumentModel>) {
        for (file in files) {
            if (file.isFile) {
                val extension = file.extension.lowercase()
                if (supportedExtensions.contains(extension)) {
                    try {
                        val document = DocumentModel.fromFile(file)
                        documentsMap[document.id] = document
                        Log.d("DocumentRepository", "Belge bulundu: ${file.name}, türü: $extension")
                    } catch (e: Exception) {
                        Log.e("DocumentRepository", "Belge işlenirken hata: ${file.name}", e)
                    }
                }
            } else if (file.isDirectory) {
                val subFiles = file.listFiles()
                if (subFiles != null) {
                    for (subFile in subFiles) {
                        if (subFile.isFile) {
                            val extension = subFile.extension.lowercase()
                            if (supportedExtensions.contains(extension)) {
                                try {
                                    val document = DocumentModel.fromFile(subFile)
                                    documentsMap[document.id] = document
                                    Log.d("DocumentRepository", "Alt klasörde belge bulundu: ${subFile.name}, türü: $extension")
                                } catch (e: Exception) {
                                    Log.e("DocumentRepository", "Alt klasörde belge işlenirken hata: ${subFile.name}", e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
} 