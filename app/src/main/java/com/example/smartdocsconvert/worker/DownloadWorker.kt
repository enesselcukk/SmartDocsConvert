package com.example.smartdocsconvert.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.smartdocsconvert.util.PdfUtil
import com.example.smartdocsconvert.util.NotificationUtil
import com.example.smartdocsconvert.util.ImageProcessingUtil
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.BackoffPolicy
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_DOWNLOAD_TYPE = "download_type"
        const val KEY_IMAGE_URI = "image_uri"
        const val KEY_IMAGE_URIS = "image_uris"
        const val KEY_FILTER = "filter"
        const val KEY_BRIGHTNESS = "brightness"
        const val KEY_CONTRAST = "contrast"
        const val KEY_ROTATION = "rotation"
        const val KEY_FILENAME = "filename"
        
        const val DOWNLOAD_TYPE_IMAGE = "image"
        const val DOWNLOAD_TYPE_PDF = "pdf"
        const val DOWNLOAD_TYPE_MULTI_PDF = "multi_pdf"
        
        // Result keys
        const val KEY_RESULT_MESSAGE = "result_message"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_OUTPUT_FILE_PATH = "output_file_path"
        
        /**
         * Helper method to create a WorkRequest for image downloads
         */
        fun createImageDownloadWork(
            imageUri: Uri,
            filter: String = "Original",
            brightness: Float = 1f,
            contrast: Float = 1f,
            rotation: Float = 0f,
            filename: String? = null
        ): OneTimeWorkRequest {
            // Create workData with all parameters
            val workData = if (filename != null) {
                workDataOf(
                    KEY_DOWNLOAD_TYPE to DOWNLOAD_TYPE_IMAGE,
                    KEY_IMAGE_URI to imageUri.toString(),
                    KEY_FILTER to filter,
                    KEY_BRIGHTNESS to brightness,
                    KEY_CONTRAST to contrast,
                    KEY_ROTATION to rotation,
                    KEY_FILENAME to filename
                )
            } else {
                workDataOf(
                    KEY_DOWNLOAD_TYPE to DOWNLOAD_TYPE_IMAGE,
                    KEY_IMAGE_URI to imageUri.toString(),
                    KEY_FILTER to filter,
                    KEY_BRIGHTNESS to brightness,
                    KEY_CONTRAST to contrast,
                    KEY_ROTATION to rotation
                )
            }
            
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workData)
                .addTag("image_download")
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    30000L, // 30 seconds minimum backoff time
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
        
        /**
         * Helper method to create a WorkRequest for PDF downloads
         */
        fun createPdfDownloadWork(
            imageUri: Uri,
            filename: String? = null
        ): OneTimeWorkRequest {
            // Create workData with or without filename
            val workData = if (filename != null) {
                workDataOf(
                    KEY_DOWNLOAD_TYPE to DOWNLOAD_TYPE_PDF,
                    KEY_IMAGE_URI to imageUri.toString(),
                    KEY_FILENAME to filename
                )
            } else {
                workDataOf(
                    KEY_DOWNLOAD_TYPE to DOWNLOAD_TYPE_PDF,
                    KEY_IMAGE_URI to imageUri.toString()
                )
            }
            
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workData)
                .addTag("pdf_download")
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    30000L, // 30 seconds minimum backoff time
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
        
        /**
         * Helper method to create a WorkRequest for multi-page PDF downloads
         */
        fun createMultiPagePdfDownloadWork(
            imageUris: List<Uri>,
            filename: String? = null
        ): OneTimeWorkRequest {
            // Convert Uri list to string list
            val imageUriStrings = imageUris.map { it.toString() }
            
            // Create workData with or without filename
            val workData = if (filename != null) {
                workDataOf(
                    KEY_DOWNLOAD_TYPE to DOWNLOAD_TYPE_MULTI_PDF,
                    KEY_IMAGE_URIS to imageUriStrings.toTypedArray(),
                    KEY_FILENAME to filename
                )
            } else {
                workDataOf(
                    KEY_DOWNLOAD_TYPE to DOWNLOAD_TYPE_MULTI_PDF,
                    KEY_IMAGE_URIS to imageUriStrings.toTypedArray()
                )
            }
            
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workData)
                .addTag("multi_pdf_download")
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    30000L, // 30 seconds minimum backoff time
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
    }
    
    override suspend fun doWork(): Result {
        try {
            val downloadType = inputData.getString(KEY_DOWNLOAD_TYPE) ?: return Result.failure()
            
           Log.d("DownloadWorker", "Starting download work: type=$downloadType")
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val randomId = Random.nextInt(1000, 9999)
            
            // Check if custom filename was provided
            val customFilename = inputData.getString(KEY_FILENAME)
           Log.d("DownloadWorker", "Custom filename provided: $customFilename")
            
            return when (downloadType) {
                DOWNLOAD_TYPE_IMAGE -> {
                    // Get image URI
                    val imageUriString = inputData.getString(KEY_IMAGE_URI) ?: return Result.failure()
                    val imageUri = Uri.parse(imageUriString)
                    
                    val selectedFilter = inputData.getString(KEY_FILTER) ?: "Original"
                    val brightness = inputData.getFloat(KEY_BRIGHTNESS, 1f)
                    val contrast = inputData.getFloat(KEY_CONTRAST, 1f)
                    val rotation = inputData.getFloat(KEY_ROTATION, 0f)
                    
                   Log.d("DownloadWorker", "Processing image with filter=$selectedFilter, brightness=$brightness, contrast=$contrast")
                    
                    // Use custom filename if provided
                    val filename = customFilename ?: "SmartDocsConvert_${timeStamp}_$randomId.jpg"
                    
                    // Process and save the image
                    val resultMessage = saveProcessedImage(
                        imageUri,
                        selectedFilter,
                        brightness,
                        contrast,
                        rotation,
                        timeStamp,
                        randomId,
                        filename
                    )
                    
                    Result.success(Data.Builder().putString(KEY_RESULT_MESSAGE, resultMessage).build())
                }
                
                DOWNLOAD_TYPE_PDF -> {
                    try {
                        // Get image URI
                        val imageUriString = inputData.getString(KEY_IMAGE_URI) ?: return Result.failure()
                        val imageUri = Uri.parse(imageUriString)
                        
                       Log.d("DownloadWorker", "Creating PDF from image: $imageUri")
                        
                        // Verify the image URI is accessible
                        val isUriAccessible = context.contentResolver.openInputStream(imageUri)?.use { 
                            it.available() > 0 
                        } ?: false
                        
                        if (!isUriAccessible) {
                           Log.e("DownloadWorker", "Image URI is not accessible: $imageUri")
                            return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, "Image is not accessible").build())
                        }
                        
                        // Check if storage is available
                        val storageAvailable = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
                        if (!storageAvailable) {
                           Log.e("DownloadWorker", "External storage is not available")
                            return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, "Cannot access storage").build())
                        }
                        
                        // Convert image to PDF
                        val fileName = customFilename ?: "SmartDocsConvert_${timeStamp}_$randomId.pdf"
                       Log.d("DownloadWorker", "Creating PDF with filename: $fileName")
                        
                        try {
                            val pdfFile = savePdfFromImage(imageUri, fileName)
                            
                            // Verify the PDF was created properly
                            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                               Log.e("DownloadWorker", "PDF file was not created or is empty: ${pdfFile.absolutePath}")
                                return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, "PDF file could not be created").build())
                            }
                            
                            // Show notification
                            NotificationUtil.createNotificationChannel(context)
                           Log.d("DownloadWorker", "Attempting to show notification for: ${pdfFile.absolutePath}")
                            
                            // Check if notification permission is granted
                            val hasNotificationPermission = NotificationUtil.hasNotificationPermission(context)
                           Log.d("DownloadWorker", "Notification permission granted: $hasNotificationPermission")
                            
                            NotificationUtil.showDownloadNotification(context, pdfFile)
                            
                           Log.d("DownloadWorker", "PDF created successfully: ${pdfFile.absolutePath}")
                            
                            return Result.success(Data.Builder().putString(KEY_RESULT_MESSAGE, "PDF saved to: Downloads/SmartDocsConvert").putString(KEY_OUTPUT_FILE_PATH, pdfFile.absolutePath).build())
                        } catch (e: Exception) {
                           Log.e("DownloadWorker", "PDF creation error: ${e.message}", e)
                            
                            // Provide more specific error message based on the exception
                            val errorMsg = when {
                                e.message?.contains("permission") == true -> "Permission error: Check storage permissions"
                                e.message?.contains("space") == true -> "Insufficient storage space"
                                e.message?.contains("decode") == true -> "Image couldn't be processed"
                                e.message?.contains("bitmap") == true -> "Image couldn't be converted"
                                e.message?.contains("PDF") == true -> "PDF creation error"
                                else -> "Error creating PDF: ${e.message}"
                            }
                            
                            return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, errorMsg).build())
                        }
                    } catch (e: Exception) {
                       Log.e("DownloadWorker", "PDF creation failed: ${e.message}", e)
                        return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, e.message).build())
                    }
                }
                
                DOWNLOAD_TYPE_MULTI_PDF -> {
                    try {
                        // Get image URIs from the input data
                        val imageUriStrings = inputData.getStringArray(KEY_IMAGE_URIS) ?: return Result.failure(
                            Data.Builder().putString(KEY_ERROR_MESSAGE, "No images provided for PDF").build()
                        )
                        
                        if (imageUriStrings.isEmpty()) {
                            return Result.failure(
                                Data.Builder().putString(KEY_ERROR_MESSAGE, "Empty image list for PDF").build()
                            )
                        }
                        
                       Log.d("DownloadWorker", "Creating multi-page PDF from ${imageUriStrings.size} images")
                        
                        // Convert string array to Uri list
                        val imageUris = imageUriStrings.map { Uri.parse(it) }
                        
                        // Verify all URIs are accessible
                        val inaccessibleUris = imageUris.filter { uri ->
                            val isAccessible = context.contentResolver.openInputStream(uri)?.use { 
                                it.available() > 0 
                            } ?: false
                            !isAccessible
                        }
                        
                        if (inaccessibleUris.isNotEmpty()) {
                           Log.e("DownloadWorker", "${inaccessibleUris.size} image URIs are not accessible")
                            return Result.failure(
                                Data.Builder().putString(KEY_ERROR_MESSAGE, "${inaccessibleUris.size} images are not accessible").build()
                            )
                        }
                        
                        // Check if storage is available
                        val storageAvailable = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
                        if (!storageAvailable) {
                           Log.e("DownloadWorker", "External storage is not available")
                            return Result.failure(
                                Data.Builder().putString(KEY_ERROR_MESSAGE, "Cannot access storage").build()
                            )
                        }
                        
                        // Convert images to multi-page PDF
                        val fileName = customFilename ?: "SmartDocsConvert_${timeStamp}_$randomId.pdf"
                       Log.d("DownloadWorker", "Creating multi-page PDF with filename: $fileName")
                        
                        try {
                            // Use the PdfUtil's createPdfFromImages method
                            val pdfFile = PdfUtil.createPdfFromImages(context, imageUris, fileName)
                            
                            // Verify the PDF was created properly
                            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                               Log.e("DownloadWorker", "Multi-page PDF file was not created or is empty: ${pdfFile.absolutePath}")
                                return Result.failure(
                                    Data.Builder().putString(KEY_ERROR_MESSAGE, "PDF file could not be created").build()
                                )
                            }
                            
                            // Show notification
                            NotificationUtil.createNotificationChannel(context)
                            NotificationUtil.showDownloadNotification(context, pdfFile)
                            
                           Log.d("DownloadWorker", "Multi-page PDF created successfully: ${pdfFile.absolutePath}")
                            
                            return Result.success(
                                Data.Builder()
                                    .putString(KEY_RESULT_MESSAGE, "Çoklu sayfalı PDF kaydedildi: Downloads/SmartDocsConvert")
                                    .putString(KEY_OUTPUT_FILE_PATH, pdfFile.absolutePath)
                                    .build()
                            )
                        } catch (e: Exception) {
                           Log.e("DownloadWorker", "Multi-page PDF creation error: ${e.message}", e)
                            
                            // Provide more specific error message based on the exception
                            val errorMsg = when {
                                e.message?.contains("permission") == true -> "Permission error: Check storage permissions"
                                e.message?.contains("space") == true -> "Insufficient storage space"
                                e.message?.contains("decode") == true -> "Images couldn't be processed"
                                e.message?.contains("bitmap") == true -> "Images couldn't be converted"
                                e.message?.contains("PDF") == true -> "PDF creation error"
                                else -> "Error creating PDF: ${e.message}"
                            }
                            
                            return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, errorMsg).build())
                        }
                    } catch (e: Exception) {
                       Log.e("DownloadWorker", "Multi-page PDF creation failed: ${e.message}", e)
                        return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, e.message).build())
                    }
                }
                
                else -> {
                   Log.e("DownloadWorker", "Unknown download type: $downloadType")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
           Log.e("DownloadWorker", "Worker failed: ${e.message}", e)
            return Result.failure(Data.Builder().putString(KEY_ERROR_MESSAGE, e.message).build())
        }
    }
    
    private fun saveProcessedImage(
        sourceUri: Uri,
        selectedFilter: String,
        brightness: Float,
        contrast: Float,
        rotationAngle: Float,
        timeStamp: String,
        randomId: Int,
        customFilename: String? = null
    ): String {
        try {
            // Load the bitmap
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return "Image could not be loaded"
            
            // Process the image using ImageProcessingUtil
            val resultBitmap = com.example.smartdocsconvert.util.ImageProcessingUtil.processImage(
                sourceBitmap = bitmap,
                filterName = selectedFilter,
                brightness = brightness,
                contrast = contrast,
                rotationAngle = rotationAngle
            )
            
            // Save the file
            val fileName = customFilename ?: "SmartDocsConvert_${timeStamp}_$randomId.jpg"
            
            // Ensure the SmartDocsConvert directory exists in Downloads
            val smartDocsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SmartDocsConvert"
            ).apply {
                if (!exists()) mkdirs()
            }
            
            // Use MediaStore API for Android Q and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SmartDocsConvert")
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let { imageUri ->
                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        outputStream.flush()
                    }
                    
                    // Clean up
                    bitmap.recycle()
                    resultBitmap.recycle()
                    
                    // Show notification for images too
                    try {
                        val imageFile = File(smartDocsDir, fileName)
                        
                        // Create a notification channel if needed
                        NotificationUtil.createNotificationChannel(context)
                        
                        // Try to locate the downloaded file
                        if (imageFile.exists()) {
                            NotificationUtil.showDownloadNotification(context, imageFile)
                           Log.d("DownloadWorker", "Showing notification for image: ${imageFile.absolutePath}")
                        } else {
                            // If file isn't accessible directly, use the content URI to create a notification
                            val resolver = context.contentResolver
                            resolver.query(imageUri, null, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val idColumn = cursor.getColumnIndex(MediaStore.Downloads._ID)
                                    if (idColumn >= 0) {
                                        val id = cursor.getLong(idColumn)
                                        val contentUri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                                       Log.d("DownloadWorker", "Using content URI for notification: $contentUri")
                                        
                                        // Pass the file URI to the notification util
                                        val virtualFile = File(smartDocsDir, fileName)
                                        NotificationUtil.showDownloadNotification(context, virtualFile)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                       Log.e("DownloadWorker", "Error showing image notification: ${e.message}")
                    }
                    
                    return "Image saved to: Downloads/SmartDocsConvert/$fileName"
                }
            } else {
                // Use direct file access for older Android versions
                val imageFile = File(smartDocsDir, fileName)
                FileOutputStream(imageFile).use { outputStream ->
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    outputStream.flush()
                }
                
                // Notify the media scanner
                val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(imageFile)
                context.sendBroadcast(mediaScanIntent)
                
                // Clean up
                bitmap.recycle()
                resultBitmap.recycle()
                
                // Show notification for the downloaded image
                try {
                    if (imageFile.exists()) {
                        // Create a notification channel if needed
                        NotificationUtil.createNotificationChannel(context)
                        
                        NotificationUtil.showDownloadNotification(context, imageFile)
                       Log.d("DownloadWorker", "Showing notification for image: ${imageFile.absolutePath}")
                    }
                } catch (e: Exception) {
                   Log.e("DownloadWorker", "Error showing image notification: ${e.message}")
                }
                
                return "Image saved to: Downloads/SmartDocsConvert/$fileName"
            }
            
            return "An error occurred during saving"
        } catch (e: Exception) {
            return "Error while saving the image: ${e.message}"
        }
    }
    
    private fun savePdfFromImage(imageUri: Uri, fileName: String): File {
        try {
           Log.d("DownloadWorker", "Starting PDF creation process")
            
            // Try two different approaches for PDF creation
            val isSuccess = try {
                // First method: Create PDF using PdfUtil
               Log.d("DownloadWorker", "Trying PdfUtil method")
                val pdfFile = PdfUtil.createPdfFromImage(context, imageUri, fileName)
               Log.d("DownloadWorker", "PdfUtil method succeeded: ${pdfFile.absolutePath}")
                return pdfFile
            } catch (e: Exception) {
               Log.e("DownloadWorker", "PdfUtil method failed: ${e.message}", e)
                false
            }
            
            if (isSuccess.not()) {
                // Second method: Use Android's built-in PDF API
               Log.d("DownloadWorker", "Trying Android built-in PDF method")
                
                // Create output directory
                val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                
                // Create SmartDocsConvert subfolder
                val smartDocsDownloadDir = File(outputDir, "SmartDocsConvert")
                if (!smartDocsDownloadDir.exists()) {
                    smartDocsDownloadDir.mkdirs()
                }
                
               Log.d("DownloadWorker", "Output directory: ${smartDocsDownloadDir.absolutePath}")
                
                val outputFile = File(smartDocsDownloadDir, fileName)
               Log.d("DownloadWorker", "Output file path: ${outputFile.absolutePath}")
                
                // Load the bitmap
                val bitmap = loadBitmapFromUri(imageUri)
                    ?: throw IllegalStateException("Failed to decode bitmap from URI")
                
                try {
                    // Create PDF
                    val document = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    // Image scaling
                    val imageWidth = bitmap.width
                    val imageHeight = bitmap.height
                    val scaleWidth = 595f / imageWidth
                    val scaleHeight = 842f / imageHeight
                    val scale = Math.min(scaleWidth, scaleHeight) * 0.85f
                    
                    val matrix = Matrix()
                    matrix.setScale(scale, scale)
                    matrix.postTranslate(
                        (595f - imageWidth * scale) / 2f,
                        (842f - imageHeight * scale) / 2f
                    )
                    
                    canvas.drawBitmap(bitmap, matrix, null)
                    document.finishPage(page)
                    
                    FileOutputStream(outputFile).use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    
                    document.close()
                    
                    return outputFile
                } finally {
                    bitmap.recycle()
                }
            }
            
            throw IllegalStateException("Both PDF creation methods failed")
        } catch (e: Exception) {
           Log.e("DownloadWorker", "Error in savePdfFromImage: ${e.message}", e)
            throw e
        }
    }
    
    private fun loadBitmapFromUri(imageUri: Uri): Bitmap? {
        try {
            // First check the image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(imageUri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }
            
           Log.d("DownloadWorker", "Original image dimensions: ${options.outWidth}x${options.outHeight}")
            
            // Sampling for large images
            options.inJustDecodeBounds = false
            if (options.outHeight > 2048 || options.outWidth > 2048) {
                val sampleSize = Math.max(
                    options.outHeight / 2048,
                    options.outWidth / 2048
                )
                options.inSampleSize = sampleSize
               Log.d("DownloadWorker", "Using sample size $sampleSize for large image")
            }
            
            // Memory optimization
            options.inPreferredConfig = Bitmap.Config.RGB_565
            
            // Load the bitmap
            return context.contentResolver.openInputStream(imageUri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
           Log.e("DownloadWorker", "Error loading bitmap: ${e.message}", e)
            return null
        }
    }
} 