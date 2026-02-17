// WorkSpaceContribution.java (Entity)
package com.novaTech.Nova.Entities.workSpace;

import com.novaTech.Nova.Entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "workspace_contributions")
@AllArgsConstructor
@NoArgsConstructor
public class WorkSpaceContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkSpaceDocs workSpace;

    @ManyToOne
    @JoinColumn(name = "contributor_id", nullable = false)
    private User contributor;

    @Column(columnDefinition = "bytea")
    private byte[] contributionData;

    private LocalDateTime submittedAt;

    private boolean approved;

    private boolean pending;

    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
}