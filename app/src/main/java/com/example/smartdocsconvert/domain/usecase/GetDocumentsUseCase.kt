package com.example.smartdocsconvert.domain.usecase

import android.util.Log
import com.example.smartdocsconvert.data.model.DocumentModel
import com.example.smartdocsconvert.data.repository.DocumentRepository
import javax.inject.Inject

class GetDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(): List<DocumentModel> {
        val downloadedDocs = documentRepository.findDownloadedDocuments()
        return downloadedDocs
    }
} 