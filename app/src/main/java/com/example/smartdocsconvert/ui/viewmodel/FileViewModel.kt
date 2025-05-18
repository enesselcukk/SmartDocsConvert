package com.example.smartdocsconvert.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartdocsconvert.data.model.DocumentModel
import com.example.smartdocsconvert.domain.usecase.DocumentUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FileViewModel @Inject constructor(
    private val documentUseCases: DocumentUseCases
) : ViewModel() {

    private val _filePickerRequested = MutableStateFlow(false)
    val filePickerRequested: StateFlow<Boolean> = _filePickerRequested
    
    private val _galleryPickerRequested = MutableStateFlow(false)
    val galleryPickerRequested: StateFlow<Boolean> = _galleryPickerRequested

    private val _documentsList = MutableStateFlow<List<DocumentModel>>(emptyList())
    val documentsList: StateFlow<List<DocumentModel>> = _documentsList

    private val _currentSortType = MutableStateFlow(SortType.DATE)
    val currentSortType: StateFlow<SortType> = _currentSortType
    
    private val _currentViewType = MutableStateFlow(ViewType.LIST)
    val currentViewType: StateFlow<ViewType> = _currentViewType

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var rawDocuments: List<DocumentModel> = emptyList()

    init {
        loadRecentDocuments()
    }
    
    fun openFilePicker() {
        _filePickerRequested.value = true
    }
    
    fun openGalleryPicker() {
        _galleryPickerRequested.value = true
    }
    
    fun onFilePickerCompleted() {
        _filePickerRequested.value = false
    }
    
    fun onGalleryPickerCompleted() {
        _galleryPickerRequested.value = false
    }
    
    private fun loadRecentDocuments() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                rawDocuments = documentUseCases.getDocuments()
                sortDocuments()
            } catch (e: Exception) {
                Log.e("FileViewModel", "Belgeler yüklenirken hata oluştu", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshDocuments() {
        loadRecentDocuments()
    }

    fun changeSortType(sortType: SortType) {
        if (_currentSortType.value != sortType) {
            _currentSortType.value = sortType
            sortDocuments()
        }
    }

    fun changeViewType(viewType: ViewType) {
        _currentViewType.value = viewType
    }

    private fun sortDocuments(documents: List<DocumentModel> = rawDocuments) {
        val sorted = when (_currentSortType.value) {
            SortType.ALPHABETICAL -> documents.sortedBy { it.name.lowercase() }
            SortType.DATE -> documents.sortedByDescending { it.createdAt }
            SortType.SIZE -> documents.sortedByDescending { it.size }
        }

        val result = if (_currentSortType.value != SortType.ALPHABETICAL) {
            val pdfDocuments = sorted.filter { it.type.equals("PDF", ignoreCase = true) }
            val otherDocuments = sorted.filter { !it.type.equals("PDF", ignoreCase = true) }
            pdfDocuments + otherDocuments
        } else {
            sorted
        }

        _documentsList.value = result
    }
}
enum class SortType {
    ALPHABETICAL,
    DATE,
    SIZE
}

enum class ViewType {
    LIST,
    GRID
}
