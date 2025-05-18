package com.example.smartdocsconvert.domain.usecase.permission

import android.content.Context
import com.example.smartdocsconvert.domain.repository.PermissionRepository
import javax.inject.Inject


class OpenAppSettingsUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    operator fun invoke(context: Context) = permissionRepository.openAppSettings(context)
} 