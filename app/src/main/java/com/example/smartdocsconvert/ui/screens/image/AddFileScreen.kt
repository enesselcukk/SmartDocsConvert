package com.example.smartdocsconvert.ui.screens.image

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartdocsconvert.R
import com.example.smartdocsconvert.ui.viewmodel.FileViewModel
import com.example.smartdocsconvert.ui.viewmodel.FileUiState
import androidx.core.content.FileProvider
import java.io.File
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.draw.clip
import com.example.smartdocsconvert.util.NotificationUtil
import androidx.navigation.NavController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFileScreen(
    viewModel: FileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    navController: NavController
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showWarningDialog by remember { mutableStateOf(false) }
    var tempUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Encode all selected URIs
            val encodedUris = uris.map { uri ->
                URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
            }
            // Join them with a delimiter
            val urisString = encodedUris.joinToString(",")
            navController.navigate("image_editor/$urisString")
        }
    }

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasPermissions = permissionsMap.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissions)
        }
    }

    // Uyarı Dialog'u
    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = {
                Text("Uyarı")
            },
            text = {
                Text("En fazla 5 dosya seçebilirsiniz. İlk 5 dosya seçilecek.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedUris = tempUris
                        showWarningDialog = false
                        if (selectedUris.isNotEmpty()) {
                            viewModel.onImagesSelected(selectedUris, context)
                        }
                        onNavigateToHome()
                    }
                ) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWarningDialog = false
                        selectedUris = emptyList()
                        tempUris = emptyList()
                    }
                ) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resim Ekle", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2A2A2A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is FileUiState.Loading -> {
                    LoadingDialog()
                }
                is FileUiState.Success -> {
                    ConversionSuccessContent(
                        file = (uiState as FileUiState.Success).file,
                        onOpenClick = { file ->
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                is FileUiState.Error -> {
                    Text("Error: ${(uiState as FileUiState.Error).message}")
                }
                FileUiState.Initial -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_empty_file),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Resim seçin (max 5)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { 
                                if (hasPermissions) {
                                    imagePicker.launch("image/*")
                                } else {
                                    permissionLauncher.launch(permissions)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A2A2A)
                            ),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text("Resim Seç", color = Color.White)
                            }
                        }

                        if (selectedUris.isNotEmpty()) {
                            Text(
                                text = "${selectedUris.size} Resim seçildi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingDialog() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(300.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Converting to PDF...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ConversionSuccessContent(
    file: File,
    onOpenClick: (File) -> Unit
) {
    val context = LocalContext.current

    // Bildirim kanalını oluştur
    LaunchedEffect(Unit) {
        NotificationUtil.createNotificationChannel(context)
    }

    PdfViewerScreen(
        pdfFile = file,
        onDownloadClick = { pdfFile ->
            // PDF'i indirme işlemi
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDir, pdfFile.name)
            
            pdfFile.copyTo(destinationFile, overwrite = true)
            
            // Bildirim göster
            NotificationUtil.showDownloadNotification(context, destinationFile)
            
            Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
        }
    )
}
