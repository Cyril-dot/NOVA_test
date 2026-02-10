package com.novaTech.Nova.Entities.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String fileType;

    private String mimeType;

    private Long fileSizeBytes;

    @Column(nullable = false)
    private Long uploadedById;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private String context; // PRIVATE_CHAT, TEAM_CHAT, MEETING

    private Long contextId;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}