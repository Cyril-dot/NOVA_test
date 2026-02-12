package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.DocType;

public record ActiveProjectDocumentWorkSpaceDocs(
        Long id,
        String title,
        String description,
        String content,
        DocType docType,
        String projectName,
        String projectDocName,
        String projectDocDescription,
        String uploadedBy
) {
}
