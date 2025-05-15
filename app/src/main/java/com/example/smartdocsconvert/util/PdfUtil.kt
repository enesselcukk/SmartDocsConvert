package com.example.smartdocsconvert.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.util.Log
import android.os.Environment

object PdfUtil {
    private const val TAG = "PdfUtil"
    
    fun createPdfFromImage(context: Context, imageUri: Uri, outputFileName: String): File {
        Log.d(TAG, "Starting PDF creation: $outputFileName for URI: $imageUri")
        
        // Save to public Downloads directory
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        
        // Create SmartDocsConvert subfolder if it doesn't exist
        val smartDocsDownloadDir = File(outputDir, "SmartDocsConvert")
        if (!smartDocsDownloadDir.exists()) {
            smartDocsDownloadDir.mkdirs()
        }
        
        Log.d(TAG, "Using output directory: ${smartDocsDownloadDir.absolutePath}")
        val outputFile = File(smartDocsDownloadDir, outputFileName)
        
        // Örnekleme yaparak bitmap yükle
        val bitmap = loadBitmapEfficiently(context, imageUri)
            ?: throw IllegalStateException("Failed to load bitmap from URI")
        
        Log.d(TAG, "Loaded bitmap: ${bitmap.width}x${bitmap.height}")
        
        try {
            // Bitmap'i byte array'e dönüştür
            val stream = ByteArrayOutputStream()
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            
            if (!compressed) {
                Log.w(TAG, "Bitmap compression returned false")
            }
            
            val bitmapData = stream.toByteArray()
            Log.d(TAG, "Compressed bitmap data: ${bitmapData.size} bytes")
            
            // PDF oluştur
            val fileOutputStream = FileOutputStream(outputFile)
            val writer = PdfWriter(fileOutputStream)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            try {
                // Bitmap'i PDF'e ekle
                val image = Image(ImageDataFactory.create(bitmapData))
                
                // Sayfa boyutuna göre resmi ölçeklendir
                val pageWidth = document.pdfDocument.defaultPageSize.width - 50
                val pageHeight = document.pdfDocument.defaultPageSize.height - 50
                
                val imageWidth = image.imageWidth
                val imageHeight = image.imageHeight
                
                Log.d(TAG, "PDF page size: ${pageWidth}x${pageHeight}")
                Log.d(TAG, "Image size in PDF: ${imageWidth}x${imageHeight}")
                
                val ratio = Math.min(
                    pageWidth / imageWidth,
                    pageHeight / imageHeight
                )
                
                // Sayfa içinde uygun şekilde konumlandır
                val scaledWidth = imageWidth * ratio
                val scaledHeight = imageHeight * ratio
                
                val xPosition = (pageWidth - scaledWidth) / 2f + 25
                val yPosition = (pageHeight - scaledHeight) / 2f + 25
                
                Log.d(TAG, "Scale ratio: $ratio, position: $xPosition, $yPosition")
                
                image.scaleToFit(scaledWidth, scaledHeight)
                image.setFixedPosition(xPosition, yPosition)
                
                // Görüntüyü PDF'e ekle
                document.add(image)
                Log.d(TAG, "Image added to PDF")
                
            } finally {
                // Kaynakları temizle
                try {
                    document.close()
                    pdf.close()
                    writer.close()
                    fileOutputStream.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error while closing PDF resources", e)
                }
            }
            
            // Dosya oluşturuldu mu kontrol et
            if (outputFile.exists() && outputFile.length() > 0) {
                Log.d(TAG, "PDF created successfully: ${outputFile.absolutePath}")
                return outputFile
            } else {
                throw IllegalStateException("PDF file was not created properly")
            }
        } finally {
            // Bitmap'i temizle
            bitmap.recycle()
        }
    }
    
    // Bitmap'i verimli bir şekilde belleğe yükle
    private fun loadBitmapEfficiently(context: Context, imageUri: Uri): Bitmap? {
        try {
            // İlk geçiş: boyutları kontrol et
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            Log.d(TAG, "Original image size: ${options.outWidth}x${options.outHeight}")
            
            // Örnekleme oranını belirle
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 2048, 2048)
            
            if (options.inSampleSize > 1) {
                Log.d(TAG, "Using sample size: ${options.inSampleSize}")
            }
            
            // Bellek kullanımını azalt
            options.inPreferredConfig = Bitmap.Config.RGB_565
            
            // İkinci geçiş: bitmap'i yükle
            return context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap", e)
            return null
        }
    }
    
    // Örnekleme boyutu hesaplama
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }

    fun createPdfFromImages(context: Context, imageUris: List<Uri>, outputFileName: String): File {
        Log.d(TAG, "Creating PDF from ${imageUris.size} images")
        
        // Save to public Downloads directory
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        
        // Create SmartDocsConvert subfolder if it doesn't exist
        val smartDocsDownloadDir = File(outputDir, "SmartDocsConvert")
        if (!smartDocsDownloadDir.exists()) {
            smartDocsDownloadDir.mkdirs()
        }
        
        val outputFile = File(smartDocsDownloadDir, outputFileName)
        
        var fileOutputStream: FileOutputStream? = null
        var writer: PdfWriter? = null
        var pdf: PdfDocument? = null
        var document: Document? = null
        
        try {
            fileOutputStream = FileOutputStream(outputFile)
            writer = PdfWriter(fileOutputStream)
            pdf = PdfDocument(writer)
            document = Document(pdf)
            
            imageUris.forEachIndexed { index, uri ->
                Log.d(TAG, "Processing image ${index + 1}/${imageUris.size}")
                val bitmap = loadBitmapEfficiently(context, uri)
                    ?: throw IllegalStateException("Failed to load bitmap for image ${index + 1}")
                
                try {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val bitmapData = stream.toByteArray()
                    
                    val image = Image(ImageDataFactory.create(bitmapData))
                    
                    val pageWidth = pdf!!.defaultPageSize.width - 50
                    val pageHeight = pdf!!.defaultPageSize.height - 50
                    
                    val imageWidth = image.imageWidth
                    val imageHeight = image.imageHeight
                    
                    val ratio = Math.min(
                        pageWidth / imageWidth,
                        pageHeight / imageHeight
                    )
                    
                    image.scaleToFit(imageWidth * ratio, imageHeight * ratio)
                    image.setFixedPosition(
                        (pageWidth - (imageWidth * ratio)) / 2 + 25,
                        (pageHeight - (imageHeight * ratio)) / 2 + 25
                    )
                    
                    document!!.add(image)
                    
                    // Sadece son resim değilse yeni sayfa ekle
                    if (index < imageUris.size - 1) {
                        document!!.add(AreaBreak())
                    }
                    
                    // Kaynakları temizle
                    stream.close()
                } finally {
                    bitmap.recycle()
                }
            }
            
            Log.d(TAG, "PDF with multiple images created successfully")
            return outputFile
            
        } finally {
            // Tüm kaynakları düzgünce kapat
            try {
                document?.close()
                pdf?.close()
                writer?.close()
                fileOutputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error while closing PDF resources", e)
            }
        }
    }
} 