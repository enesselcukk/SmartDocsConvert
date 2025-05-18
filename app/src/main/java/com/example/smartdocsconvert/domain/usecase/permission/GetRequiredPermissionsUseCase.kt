package com.example.smartdocsconvert.domain.usecase.permission

import com.example.smartdocsconvert.domain.repository.PermissionRepository
import javax.inject.Inject

class GetRequiredPermissionsUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    operator fun invoke(): Array<String> = permissionRepository.getRequiredPermissions()
} 