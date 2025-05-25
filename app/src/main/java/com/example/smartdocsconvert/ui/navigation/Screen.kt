package com.example.smartdocsconvert.ui.navigation

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.example.smartdocsconvert.R
import java.io.File

/**
 * Uygulama içinde navigasyon için rota tanımlarını içerir
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: Int
) {
    object Home : Screen("home_screen", "Home", R.drawable.ic_home)
    object FileConverter : Screen("file_converter_screen", "Convert File", 0)
    object EditOptimize : Screen("edit_optimize_screen", "Edit & Optimize", 0) {
        fun createRoute(filePathsString: String): String {
            val encodedPaths = URLEncoder.encode(filePathsString, StandardCharsets.UTF_8.toString())
            return "$route/$encodedPaths"
        }
    }
    object SaveShare : Screen("save_share_screen", "Save & Share", 0) {
        fun createRoute(optimizedFiles: List<File>): String {
            val pathsString = optimizedFiles.joinToString(",") { it.absolutePath }
            val encodedPaths = URLEncoder.encode(pathsString, StandardCharsets.UTF_8.toString())
            return "$route/$encodedPaths"
        }
    }
    object SelectedImage : Screen("selected_image_screen", "Selected Image", 0) {
        fun selectedImageFilterRoute(imageUrisString: String): String {
            return "$route/$imageUrisString"
        }
    }
    object Filter : Screen("filter_screen", "Filter", 0) {
        fun createRoute(selectedImages: String): String {
            return "$route/$selectedImages"
        }
    }
    object SelectPermissions : Screen("select_permissions_screen", "Select Permissions", 0)
    object DocumentViewer : Screen("document_viewer_screen", "Document Viewer", 0) {
        fun createRoute(documentPath: String): String {
            val encodedPath = URLEncoder.encode(documentPath, StandardCharsets.UTF_8.toString())
            return "$route/$encodedPath"
        }
    }
} 