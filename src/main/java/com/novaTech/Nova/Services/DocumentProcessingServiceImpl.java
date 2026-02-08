package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Document;
import com.novaTech.Nova.Entities.Enums.FunctionalityType;
import com.novaTech.Nova.Entities.ProcessingHistory;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.ProcessingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private final DocumentService documentService;
    private final HuggingFaceAiService aiService;
    private final ProcessingHistoryRepository historyRepository;

    @Override
    @Transactional
    public DocumentProcessResponse processDocument(MultipartFile file, DocumentProcessRequest request, User user) {
        try {
            log.info("Starting processDocument for user: {}", user.getUsername());

            // Step 1: Upload document for this user
            log.info("Step 1: Uploading document for user: {}", user.getUsername());
            DocumentUploadResponse uploadResponse = documentService.uploadDocument(file, user);

            if (!uploadResponse.isSuccess()) {
                log.warn("Document upload failed for user: {}. Error: {}", user.getUsername(), uploadResponse.getError());
                return buildErrorResponse(uploadResponse.getError());
            }

            // Step 2: Process with AI
            log.info("Step 2: Processing with AI functionality: {} for user: {}",
                    request.getFunctionality(), user.getUsername());

            Document document = documentService.getDocumentById(uploadResponse.getDocumentId(), user);
            log.debug("Document retrieved successfully. ID: {}, Name: {}",
                    document.getId(), document.getFileName());

            return executeAiProcessing(document, request);

        } catch (Exception e) {
            log.error("Error in processDocument for user: {}", user.getUsername(), e);
            return buildErrorResponse("Error processing document: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DocumentProcessResponse processExistingDocument(Long documentId, DocumentProcessRequest request, User user) {
        try {
            log.info("Processing existing document ID: {} for user: {}", documentId, user.getUsername());

            // Ensure document belongs to this user
            Document document = documentService.getDocumentById(documentId, user);
            log.debug("Existing document retrieved. ID: {}, Name: {}", document.getId(), document.getFileName());

            return executeAiProcessing(document, request);

        } catch (Exception e) {
            log.error("Error in processExistingDocument for user: {}", user.getUsername(), e);
            return buildErrorResponse("Error processing document: " + e.getMessage());
        }
    }

    private DocumentProcessResponse executeAiProcessing(Document document, DocumentProcessRequest request) {
        String extractedText = document.getExtractedText();
        FunctionalityType functionality = request.getFunctionality();

        log.info("Executing AI processing. Document ID: {}, Functionality: {}, Text length: {}",
                document.getId(), functionality, extractedText.length());

        // ✅ Validate extracted text
        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("Document ID: {} has no extracted text", document.getId());
            return buildErrorResponse("Document has no text content to process");
        }

        ProcessingHistory history = ProcessingHistory.builder()
                .document(document)
                .functionalityType(functionality.name())
                .inputQuestion(request.getQuestion())
                .inputPrompt(request.getCustomPrompt())
                .processedAt(LocalDateTime.now())
                .build();

        try {
            AiModelResponse aiResponse;
            Map<String, Object> resultData = new HashMap<>();

            // Route to appropriate AI model
            switch (functionality) {
                case SUMMARIZATION:
                    log.info("Calling AI summarization for document ID: {}", document.getId());
                    aiResponse = aiService.summarize(extractedText);
                    resultData.put("summary", aiResponse.getResponse());
                    log.info("Summarization completed. Model: {}, Response length: {}",
                            aiResponse.getModelUsed(), aiResponse.getResponse().length());
                    break;

                case QUESTION_ANSWERING:
                    log.info("Calling AI question answering for document ID: {}", document.getId());
                    if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                        log.warn("Q&A requested without question for document ID: {}", document.getId());
                        return buildErrorResponse("Question is required for Q&A functionality");
                    }
                    aiResponse = aiService.answerQuestion(request.getQuestion(), extractedText);
                    resultData.put("question", request.getQuestion());
                    resultData.put("answer", aiResponse.getResponse());
                    if (aiResponse.getConfidenceScore() != null) {
                        resultData.put("confidence", aiResponse.getConfidenceScore());
                    }
                    log.info("Q&A completed. Model: {}, Confidence: {}",
                            aiResponse.getModelUsed(), aiResponse.getConfidenceScore());
                    break;

                case CHAT:
                    log.info("Calling AI chat for document ID: {}", document.getId());
                    if (request.getCustomPrompt() == null || request.getCustomPrompt().trim().isEmpty()) {
                        log.warn("Chat requested without prompt for document ID: {}", document.getId());
                        return buildErrorResponse("Custom prompt is required for chat functionality");
                    }
                    aiResponse = aiService.chat(request.getCustomPrompt(), extractedText);
                    resultData.put("prompt", request.getCustomPrompt());
                    resultData.put("response", aiResponse.getResponse());
                    log.info("Chat completed. Model: {}, Response length: {}",
                            aiResponse.getModelUsed(), aiResponse.getResponse().length());
                    break;

                case MULTI_FEATURE:
                    log.info("Calling AI multi-feature analysis for document ID: {}", document.getId());
                    aiResponse = aiService.multiFeatureAnalysis(extractedText, request.getCustomPrompt());
                    resultData.put("analysis", aiResponse.getResponse());
                    if (request.getCustomPrompt() != null) {
                        resultData.put("additionalFocus", request.getCustomPrompt());
                    }
                    log.info("Multi-feature analysis completed. Model: {}, Response length: {}",
                            aiResponse.getModelUsed(), aiResponse.getResponse().length());
                    break;

                default:
                    log.warn("Unsupported functionality type: {}", functionality);
                    return buildErrorResponse("Unsupported functionality type: " + functionality);
            }

            // Add common data
            resultData.put("documentId", document.getId());
            resultData.put("documentName", document.getFileName());
            resultData.put("extractedTextLength", extractedText.length());
            resultData.put("model", aiResponse.getModelUsed());

            // Save successful history
            history.setAiResponse(aiResponse.getResponse());
            history.setConfidenceScore(aiResponse.getConfidenceScore());
            history.setSuccess(true);
            history = historyRepository.save(history);

            log.info("AI processing completed successfully. Document ID: {}, Processing ID: {}, Model: {}",
                    document.getId(), history.getId(), aiResponse.getModelUsed());

            return DocumentProcessResponse.builder()
                    .success(true)
                    .message("Document processed successfully")
                    .documentId(document.getId())
                    .processingId(history.getId())
                    .functionality(functionality)
                    .modelUsed(aiResponse.getModelUsed())
                    .data(resultData)
                    .processedAt(history.getProcessedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error in AI processing for document ID: {}. Error: {}",
                    document.getId(), e.getMessage(), e);

            // Save failed history
            history.setSuccess(false);
            history.setErrorMessage(e.getMessage());
            historyRepository.save(history);

            return buildErrorResponse("AI processing failed: " + e.getMessage());
        }
    }

    @Override
    public List<ProcessingHistoryResponse> getDocumentHistory(Long documentId, User user) throws Exception {
        log.info("Fetching processing history for document ID: {} and user: {}",
                documentId, user.getUsername());

        // Ensure document belongs to this user
        Document document = documentService.getDocumentById(documentId, user);
        List<ProcessingHistory> histories = historyRepository.findByDocumentOrderByProcessedAtDesc(document);

        log.info("Found {} history records for document ID: {}", histories.size(), documentId);

        return histories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProcessingHistoryResponse> getUserHistory(User user) {
        log.info("Fetching processing history for user: {}", user.getUsername());

        List<ProcessingHistory> histories = historyRepository.findByUserOrderByProcessedAtDesc(user);
        log.info("Found {} history records for user: {}", histories.size(), user.getUsername());

        return histories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProcessingHistoryResponse> getUserHistoryByFunctionality(User user, String functionalityType) {
        log.info("Fetching history for user: {} with functionality: {}",
                user.getUsername(), functionalityType);

        // ✅ Validate functionality type
        try {
            FunctionalityType.valueOf(functionalityType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid functionality type requested: {}", functionalityType);
            return List.of(); // Return empty list for invalid types
        }

        List<ProcessingHistory> histories =
                historyRepository.findByUserAndFunctionalityTypeOrderByProcessedAtDesc(user, functionalityType);

        log.info("Found {} history records for user: {} and functionality: {}",
                histories.size(), user.getUsername(), functionalityType);

        return histories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Admin methods
    @Override
    public List<ProcessingHistoryResponse> getAllHistory() {
        log.info("Admin fetching all processing history");

        List<ProcessingHistory> histories = historyRepository.findAllByOrderByProcessedAtDesc();
        log.info("Admin retrieved {} total history records", histories.size());

        return histories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProcessingHistoryResponse> getHistoryByFunctionality(String functionalityType) {
        log.info("Admin fetching history by functionality: {}", functionalityType);

        // ✅ Validate functionality type
        try {
            FunctionalityType.valueOf(functionalityType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid functionality type requested: {}", functionalityType);
            return List.of(); // Return empty list for invalid types
        }

        List<ProcessingHistory> histories =
                historyRepository.findByFunctionalityTypeOrderByProcessedAtDesc(functionalityType);

        log.info("Admin retrieved {} history records for functionality: {}",
                histories.size(), functionalityType);

        return histories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ProcessingHistoryResponse convertToResponse(ProcessingHistory history) {
        return ProcessingHistoryResponse.builder()
                .id(history.getId())
                .documentId(history.getDocument().getId())
                .documentName(history.getDocument().getFileName())
                .functionalityType(FunctionalityType.valueOf(history.getFunctionalityType()))
                .inputQuestion(history.getInputQuestion())
                .inputPrompt(history.getInputPrompt())
                .aiResponse(history.getAiResponse())
                .modelUsed(null) // Model info not stored in history
                .processedAt(history.getProcessedAt())
                .confidenceScore(history.getConfidenceScore())
                .success(history.getSuccess())
                .errorMessage(history.getErrorMessage())
                .build();
    }

    private DocumentProcessResponse buildErrorResponse(String errorMessage) {
        log.warn("Building error response: {}", errorMessage);
        return DocumentProcessResponse.builder()
                .success(false)
                .error(errorMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }
}