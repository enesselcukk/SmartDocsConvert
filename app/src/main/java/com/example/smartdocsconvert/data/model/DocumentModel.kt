package com.example.smartdocsconvert.data.model

import com.example.smartdocsconvert.R
import java.io.File
import java.util.Date
import java.util.UUID


data class DocumentModel(
    val id: String,
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val createdAt: Date,
    val lastModified: Date = createdAt,
    val thumbnailPath: String? = null
) {
    companion object {
        fun fromFile(file: File): DocumentModel {
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
                thumbnailPath = null
            )
        }
    }

    fun getDocumentIcon(): Int {
        return when (type.lowercase()) {
            "pdf" -> R.drawable.ic_pdf
            "doc", "docx", "rtf", "odt" -> R.drawable.ic_doc
            "xls", "xlsx", "csv", "ods" -> R.drawable.ic_xls
            "ppt", "pptx", "pps", "odp" -> R.drawable.ic_ppt
            "txt", "md", "log", "json", "xml", "html", "htm", "css", "js" -> R.drawable.ic_txt
            "jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp", "gif", "svg", "ico" -> R.drawable.ic_image
            else -> R.drawable.ic_file
        }
    }
} 