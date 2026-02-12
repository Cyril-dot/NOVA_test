package com.novaTech.Nova.Entities.Enums;

public enum DocType {

    PDF("application/pdf", ".pdf"),
    WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    ZIP("application/zip", ".zip"),

    HTML("text/html", ".html"),
    CSS("text/css", ".css"),
    JS("application/javascript", ".js"),

    JAVA("text/x-java-source", ".java"),
    JSON("application/json", ".json"),
    XML("application/xml", ".xml"),
    TEXT("text/plain", ".txt"),
    MARKDOWN("text/markdown", ".md"),

    PYTHON("text/x-python", ".py"),
    C("text/x-c", ".c"),
    C_SHARP("text/x-csharp", ".cs"),
    C_PLUS_PLUS("text/x-c++", ".cpp"),
    RUBY("text/x-ruby", ".rb"),
    PHP("application/x-httpd-php", ".php"),
    SWIFT("text/x-swift", ".swift"),
    GO("text/x-go", ".go"),
    R("text/x-r", ".r"),
    KOTLIN("text/x-kotlin", ".kt"),
    SCALA("text/x-scala", ".scala"),
    TYPESCRIPT("application/typescript", ".ts"),

    SQL("application/sql", ".sql"),
    NO_SQL("application/json", ".json");

    private final String mimeType;
    private final String extension;

    DocType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }
}
