package com.example.smartdocsconvert.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.smartdocsconvert.R
import java.io.File
import androidx.core.content.FileProvider
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.app.DownloadManager
import android.util.Log
import kotlin.math.absoluteValue

object NotificationUtil {
    private const val CHANNEL_ID = "pdf_download_channel"
    private const val NOTIFICATION_ID_BASE = 1000
    
    // Track used notification IDs to ensure uniqueness
    private val usedNotificationIds = mutableSetOf<Int>()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PDF Downloads"
            val descriptionText = "Notifications for downloaded PDF files"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Prior to Android 13, notification permission is granted automatically
        }
    }

    @SuppressLint("MissingPermission")
    fun showDownloadNotification(context: Context, file: File) {
        // Generate a unique notification ID based on the filename
        val notificationId = generateNotificationId(file.name)

        // Get file type and proper mime type
        val fileName = file.name
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()
        val mimeType = when (fileExtension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "*/*"
        }
        
        // Create URI for the file using FileProvider
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        // Intent to directly open the file
        val fileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Intent to open containing folder
        val folderIntent = if (fileExtension == "pdf") {
            // For PDFs, use the Downloads folder viewer
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            // For images, open gallery app
            Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
        }

        // Create pending intents
        val filePendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fileIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val folderPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            folderIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create notification with proper title based on file type
        val title = when (fileExtension) {
            "pdf" -> "PDF İndirildi"
            "jpg", "jpeg", "png" -> "Görüntü İndirildi"
            else -> "Dosya İndirildi"
        }
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title)
            .setContentText("${file.name} başarıyla indirildi")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(filePendingIntent) // Open file directly when notification is clicked
            .addAction(
                R.drawable.ic_file,
                "Dosyayı Aç",
                filePendingIntent
            )
            .addAction(
                R.drawable.ic_folder,
                "Klasörü Aç",
                folderPendingIntent
            )

        // Show notification if permission granted
        with(NotificationManagerCompat.from(context)) {
            try {
                // Check if notification permission is granted
                if (hasNotificationPermission(context)) {
                    notify(notificationId, builder.build())
                   Log.d("NotificationUtil", "Notification shown for file: ${file.name} with ID: $notificationId")
                } else {
                   Log.e("NotificationUtil", "Notification permission not granted")
                    openFile(context, fileIntent)
                }
            } catch (e: SecurityException) {
               Log.e("NotificationUtil", "Security exception showing notification: ${e.message}")
                e.printStackTrace()
                openFile(context, fileIntent)
            }
        }
    }
    
    /**
     * Generate a unique notification ID based on the filename
     */
    private fun generateNotificationId(filename: String): Int {
        // Use the hash code of the filename as a basis for the notification ID
        val baseId = filename.hashCode().absoluteValue % 10000 + NOTIFICATION_ID_BASE
        
        // Find an unused ID
        var notificationId = baseId
        while (usedNotificationIds.contains(notificationId)) {
            notificationId++
        }
        
        // Record this ID as used
        usedNotificationIds.add(notificationId)
        
        // If we have too many IDs recorded, remove the oldest ones
        if (usedNotificationIds.size > 100) {
            usedNotificationIds.clear()
            usedNotificationIds.add(notificationId)
        }
        
        return notificationId
    }
    
    private fun openFile(context: Context, intent: Intent) {
        try {
            // Bildirim gösterilemezse direkt dosya yöneticisini aç
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Son çare olarak indirilenler klasörünü aç
            try {
                val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                downloadsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(downloadsIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 