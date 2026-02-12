package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.DocType;

import java.util.UUID;

public record ActiveWorkSpaceDocs(
        Long id,
        String title,
        String description,
        String content,
        DocType docType
) {
}
