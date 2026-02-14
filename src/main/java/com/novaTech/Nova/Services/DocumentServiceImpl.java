package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.DocumentUploadResponse;
import com.novaTech.Nova.Entities.Document;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "documents")
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentExtractionService extractionService;
    private final EmailService emailService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'user:' + #user.id + '_documents'"),
            @CacheEvict(key = "'user:' + #user.id + '_documentCount'"),
            @CacheEvict(key = "'user:' + #user.id + '_status:' + 'COMPLETED' + '_documents'")
    })
    public DocumentUploadResponse uploadDocument(MultipartFile file, User user) throws Exception {
        log.info("Uploading document: {} for user: {}", file.getOriginalFilename(), user.getUsername());

        try {
            // Validate file
            if (file.isEmpty()) {
                log.warn("Upload failed: file is empty for user: {}", user.getUsername());
                throw new IllegalArgumentException("File is empty");
            }

            log.debug("Validating file type for: {}", file.getOriginalFilename());
            if (!extractionService.isValidFileType(file)) {
                log.warn("Unsupported file type: {} for user: {}", file.getOriginalFilename(), user.getUsername());
                throw new IllegalArgumentException("Unsupported file type. Only PDF and DOCX files are allowed.");
            }

            // Extract text
            log.debug("Starting text extraction for file: {}", file.getOriginalFilename());
            String extractedText = extractionService.extractText(file);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("Text extraction returned empty result for file: {}", file.getOriginalFilename());
                throw new Exception("No text could be extracted from the document");
            }

            log.debug("Text extraction successful. Length: {}", extractedText.length());

            // Create document entity
            Document document = Document.builder()
                    .user(user)
                    .fileName(file.getOriginalFilename())
                    .fileType(extractionService.getFileExtension(file))
                    .fileSize(file.getSize())
                    .extractedText(extractedText)
                    .uploadedAt(LocalDateTime.now())
                    .status("COMPLETED")
                    .build();

            log.debug("Saving document entity for file: {}", document.getFileName());

            // Save to database
            document = documentRepository.save(document);


            log.info("Document uploaded successfully with ID: {} for user: {}", document.getId(), user.getUsername());

            return DocumentUploadResponse.builder()
                    .success(true)
                    .message("Document uploaded and processed successfully")
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .fileType(document.getFileType())
                    .fileSize(document.getFileSize())
                    .extractedTextLength(extractedText.length())
                    .uploadedAt(document.getUploadedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error uploading document: {}", e.getMessage(), e);

            return DocumentUploadResponse.builder()
                    .success(false)
                    .error("Failed to upload document: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "'doc:' + #id + '_user:' + #user.id")
    public Document getDocumentById(Long id, User user) throws Exception {
        log.debug("Fetching document with ID: {} for user: {}", id, user.getUsername());
        return documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> {
                    log.warn("Document not found with ID: {} for user: {}", id, user.getUsername());
                    return new Exception("Document not found with ID: " + id + " for user: " + user.getUsername());
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #user.id + '_documents'")
    public List<Document> getUserDocuments(User user) {
        log.debug("Fetching all documents for user: {}", user.getUsername());
        List<Document> documents = documentRepository.findByUserOrderByUploadedAtDesc(user);
        log.info("Found {} documents for user: {}", documents.size(), user.getUsername());
        return documents;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'doc:' + #id + '_user:' + #user.id"),
            @CacheEvict(key = "'user:' + #user.id + '_documents'"),
            @CacheEvict(key = "'user:' + #user.id + '_documentCount'"),
            @CacheEvict(key = "'user:' + #user.id + '_status:*'", allEntries = true, beforeInvocation = true)
    })
    public void deleteDocument(Long id, User user) throws Exception {
        log.debug("Attempting to delete document with ID: {} for user: {}", id, user.getUsername());
        Document document = getDocumentById(id, user);
        documentRepository.delete(document);
        log.info("Document deleted with ID: {} for user: {}", id, user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #user.id + '_status:' + #status + '_documents'")
    public List<Document> getUserDocumentsByStatus(User user, String status) {
        log.debug("Fetching documents for user: {} with status: {}", user.getUsername(), status);
        List<Document> documents = documentRepository.findByUserAndStatusOrderByUploadedAtDesc(user, status);
        log.info("Found {} documents with status {} for user: {}", documents.size(), status, user.getUsername());
        return documents;
    }

    @Override
    @Cacheable(key = "'user:' + #user.id + '_documentCount'")
    public Long getUserDocumentCount(User user) {
        log.debug("Counting documents for user: {}", user.getUsername());
        Long count = documentRepository.countByUser(user);
        log.info("User {} has {} documents", user.getUsername(), count);
        return count;
    }

    // Admin methods
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "adminCache", key = "'allDocuments'")
    public List<Document> getAllDocuments() {
        log.debug("Fetching all documents (admin request)");
        List<Document> documents = documentRepository.findAllByOrderByUploadedAtDesc();
        log.info("Admin fetched total {} documents", documents.size());
        return documents;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "adminCache", key = "'doc:' + #id")
    public Document getDocumentByIdAdmin(Long id) throws Exception {
        log.debug("Admin fetching document with ID: {}", id);
        return documentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Admin requested non-existing document with ID: {}", id);
                    return new Exception("Document not found with ID: " + id);
                });
    }
}
