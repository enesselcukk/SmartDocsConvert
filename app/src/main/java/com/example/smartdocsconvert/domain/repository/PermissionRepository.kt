package com.example.smartdocsconvert.domain.repository

import android.content.Context

interface PermissionRepository {
    fun hasStoragePermissions(): Boolean
    fun getRequiredPermissions(): Array<String>
    fun openAppSettings(context: Context)
} 