package com.example.smartdocsconvert.data.model

import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.example.smartdocsconvert.ui.components.ShapeItem

/**
 * State holder class for the ImageFilter feature
 */
data class ImageFilterState(
    // Navigation and selection state
    val currentImageIndex: Int = 0,
    val previousImageIndex: Int = 0,
    val activeFeature: String? = null,
    
    // Image collections
    val processedImageUris: List<Uri> = emptyList(),
    val croppedImageUris: List<Uri?> = emptyList(),
    
    // Processing state flags
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isImageLoading: Boolean = false,
    val isCropped: Boolean = false,
    val isDownloading: Boolean = false,
    val showDownloadAnimation: Boolean = false,
    val showDownloadOptions: Boolean = false,
    val showDownloadConfirmation: Boolean = false,
    
    // Download details
    val pendingDownloadFilename: String? = null,
    val userEnteredFilename: String = "",
    val pendingDownloadType: String? = null,
    val pendingDownloadUri: Uri? = null,
    val pendingDownloadFilter: String? = null,
    val pendingDownloadBrightness: Float = 1f,
    val pendingDownloadContrast: Float = 1f,
    val pendingDownloadRotation: Float = 0f,
    val downloadAllImages: Boolean = false,
    
    // Image edits and transformations
    val rotationAngles: List<Float> = emptyList(),
    val brightnessValues: List<Float> = emptyList(),
    val contrastValues: List<Float> = emptyList(),
    val selectedFilters: List<String> = emptyList(),
    val filterIntensityValues: List<Float> = emptyList(),
    
    // Transform state
    val rotationY: Float = 0f,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    
    // Crop state
    val cropRect: Rect = Rect(0f, 0f, 1f, 1f),
    var imageSize: IntSize = IntSize(0, 0),
    
    // Shapes state (shape items for each image)
    val shapeItems: Map<Int, List<ShapeItem>> = emptyMap(),
    
    // Messaging
    val toastMessage: String? = null,

    val navigateBack: Boolean = false  // New flag to trigger navigation back
) 