package com.example.smartdocsconvert.data.repository

import android.content.Context
import com.example.smartdocsconvert.domain.repository.PermissionRepository
import com.example.smartdocsconvert.util.PermissionHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    private val permissionHelper: PermissionHelper
) : PermissionRepository {
    
    override fun hasStoragePermissions(): Boolean =
        permissionHelper.hasStoragePermissions()
    
    override fun getRequiredPermissions(): Array<String> =
        permissionHelper.getRequiredPermissions()
    
    override fun openAppSettings(context: Context) =
        permissionHelper.openAppSettings(context)
} 