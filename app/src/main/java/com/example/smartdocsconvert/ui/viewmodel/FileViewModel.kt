package com.example.smartdocsconvert.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FileViewModel @Inject constructor() : ViewModel() {

    // Picker event state flows
    private val _filePickerRequested = MutableStateFlow(false)
    val filePickerRequested: StateFlow<Boolean> = _filePickerRequested
    
    private val _galleryPickerRequested = MutableStateFlow(false)
    val galleryPickerRequested: StateFlow<Boolean> = _galleryPickerRequested

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

}