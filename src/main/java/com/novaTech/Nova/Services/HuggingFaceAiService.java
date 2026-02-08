package com.novaTech.Nova.Services;


import com.novaTech.Nova.DTO.AiModelResponse;

public interface HuggingFaceAiService {

    /**
     * Summarize text using BART model
     */
    AiModelResponse summarize(String text) throws Exception;

    /**
     * Answer question about context using RoBERTa model
     */
    AiModelResponse answerQuestion(String question, String context) throws Exception;

    /**
     * Interactive chat using instruction-tuned model
     */
    AiModelResponse chat(String prompt, String context) throws Exception;

    /**
     * Multi-feature analysis: summary + insights + key points
     */
    AiModelResponse multiFeatureAnalysis(String text, String additionalPrompt) throws Exception;
}