package com.example.smartdocsconvert.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

/**
 * Utility class for image processing operations like filtering, rotation, and effects.
 */
object ImageProcessingUtil {
    
    /**
     * Process an image with filters, brightness, contrast, and rotation
     * 
     * @param sourceBitmap The original bitmap to process
     * @param filterName The name of the filter to apply
     * @param brightness The brightness adjustment (1.0f is normal)
     * @param contrast The contrast adjustment (1.0f is normal)
     * @param rotationAngle The rotation angle in degrees
     * @return The processed bitmap
     */
    fun processImage(
        sourceBitmap: Bitmap,
        filterName: String,
        brightness: Float,
        contrast: Float,
        rotationAngle: Float
    ): Bitmap {
        // Apply filters and effects
        val colorMatrix = ColorMatrix()
        applyColorMatrixForFilter(
            colorMatrix,
            filterName,
            brightness,
            contrast
        )
        
        val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
        val paint = Paint().apply {
            colorFilter = colorMatrixFilter
        }
        
        // Apply rotation if needed
        val rotatedBitmap = if (rotationAngle != 0f) {
            val rotationMatrix = Matrix().apply {
                postRotate(rotationAngle)
            }
            Bitmap.createBitmap(
                sourceBitmap,
                0,
                0,
                sourceBitmap.width,
                sourceBitmap.height,
                rotationMatrix,
                true
            )
        } else {
            sourceBitmap
        }
        
        // Draw the processed image to a canvas
        val resultBitmap = Bitmap.createBitmap(
            rotatedBitmap.width,
            rotatedBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(rotatedBitmap, 0f, 0f, paint)
        
        // Clean up if a new bitmap was created for rotation
        if (rotatedBitmap != sourceBitmap) {
            rotatedBitmap.recycle()
        }
        
        return resultBitmap
    }
    
    /**
     * Apply a color matrix for the selected filter
     */
    private fun applyColorMatrixForFilter(
        colorMatrix: ColorMatrix,
        filterName: String,
        brightness: Float,
        contrast: Float
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
            
            "Moon" -> {
                val greyScale = ColorMatrix()
                greyScale.setSaturation(0.5f)
                
                val tint = ColorMatrix()
                tint.setScale(1.0f, 0.95f, 1.05f, 1.0f)
                
                val adjustedBrightness = ColorMatrix()
                adjustedBrightness.setScale(brightness, brightness, brightness, 1.0f)
                
                val adjustedContrast = ColorMatrix()
                val adjustedContrastValue = contrast * 1.1f
                val adjustedTranslate = (-.5f * adjustedContrastValue + .5f) * 255f
                adjustedContrast.set(floatArrayOf(
                    adjustedContrastValue, 0f, 0f, 0f, adjustedTranslate,
                    0f, adjustedContrastValue, 0f, 0f, adjustedTranslate,
                    0f, 0f, adjustedContrastValue, 0f, adjustedTranslate,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                colorMatrix.postConcat(greyScale)
                colorMatrix.postConcat(tint)
                colorMatrix.postConcat(adjustedBrightness)
                colorMatrix.postConcat(adjustedContrast)
            }
            
            "Lark" -> {
                val coolerTint = ColorMatrix()
                coolerTint.setScale(0.95f, 1.05f, 1.05f, 1.0f)
                
                val adjustedBrightness = ColorMatrix()
                val brightnessValue = brightness * 1.1f
                adjustedBrightness.setScale(brightnessValue, brightnessValue, brightnessValue, 1.0f)
                
                val adjustedContrast = ColorMatrix()
                val adjustedContrastValue = contrast * 1.1f
                val adjustedTranslate = (-.5f * adjustedContrastValue + .5f) * 255f
                adjustedContrast.set(floatArrayOf(
                    adjustedContrastValue, 0f, 0f, 0f, adjustedTranslate,
                    0f, adjustedContrastValue, 0f, 0f, adjustedTranslate + 3f,
                    0f, 0f, adjustedContrastValue, 0f, adjustedTranslate,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                colorMatrix.postConcat(coolerTint)
                colorMatrix.postConcat(adjustedBrightness)
                colorMatrix.postConcat(adjustedContrast)
            }
            
            "Reyes" -> {
                val warmthMatrix = ColorMatrix()
                warmthMatrix.setScale(1.1f, 1.0f, 0.9f, 1.0f)
                
                val desaturate = ColorMatrix()
                desaturate.setSaturation(0.8f)
                
                val adjustedBrightness = ColorMatrix()
                val brightnessValue = brightness * 1.1f
                adjustedBrightness.setScale(brightnessValue, brightnessValue, brightnessValue, 1.0f)
                
                val adjustedContrast = ColorMatrix()
                val adjustedContrastValue = contrast * 0.9f
                val adjustedTranslate = (-.5f * adjustedContrastValue + .5f) * 255f
                adjustedContrast.set(floatArrayOf(
                    adjustedContrastValue, 0f, 0f, 0f, adjustedTranslate + 10f,
                    0f, adjustedContrastValue, 0f, 0f, adjustedTranslate + 10f,
                    0f, 0f, adjustedContrastValue, 0f, adjustedTranslate,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                colorMatrix.postConcat(warmthMatrix)
                colorMatrix.postConcat(desaturate)
                colorMatrix.postConcat(adjustedBrightness)
                colorMatrix.postConcat(adjustedContrast)
            }
            
            "Juno" -> {
                val warmthMatrix = ColorMatrix()
                warmthMatrix.setScale(1.05f, 1.0f, 0.95f, 1.0f)
                
                val contrastMatrix = ColorMatrix()
                val contrastValue = contrast * 1.2f
                val translate = (-.5f * contrastValue + .5f) * 255f
                contrastMatrix.set(floatArrayOf(
                    contrastValue, 0f, 0f, 0f, translate - 5f,
                    0f, contrastValue, 0f, 0f, translate,
                    0f, 0f, contrastValue, 0f, translate + 5f,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                val brightnessMatrix = ColorMatrix()
                val brightnessValue = brightness * 1.05f
                brightnessMatrix.setScale(brightnessValue, brightnessValue, brightnessValue, 1.0f)
                
                colorMatrix.postConcat(warmthMatrix)
                colorMatrix.postConcat(contrastMatrix)
                colorMatrix.postConcat(brightnessMatrix)
            }
            
            "Gingham" -> {
                val coolTint = ColorMatrix()
                coolTint.setScale(0.95f, 0.95f, 1.1f, 1.0f)
                
                val vintageLook = ColorMatrix()
                vintageLook.set(floatArrayOf(
                    0.9f, 0.1f, 0.0f, 0f, 0f,
                    0.07f, 0.9f, 0.03f, 0f, 0f,
                    0.0f, 0.2f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                val adjustedContrast = ColorMatrix()
                val adjustedContrastValue = contrast * 0.9f
                val adjustedTranslate = (-.5f * adjustedContrastValue + .5f) * 255f
                adjustedContrast.set(floatArrayOf(
                    adjustedContrastValue, 0f, 0f, 0f, adjustedTranslate + 5f,
                    0f, adjustedContrastValue, 0f, 0f, adjustedTranslate,
                    0f, 0f, adjustedContrastValue, 0f, adjustedTranslate + 5f,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                val adjustedBrightness = ColorMatrix()
                val brightnessValue = brightness * 1.05f
                adjustedBrightness.setScale(brightnessValue, brightnessValue, brightnessValue, 1.0f)
                
                colorMatrix.postConcat(coolTint)
                colorMatrix.postConcat(vintageLook)
                colorMatrix.postConcat(adjustedContrast)
                colorMatrix.postConcat(adjustedBrightness)
            }

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