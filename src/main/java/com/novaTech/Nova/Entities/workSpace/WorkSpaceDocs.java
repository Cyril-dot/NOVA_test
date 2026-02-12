package com.novaTech.Nova.Entities.workSpace;

import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.ProjectDocument;
import com.novaTech.Nova.Entities.Team;
import com.novaTech.Nova.Entities.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "work_space")
@AllArgsConstructor
@NoArgsConstructor
public class WorkSpaceDocs {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    private byte[] workSpaceData;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastViewed;

    @Enumerated(EnumType.STRING)
    private DocType docType;

    // Linked to a user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)  // ✅ Only @JoinColumn
    private User user;

    // Linked to a team
    @ManyToOne
    @JoinColumn(name = "team_id", nullable = true)  // ✅ Only @JoinColumn
    private Team team;

    // Linked to a project
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = true)  // ✅ Only @JoinColumn
    private Project project;

    // Linked to a document
    @ManyToOne
    @JoinColumn(name = "project_document_id", nullable = true)  // ✅ Only @JoinColumn
    private ProjectDocument projectDocument;
}