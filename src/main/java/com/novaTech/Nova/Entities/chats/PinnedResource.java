package com.novaTech.Nova.Entities.chats;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pinned_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String url;

    private String fileUrl;

    @Column(nullable = false)
    private Long pinnedById;

    @Column(nullable = false)
    private LocalDateTime pinnedAt;

    @PrePersist
    protected void onCreate() {
        pinnedAt = LocalDateTime.now();
    }
}