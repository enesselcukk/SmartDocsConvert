package com.example.smartdocsconvert.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
                val (bitmap, orientation) = loadBitmapWithOrientationInfo(uri) ?: return@withContext null
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
                }

                if (!matrix.isIdentity) {
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )

                    if (rotatedBitmap != bitmap) {
                        bitmap.recycle()
                        return@withContext rotatedBitmap
                    }
                }
                
                return@withContext bitmap
            } catch (e: Exception) {
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
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                }

                val exifOrientation = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val exif = ExifInterface(input)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

                val originalBitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                } ?: return@withContext null

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

                if (needsTransformation) {
                    if (rotationAngle == 90f || rotationAngle == 270f) {
                        matrix.postRotate(rotationAngle)
                        val rotatedBitmap = Bitmap.createBitmap(
                            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                        )
                        
                        if (rotatedBitmap != originalBitmap) {
                            bitmapToCrop = rotatedBitmap
                            originalBitmap.recycle()
                        }
                    }
                }

                adjustedCropX = adjustedCropX.coerceIn(0, bitmapToCrop.width - 1)
                adjustedCropY = adjustedCropY.coerceIn(0, bitmapToCrop.height - 1)
                adjustedCropWidth = adjustedCropWidth.coerceIn(1, bitmapToCrop.width - adjustedCropX)
                adjustedCropHeight = adjustedCropHeight.coerceIn(1, bitmapToCrop.height - adjustedCropY)

                val croppedBitmap = try {
                    Bitmap.createBitmap(
                        bitmapToCrop,
                        adjustedCropX,
                        adjustedCropY,
                        adjustedCropWidth,
                        adjustedCropHeight
                    )
                } catch (e: Exception) {
                    bitmapToCrop.recycle()
                    return@withContext null
                }
                if (bitmapToCrop != croppedBitmap && bitmapToCrop != originalBitmap) {
                    bitmapToCrop.recycle()
                }
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

                    ExifInterface(imageFile.path).apply {
                        setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                        saveAttributes()
                    }
                    
                    croppedBitmap.recycle()

                    return@withContext Uri.fromFile(imageFile)
                } catch (e: Exception) {
                    croppedBitmap.recycle()
                    return@withContext null
                }
            } catch (e: Exception) {
                return@withContext null
            }
        }
    }

    private fun loadBitmapWithOrientationInfo(uri: Uri): Pair<Bitmap, Int>? {
        try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
                try {
                    val exif = ExifInterface(input)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (e: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }
            } ?: ExifInterface.ORIENTATION_NORMAL

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
            }
            
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return null
            
            return Pair(bitmap, orientation)
        } catch (e: Exception) {
            return null
        }
    }
    
    suspend fun saveProcessedImage(
        sourceUri: Uri,
        filterName: String,
        brightness: Float,
        contrast: Float,
        rotationAngle: Float
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmap(sourceUri) ?: return@withContext "Görüntü yüklenemedi"

                val resultBitmap = com.example.smartdocsconvert.util.ImageProcessingUtil.processImage(
                    sourceBitmap = bitmap,
                    filterName = filterName,
                    brightness = brightness,
                    contrast = contrast,
                    rotationAngle = rotationAngle
                )

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "SmartDocsConvert_$timeStamp.jpg"

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

                        bitmap.recycle()
                        resultBitmap.recycle()
                        
                        return@withContext "Görüntü kaydedildi"
                    }
                } else {
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

                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(imageFile)
                    context.sendBroadcast(mediaScanIntent)
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

} 