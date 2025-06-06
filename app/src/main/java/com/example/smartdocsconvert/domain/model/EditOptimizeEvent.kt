package com.example.smartdocsconvert.domain.model

sealed interface EditOptimizeEvent {
    data class SelectFile(val index: Int) : EditOptimizeEvent
    data class UpdateQualityLevel(val level: Int) : EditOptimizeEvent
    data class UpdateCompression(val enabled: Boolean) : EditOptimizeEvent
    data class UpdateFileName(val name: String) : EditOptimizeEvent
    
    sealed interface Dialog : EditOptimizeEvent {
        data object Show : Dialog
        data object Hide : Dialog
        data object Confirm : Dialog
    }

} 