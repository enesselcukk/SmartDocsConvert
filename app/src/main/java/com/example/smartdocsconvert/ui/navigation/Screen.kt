package com.example.smartdocsconvert.ui.navigation

import com.example.smartdocsconvert.R

sealed class Screen(
    val route: String,
    val title: String,
    val icon: Int
) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object AddFile : Screen("add_file", "Add file", R.drawable.ic_add)
    object Tools : Screen("tools?files={files}", "Tools", R.drawable.ic_tools) {
        fun createRoute(files: List<String>) = "tools?files=${files.joinToString(",")}"
    }
    object FileConverter : Screen("convert_file", "Convert File", 0)
    object ImageFilter : Screen("image_filter", "Image Filter", 0) {
        fun createImageFilterRoute(imageUris: String) = "image_filter/$imageUris"
    }
    object ImageEditor : Screen("image_editor/{imageUris}", "Image Editor", 0) {
        fun createRoute(imageUris: String) = "image_editor/$imageUris"
    }
} 