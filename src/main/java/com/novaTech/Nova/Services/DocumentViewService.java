package com.novaTech.Nova.Services;

import com.novaTech.Nova.Entities.ProjectDocument;
import com.novaTech.Nova.Entities.repo.ProjectDocumentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "documentViews")
public class DocumentViewService {

    private final ProjectDocumentRepo projectDocumentRepo;

    @Transactional(readOnly = true)
    @Cacheable(key = "'doc:' + #documentId + '_user:' + #userId + '_content'")
    public byte[] getDocumentContent(UUID documentId, UUID userId) {
        log.info("Fetching document ID: {} for user ID: {}", documentId, userId);

        ProjectDocument document = projectDocumentRepo.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Check if user has access (is project owner)
        // Note: In a real app, you might want more granular permissions (e.g., team members)
        if (!document.getProject().getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to access this document");
        }

        if (!document.hasContent()) {
            throw new RuntimeException("Document has no content");
        }

        try {
            byte[] documentBytes = Base64.getDecoder().decode(document.getFileContent());
            log.info("Document retrieved successfully - ID: {}, Size: {} bytes", documentId, documentBytes.length);
            return documentBytes;
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode document ID: {}", documentId, e);
            throw new RuntimeException("Invalid document data");
        }
    }

    public MediaType determineMediaType(byte[] fileBytes, String mimeType) {
        if (mimeType != null) {
            try {
                return MediaType.parseMediaType(mimeType);
            } catch (Exception e) {
                log.warn("Failed to parse MIME type: {}", mimeType);
            }
        }

        // Fallback: check file signature
        if (fileBytes != null && fileBytes.length >= 4) {
            // PDF: %PDF (25 50 44 46)
            if (fileBytes[0] == 0x25 && fileBytes[1] == 0x50 &&
                    fileBytes[2] == 0x44 && fileBytes[3] == 0x46) {
                return MediaType.APPLICATION_PDF;
            }

            // ZIP-based formats (DOCX, PPTX, XLSX): PK (50 4B)
            if (fileBytes[0] == 0x50 && fileBytes[1] == 0x4B) {
                return MediaType.parseMediaType("application/octet-stream");
            }
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
