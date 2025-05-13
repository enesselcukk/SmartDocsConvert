package com.example.smartdocsconvert.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import android.app.Application
import com.example.smartdocsconvert.data.repository.ImageRepository
import com.example.smartdocsconvert.ui.components.ShapeItem

@HiltViewModel
class ImageFilterViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageFilterState())
    val uiState: StateFlow<ImageFilterState> = _uiState.asStateFlow()

    var tempCameraUri: Uri? = null

    fun initializeWithImages(imageUris: List<Uri>) {
        // Don't proceed if the list is empty
        if (imageUris.isEmpty()) {
            _uiState.update { currentState ->
                currentState.copy(
                    processedImageUris = emptyList(),
                    rotationAngles = emptyList(),
                    brightnessValues = emptyList(),
                    contrastValues = emptyList(),
                    selectedFilters = emptyList(),
                    filterIntensityValues = emptyList(),
                    currentImageIndex = 0
                )
            }
            return
        }
        
        _uiState.update { currentState ->
            currentState.copy(
                processedImageUris = imageUris,
                rotationAngles = List(imageUris.size) { 0f },
                brightnessValues = List(imageUris.size) { 1f },
                contrastValues = List(imageUris.size) { 1f },
                selectedFilters = List(imageUris.size) { "Original" },
                filterIntensityValues = List(imageUris.size) { 0f },
                currentImageIndex = 0  // Explicitly set to 0 to avoid index issues
            )
        }
    }

    fun onImageSelected(index: Int) {
        val currentState = _uiState.value
        
        // Safety check - make sure index is valid
        if (index < 0 || index >= currentState.processedImageUris.size) {
            return
        }
        
        _uiState.update {
            it.copy(
                activeFeature = null,
                previousImageIndex = currentState.currentImageIndex,
                currentImageIndex = index
            )
        }
    }

    // Image feature control functions
    fun setBrightness(value: Float) {
        val currentState = _uiState.value
        
        // Safety check - make sure we have images and a valid index
        if (currentState.processedImageUris.isEmpty() || 
            currentState.currentImageIndex >= currentState.brightnessValues.size) {
            return
        }
        
        val newValues = currentState.brightnessValues.toMutableList()
        newValues[currentState.currentImageIndex] = value
        _uiState.update {
            it.copy(brightnessValues = newValues)
        }
    }

    fun setContrast(value: Float) {
        val currentState = _uiState.value
        
        // Safety check - make sure we have images and a valid index
        if (currentState.processedImageUris.isEmpty() || 
            currentState.currentImageIndex >= currentState.contrastValues.size) {
            return
        }
        
        val newValues = currentState.contrastValues.toMutableList()
        newValues[currentState.currentImageIndex] = value
        _uiState.update {
            it.copy(contrastValues = newValues)
        }
    }

    fun setFilter(value: String) {
        val currentState = _uiState.value
        
        // Safety check - make sure we have images and a valid index
        if (currentState.processedImageUris.isEmpty() || 
            currentState.currentImageIndex >= currentState.selectedFilters.size) {
            return
        }
        
        val newValues = currentState.selectedFilters.toMutableList()
        newValues[currentState.currentImageIndex] = value
        _uiState.update {
            it.copy(selectedFilters = newValues)
        }
    }

    fun setFilterIntensity(value: Float) {
        val currentState = _uiState.value
        
        // Safety check - make sure we have images and a valid index
        if (currentState.processedImageUris.isEmpty() || 
            currentState.currentImageIndex >= currentState.filterIntensityValues.size) {
            return
        }
        
        val newValues = currentState.filterIntensityValues.toMutableList()
        newValues[currentState.currentImageIndex] = value
        _uiState.update {
            it.copy(filterIntensityValues = newValues)
        }
    }

    fun resetImageAdjustments() {
        val currentState = _uiState.value
        val index = currentState.currentImageIndex
        
        // Safety check - make sure we have images and a valid index
        if (currentState.processedImageUris.isEmpty() || 
            index >= currentState.brightnessValues.size ||
            index >= currentState.contrastValues.size ||
            index >= currentState.selectedFilters.size ||
            index >= currentState.filterIntensityValues.size) {
            return
        }
        
        val newBrightnessValues = currentState.brightnessValues.toMutableList().also { it[index] = 1f }
        val newContrastValues = currentState.contrastValues.toMutableList().also { it[index] = 1f }
        val newSelectedFilters = currentState.selectedFilters.toMutableList().also { it[index] = "Original" }
        val newFilterIntensityValues = currentState.filterIntensityValues.toMutableList().also { it[index] = 0f }
        
        _uiState.update {
            it.copy(
                brightnessValues = newBrightnessValues,
                contrastValues = newContrastValues,
                selectedFilters = newSelectedFilters,
                filterIntensityValues = newFilterIntensityValues
            )
        }
    }

    fun rotateImage() {
        val currentState = _uiState.value
        
        // Safety check - make sure we have images and a valid index
        if (currentState.processedImageUris.isEmpty() || 
            currentState.currentImageIndex >= currentState.rotationAngles.size) {
            return
        }
        
        val newRotationAngles = currentState.rotationAngles.toMutableList()
        newRotationAngles[currentState.currentImageIndex] = 
            (newRotationAngles[currentState.currentImageIndex] + 90f) % 360f
        
        _uiState.update {
            it.copy(rotationAngles = newRotationAngles)
        }
    }

    fun updateActiveFeature(feature: String?) {
        _uiState.update {
            it.copy(activeFeature = feature)
        }
    }

    fun updateTransformState(newScale: Float, newOffsetX: Float, newOffsetY: Float) {
        _uiState.update {
            it.copy(
                scale = newScale.coerceIn(0.5f, 3f),
                offsetX = newOffsetX,
                offsetY = newOffsetY
            )
        }
    }

    fun updateRotationY(value: Float) {
        _uiState.update {
            it.copy(rotationY = value)
        }
    }

    fun updateCropRect(rect: Rect) {
        _uiState.update {
            it.copy(cropRect = rect)
        }
    }

    fun applyAndSaveCrop() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Safety check - make sure we have valid data
            if (currentState.processedImageUris.isEmpty() || 
                currentState.currentImageIndex >= currentState.processedImageUris.size) {
                _uiState.update { 
                    it.copy(toastMessage = "No image available to crop") 
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Make sure we have valid image dimensions
                if (currentState.imageSize.width <= 0 || currentState.imageSize.height <= 0) {
                    _uiState.update {
                        it.copy(
                            toastMessage = "Invalid image dimensions for cropping",
                            isLoading = false
                        )
                    }
                    return@launch
                }
                
                // This is the actual bitmap width/height
                val width = currentState.imageSize.width.coerceAtLeast(1) // Ensure non-zero width
                val height = currentState.imageSize.height.coerceAtLeast(1) // Ensure non-zero height
                
                // Ensure crop rect values are within valid range (0.0-1.0)
                val safeRect = Rect(
                    left = currentState.cropRect.left.coerceIn(0f, 1f),
                    top = currentState.cropRect.top.coerceIn(0f, 1f),
                    right = currentState.cropRect.right.coerceIn(0f, 1f),
                    bottom = currentState.cropRect.bottom.coerceIn(0f, 1f)
                )
                
                // Ensure the rectangle has width and height
                if (safeRect.width <= 0.05f || safeRect.height <= 0.05f) {
                    _uiState.update {
                        it.copy(
                            toastMessage = "Cropping area too small",
                            isLoading = false
                        )
                    }
                    return@launch
                }

                // Log normalized crop rect
                android.util.Log.d("ImageFilterViewModel", "Normalized crop rect: $safeRect")
                
                // Calculate pixel coordinates for cropping - directly map normalized coordinates to actual pixel values
                val cropX = (safeRect.left * width).toInt().coerceAtLeast(0)
                val cropY = (safeRect.top * height).toInt().coerceAtLeast(0)
                val cropWidth = ((safeRect.right - safeRect.left) * width).toInt()
                    .coerceIn(1, width - cropX) // Ensure positive width within bounds
                val cropHeight = ((safeRect.bottom - safeRect.top) * height).toInt()
                    .coerceIn(1, height - cropY) // Ensure positive height within bounds
                
                // Check if the crop is a portrait (vertical) crop
                val isPortraitCrop = cropHeight > cropWidth
                
                // Log detailed crop information for debugging
                val logMessage = "Cropping: X=$cropX, Y=$cropY, Width=$cropWidth, Height=$cropHeight, " +
                    "Bitmap Dimensions=${width}x${height}, IsPortrait=$isPortraitCrop, " +
                    "CropRect=(${safeRect.left}, ${safeRect.top}, ${safeRect.right}, ${safeRect.bottom})"
                android.util.Log.d("ImageFilterViewModel", logMessage)
                
                // Perform the crop operation
                val sourceUri = currentState.processedImageUris[currentState.currentImageIndex]
                val croppedUri = imageRepository.saveCroppedImage(
                    sourceUri,
                    cropX,
                    cropY,
                    cropWidth,
                    cropHeight,
                    currentState.currentImageIndex
                )
                
                // Handle the result
                if (croppedUri != null) {
                    android.util.Log.d("ImageFilterViewModel", "Successfully got cropped URI: $croppedUri")
                    
                    // Success - update the UI with the cropped image
                    val newCroppedUris = currentState.croppedImageUris.toMutableList()
                    while (newCroppedUris.size <= currentState.currentImageIndex) {
                        newCroppedUris.add(null)
                    }
                    
                    newCroppedUris[currentState.currentImageIndex] = croppedUri
                    
                    // Update the processed image URIs list to show the cropped image immediately
                    val newProcessedUris = currentState.processedImageUris.toMutableList()
                    newProcessedUris[currentState.currentImageIndex] = croppedUri
                    
                    _uiState.update { state ->
                        state.copy(
                            processedImageUris = newProcessedUris,
                            croppedImageUris = newCroppedUris,
                            isCropped = true,
                            cropRect = Rect(0f, 0f, 1f, 1f), // Reset crop rect
                            activeFeature = null // Exit crop mode
                        )
                    }
                    
                    // Show a success message
                    _uiState.update {
                        it.copy(
                            toastMessage = "Image cropped successfully",
                            isLoading = false
                        )
                    }
                } else {
                    // If cropping failed, show an error
                    android.util.Log.e("ImageFilterViewModel", "Failed to get cropped URI")
                    _uiState.update { 
                        it.copy(
                            toastMessage = "Failed to crop the image",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ImageFilterViewModel", "Error during crop: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        toastMessage = "Kırpma işlemi sırasında hata oluştu: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveProcessedImage() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Safety check - make sure we have valid data
            if (currentState.processedImageUris.isEmpty() || 
                currentState.currentImageIndex >= currentState.processedImageUris.size) {
                _uiState.update { 
                    it.copy(toastMessage = "No image available to save") 
                }
                return@launch
            }
            
            _uiState.update { it.copy(isSaving = true) }
            
            try {
                // Determine the appropriate source URI
                val sourceUri = if (currentState.currentImageIndex < currentState.croppedImageUris.size && 
                                    currentState.croppedImageUris[currentState.currentImageIndex] != null) {
                    currentState.croppedImageUris[currentState.currentImageIndex]!!
                } else {
                    currentState.processedImageUris[currentState.currentImageIndex]
                }
                
                // Use safe accessors for state values
                val selectedFilter = currentState.selectedFilters.getOrNull(currentState.currentImageIndex) ?: "Original"
                val brightness = currentState.brightnessValues.getOrNull(currentState.currentImageIndex) ?: 1f
                val contrast = currentState.contrastValues.getOrNull(currentState.currentImageIndex) ?: 1f
                val filterIntensity = currentState.filterIntensityValues.getOrNull(currentState.currentImageIndex) ?: 0f
                val rotationAngle = currentState.rotationAngles.getOrNull(currentState.currentImageIndex) ?: 0f
                
                // Repository'yi kullanarak görüntüyü kaydet
                val resultMessage = imageRepository.saveProcessedImage(
                    sourceUri,
                    selectedFilter,
                    brightness,
                    contrast,
                    filterIntensity,
                    rotationAngle
                )
                
                _uiState.update {
                    it.copy(toastMessage = resultMessage)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(toastMessage = "Görüntü kaydedilirken hata oluştu: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun autoEnhanceImage() {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val bitmap = imageRepository.loadBitmap(currentState.processedImageUris[currentState.currentImageIndex])

                bitmap?.let { originalBitmap ->
                    var totalBrightness = 0L
                    var pixelCount = 0
                    val sampleStep = 4

                    for (x in 0 until originalBitmap.width step sampleStep) {
                        for (y in 0 until originalBitmap.height step sampleStep) {
                            val pixel = originalBitmap.getPixel(x, y)
                            val red = android.graphics.Color.red(pixel)
                            val green = android.graphics.Color.green(pixel)
                            val blue = android.graphics.Color.blue(pixel)
                            totalBrightness += (red + green + blue) / 3
                            pixelCount++
                        }
                    }

                    val averageBrightness = totalBrightness.toFloat() / (pixelCount * 255f)

                    // Adjust brightness based on the average
                    val newBrightness = when {
                        averageBrightness < 0.35f -> 1.4f
                        averageBrightness < 0.45f -> 1.3f
                        averageBrightness > 0.65f -> 0.8f
                        averageBrightness > 0.55f -> 0.9f
                        else -> 1.1f
                    }
                    setBrightness(newBrightness)

                    // Adjust contrast based on the average
                    val newContrast = when {
                        averageBrightness < 0.35f -> 1.3f
                        averageBrightness < 0.45f -> 1.2f
                        averageBrightness > 0.65f -> 1.15f
                        averageBrightness > 0.55f -> 1.1f
                        else -> 1.15f
                    }
                    setContrast(newContrast)

                    // Apply appropriate filter based on the average
                    val newFilter = when {
                        averageBrightness < 0.4f -> "Clarendon"
                        averageBrightness > 0.6f -> "Lark"
                        else -> "Reyes"
                    }
                    setFilter(newFilter)
                    setFilterIntensity(0.4f)

                    originalBitmap.recycle()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(toastMessage = "Otomatik iyileştirme sırasında hata oluştu: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun createTempCameraUri(context: Context): Uri? {
        return try {
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val storageDir = java.io.File(context.externalCacheDir, "camera_photos")
            if (!storageDir.exists()) storageDir.mkdirs()
            
            val photoFile = java.io.File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            
            tempCameraUri = uri
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Add a shape to the current image
     */
    fun addShape(shape: ShapeItem) {
        val currentState = _uiState.value
        val currentIndex = currentState.currentImageIndex
        
        // Safety check - ensure valid index
        if (currentState.processedImageUris.isEmpty() || 
            currentIndex >= currentState.processedImageUris.size) {
            return
        }
        
        // Get current shapes for this image or empty list if none exist
        val currentShapes = currentState.shapeItems[currentIndex] ?: emptyList()
        
        // Create a new map with updated shapes for this image
        val newShapesMap = currentState.shapeItems.toMutableMap()
        newShapesMap[currentIndex] = currentShapes + shape
        
        _uiState.update { state ->
            state.copy(shapeItems = newShapesMap)
        }
        
        android.util.Log.d("ImageFilterViewModel", "Added shape to image $currentIndex, total shapes: ${newShapesMap[currentIndex]?.size}")
    }

    /**
     * Update a shape at the specified index for the current image
     */
    fun updateShape(shapeIndex: Int, updatedShape: ShapeItem) {
        val currentState = _uiState.value
        val currentImageIndex = currentState.currentImageIndex
        
        // Get current shapes for this image
        val currentShapes = currentState.shapeItems[currentImageIndex] ?: return
        
        // Make sure index is valid
        if (shapeIndex < 0 || shapeIndex >= currentShapes.size) {
            return
        }
        
        // Create updated list
        val updatedShapes = currentShapes.toMutableList()
        updatedShapes[shapeIndex] = updatedShape
        
        // Update the state
        val newShapesMap = currentState.shapeItems.toMutableMap()
        newShapesMap[currentImageIndex] = updatedShapes
        
        _uiState.update { state ->
            state.copy(shapeItems = newShapesMap)
        }
    }

    /**
     * Remove a shape at the specified index from the current image
     */
    fun removeShape(shapeIndex: Int) {
        val currentState = _uiState.value
        val currentImageIndex = currentState.currentImageIndex
        
        // Get current shapes for this image
        val currentShapes = currentState.shapeItems[currentImageIndex] ?: return
        
        // Make sure index is valid
        if (shapeIndex < 0 || shapeIndex >= currentShapes.size) {
            return
        }
        
        // Create updated list without the removed shape
        val updatedShapes = currentShapes.toMutableList()
        updatedShapes.removeAt(shapeIndex)
        
        // Update the state
        val newShapesMap = currentState.shapeItems.toMutableMap()
        newShapesMap[currentImageIndex] = updatedShapes
        
        _uiState.update { state ->
            state.copy(shapeItems = newShapesMap)
        }
    }

    /**
     * Clear all shapes for the current image
     */
    fun clearShapes() {
        val currentState = _uiState.value
        val currentImageIndex = currentState.currentImageIndex
        
        // Remove shapes for this image
        val newShapesMap = currentState.shapeItems.toMutableMap()
        newShapesMap.remove(currentImageIndex)
        
        _uiState.update { state ->
            state.copy(shapeItems = newShapesMap)
        }
    }

    /**
     * Get shapes for the current image
     */
    fun getShapesForCurrentImage(): List<ShapeItem> {
        val currentState = _uiState.value
        return currentState.shapeItems[currentState.currentImageIndex] ?: emptyList()
    }
} 