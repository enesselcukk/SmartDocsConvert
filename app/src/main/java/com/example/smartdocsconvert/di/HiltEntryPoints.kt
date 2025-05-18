package com.example.smartdocsconvert.di

import com.example.smartdocsconvert.util.PermissionHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PermissionHelperEntryPoint {
    fun permissionHelper(): PermissionHelper
} 