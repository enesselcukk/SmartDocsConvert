package com.example.smartdocsconvert.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun loadBitmap(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Load bitmap with orientation preserved
                val (bitmap, orientation) = loadBitmapWithOrientationInfo(uri) ?: return@withContext null
                
                // Apply EXIF rotation to get correctly oriented bitmap
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        matrix.preScale(-1f, 1f)
                        matrix.postRotate(270f)
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        matrix.preScale(-1f, 1f)
                        matrix.postRotate(90f)
                    }
                    // For normal orientation, no transformation needed
                }
                
                // Apply the transformation if needed
                if (!matrix.isIdentity) {
                    android.util.Log.d("ImageRepository", "Applying EXIF rotation: $orientation")
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    
                    // If a new bitmap was created, recycle the old one
                    if (rotatedBitmap != bitmap) {
                        bitmap.recycle()
                        return@withContext rotatedBitmap
                    }
                }
                
                return@withContext bitmap
            } catch (e: Exception) {
                android.util.Log.e("ImageRepository", "Error loading bitmap", e)
                return@withContext null
            }
        }
    }
    
    suspend fun saveCroppedImage(
        sourceUri: Uri,
        cropX: Int,
        cropY: Int,
        cropWidth: Int,
        cropHeight: Int,
        index: Int
    ): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ImageRepository", "Starting crop operation - Original params: X:$cropX, Y:$cropY, W:$cropWidth, H:$cropHeight")
                
                // Step 1: Load bitmap with orientation information
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                }
                
                // Read EXIF orientation
                val exifOrientation = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val exif = ExifInterface(input)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
                
                android.util.Log.d("ImageRepository", "Image EXIF orientation: $exifOrientation")
                
                // Load the original image without any rotation applied
                val originalBitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                } ?: return@withContext null
                
                android.util.Log.d("ImageRepository", "Original bitmap dimensions: ${originalBitmap.width}x${originalBitmap.height}")
                
                // Step 2: Handle EXIF orientation before cropping
                val matrix = Matrix()
                val rotationAngle = when (exifOrientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                
                val needsTransformation = exifOrientation != ExifInterface.ORIENTATION_NORMAL &&
                                         exifOrientation != ExifInterface.ORIENTATION_UNDEFINED
                
                var bitmapToCrop = originalBitmap
                var adjustedCropX = cropX
                var adjustedCropY = cropY
                var adjustedCropWidth = cropWidth
                var adjustedCropHeight = cropHeight
                
                // If image has EXIF rotation, we need to adjust the crop coordinates
                if (needsTransformation) {
                    android.util.Log.d("ImageRepository", "Adjusting for EXIF rotation: $rotationAngle degrees")
                    
                    // For 90 or 270 rotation, swap width and height
                    if (rotationAngle == 90f || rotationAngle == 270f) {
                        // Create a properly rotated bitmap for cropping
                        matrix.postRotate(rotationAngle)
                        val rotatedBitmap = Bitmap.createBitmap(
                            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                        )
                        
                        if (rotatedBitmap != originalBitmap) {
                            bitmapToCrop = rotatedBitmap
                            originalBitmap.recycle()
                        }
                        
                        android.util.Log.d("ImageRepository", "Rotated bitmap dimensions: ${bitmapToCrop.width}x${bitmapToCrop.height}")
                    }
                }
                
                // Ensure crop parameters are within bounds
                adjustedCropX = adjustedCropX.coerceIn(0, bitmapToCrop.width - 1)
                adjustedCropY = adjustedCropY.coerceIn(0, bitmapToCrop.height - 1)
                adjustedCropWidth = adjustedCropWidth.coerceIn(1, bitmapToCrop.width - adjustedCropX)
                adjustedCropHeight = adjustedCropHeight.coerceIn(1, bitmapToCrop.height - adjustedCropY)
                
                android.util.Log.d("ImageRepository", "Final crop parameters: X:$adjustedCropX, Y:$adjustedCropY, W:$adjustedCropWidth, H:$adjustedCropHeight")
                
                // Step 3: Perform the crop operation
                val croppedBitmap = try {
                    Bitmap.createBitmap(
                        bitmapToCrop,
                        adjustedCropX,
                        adjustedCropY,
                        adjustedCropWidth,
                        adjustedCropHeight
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ImageRepository", "Error creating cropped bitmap: ${e.message}", e)
                    bitmapToCrop.recycle()
                    return@withContext null
                }
                
                android.util.Log.d("ImageRepository", "Cropped bitmap dimensions: ${croppedBitmap.width}x${croppedBitmap.height}")
                
                // If bitmapToCrop is not the same as croppedBitmap, and not the same as originalBitmap, recycle it
                if (bitmapToCrop != croppedBitmap && bitmapToCrop != originalBitmap) {
                    bitmapToCrop.recycle()
                }
                
                // Step 4: Save the cropped image
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "CROPPED_${timeStamp}_$index.jpg"
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val imageFile = File(cachePath, fileName)
                
                try {
                    FileOutputStream(imageFile).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        out.flush()
                    }
                    
                    // Don't write the original EXIF orientation to the output file,
                    // since we already applied it during crop
                    ExifInterface(imageFile.path).apply {
                        setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                        saveAttributes()
                    }
                    
                    croppedBitmap.recycle()
                    
                    android.util.Log.d("ImageRepository", "Successfully saved cropped image to: ${imageFile.path}")
                    return@withContext Uri.fromFile(imageFile)
                } catch (e: Exception) {
                    android.util.Log.e("ImageRepository", "Error saving cropped image: ${e.message}", e)
                    croppedBitmap.recycle()
                    return@withContext null
                }
            } catch (e: Exception) {
                android.util.Log.e("ImageRepository", "Unexpected error in crop operation: ${e.message}", e)
                return@withContext null
            }
        }
    }
    
    /**
     * Loads a bitmap and its EXIF orientation from a Uri
     * @return Pair of (Bitmap, EXIF orientation constant)
     */
    private fun loadBitmapWithOrientationInfo(uri: Uri): Pair<Bitmap, Int>? {
        try {
            // Read the EXIF orientation
            val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
                try {
                    val exif = ExifInterface(input)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ImageRepository", "Error reading EXIF orientation", e)
                    ExifInterface.ORIENTATION_NORMAL
                }
            } ?: ExifInterface.ORIENTATION_NORMAL
            
            // Load the bitmap without decoding EXIF rotation automatically (we'll handle it separately)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
            }
            
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return null
            
            return Pair(bitmap, orientation)
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error loading bitmap with orientation", e)
            return null
        }
    }
    
    suspend fun saveProcessedImage(
        sourceUri: Uri,
        filterName: String,
        brightness: Float,
        contrast: Float,
        intensity: Float,
        rotationAngle: Float
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmap(sourceUri) ?: return@withContext "Görüntü yüklenemedi"
                
                // Apply filters and effects
                val androidColorMatrix = ColorMatrix()
                applyColorMatrixForFilter(
                    androidColorMatrix,
                    filterName,
                    brightness,
                    contrast,
                    intensity
                )
                
                val colorMatrixFilter = ColorMatrixColorFilter(androidColorMatrix)
                val paint = Paint().apply {
                    colorFilter = colorMatrixFilter
                }
                
                // Apply rotation if needed
                val rotatedBitmap = if (rotationAngle != 0f) {
                    val rotationMatrix = Matrix().apply {
                        postRotate(rotationAngle)
                    }
                    Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        rotationMatrix,
                        true
                    )
                } else {
                    bitmap
                }
                
                // Draw the processed image to a canvas
                val resultBitmap = Bitmap.createBitmap(
                    rotatedBitmap.width,
                    rotatedBitmap.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(resultBitmap)
                canvas.drawBitmap(rotatedBitmap, 0f, 0f, paint)
                
                // Save the file
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "SmartDocsConvert_$timeStamp.jpg"
                
                // Use MediaStore API for Android Q and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SmartDocsConvert")
                    }
                    
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    uri?.let { imageUri ->
                        context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                            outputStream.flush()
                        }
                        
                        // Clean up
                        if (rotatedBitmap != bitmap) {
                            rotatedBitmap.recycle()
                        }
                        bitmap.recycle()
                        resultBitmap.recycle()
                        
                        return@withContext "Görüntü kaydedildi"
                    }
                } else {
                    // Use direct file access for older Android versions
                    val imageDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "SmartDocsConvert"
                    ).apply {
                        if (!exists()) mkdirs()
                    }
                    
                    val imageFile = File(imageDir, fileName)
                    FileOutputStream(imageFile).use { outputStream ->
                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        outputStream.flush()
                    }
                    
                    // Notify the media scanner
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(imageFile)
                    context.sendBroadcast(mediaScanIntent)
                    
                    // Clean up
                    if (rotatedBitmap != bitmap) {
                        rotatedBitmap.recycle()
                    }
                    bitmap.recycle()
                    resultBitmap.recycle()
                    
                    return@withContext "Görüntü kaydedildi: ${imageFile.absolutePath}"
                }
                
                return@withContext "Kayıt sırasında bir hata oluştu"
            } catch (e: Exception) {
                return@withContext "Görüntü kaydedilirken hata oluştu: ${e.message}"
            }
        }
    }
    
    private fun applyColorMatrixForFilter(
        colorMatrix: ColorMatrix,
        filterName: String,
        brightness: Float,
        contrast: Float,
        intensity: Float
    ) {
        when (filterName) {
            "Original" -> {
                val scale = contrast
                val translate = (-.5f * scale + .5f) * 255f
                
                val matrixValues = floatArrayOf(
                    brightness * scale, 0f, 0f, 0f, translate,
                    0f, brightness * scale, 0f, 0f, translate,
                    0f, 0f, brightness * scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(matrixValues)
            }
            
            "Clarendon" -> {
                val warmth = 1.1f
                val filterBrightness = 1.1f * brightness
                val scale = contrast * 1.2f
                val translate = (-.5f * scale + .5f) * 255f
                
                val matrixValues = floatArrayOf(
                    (1.2f * warmth) * scale * filterBrightness, 0f, 0f, 0f, translate,
                    0f, 1.1f * scale * filterBrightness, 0f, 0f, translate,
                    0f, 0f, scale * filterBrightness, 0f, translate + 5f,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(matrixValues)
            }
            
            // Diğer filtreler için benzer kod...
            else -> {
                // Default to original
                val scale = contrast
                val translate = (-.5f * scale + .5f) * 255f
                
                val matrixValues = floatArrayOf(
                    brightness * scale, 0f, 0f, 0f, translate,
                    0f, brightness * scale, 0f, 0f, translate,
                    0f, 0f, brightness * scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(matrixValues)
            }
        }
    }

} 