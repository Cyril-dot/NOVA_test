package com.novaTech.Nova.Services;

import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.Task;
import com.novaTech.Nova.Entities.repo.ProjectRepo;
import com.novaTech.Nova.Entities.repo.TaskRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final ProjectRepo projectRepo;
    private final TaskRepo taskRepo;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyNotifications() {
        log.info("Running daily notification scheduler...");

        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        // Process projects
        processProjectsDueSoon(today, threeDaysFromNow);
        processProjectsDueToday(today);
        processOverdueProjects(today);

        // Process tasks
        processTasksDueSoon(today, threeDaysFromNow);
        processTasksDueToday(today);
        processOverdueTasks(today);

        log.info("Daily notification scheduler finished.");
    }

    private void processProjectsDueSoon(LocalDate today, LocalDate threeDaysFromNow) {
        try {
            List<Project> projects = projectRepo.findByEndDateBetweenAndStatusNot(
                    today.plusDays(1),
                    threeDaysFromNow,
                    ProjectStatus.COMPLETED
            );
            log.info("Found {} projects due in 1-3 days.", projects.size());

            for (Project project : projects) {
                if (project.getEndDate().equals(threeDaysFromNow)) {
                    try {
                        notificationService.sendProjectDueSoonEmail(project.getUser(), project);
                        log.debug("Sent 'due soon' email for project: {}", project.getTitle());
                    } catch (Exception e) {
                        log.error("Failed to send 'due soon' email for project: {} to user: {}. Error: {}",
                                project.getTitle(),
                                project.getUser().getEmail(),
                                e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing projects due soon: {}", e.getMessage(), e);
        }
    }

    private void processProjectsDueToday(LocalDate today) {
        try {
            List<Project> projects = projectRepo.findByEndDateAndStatusNot(today, ProjectStatus.COMPLETED);
            log.info("Found {} projects due today.", projects.size());

            for (Project project : projects) {
                try {
                    notificationService.sendProjectDueDateEmail(project.getUser(), project);
                    log.debug("Sent 'due today' email for project: {}", project.getTitle());
                } catch (Exception e) {
                    log.error("Failed to send 'due today' email for project: {} to user: {}. Error: {}",
                            project.getTitle(),
                            project.getUser().getEmail(),
                            e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error processing projects due today: {}", e.getMessage(), e);
        }
    }

    private void processOverdueProjects(LocalDate today) {
        try {
            LocalDate yesterday = today.minusDays(1);
            List<Project> projects = projectRepo.findByEndDateAndStatusNot(yesterday, ProjectStatus.COMPLETED);

            log.info("Found {} newly overdue projects.", projects.size());

            for (Project project : projects) {
                try {
                    notificationService.sendProjectOverdueEmail(project.getUser(), project);
                    log.debug("Sent 'overdue' email for project: {}", project.getTitle());
                } catch (Exception e) {
                    log.error("Failed to send 'overdue' email for project: {} to user: {}. Error: {}",
                            project.getTitle(),
                            project.getUser().getEmail(),
                            e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error processing overdue projects: {}", e.getMessage(), e);
        }
    }

    private void processTasksDueSoon(LocalDate today, LocalDate threeDaysFromNow) {
        try {
            List<Task> tasks = taskRepo.findByDueDateBetweenAndStatusNot(
                    today.plusDays(1),
                    threeDaysFromNow,
                    TaskStatus.DONE
            );
            log.info("Found {} tasks due in 1-3 days.", tasks.size());

            for (Task task : tasks) {
                if (task.getDueDate().equals(threeDaysFromNow)) {
                    try {
                        notificationService.sendTaskDueSoonEmail(task.getUser(), task);
                        log.debug("Sent 'due soon' email for task: {}", task.getTitle());
                    } catch (Exception e) {
                        log.error("Failed to send 'due soon' email for task: {} to user: {}. Error: {}",
                                task.getTitle(),
                                task.getUser().getEmail(),
                                e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing tasks due soon: {}", e.getMessage(), e);
        }
    }

    private void processTasksDueToday(LocalDate today) {
        try {
            List<Task> tasks = taskRepo.findByDueDateAndStatusNot(today, TaskStatus.DONE);
            log.info("Found {} tasks due today.", tasks.size());

            for (Task task : tasks) {
                try {
                    notificationService.sendTaskDueDateEmail(task.getUser(), task);
                    log.debug("Sent 'due today' email for task: {}", task.getTitle());
                } catch (Exception e) {
                    log.error("Failed to send 'due today' email for task: {} to user: {}. Error: {}",
                            task.getTitle(),
                            task.getUser().getEmail(),
                            e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error processing tasks due today: {}", e.getMessage(), e);
        }
    }

    private void processOverdueTasks(LocalDate today) {
        try {
            LocalDate yesterday = today.minusDays(1);
            List<Task> tasks = taskRepo.findByDueDateAndStatusNot(yesterday, TaskStatus.DONE);

            log.info("Found {} newly overdue tasks.", tasks.size());

            for (Task task : tasks) {
                try {
                    notificationService.sendTaskOverdueEmail(task.getUser(), task);
                    log.debug("Sent 'overdue' email for task: {}", task.getTitle());
                } catch (Exception e) {
                    log.error("Failed to send 'overdue' email for task: {} to user: {}. Error: {}",
                            task.getTitle(),
                            task.getUser().getEmail(),
                            e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error processing overdue tasks: {}", e.getMessage(), e);
        }
    }
}