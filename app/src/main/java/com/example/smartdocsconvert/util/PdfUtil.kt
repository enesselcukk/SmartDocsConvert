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

object PdfUtil {
    fun createPdfFromImage(context: Context, imageUri: Uri, outputFileName: String): File {
        // Çıktı dosyasını oluştur
        val outputDir = context.getExternalFilesDir(null)
        val outputFile = File(outputDir, outputFileName)

        // Uri'den bitmap oluştur
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Bitmap'i byte array'e dönüştür
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bitmapData = stream.toByteArray()

        // PDF oluştur
        PdfWriter(FileOutputStream(outputFile)).use { writer ->
            PdfDocument(writer).use { pdf ->
                Document(pdf).use { document ->
                    // Bitmap'i PDF'e ekle
                    val image = Image(ImageDataFactory.create(bitmapData))
                    
                    // Sayfa boyutuna göre resmi ölçeklendir
                    val pageWidth = document.pdfDocument.defaultPageSize.width - 50
                    val pageHeight = document.pdfDocument.defaultPageSize.height - 50
                    
                    val imageWidth = image.imageWidth
                    val imageHeight = image.imageHeight
                    
                    val ratio = Math.min(
                        pageWidth / imageWidth,
                        pageHeight / imageHeight
                    )
                    
                    image.scaleToFit(imageWidth * ratio, imageHeight * ratio)
                    
                    // Resmi sayfanın ortasına yerleştir
                    image.setFixedPosition(
                        (pageWidth - (imageWidth * ratio)) / 2 + 25,
                        (pageHeight - (imageHeight * ratio)) / 2 + 25
                    )
                    
                    document.add(image)
                }
            }
        }

        return outputFile
    }

    fun readPdfFile(context: Context, pdfUri: Uri): ByteArray {
        return context.contentResolver.openInputStream(pdfUri)?.use { 
            it.readBytes() 
        } ?: ByteArray(0)
    }

    fun createPdfFromImages(context: Context, imageUris: List<Uri>, outputFileName: String): File {
        val outputDir = context.getExternalFilesDir(null)
        val outputFile = File(outputDir, outputFileName)

        PdfWriter(FileOutputStream(outputFile)).use { writer ->
            PdfDocument(writer).use { pdf ->
                Document(pdf).use { document ->
                    imageUris.forEachIndexed { index, uri ->
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val bitmapData = stream.toByteArray()
                        
                        val image = Image(ImageDataFactory.create(bitmapData))
                        
                        val pageWidth = document.pdfDocument.defaultPageSize.width - 50
                        val pageHeight = document.pdfDocument.defaultPageSize.height - 50
                        
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
                        
                        document.add(image)
                        // Sadece son resim değilse yeni sayfa ekle
                        if (index < imageUris.size - 1) {
                            document.add(AreaBreak())
                        }
                    }
                }
            }
        }

        return outputFile
    }
} 