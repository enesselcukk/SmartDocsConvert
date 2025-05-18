package com.example.smartdocsconvert.domain.usecase.document

import com.example.smartdocsconvert.data.model.DocumentModel
import javax.inject.Inject


class ClearAllDocumentsUseCase @Inject constructor() {
    operator fun invoke(): List<DocumentModel> {
        return emptyList()
    }
} 