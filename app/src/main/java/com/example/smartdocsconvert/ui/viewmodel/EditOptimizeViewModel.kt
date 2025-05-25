package com.example.smartdocsconvert.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdocsconvert.domain.model.EditOptimizeEvent
import com.example.smartdocsconvert.domain.model.EditOptimizeState
import com.example.smartdocsconvert.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditOptimizeViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditOptimizeState())
    val state: StateFlow<EditOptimizeState> = _state.asStateFlow()

    fun initializeFiles(files: List<File>) {
        _state.update { currentState ->
            currentState.copy(
                files = files,
                dialog = currentState.dialog.copy(
                    fileName = files.firstOrNull()?.name ?: ""
                )
            )
        }
    }

    fun onEvent(event: EditOptimizeEvent) {
        when (event) {
            is EditOptimizeEvent.SelectFile -> {
                _state.update { currentState -> 
                    currentState.copy(
                        currentFileIndex = event.index,
                        dialog = currentState.dialog.copy(
                            fileName = currentState.files.getOrNull(event.index)?.name ?: ""
                        )
                    )
                }
            }
            
            is EditOptimizeEvent.UpdateQualityLevel -> {
                _state.update { it.copy(qualityLevel = event.level) }
            }
            
            is EditOptimizeEvent.UpdateCompression -> {
                _state.update { it.copy(compressionEnabled = event.enabled) }
            }
            
            is EditOptimizeEvent.UpdateFileName -> {
                _state.update { currentState ->
                    currentState.copy(
                        dialog = currentState.dialog.copy(fileName = event.name)
                    )
                }
            }
            
            EditOptimizeEvent.Dialog.Show -> {
                _state.update { currentState ->
                    currentState.copy(
                        dialog = currentState.dialog.copy(
                            isVisible = true,
                            fileName = currentState.currentFile?.name ?: ""
                        )
                    )
                }
            }
            
            EditOptimizeEvent.Dialog.Hide -> {
                _state.update { currentState ->
                    currentState.copy(
                        dialog = currentState.dialog.copy(isVisible = false)
                    )
                }
            }
            
            EditOptimizeEvent.Dialog.Confirm -> {
                renameCurrentFile()
            }
        }
    }

    private fun renameCurrentFile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val currentState = _state.value
                val currentFile = currentState.currentFile ?: return@launch
                
                fileRepository.renameFile(currentFile, currentState.dialog.fileName)
                    .onSuccess { newFile ->
                        val updatedFiles = currentState.files.toMutableList()
                        updatedFiles[currentState.currentFileIndex] = newFile
                        
                        _state.update { 
                            it.copy(
                                files = updatedFiles,
                                dialog = it.dialog.copy(isVisible = false),
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update { 
                            it.copy(
                                error = "Dosya adı değiştirilemedi: ${error.message}",
                                dialog = it.dialog.copy(isVisible = false),
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Beklenmeyen bir hata oluştu: ${e.message}",
                        dialog = it.dialog.copy(isVisible = false),
                        isLoading = false
                    )
                }
            }
        }
    }
} 