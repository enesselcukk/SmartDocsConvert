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
    fun createPdfFromImage(context: Context, imageUri: Uri, outputFileName: String): File {
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val smartDocsDownloadDir = File(outputDir, "SmartDocsConvert")
        if (!smartDocsDownloadDir.exists()) {
            smartDocsDownloadDir.mkdirs()
        }

        val outputFile = File(smartDocsDownloadDir, outputFileName)
        val bitmap = loadBitmapEfficiently(context, imageUri) ?: throw IllegalStateException("Failed to load bitmap from URI")
        
        try {
            val stream = ByteArrayOutputStream()
            val bitmapData = stream.toByteArray()

            val fileOutputStream = FileOutputStream(outputFile)
            val writer = PdfWriter(fileOutputStream)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            try {
                val image = Image(ImageDataFactory.create(bitmapData))

                val pageWidth = document.pdfDocument.defaultPageSize.width - 50
                val pageHeight = document.pdfDocument.defaultPageSize.height - 50
                
                val imageWidth = image.imageWidth
                val imageHeight = image.imageHeight
                
                val ratio = (pageWidth / imageWidth).coerceAtMost(pageHeight / imageHeight)

                val scaledWidth = imageWidth * ratio
                val scaledHeight = imageHeight * ratio
                
                val xPosition = (pageWidth - scaledWidth) / 2f + 25
                val yPosition = (pageHeight - scaledHeight) / 2f + 25
                
                image.scaleToFit(scaledWidth, scaledHeight)
                image.setFixedPosition(xPosition, yPosition)

                document.add(image)
                
            } finally {
                try {
                    document.close()
                    pdf.close()
                    writer.close()
                    fileOutputStream.close()
                } catch (e: Exception) {
                    Log.e("PdfUtil", "Error while closing PDF resources", e)
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile
            } else {
                throw IllegalStateException("PDF file was not created properly")
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadBitmapEfficiently(context: Context, imageUri: Uri): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            options.inJustDecodeBounds = false
            options.inSampleSize =
                2048.calculateInSampleSize(options.outWidth, options.outHeight, 2048)
            options.inPreferredConfig = Bitmap.Config.RGB_565

            return context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun Int.calculateInSampleSize(width: Int, height: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        
        if (height > reqHeight || width > this) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= this) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }

    fun createPdfFromImages(context: Context, imageUris: List<Uri>, outputFileName: String): File {
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

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
                val bitmap = loadBitmapEfficiently(context, uri) ?: throw IllegalStateException("Failed to load bitmap for image ${index + 1}")
                
                try {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val bitmapData = stream.toByteArray()
                    
                    val image = Image(ImageDataFactory.create(bitmapData))
                    
                    val pageWidth = pdf.defaultPageSize.width - 50
                    val pageHeight = pdf.defaultPageSize.height - 50

                    val imageWidth = image.imageWidth
                    val imageHeight = image.imageHeight
                    
                    val ratio = (pageWidth / imageWidth).coerceAtMost(pageHeight / imageHeight)
                    
                    image.scaleToFit(imageWidth * ratio, imageHeight * ratio)
                    image.setFixedPosition(
                        (pageWidth - (imageWidth * ratio)) / 2 + 25,
                        (pageHeight - (imageHeight * ratio)) / 2 + 25
                    )
                    
                    document.add(image)

                    if (index < imageUris.size - 1) {
                        document.add(AreaBreak())
                    }

                    stream.close()
                } finally {
                    bitmap.recycle()
                }
            }

            return outputFile
            
        } finally {
            try {
                document?.close()
                pdf?.close()
                writer?.close()
                fileOutputStream?.close()
            } catch (e: Exception) {
                Log.e("pdfClose", "Error while closing PDF resources", e)
            }
        }
    }
} 