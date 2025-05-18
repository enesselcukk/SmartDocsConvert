package com.example.smartdocsconvert.domain.usecase.document

import com.example.smartdocsconvert.data.model.DocumentModel
import java.io.File
import javax.inject.Inject


class AddDocumentUseCase @Inject constructor() {
    operator fun invoke(file: File, currentList: List<DocumentModel>): List<DocumentModel> {
        val newDocument = DocumentModel.fromFile(file)
        val updatedList = currentList.toMutableList()

        updatedList.removeIf { it.path == newDocument.path }
        updatedList.add(0, newDocument)
        return updatedList.take(20)
    }
} 