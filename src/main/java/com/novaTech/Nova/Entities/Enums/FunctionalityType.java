package com.novaTech.Nova.Entities.Enums;

public enum FunctionalityType {
    SUMMARIZATION("Summarize the document", "facebook/bart-large-cnn"),
    QUESTION_ANSWERING("Answer questions about the document", "deepset/roberta-base-squad2"),
    CHAT("Interactive chat about the document", "Qwen/Qwen2.5-Coder-32B-Instruct"),
    MULTI_FEATURE("Combined: Summarize + Answer Questions + Insights", "Qwen/Qwen2.5-Coder-32B-Instruct");

    private final String description;
    private final String defaultModel;

    FunctionalityType(String description, String defaultModel) {
        this.description = description;
        this.defaultModel = defaultModel;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}