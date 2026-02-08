package com.novaTech.Nova.Services;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentExtractionService {

    /**
     * Extract text from PDF or Word document
     */
    String extractText(MultipartFile file) throws Exception;

    /**
     * Check if file is PDF
     */
    boolean isPdfFile(MultipartFile file);

    /**
     * Check if file is Word document
     */
    boolean isWordFile(MultipartFile file);

    /**
     * Validate file type
     */
    boolean isValidFileType(MultipartFile file);

    /**
     * Get file extension
     */
    String getFileExtension(MultipartFile file);
}