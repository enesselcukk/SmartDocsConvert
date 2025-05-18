package com.example.smartdocsconvert.domain.usecase

import com.example.smartdocsconvert.domain.usecase.document.AddDocumentUseCase
import com.example.smartdocsconvert.domain.usecase.document.ClearAllDocumentsUseCase
import com.example.smartdocsconvert.domain.usecase.document.RemoveDocumentUseCase
import javax.inject.Inject


data class DocumentUseCases @Inject constructor(
    val getDocuments: GetDocumentsUseCase,
    val formatFileSize: FormatFileSizeUseCase,
    val addDocument: AddDocumentUseCase,
    val removeDocument: RemoveDocumentUseCase,
    val clearAllDocuments: ClearAllDocumentsUseCase
) 