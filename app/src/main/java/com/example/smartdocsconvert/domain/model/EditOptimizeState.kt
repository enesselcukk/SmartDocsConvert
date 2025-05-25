package com.example.smartdocsconvert.domain.model

import java.io.File

data class EditOptimizeState(
    val files: List<File> = emptyList(),
    val currentFileIndex: Int = 0,
    val qualityLevel: Int = 80,
    val compressionEnabled: Boolean = true,
    val dialog: Dialog = Dialog(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    data class Dialog(
        val isVisible: Boolean = false,
        val fileName: String = ""
    )
    
    val currentFile: File?
        get() = files.getOrNull(currentFileIndex)
        
    val hasFiles: Boolean
        get() = files.isNotEmpty()
        
    val hasMultipleFiles: Boolean
        get() = files.size > 1
} 