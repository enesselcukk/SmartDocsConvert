package com.example.smartdocsconvert.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.smartdocsconvert.R
import java.io.File
import androidx.core.content.FileProvider
import android.app.DownloadManager
import kotlin.math.absoluteValue

object NotificationUtil {
    private const val CHANNEL_ID = "pdf_download_channel"
    private const val NOTIFICATION_ID_BASE = 1000

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

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun showDownloadNotification(context: Context, file: File) {
        val notificationId = generateNotificationId(file.name)

        val fileName = file.name
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()
        val mimeType = when (fileExtension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "*/*"
        }

        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val fileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val folderIntent = if (fileExtension == "pdf") {
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
        }

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
            .setContentIntent(filePendingIntent)
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

        with(NotificationManagerCompat.from(context)) {
            try {
                if (hasNotificationPermission(context)) {
                    notify(notificationId, builder.build())
                } else {
                    openFile(context, fileIntent)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                openFile(context, fileIntent)
            }
        }
    }

    private fun generateNotificationId(filename: String): Int {
        val baseId = filename.hashCode().absoluteValue % 10000 + NOTIFICATION_ID_BASE

        var notificationId = baseId
        while (usedNotificationIds.contains(notificationId)) {
            notificationId++
        }
        usedNotificationIds.add(notificationId)

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