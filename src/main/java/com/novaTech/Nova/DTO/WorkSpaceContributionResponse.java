// WorkSpaceContributionResponse.java (DTO)
package com.novaTech.Nova.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkSpaceContributionResponse {
    private Long contributionId;
    private Long workspaceId;
    private String workspaceName;
    private String contributorName;
    private String contributorEmail;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String status;
    private String rejectionReason;
    private String message;
}