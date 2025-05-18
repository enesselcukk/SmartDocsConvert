package com.example.smartdocsconvert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.smartdocsconvert.ui.navigation.NavGraph
import com.example.smartdocsconvert.ui.theme.SmartDocsConvertTheme
import com.example.smartdocsconvert.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.app.ActivityCompat
import android.widget.Toast

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionRequested = mutableStateOf(false)
    private val storagePermissionRequested = mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Bildirim izni verildi")
        } else {
            Log.d("MainActivity", "Bildirim izni reddedildi")
        }
    }
    
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("MainActivity", "Tüm depolama izinleri verildi")
        } else {
            Log.d("MainActivity", "Bazı depolama izinleri reddedildi")
            
            // Android 11+ için tüm dosyalara erişim izni gerekiyorsa
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Dosya erişim ayarları açılamadı", e)
                    }
                }
            }
        }
    }
    
    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationUtil.createNotificationChannel(this)
        
        requestNotificationPermissionIfNeeded()
        requestStoragePermissionIfNeeded()
        
        setContent {
            SmartDocsConvertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (notificationPermissionRequested.value) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                Log.d("MainActivity", "Bildirim izni isteniyor")
            } else {
                Log.d("MainActivity", "Bildirim izni zaten verilmiş")
            }
        }
        
        notificationPermissionRequested.value = true
    }
    
    private fun requestStoragePermissionIfNeeded() {
        if (storagePermissionRequested.value) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ için
                val permissions = arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
                
                requestMultiplePermissionsLauncher.launch(permissions)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11-12 için
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Dosya erişim ayarları açılamadı", e)
                        
                        // Klasik izinleri iste
                        val permissions = arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        requestMultiplePermissionsLauncher.launch(permissions)
                    }
                }
            } else {
                // Android 10 ve altı için
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                requestMultiplePermissionsLauncher.launch(permissions)
            }
            
            storagePermissionRequested.value = true
        } catch (e: Exception) {
            Log.e("MainActivity", "İzin talep ederken beklenmeyen hata", e)
        }
    }
    
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ için
            Log.d("MainActivity", "Android 11+ için yetki kontrolü")
            try {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION_REQUEST_CODE
                    )
                }
                
                // Android 13+ için
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // READ_MEDIA_IMAGES, READ_MEDIA_VIDEO ve READ_MEDIA_AUDIO
                    val permissions = arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                    
                    val neededPermissions = permissions.filter {
                        ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                    }.toTypedArray()
                    
                    if (neededPermissions.isNotEmpty()) {
                        ActivityCompat.requestPermissions(
                            this,
                            neededPermissions,
                            STORAGE_PERMISSION_REQUEST_CODE
                        )
                    }
                }
                
                // Android 14+ (API 34) için
                if (Build.VERSION.SDK_INT >= 34) {
                    val documentPermissions = if (Build.VERSION.SDK_INT >= 34) {
                        try {
                            val readMediaDocumentsField = Manifest.permission::class.java.getField("READ_MEDIA_DOCUMENTS")
                            arrayOf(readMediaDocumentsField.get(null) as String)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "READ_MEDIA_DOCUMENTS izni bulunamadı", e)
                            emptyArray() // Bu izin bulunamamış gibi davran
                        }
                    } else {
                        emptyArray()
                    }
                    
                    if (documentPermissions.isNotEmpty()) {
                        val neededDocPermissions = documentPermissions.filter {
                            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                        }.toTypedArray()
                        
                        if (neededDocPermissions.isNotEmpty()) {
                            try {
                                ActivityCompat.requestPermissions(
                                    this, 
                                    neededDocPermissions,
                                    STORAGE_PERMISSION_REQUEST_CODE
                                )
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Doküman izni isteme hatası", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Yetki isteme hatası: ${e.message}")
            }
        } else {
            // Android 10 ve öncesi için
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Tüm yetkiler verildi
                Log.d("MainActivity", "Tüm yetkiler verildi")
            } else {
                // Bazı yetkiler reddedildi
                Toast.makeText(
                    this,
                    "Dosyalara erişim için gereken izinleri vermeniz gerekmektedir.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
