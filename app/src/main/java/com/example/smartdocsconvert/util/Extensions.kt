package com.example.smartdocsconvert.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun Context.getUri(path:String): Uri {
    val file = File(path)
    return FileProvider.getUriForFile(
        this,
        "${this.packageName}.provider",
        file
    )
}