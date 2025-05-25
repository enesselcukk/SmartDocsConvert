package com.example.smartdocsconvert.data.model

import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.example.smartdocsconvert.ui.components.ShapeItem


data class ImageFilterState(
    val currentImageIndex: Int = 0,
    val previousImageIndex: Int = 0,
    val activeFeature: String? = null,

    val processedImageUris: List<Uri> = emptyList(),
    val croppedImageUris: List<Uri?> = emptyList(),

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isImageLoading: Boolean = false,
    val isCropped: Boolean = false,
    val isDownloading: Boolean = false,
    val showDownloadAnimation: Boolean = false,
    val showDownloadOptions: Boolean = false,
    val showDownloadConfirmation: Boolean = false,

    val pendingDownloadFilename: String? = null,
    val userEnteredFilename: String = "",
    val pendingDownloadType: String? = null,
    val pendingDownloadUri: Uri? = null,
    val pendingDownloadFilter: String? = null,
    val pendingDownloadBrightness: Float = 1f,
    val pendingDownloadContrast: Float = 1f,
    val pendingDownloadRotation: Float = 0f,
    val downloadAllImages: Boolean = false,

    val rotationAngles: List<Float> = emptyList(),
    val brightnessValues: List<Float> = emptyList(),
    val contrastValues: List<Float> = emptyList(),
    val selectedFilters: List<String> = emptyList(),
    val filterIntensityValues: List<Float> = emptyList(),

    val rotationY: Float = 0f,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val cropRect: Rect = Rect(0f, 0f, 1f, 1f),
    var imageSize: IntSize = IntSize(0, 0),

    val shapeItems: Map<Int, List<ShapeItem>> = emptyMap(),

    val toastMessage: String? = null,

    val navigateBack: Boolean = false
) 