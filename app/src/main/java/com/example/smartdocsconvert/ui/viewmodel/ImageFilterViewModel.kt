package com.example.smartdocsconvert.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.geometry.Rect
import androidx.core.content.FileProvider
import com.example.smartdocsconvert.data.repository.ImageRepository
import com.example.smartdocsconvert.ui.components.ShapeItem
import kotlinx.coroutines.delay
import androidx.work.WorkManager
import com.example.smartdocsconvert.worker.DownloadWorker
import androidx.work.WorkInfo
import androidx.lifecycle.Observer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import androidx.work.OneTimeWorkRequest

@HiltViewModel
class ImageFilterViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    @ApplicationContext private val applicationContext: Context
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
                
                // Repository'yi kullanarak görüntüyü kaydet
                val resultMessage = imageRepository.saveProcessedImage(
                    sourceUri,
                    selectedFilter,
                    brightness,
                    contrast,
                    filterIntensity
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
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(context.externalCacheDir, "camera_photos")
            if (!storageDir.exists()) storageDir.mkdirs()
            
            val photoFile = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            
            val uri = FileProvider.getUriForFile(
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
     * Get shapes for the current image
     */
    fun getShapesForCurrentImage(): List<ShapeItem> {
        val currentState = _uiState.value
        return currentState.shapeItems[currentState.currentImageIndex] ?: emptyList()
    }

    /**
     * Prepare the image download by generating a filename and showing the confirmation dialog
     */
    fun prepareImageDownload() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Safety check - make sure we have valid data
            if (currentState.processedImageUris.isEmpty() || 
                currentState.currentImageIndex >= currentState.processedImageUris.size) {
                _uiState.update { 
                    it.copy(toastMessage = "No image available to download") 
                }
                return@launch
            }
            
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
                val rotationAngle = currentState.rotationAngles.getOrNull(currentState.currentImageIndex) ?: 0f
                
                // Generate filename
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val randomId = Random.nextInt(1000, 9999)
                val filename = "SmartDocsConvert_${timeStamp}_$randomId.jpg"
                
                // Update state with download information
                _uiState.update {
                    it.copy(
                        pendingDownloadFilename = filename,
                        userEnteredFilename = filename,
                        pendingDownloadType = "image",
                        pendingDownloadUri = sourceUri,
                        pendingDownloadFilter = selectedFilter,
                        pendingDownloadBrightness = brightness,
                        pendingDownloadContrast = contrast,
                        pendingDownloadRotation = rotationAngle,
                        showDownloadOptions = false,
                        showDownloadConfirmation = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        toastMessage = "Hazırlık sırasında hata oluştu: ${e.message}",
                        showDownloadOptions = false
                    )
                }
            }
        }
    }
    
    /**
     * Prepare the PDF download by generating a filename and showing the confirmation dialog
     */
    fun preparePdfDownload() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Safety check - make sure we have valid data
            if (currentState.processedImageUris.isEmpty() || 
                currentState.currentImageIndex >= currentState.processedImageUris.size) {
                _uiState.update { 
                    it.copy(toastMessage = "No image available to save as PDF") 
                }
                return@launch
            }
            
            try {
                // Determine the appropriate source URI
                val sourceUri = if (currentState.currentImageIndex < currentState.croppedImageUris.size && 
                                    currentState.croppedImageUris[currentState.currentImageIndex] != null) {
                    currentState.croppedImageUris[currentState.currentImageIndex]!!
                } else {
                    currentState.processedImageUris[currentState.currentImageIndex]
                }
                
                // Generate filename
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val randomId = Random.nextInt(1000, 9999)
                val filename = "SmartDocsConvert_${timeStamp}_$randomId.pdf"
                
                // Update state with download information
                _uiState.update {
                    it.copy(
                        pendingDownloadFilename = filename,
                        userEnteredFilename = filename,
                        pendingDownloadType = "pdf",
                        pendingDownloadUri = sourceUri,
                        showDownloadOptions = false,
                        showDownloadConfirmation = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        toastMessage = "Hazırlık sırasında hata oluştu: ${e.message}",
                        showDownloadOptions = false
                    )
                }
            }
        }
    }

    /**
     * Update the download filename
     */
    fun updateDownloadFilename(newFilename: String) {
        _uiState.update {
            it.copy(
                userEnteredFilename = newFilename
            )
        }
    }
    
    /**
     * Update the download all images flag
     */
    fun updateDownloadAllImages(downloadAll: Boolean) {
        _uiState.update {
            it.copy(
                downloadAllImages = downloadAll
            )
        }
    }
    
    /**
     * Confirm and start the download process
     */
    fun confirmDownload() {
        val currentState = _uiState.value
        
        // Get the custom filename entered by the user
        val customFilename = currentState.userEnteredFilename
        
        // Close the confirmation dialog
        _uiState.update {
            it.copy(
                showDownloadConfirmation = false,
                pendingDownloadFilename = customFilename // Use the user-edited filename
            )
        }
        
        when (currentState.pendingDownloadType) {
            "image" -> {
                // Check if there are multiple images - if so, always download all
                if (currentState.processedImageUris.size > 1 || currentState.downloadAllImages) {
                    // Download all images
                    downloadAllImages(customFilename)
                } else {
                    // Download only the current image
                    currentState.pendingDownloadUri?.let { uri ->
                        saveAsImage(
                            uri,
                            currentState.pendingDownloadFilter ?: "Original",
                            currentState.pendingDownloadBrightness,
                            currentState.pendingDownloadContrast,
                            currentState.pendingDownloadRotation
                        )
                    }
                }
            }
            "pdf" -> {
                currentState.pendingDownloadUri?.let { uri ->
                    saveAsPdf(uri)
                }
            }
        }
    }
    
    /**
     * Download all images
     */
    private fun downloadAllImages(baseFilename: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            if (currentState.processedImageUris.isEmpty()) {
                _uiState.update { it.copy(toastMessage = "İndirilecek resim bulunamadı") }
                return@launch
            }
            
            // Show initial status message with count of images
            val totalImages = currentState.processedImageUris.size
            _uiState.update { 
                it.copy(
                    toastMessage = "$totalImages resim indiriliyor...",
                    isDownloading = true,
                    showDownloadAnimation = true
                ) 
            }
            
            // Get an instance of WorkManager
            val workManager = WorkManager.getInstance(applicationContext)
            
            // Track download progress
            val downloadRequests = mutableListOf<OneTimeWorkRequest>()
            
            // For each image in the list, create a download job
            currentState.processedImageUris.forEachIndexed { index, imageUri ->
                try {
                    // Get the appropriate source URI (cropped if available)
                    val sourceUri = if (index < currentState.croppedImageUris.size && 
                                       currentState.croppedImageUris[index] != null) {
                        currentState.croppedImageUris[index]!!
                    } else {
                        imageUri
                    }
                    
                    // Get the filter settings for this specific image
                    val selectedFilter = currentState.selectedFilters.getOrNull(index) ?: "Original"
                    val brightness = currentState.brightnessValues.getOrNull(index) ?: 1f
                    val contrast = currentState.contrastValues.getOrNull(index) ?: 1f
                    val rotation = currentState.rotationAngles.getOrNull(index) ?: 0f
                    
                    // Generate unique filename with index
                    val filename = if (currentState.processedImageUris.size > 1) {
                        // If there are multiple images, add an index to the filename
                        "${baseFilename.substringBeforeLast(".")}_${index + 1}.jpg"
                    } else {
                        // If there's only one image, use the original filename
                        baseFilename
                    }
                    
                    // Create the download request
                    val downloadRequest = DownloadWorker.createImageDownloadWork(
                        imageUri = sourceUri,
                        filter = selectedFilter,
                        brightness = brightness,
                        contrast = contrast,
                        rotation = rotation,
                        filename = filename
                    )
                    
                    downloadRequests.add(downloadRequest)
                } catch (e: Exception) {
                    android.util.Log.e("ImageFilterViewModel", "Error setting up download for image $index: ${e.message}")
                }
            }
            
            if (downloadRequests.isEmpty()) {
                _uiState.update { 
                    it.copy(
                        toastMessage = "İndirme hazırlığında hata oluştu",
                        isDownloading = false,
                        showDownloadAnimation = false
                    ) 
                }
                return@launch
            }
            
            // Enqueue all download requests
            downloadRequests.forEach { request ->
                workManager.enqueue(request)
            }
            
            // Allow downloads to run in the background
            _uiState.update { 
                it.copy(
                    toastMessage = "$totalImages resim İndirilenler/SmartDocsConvert klasörüne indiriliyor. Her resim için bildirim alacaksınız.",
                    isDownloading = false,
                    showDownloadAnimation = false
                ) 
            }
        }
    }
    
    /**
     * Cancel the download process
     */
    fun cancelDownload() {
        _uiState.update {
            it.copy(
                showDownloadConfirmation = false,
                pendingDownloadFilename = null,
                pendingDownloadType = null,
                pendingDownloadUri = null,
                pendingDownloadFilter = null
            )
        }
    }

    /**
     * Implementation of image download
     */
    private fun saveAsImage(
        sourceUri: Uri,
        selectedFilter: String,
        brightness: Float,
        contrast: Float,
        rotationAngle: Float
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.update { it.copy(isDownloading = true) }
            
            try {
                // Create work request using the helper method in DownloadWorker
                val downloadRequest = DownloadWorker.createImageDownloadWork(
                    imageUri = sourceUri,
                    filter = selectedFilter,
                    brightness = brightness,
                    contrast = contrast,
                    rotation = rotationAngle,
                    filename = currentState.pendingDownloadFilename
                )
                
                val workManager = WorkManager.getInstance(applicationContext)
                
                // Track the work progress
                workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                    .observeForever(object : Observer<WorkInfo> {
                        override fun onChanged(value: WorkInfo) {
                            when (value.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    val resultMessage = value.outputData.getString(DownloadWorker.KEY_RESULT_MESSAGE)
                                        ?: "Görüntü kaydedildi"
                                    _uiState.update {
                                        it.copy(
                                            toastMessage = resultMessage,
                                            isDownloading = false
                                        )
                                    }
                                    // Remove the observer to prevent memory leaks
                                    workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                                        .removeObserver(this)
                                }
                                WorkInfo.State.FAILED -> {
                                    val errorMsg = value.outputData.getString(DownloadWorker.KEY_ERROR_MESSAGE)
                                        ?: "İndirme işlemi başarısız oldu"
                                    _uiState.update {
                                        it.copy(
                                            toastMessage = "Görüntü kaydedilirken hata oluştu: $errorMsg",
                                            isDownloading = false
                                        )
                                    }
                                    // Remove the observer to prevent memory leaks
                                    workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                                        .removeObserver(this)
                                }
                                WorkInfo.State.CANCELLED -> {
                                    _uiState.update {
                                        it.copy(
                                            toastMessage = "İndirme işlemi iptal edildi",
                                            isDownloading = false
                                        )
                                    }
                                    // Remove the observer to prevent memory leaks
                                    workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                                        .removeObserver(this)
                                }
                                else -> {} // Other states like RUNNING, ENQUEUED, BLOCKED
                            }
                        }
                    })
                
                // Update the UI with the custom filename before enqueueing the work
                _uiState.update {
                    it.copy(
                        toastMessage = "${currentState.pendingDownloadFilename} indiriliyor...",
                        showDownloadAnimation = true
                    )
                }
                
                // Enqueue the work request
                workManager.enqueue(downloadRequest)
                
                // Reset the download animation after a delay
                delay(2000)
                _uiState.update {
                    it.copy(showDownloadAnimation = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        toastMessage = "Görüntü kaydedilirken hata oluştu: ${e.message}",
                        isDownloading = false
                    )
                }
            }
        }
    }
    
    /**
     * Implementation of PDF download
     */
    private fun saveAsPdf(sourceUri: Uri) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.update { it.copy(isDownloading = true) }
            
            try {
                // Create work request using the helper method in DownloadWorker
                val downloadRequest = DownloadWorker.createPdfDownloadWork(
                    imageUri = sourceUri,
                    filename = currentState.pendingDownloadFilename
                )
                
                val workManager = WorkManager.getInstance(applicationContext)
                
                // Track the work progress
                workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                    .observeForever(object : Observer<WorkInfo> {
                        override fun onChanged(value: WorkInfo) {
                            when (value.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    val resultMessage = value.outputData.getString(DownloadWorker.KEY_RESULT_MESSAGE)
                                        ?: "PDF olarak kaydedildi"
                                    _uiState.update {
                                        it.copy(
                                            toastMessage = resultMessage,
                                            isDownloading = false
                                        )
                                    }
                                    // Remove the observer to prevent memory leaks
                                    workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                                        .removeObserver(this)
                                }
                                WorkInfo.State.FAILED -> {
                                    val errorMsg = value.outputData.getString(DownloadWorker.KEY_ERROR_MESSAGE)
                                        ?: "İndirme işlemi başarısız oldu"
                                    _uiState.update {
                                        it.copy(
                                            toastMessage = "PDF olarak kaydedilirken hata oluştu: $errorMsg",
                                            isDownloading = false
                                        )
                                    }
                                    // Remove the observer to prevent memory leaks
                                    workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                                        .removeObserver(this)
                                }
                                WorkInfo.State.CANCELLED -> {
                                    _uiState.update {
                                        it.copy(
                                            toastMessage = "İndirme işlemi iptal edildi",
                                            isDownloading = false
                                        )
                                    }
                                    // Remove the observer to prevent memory leaks
                                    workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                                        .removeObserver(this)
                                }
                                else -> {} // Other states like RUNNING, ENQUEUED, BLOCKED
                            }
                        }
                    })
                
                // Update the UI with the custom filename before enqueueing the work
                _uiState.update { 
                    it.copy(
                        toastMessage = "${currentState.pendingDownloadFilename} olarak indiriliyor...",
                        showDownloadAnimation = true
                    )
                }
                
                // Enqueue the work request
                workManager.enqueue(downloadRequest)
                
                // Reset the download animation after a delay
                delay(2000)
                _uiState.update {
                    it.copy(showDownloadAnimation = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        toastMessage = "PDF olarak kaydedilirken hata oluştu: ${e.message}",
                        isDownloading = false
                    )
                }
            }
        }
    }

    fun toggleDownloadOptions() {
        _uiState.update {
            it.copy(showDownloadOptions = !it.showDownloadOptions)
        }
    }
    
    fun hideDownloadOptions() {
        _uiState.update {
            it.copy(showDownloadOptions = false)
        }
    }
} 