package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.DocType;

public record ProjectDocumentWorkSpaceDocs(
        Long id,
        String title,
        String description,
        String content,
        DocType docType,
        String projectName,
        String projectDescription,
        String projectDocName,
        String projectDocDescription
) {
}
