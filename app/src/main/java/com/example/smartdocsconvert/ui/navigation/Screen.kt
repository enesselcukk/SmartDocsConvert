package com.example.smartdocsconvert.ui.navigation

import com.example.smartdocsconvert.R

enum class Screen(
    val route: String,
    val title: String,
    val icon: Int
) {
    Home("home", "Home", R.drawable.ic_home),
    AddFile("add_file", "Add file", R.drawable.ic_add),
    Tools("tools", "Tools", R.drawable.ic_tools),
    ConvertFile("convert_file", "Convert File", 0)
} 