package com.example.smartdocsconvert.domain.usecase.file

import android.content.Context
import com.example.smartdocsconvert.domain.repository.FileRepository
import java.io.File
import javax.inject.Inject


class ForceRefreshFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(context: Context): List<File> = 
        fileRepository.forceRefreshFiles(context)
} 