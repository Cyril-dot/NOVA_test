package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.DocType;

public record UserWorkSpaceDocs(
        Long id,
        String title,
        String description,
        String content,
        String username,
        DocType docType
) {
}
