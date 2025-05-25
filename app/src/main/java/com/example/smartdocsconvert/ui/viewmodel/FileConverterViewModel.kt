package com.example.smartdocsconvert.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdocsconvert.domain.usecase.file.ForceRefreshFilesUseCase
import com.example.smartdocsconvert.domain.usecase.file.GetSupportedFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileConverterViewModel @Inject constructor(
    private val getSupportedFilesUseCase: GetSupportedFilesUseCase,
    private val forceRefreshFilesUseCase: ForceRefreshFilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileConverterUiState())
    val uiState: StateFlow<FileConverterUiState> = _uiState.asStateFlow()
    
    private var fileLoadingJob: Job? = null

    fun handleSelectedDocuments(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val files = uris.mapNotNull { uri ->
                    // Get the document file from URI
                    val fileName = getFileName(context, uri)
                    if (fileName != null) {
                        // Create a temporary file in the cache directory
                        val tempFile = File(context.cacheDir, fileName)
                        
                        // Copy the content to the temporary file
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    } else null
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        files = (currentState.files + files).distinct(),
                        isLoading = false,
                        lastError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    lastError = "Failed to process selected documents: ${e.message}"
                ) }
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    fun toggleFileSelection(file: File) {
        _uiState.update { currentState ->
            val newSelectedFiles = currentState.selectedFiles.toMutableSet()
            if (newSelectedFiles.contains(file)) {
                newSelectedFiles.remove(file)
            } else {
                newSelectedFiles.add(file)
            }
            currentState.copy(selectedFiles = newSelectedFiles)
        }
    }

    fun refreshFiles(context: Context) {
        fileLoadingJob?.cancel()
        
        fileLoadingJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, lastError = null) }
                
                val files = getSupportedFilesUseCase(context)
                
                _uiState.update { it.copy(files = files, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(lastError = e.message, isLoading = false) }
            }
        }
    }

    fun forceRefreshFiles(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Scan both external storage and cache directory
                val externalFiles = scanExternalStorage(context)
                val cacheFiles = context.cacheDir.listFiles()?.filter { 
                    it.isFile && isDocumentFile(it)
                } ?: emptyList()

                _uiState.update { it.copy(
                    files = (externalFiles + cacheFiles).distinct(),
                    isLoading = false,
                    lastError = null
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    lastError = "Failed to refresh files: ${e.message}"
                ) }
            }
        }
    }
    
    private fun scanExternalStorage(context: Context): List<File> {
        return try {
            val externalDirs = context.getExternalFilesDirs(null)
            externalDirs.filterNotNull().flatMap { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && isDocumentFile(it) }
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isDocumentFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in setOf(
            "pdf", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "txt", "rtf", "odt"
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        fileLoadingJob?.cancel()
    }
}

data class FileConverterUiState(
    val files: List<File> = emptyList(),
    val selectedFiles: Set<File> = emptySet(),
    val isLoading: Boolean = false,
    val lastError: String? = null
) {
    val hasFiles: Boolean get() = files.isNotEmpty()
}
