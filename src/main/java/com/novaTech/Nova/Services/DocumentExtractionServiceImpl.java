package com.novaTech.Nova.Services;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class DocumentExtractionServiceImpl implements DocumentExtractionService {

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("pdf", "docx", "doc");

    @Override
    public String extractText(MultipartFile file) throws Exception {
        log.info("Extracting text from file: {}", file.getOriginalFilename());

        if (!isValidFileType(file)) {
            log.warn("Invalid file type for file: {}", file.getOriginalFilename());
            throw new IllegalArgumentException(
                    "Unsupported file type. Please upload PDF or DOCX files only."
            );
        }

        if (isPdfFile(file)) {
            log.debug("Detected PDF file: {}", file.getOriginalFilename());
            return extractTextFromPdf(file);
        } else if (isWordFile(file)) {
            log.debug("Detected Word file: {}", file.getOriginalFilename());
            return extractTextFromWord(file);
        } else {
            log.warn("Unsupported file type encountered: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("Unsupported file type");
        }
    }

    @Override
    public boolean isPdfFile(MultipartFile file) {
        String extension = getFileExtension(file);
        boolean result = "pdf".equalsIgnoreCase(extension);
        log.debug("isPdfFile check: {} -> {}", file.getOriginalFilename(), result);
        return result;
    }

    @Override
    public boolean isWordFile(MultipartFile file) {
        String extension = getFileExtension(file);
        boolean result = "docx".equalsIgnoreCase(extension) || "doc".equalsIgnoreCase(extension);
        log.debug("isWordFile check: {} -> {}", file.getOriginalFilename(), result);
        return result;
    }

    @Override
    public boolean isValidFileType(MultipartFile file) {
        String extension = getFileExtension(file);
        boolean result = SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
        log.debug("isValidFileType check: {} (ext: {}) -> {}", file.getOriginalFilename(), extension, result);
        return result;
    }

    @Override
    public String getFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            log.debug("File extension check: filename is null or empty");
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            log.debug("File extension check: no extension found for {}", filename);
            return "";
        }

        String extension = filename.substring(lastDotIndex + 1);
        log.debug("Extracted file extension: {} -> {}", filename, extension);
        return extension;
    }

    private String extractTextFromPdf(MultipartFile file) throws Exception {
        log.debug("Starting PDF text extraction: {}", file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            byte[] pdfBytes = inputStream.readAllBytes();
            log.debug("Read {} bytes from PDF file", pdfBytes.length);

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);

                String text = stripper.getText(document);

                log.info("Successfully extracted {} characters from PDF with {} pages",
                        text.length(), document.getNumberOfPages());

                return text.trim();
            }
        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", e.getMessage(), e);
            throw new Exception("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    private String extractTextFromWord(MultipartFile file) throws Exception {
        log.debug("Starting Word text extraction: {}", file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            StringBuilder text = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            log.debug("Word document contains {} paragraphs", paragraphs.size());

            for (XWPFParagraph paragraph : paragraphs) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                    text.append(paragraphText).append("\n");
                }
            }

            String extractedText = text.toString().trim();
            log.info("Successfully extracted {} characters from Word document with {} paragraphs",
                    extractedText.length(), paragraphs.size());

            return extractedText;
        } catch (Exception e) {
            log.error("Error extracting text from Word document: {}", e.getMessage(), e);
            throw new Exception("Failed to extract text from Word document: " + e.getMessage(), e);
        }
    }
}
