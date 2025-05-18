package com.example.smartdocsconvert.ui.viewmodel

import android.content.Context
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
        fileLoadingJob?.cancel()
        
        fileLoadingJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, lastError = null) }
                
                val files = forceRefreshFilesUseCase(context)
                
                _uiState.update { it.copy(files = files, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(lastError = e.message, isLoading = false) }
            }
        }
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
)
