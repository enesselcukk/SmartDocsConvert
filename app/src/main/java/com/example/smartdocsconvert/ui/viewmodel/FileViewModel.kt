package com.example.smartdocsconvert.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdocsconvert.util.PdfUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<FileUiState>(FileUiState.Initial)
    val uiState: StateFlow<FileUiState> = _uiState

    fun onImagesSelected(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = FileUiState.Loading
                withContext(Dispatchers.IO) {
                    val outputFile = PdfUtil.createPdfFromImages(
                        context,
                        uris,
                        "converted_${System.currentTimeMillis()}.pdf"
                    )
                    _uiState.value = FileUiState.Success(outputFile)
                }
            } catch (e: Exception) {
                _uiState.value = FileUiState.Error("Conversion failed: ${e.message}")
            }
        }
    }
}

sealed class FileUiState {
    data object Initial : FileUiState()
    data object Loading : FileUiState()
    data class Success(val file: File) : FileUiState()
    data class Error(val message: String) : FileUiState()
} 