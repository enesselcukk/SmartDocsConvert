package com.example.smartdocsconvert.domain.repository

import android.content.Context
import java.io.File


interface FileRepository {
    suspend fun getAllSupportedFiles(context: Context): List<File>
    suspend fun forceRefreshFiles(context: Context): List<File>
} 