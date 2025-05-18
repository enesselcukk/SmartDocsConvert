package com.example.smartdocsconvert.domain.usecase

import com.example.smartdocsconvert.data.repository.DocumentRepository
import javax.inject.Inject


class FormatFileSizeUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    operator fun invoke(size: Long): String {
        return documentRepository.formatFileSize(size)
    }
} 