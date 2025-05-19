package com.example.smartdocsconvert.data.model

import com.example.smartdocsconvert.R
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * Model representing a converted document
 */
data class DocumentModel(
    val id: String,
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val createdAt: Date,
    val lastModified: Date = createdAt, // Son değiştirilme tarihi, varsayılan olarak oluşturma tarihi
    val thumbnailPath: String? = null
) {
    companion object {
        // Factory method to create from a File
        fun fromFile(file: File): DocumentModel {
            // Dosya yolu ve son değişiklik zamanına göre benzersiz bir ID oluştur
            val uniqueId = UUID.nameUUIDFromBytes(
                (file.absolutePath + file.lastModified()).toByteArray()
            ).toString()
            
            return DocumentModel(
                id = uniqueId,
                name = file.nameWithoutExtension,
                path = file.absolutePath,
                type = file.extension.uppercase(),
                size = file.length(),
                createdAt = Date(file.lastModified()),
                thumbnailPath = null // This would be set separately after generating a thumbnail
            )
        }
    }
    
    // Döküman türüne göre ikon kaynağını döndür
    fun getDocumentIcon(): Int {
        return when (type.lowercase()) {
            // Doküman formatları
            "pdf" -> R.drawable.ic_pdf
            "doc", "docx", "rtf", "odt" -> R.drawable.ic_doc
            "xls", "xlsx", "csv", "ods" -> R.drawable.ic_xls
            "ppt", "pptx", "pps", "odp" -> R.drawable.ic_ppt
            "txt", "md", "log", "json", "xml", "html", "htm", "css", "js" -> R.drawable.ic_txt
            
            // Görüntü formatları
            "jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp", "gif", "svg", "ico" -> R.drawable.ic_image
            
            // Diğer tüm formatlar
            else -> R.drawable.ic_file
        }
    }
} 