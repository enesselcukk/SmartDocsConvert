package com.example.smartdocsconvert.ui.navigation

import com.example.smartdocsconvert.R

sealed class Screen(
    val route: String,
    val title: String,
    val icon: Int
) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object FileConverter : Screen("convert_file", "Convert File", 0)
    object SelectedImage : Screen("selected_image", "Selected Image", 0) {
        fun selectedImageFilterRoute(imageUris: String) = "selected_image/$imageUris"
    }
    object Filter : Screen("filter", "Filter", 0) {
        fun createRoute(selectedImages: String) = "filter/$selectedImages"
    }

    object SelectPermissions : Screen("select_permissions", "Select Permissions", 0)

} 