package com.novaTech.Nova.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private String functionalityType; // SUMMARIZATION, QUESTION_ANSWERING, CHAT, MULTI_FEATURE

    @Column(columnDefinition = "TEXT")
    private String inputQuestion;

    @Column(columnDefinition = "TEXT")
    private String inputPrompt;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    // ✅ REMOVED modelUsed field

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column
    private Double confidenceScore;

    @Column(nullable = false)
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}