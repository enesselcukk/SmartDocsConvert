package com.example.smartdocsconvert.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.smartdocsconvert.R
import java.io.File
import androidx.core.content.FileProvider
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.app.DownloadManager

object NotificationUtil {
    private const val CHANNEL_ID = "pdf_download_channel"
    private const val NOTIFICATION_ID = 1

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

    @SuppressLint("NewApi")
    fun showDownloadNotification(context: Context, file: File) {
        // İndirilenler klasörünü açmak için intent
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            // Downloads klasörünü başlangıç konumu olarak ayarla
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            )
        }

        // PDF dosyasını açmak için FileProvider kullan
        val pdfUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("PDF İndirildi")
            .setContentText("${file.name} başarıyla indirildi")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_file,
                "PDF'i Aç",
                PendingIntent.getActivity(
                    context,
                    1,
                    pdfIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
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
    }
} 