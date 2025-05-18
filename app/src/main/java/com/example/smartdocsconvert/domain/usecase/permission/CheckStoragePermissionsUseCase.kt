package com.example.smartdocsconvert.domain.usecase.permission

import com.example.smartdocsconvert.domain.repository.PermissionRepository
import javax.inject.Inject

class CheckStoragePermissionsUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    operator fun invoke(): Boolean = permissionRepository.hasStoragePermissions()
} 