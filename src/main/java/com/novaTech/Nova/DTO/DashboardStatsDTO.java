package com.novaTech.Nova.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalProjects;
    private long completedProjects;
    private long inProgressProjects;
    
    private long totalTasks;
    private long completedTasks;
    private long inProgressTasks;
}
