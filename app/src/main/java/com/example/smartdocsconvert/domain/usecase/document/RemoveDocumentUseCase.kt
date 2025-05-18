package com.example.smartdocsconvert.domain.usecase.document

import com.example.smartdocsconvert.data.model.DocumentModel
import javax.inject.Inject


class RemoveDocumentUseCase @Inject constructor() {
    operator fun invoke(document: DocumentModel, currentList: List<DocumentModel>): List<DocumentModel> {
        return currentList.filter { it.id != document.id }
    }
} 