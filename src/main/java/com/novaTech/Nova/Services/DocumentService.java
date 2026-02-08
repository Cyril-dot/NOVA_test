package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.DocumentUploadResponse;
import com.novaTech.Nova.Entities.Document;
import com.novaTech.Nova.Entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    /**
     * Upload and extract text from document for a specific user
     */
    DocumentUploadResponse uploadDocument(MultipartFile file, User user) throws Exception;

    /**
     * Get document by ID (ensures it belongs to the user)
     */
    Document getDocumentById(Long id, User user) throws Exception;

    /**
     * Get all documents for a specific user
     */
    List<Document> getUserDocuments(User user);

    /**
     * Delete document by ID (ensures it belongs to the user)
     */
    void deleteDocument(Long id, User user) throws Exception;

    /**
     * Get documents by status for a specific user
     */
    List<Document> getUserDocumentsByStatus(User user, String status);

    /**
     * Get document count for user
     */
    Long getUserDocumentCount(User user);

    // Admin methods (all users)
    List<Document> getAllDocuments();
    Document getDocumentByIdAdmin(Long id) throws Exception;
}