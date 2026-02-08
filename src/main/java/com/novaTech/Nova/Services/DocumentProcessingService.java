package com.novaTech.Nova.Services;


import com.novaTech.Nova.DTO.DocumentProcessRequest;
import com.novaTech.Nova.DTO.DocumentProcessResponse;
import com.novaTech.Nova.DTO.ProcessingHistoryResponse;
import com.novaTech.Nova.Entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentProcessingService {

    /**
     * Process document with AI functionality (upload + process in one call) for a specific user
     */
    DocumentProcessResponse processDocument(MultipartFile file, DocumentProcessRequest request, User user);

    /**
     * Process existing document with AI functionality (ensures document belongs to user)
     */
    DocumentProcessResponse processExistingDocument(Long documentId, DocumentProcessRequest request, User user);

    /**
     * Get processing history for a specific document (ensures document belongs to user)
     */
    List<ProcessingHistoryResponse> getDocumentHistory(Long documentId, User user) throws Exception;

    /**
     * Get all processing history for a specific user
     */
    List<ProcessingHistoryResponse> getUserHistory(User user);

    /**
     * Get processing history by functionality type for a specific user
     */
    List<ProcessingHistoryResponse> getUserHistoryByFunctionality(User user, String functionalityType);

    // Admin methods (all users)
    List<ProcessingHistoryResponse> getAllHistory();
    List<ProcessingHistoryResponse> getHistoryByFunctionality(String functionalityType);
}