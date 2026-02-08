package com.novaTech.Nova.Services;

import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.Task;
import com.novaTech.Nova.Entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;

    // Helper method for date formatting
    private String formatDate(LocalDate date) {
        if (date == null) return "No date set";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
        return date.format(formatter);
    }

    // Helper method for days calculation
    private String getDaysText(LocalDate dueDate) {
        if (dueDate == null) return "";
        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(today, dueDate);

        if (daysBetween == 0) return "today";
        else if (daysBetween == 1) return "tomorrow";
        else if (daysBetween > 0) return "in " + daysBetween + " days";
        else return Math.abs(daysBetween) + " days ago";
    }

    // ========================
    // PROJECT NOTIFICATIONS
    // ========================

    @Async
    public void sendProjectDueSoonEmail(User user, Project project) {
        String title = "Project Deadline Approaching: " + project.getTitle();
        String formattedDate = formatDate(project.getEndDate());
        String daysText = getDaysText(project.getEndDate());

        String body = "<div style='text-align: center; margin-bottom: 40px;'>" +
                "<div style='font-size: 48px; color: #f59e0b; margin-bottom: 20px;'>⏰</div>" +
                "<h1 style='color: #1e293b; margin-bottom: 15px; font-size: 28px; font-weight: 700;'>" +
                "Project Deadline Approaching" +
                "</h1>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); " +
                "color: white; padding: 8px 24px; border-radius: 50px; font-weight: 600; " +
                "font-size: 14px; letter-spacing: 0.5px; margin-bottom: 10px;'>" +
                project.getTitle() +
                "</div>" +
                "</div>" +

                "<div style='margin: 30px 0;'>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px; margin-bottom: 25px;'>" +
                "Hello <strong>" + user.getFirstName() + "</strong>,<br>" +
                "Your project deadline is approaching " + daysText + ". " +
                "Please review the project status and ensure all tasks are on track." +
                "</p>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); " +
                "border-radius: 16px; padding: 30px; margin: 40px 0; border: 2px solid #fde68a;'>" +
                "<div style='text-align: center;'>" +
                "<div style='font-size: 24px; color: #d97706; margin-bottom: 15px;'>📅</div>" +
                "<h3 style='color: #d97706; margin-bottom: 20px; font-size: 18px; font-weight: 600;'>" +
                "Deadline Details" +
                "</h3>" +
                "<div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px;'>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border-left: 4px solid #d97706;'>" +
                "<div style='font-size: 20px; color: #d97706; margin-bottom: 10px;'>🎯</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Project</div>" +
                "<div style='color: #1e293b; font-size: 16px; font-weight: 700;'>" + project.getTitle() + "</div>" +
                "</div>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border-left: 4px solid #d97706;'>" +
                "<div style='font-size: 20px; color: #d97706; margin-bottom: 10px;'>📅</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Due Date</div>" +
                "<div style='color: #d97706; font-size: 16px; font-weight: 700;'>" + formattedDate + "</div>" +
                "</div>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border-left: 4px solid #d97706;'>" +
                "<div style='font-size: 20px; color: #d97706; margin-bottom: 10px;'>⏳</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Time Remaining</div>" +
                "<div style='color: #d97706; font-size: 16px; font-weight: 700;'>" + daysText + "</div>" +
                "</div>" +

                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #0369a1; font-size: 24px;'>💡</div>" +
                "<div>" +
                "<div style='font-weight: 600; color: #0369a1; margin-bottom: 8px; font-size: 16px;'>" +
                "Recommended Actions" +
                "</div>" +
                "<div style='color: #475569; font-size: 14px; line-height: 1.6;'>" +
                "1. Review all project tasks and their status<br>" +
                "2. Check for any blocked or delayed tasks<br>" +
                "3. Update task priorities if needed<br>" +
                "4. Communicate with team members about the deadline<br>" +
                "5. Consider adjusting timeline if necessary" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 40px; padding-top: 25px; border-top: 1px solid #e2e8f0;'>" +
                "<div style='text-align: center;'>" +
                "<h4 style='color: #475569; margin-bottom: 15px; font-size: 16px; font-weight: 600;'>" +
                "Review Your Project Now" +
                "</h4>" +
                "<div style='color: #64748b; font-size: 14px; line-height: 1.6; margin-bottom: 20px;'>" +
                "Click below to access your project and make any necessary adjustments." +
                "</div>" +
                "<div style='display: inline-flex; gap: 15px; flex-wrap: wrap; justify-content: center;'>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>View Project</div>" +
                "<div style='display: inline-block; background: #f1f5f9; color: #475569; " +
                "padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px; border: 1px solid #cbd5e1;'>Task Status Report</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 30px; text-align: center; color: #64748b; font-size: 13px; line-height: 1.6;'>" +
                "This is an automated deadline reminder from NOVA SPACE.<br>" +
                "Need to adjust the deadline? Contact your project administrator." +
                "</div>";

        emailService.sendEmail(user.getEmail(), title, getStyledHtml(title, body));
    }

    @Async
    public void sendProjectDueDateEmail(User user, Project project) {
        String title = "Project Due Today: " + project.getTitle();
        String formattedDate = formatDate(project.getEndDate());

        String body = "<div style='text-align: center; margin-bottom: 40px;'>" +
                "<div style='font-size: 48px; color: #ef4444; margin-bottom: 20px;'>🚨</div>" +
                "<h1 style='color: #1e293b; margin-bottom: 15px; font-size: 28px; font-weight: 700;'>" +
                "Project Due Today" +
                "</h1>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 8px 24px; border-radius: 50px; font-weight: 600; " +
                "font-size: 14px; letter-spacing: 0.5px; margin-bottom: 10px;'>" +
                "URGENT - FINAL DAY" +
                "</div>" +
                "</div>" +

                "<div style='margin: 30px 0;'>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px; margin-bottom: 25px;'>" +
                "Hello <strong>" + user.getFirstName() + "</strong>,<br>" +
                "Your project <strong>" + project.getTitle() + "</strong> is due <strong>today</strong>. " +
                "This is the final day to complete any remaining work." +
                "</p>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px;'>" +
                "Please ensure all tasks are completed, documents are finalized, " +
                "and the project is ready for submission or review." +
                "</p>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%); " +
                "border-radius: 16px; padding: 30px; margin: 40px 0; border: 2px solid #fca5a5;'>" +
                "<div style='text-align: center;'>" +
                "<div style='font-size: 24px; color: #dc2626; margin-bottom: 15px;'>⏰</div>" +
                "<h3 style='color: #dc2626; margin-bottom: 20px; font-size: 18px; font-weight: 600;'>" +
                "Final Day Checklist" +
                "</h3>" +
                "<div style='max-width: 500px; margin: 0 auto; text-align: left;'>" +
                "<div style='display: flex; align-items: center; margin-bottom: 12px;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>✓</div>" +
                "<span style='color: #475569;'>Verify all tasks are marked as complete</span>" +
                "</div>" +
                "<div style='display: flex; align-items: center; margin-bottom: 12px;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>✓</div>" +
                "<span style='color: #475569;'>Review and finalize all project documents</span>" +
                "</div>" +
                "<div style='display: flex; align-items: center; margin-bottom: 12px;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>✓</div>" +
                "<span style='color: #475569;'>Submit project deliverables if required</span>" +
                "</div>" +
                "<div style='display: flex; align-items: center;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>✓</div>" +
                "<span style='color: #475569;'>Update project status to 'Completed'</span>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0; border-left: 4px solid #f59e0b;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #d97706; font-size: 24px;'>⚠️</div>" +
                "<div>" +
                "<div style='font-weight: 700; color: #92400e; margin-bottom: 8px; font-size: 16px;'>" +
                "Immediate Attention Required" +
                "</div>" +
                "<div style='color: #78350f; font-size: 14px; line-height: 1.6;'>" +
                "If you cannot meet today's deadline, please contact your project manager immediately " +
                "to discuss extension options or next steps." +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 40px; padding-top: 25px; border-top: 1px solid #e2e8f0;'>" +
                "<div style='text-align: center;'>" +
                "<h4 style='color: #475569; margin-bottom: 15px; font-size: 16px; font-weight: 600;'>" +
                "Finalize Your Project" +
                "</h4>" +
                "<div style='color: #64748b; font-size: 14px; line-height: 1.6; margin-bottom: 20px;'>" +
                "Access your project to complete final tasks and update the project status." +
                "</div>" +
                "<div style='display: inline-flex; gap: 15px; flex-wrap: wrap; justify-content: center;'>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>Complete Project</div>" +
                "<div style='display: inline-block; background: #f1f5f9; color: #475569; " +
                "padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px; border: 1px solid #cbd5e1;'>Request Extension</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 30px; text-align: center; color: #64748b; font-size: 13px; line-height: 1.6;'>" +
                "This is an urgent deadline notification from NOVA SPACE.<br>" +
                "Please take immediate action to avoid project delays." +
                "</div>";

        emailService.sendEmail(user.getEmail(), title, getStyledHtml(title, body));
    }

    @Async
    public void sendProjectOverdueEmail(User user, Project project) {
        String title = "Project Overdue: " + project.getTitle();
        String formattedDate = formatDate(project.getEndDate());
        String daysText = getDaysText(project.getEndDate());

        String body = "<div style='text-align: center; margin-bottom: 40px;'>" +
                "<div style='font-size: 48px; color: #ef4444; margin-bottom: 20px;'>⚠️</div>" +
                "<h1 style='color: #1e293b; margin-bottom: 15px; font-size: 28px; font-weight: 700;'>" +
                "Project Deadline Missed" +
                "</h1>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 8px 24px; border-radius: 50px; font-weight: 600; " +
                "font-size: 14px; letter-spacing: 0.5px; margin-bottom: 10px;'>" +
                "OVERDUE - ACTION REQUIRED" +
                "</div>" +
                "</div>" +

                "<div style='margin: 30px 0;'>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px; margin-bottom: 25px;'>" +
                "Hello <strong>" + user.getFirstName() + "</strong>,<br>" +
                "Your project <strong>" + project.getTitle() + "</strong> is now <strong>overdue</strong>. " +
                "The deadline was " + formattedDate + " and has been missed by " + daysText + "." +
                "</p>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px;'>" +
                "Immediate action is required to address this delay and minimize impact." +
                "</p>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%); " +
                "border-radius: 16px; padding: 30px; margin: 40px 0; border: 2px solid #fca5a5;'>" +
                "<div style='text-align: center;'>" +
                "<div style='font-size: 24px; color: #dc2626; margin-bottom: 15px;'>🚨</div>" +
                "<h3 style='color: #dc2626; margin-bottom: 20px; font-size: 18px; font-weight: 600;'>" +
                "Critical Status Alert" +
                "</h3>" +
                "<div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px;'>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border: 2px solid #ef4444;'>" +
                "<div style='font-size: 20px; color: #ef4444; margin-bottom: 10px;'>📅</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Original Deadline</div>" +
                "<div style='color: #ef4444; font-size: 16px; font-weight: 700; text-decoration: line-through;'>" + formattedDate + "</div>" +
                "</div>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border: 2px solid #ef4444;'>" +
                "<div style='font-size: 20px; color: #ef4444; margin-bottom: 10px;'>⏰</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Time Overdue</div>" +
                "<div style='color: #ef4444; font-size: 16px; font-weight: 700;'>" + daysText + "</div>" +
                "</div>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border: 2px solid #ef4444;'>" +
                "<div style='font-size: 20px; color: #ef4444; margin-bottom: 10px;'>⚡</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Status</div>" +
                "<div style='color: #ef4444; font-size: 16px; font-weight: 700;'>OVERDUE</div>" +
                "</div>" +

                "</div>" +
                "<div style='color: #b91c1c; font-size: 14px; margin-top: 20px; font-weight: 600; padding: 10px; background: white; border-radius: 8px;'>" +
                "⚠️ This project delay may affect related work and deadlines" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0; border-left: 4px solid #f59e0b;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #d97706; font-size: 24px;'>📋</div>" +
                "<div>" +
                "<div style='font-weight: 700; color: #92400e; margin-bottom: 8px; font-size: 16px;'>" +
                "Immediate Next Steps" +
                "</div>" +
                "<div style='color: #78350f; font-size: 14px; line-height: 1.6;'>" +
                "1. Contact your project manager immediately<br>" +
                "2. Provide an updated completion estimate<br>" +
                "3. Submit any completed work immediately<br>" +
                "4. Document reasons for the delay<br>" +
                "5. Request formal extension if needed" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #0369a1; font-size: 24px;'>💡</div>" +
                "<div>" +
                "<div style='font-weight: 600; color: #0369a1; margin-bottom: 5px; font-size: 16px;'>" +
                "Damage Control Actions" +
                "</div>" +
                "<div style='color: #475569; font-size: 14px; line-height: 1.6;'>" +
                "• Update all stakeholders about the delay<br>" +
                "• Prioritize remaining critical tasks<br>" +
                "• Consider temporary workarounds<br>" +
                "• Review lessons learned for future projects" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 40px; padding-top: 25px; border-top: 1px solid #e2e8f0;'>" +
                "<div style='text-align: center;'>" +
                "<h4 style='color: #475569; margin-bottom: 15px; font-size: 16px; font-weight: 600;'>" +
                "Address This Issue Now" +
                "</h4>" +
                "<div style='color: #64748b; font-size: 14px; line-height: 1.6; margin-bottom: 20px;'>" +
                "Click below to update the project status and communicate with your team." +
                "</div>" +
                "<div style='display: inline-flex; gap: 15px; flex-wrap: wrap; justify-content: center;'>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>Update Project Status</div>" +
                "<div style='display: inline-block; background: #f1f5f9; color: #475569; " +
                "padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px; border: 1px solid #cbd5e1;'>Contact Manager</div>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>Submit Work</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 30px; text-align: center; color: #64748b; font-size: 13px; line-height: 1.6;'>" +
                "This is a critical overdue notification from NOVA SPACE.<br>" +
                "Immediate action is required to resolve this project delay." +
                "</div>";

        emailService.sendEmail(user.getEmail(), title, getStyledHtml(title, body));
    }

    // ========================
    // TASK NOTIFICATIONS
    // ========================

    @Async
    public void sendTaskDueSoonEmail(User user, Task task) {
        String title = "Task Deadline Approaching: " + task.getTitle();
        String formattedDate = formatDate(task.getDueDate());
        String daysText = getDaysText(task.getDueDate());

        String priorityIcon = "";
        String priorityColor = "";

        if (task.getPriority() != null) {
            switch(task.getPriority()) {
                case HIGH:
                    priorityIcon = "🚨";
                    priorityColor = "#ef4444";
                    break;
                case MEDIUM:
                    priorityIcon = "⚠️";
                    priorityColor = "#f59e0b";
                    break;
                case LOW:
                    priorityIcon = "📋";
                    priorityColor = "#10b981";
                    break;
            }
        }

        String body = "<div style='text-align: center; margin-bottom: 40px;'>" +
                "<div style='font-size: 48px; color: #f59e0b; margin-bottom: 20px;'>⏰</div>" +
                "<h1 style='color: #1e293b; margin-bottom: 15px; font-size: 28px; font-weight: 700;'>" +
                "Task Deadline Approaching" +
                "</h1>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); " +
                "color: white; padding: 8px 24px; border-radius: 50px; font-weight: 600; " +
                "font-size: 14px; letter-spacing: 0.5px; margin-bottom: 10px;'>" +
                task.getTitle() +
                "</div>" +
                "</div>" +

                "<div style='margin: 30px 0;'>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px; margin-bottom: 25px;'>" +
                "Hello <strong>" + user.getFirstName() + "</strong>,<br>" +
                "Your task deadline is approaching " + daysText + ". " +
                "Please ensure you complete the task by the due date." +
                "</p>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); " +
                "border-radius: 16px; padding: 30px; margin: 40px 0; border: 2px solid #fde68a;'>" +
                "<div style='text-align: center;'>" +
                "<div style='font-size: 24px; color: #d97706; margin-bottom: 15px;'>📋</div>" +
                "<h3 style='color: #d97706; margin-bottom: 20px; font-size: 18px; font-weight: 600;'>" +
                "Task Details" +
                "</h3>" +
                "<div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px;'>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border-left: 4px solid #d97706;'>" +
                "<div style='font-size: 20px; color: #d97706; margin-bottom: 10px;'>🎯</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Task</div>" +
                "<div style='color: #1e293b; font-size: 16px; font-weight: 700;'>" + task.getTitle() + "</div>" +
                "</div>" +

                (task.getPriority() != null ?
                        "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border-left: 4px solid " + priorityColor + ";'>" +
                                "<div style='font-size: 20px; margin-bottom: 10px;'>" + priorityIcon + "</div>" +
                                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Priority</div>" +
                                "<div style='color: " + priorityColor + "; font-size: 16px; font-weight: 700; text-transform: uppercase;'>" +
                                task.getPriority().toString() + "</div>" +
                                "</div>" : "") +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border-left: 4px solid #d97706;'>" +
                "<div style='font-size: 20px; color: #d97706; margin-bottom: 10px;'>📅</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Due Date</div>" +
                "<div style='color: #d97706; font-size: 16px; font-weight: 700;'>" + formattedDate + "</div>" +
                "</div>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border-left: 4px solid #d97706;'>" +
                "<div style='font-size: 20px; color: #d97706; margin-bottom: 10px;'>⏳</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Time Remaining</div>" +
                "<div style='color: #d97706; font-size: 16px; font-weight: 700;'>" + daysText + "</div>" +
                "</div>" +

                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #0369a1; font-size: 24px;'>💡</div>" +
                "<div>" +
                "<div style='font-weight: 600; color: #0369a1; margin-bottom: 8px; font-size: 16px;'>" +
                "Quick Completion Tips" +
                "</div>" +
                "<div style='color: #475569; font-size: 14px; line-height: 1.6;'>" +
                "1. Review task requirements and scope<br>" +
                "2. Break down remaining work into smaller steps<br>" +
                "3. Set aside dedicated time to work on this task<br>" +
                "4. Update task status as you make progress<br>" +
                "5. Ask for help if you're blocked or need clarification" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 40px; padding-top: 25px; border-top: 1px solid #e2e8f0;'>" +
                "<div style='text-align: center;'>" +
                "<h4 style='color: #475569; margin-bottom: 15px; font-size: 16px; font-weight: 600;'>" +
                "Work on Your Task Now" +
                "</h4>" +
                "<div style='color: #64748b; font-size: 14px; line-height: 1.6; margin-bottom: 20px;'>" +
                "Click below to access your task and update your progress." +
                "</div>" +
                "<div style='display: inline-flex; gap: 15px; flex-wrap: wrap; justify-content: center;'>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>View Task</div>" +
                "<div style='display: inline-block; background: #f1f5f9; color: #475569; " +
                "padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px; border: 1px solid #cbd5e1;'>Update Status</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 30px; text-align: center; color: #64748b; font-size: 13px; line-height: 1.6;'>" +
                "This is an automated task reminder from NOVA SPACE.<br>" +
                "Need more time? Contact your task assigner to discuss deadline adjustments." +
                "</div>";

        emailService.sendEmail(user.getEmail(), title, getStyledHtml(title, body));
    }

    @Async
    public void sendTaskDueDateEmail(User user, Task task) {
        String title = "Task Due Today: " + task.getTitle();
        String formattedDate = formatDate(task.getDueDate());

        String priorityIcon = "";
        String priorityColor = "";

        if (task.getPriority() != null) {
            switch(task.getPriority()) {
                case HIGH:
                    priorityIcon = "🚨";
                    priorityColor = "#ef4444";
                    break;
                case MEDIUM:
                    priorityIcon = "⚠️";
                    priorityColor = "#f59e0b";
                    break;
                case LOW:
                    priorityIcon = "📋";
                    priorityColor = "#10b981";
                    break;
            }
        }

        String body = "<div style='text-align: center; margin-bottom: 40px;'>" +
                "<div style='font-size: 48px; color: #ef4444; margin-bottom: 20px;'>📅</div>" +
                "<h1 style='color: #1e293b; margin-bottom: 15px; font-size: 28px; font-weight: 700;'>" +
                "Task Due Today" +
                "</h1>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 8px 24px; border-radius: 50px; font-weight: 600; " +
                "font-size: 14px; letter-spacing: 0.5px; margin-bottom: 10px;'>" +
                "FINAL DAY - COMPLETE TODAY" +
                "</div>" +
                "</div>" +

                "<div style='margin: 30px 0;'>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px; margin-bottom: 25px;'>" +
                "Hello <strong>" + user.getFirstName() + "</strong>,<br>" +
                "Your task <strong>" + task.getTitle() + "</strong> is due <strong>today</strong>. " +
                "This is the final day to complete this task." +
                "</p>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px;'>" +
                "Please ensure you complete all required work and mark the task as done before the end of the day." +
                "</p>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%); " +
                "border-radius: 16px; padding: 30px; margin: 40px 0; border: 2px solid #fca5a5;'>" +
                "<div style='text-align: center;'>" +
                "<div style='font-size: 24px; color: #dc2626; margin-bottom: 15px;'>⚡</div>" +
                "<h3 style='color: #dc2626; margin-bottom: 20px; font-size: 18px; font-weight: 600;'>" +
                "Today's Action Required" +
                "</h3>" +
                "<div style='max-width: 500px; margin: 0 auto; text-align: left;'>" +
                "<div style='display: flex; align-items: center; margin-bottom: 12px;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>1</div>" +
                "<span style='color: #475569;'>Complete all remaining work for this task</span>" +
                "</div>" +
                "<div style='display: flex; align-items: center; margin-bottom: 12px;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>2</div>" +
                "<span style='color: #475569;'>Attach any required documents or files</span>" +
                "</div>" +
                "<div style='display: flex; align-items: center; margin-bottom: 12px;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>3</div>" +
                "<span style='color: #475569;'>Add completion notes or comments</span>" +
                "</div>" +
                "<div style='display: flex; align-items: center;'>" +
                "<div style='background: #dc2626; color: white; width: 24px; height: 24px; " +
                "border-radius: 50%; display: flex; align-items: center; justify-content: center; " +
                "font-size: 12px; font-weight: bold; margin-right: 12px;'>4</div>" +
                "<span style='color: #475569;'>Mark task status as 'DONE'</span>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0; border-left: 4px solid #f59e0b;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #d97706; font-size: 24px;'>⏰</div>" +
                "<div>" +
                "<div style='font-weight: 700; color: #92400e; margin-bottom: 8px; font-size: 16px;'>" +
                "Time Management Tips" +
                "</div>" +
                "<div style='color: #78350f; font-size: 14px; line-height: 1.6;'>" +
                "• Block dedicated time in your schedule today<br>" +
                "• Focus on essential requirements first<br>" +
                "• Ask for help if you encounter obstacles<br>" +
                "• Submit partial work if full completion isn't possible" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 40px; padding-top: 25px; border-top: 1px solid #e2e8f0;'>" +
                "<div style='text-align: center;'>" +
                "<h4 style='color: #475569; margin-bottom: 15px; font-size: 16px; font-weight: 600;'>" +
                "Complete Your Task Now" +
                "</h4>" +
                "<div style='color: #64748b; font-size: 14px; line-height: 1.6; margin-bottom: 20px;'>" +
                "Access your task to complete final work and update the task status." +
                "</div>" +
                "<div style='display: inline-flex; gap: 15px; flex-wrap: wrap; justify-content: center;'>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>Complete Task</div>" +
                "<div style='display: inline-block; background: #f1f5f9; color: #475569; " +
                "padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px; border: 1px solid #cbd5e1;'>Request Extension</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 30px; text-align: center; color: #64748b; font-size: 13px; line-height: 1.6;'>" +
                "This is an urgent task deadline notification from NOVA SPACE.<br>" +
                "Please complete this task today to avoid missing the deadline." +
                "</div>";

        emailService.sendEmail(user.getEmail(), title, getStyledHtml(title, body));
    }

    @Async
    public void sendTaskOverdueEmail(User user, Task task) {
        String title = "Task Overdue: " + task.getTitle();
        String formattedDate = formatDate(task.getDueDate());
        String daysText = getDaysText(task.getDueDate());

        String priorityIcon = "";
        String priorityColor = "";

        if (task.getPriority() != null) {
            switch(task.getPriority()) {
                case HIGH:
                    priorityIcon = "🚨";
                    priorityColor = "#ef4444";
                    break;
                case MEDIUM:
                    priorityIcon = "⚠️";
                    priorityColor = "#f59e0b";
                    break;
                case LOW:
                    priorityIcon = "📋";
                    priorityColor = "#10b981";
                    break;
            }
        }

        String body = "<div style='text-align: center; margin-bottom: 40px;'>" +
                "<div style='font-size: 48px; color: #ef4444; margin-bottom: 20px;'>⚠️</div>" +
                "<h1 style='color: #1e293b; margin-bottom: 15px; font-size: 28px; font-weight: 700;'>" +
                "Task Deadline Missed" +
                "</h1>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 8px 24px; border-radius: 50px; font-weight: 600; " +
                "font-size: 14px; letter-spacing: 0.5px; margin-bottom: 10px;'>" +
                "OVERDUE - IMMEDIATE ACTION NEEDED" +
                "</div>" +
                "</div>" +

                "<div style='margin: 30px 0;'>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px; margin-bottom: 25px;'>" +
                "Hello <strong>" + user.getFirstName() + "</strong>,<br>" +
                "Your task <strong>" + task.getTitle() + "</strong> is now <strong>overdue</strong>. " +
                "The deadline was " + formattedDate + " and has been missed by " + daysText + "." +
                "</p>" +
                "<p style='color: #334155; line-height: 1.8; font-size: 16px;'>" +
                "This task delay may affect project timelines and team dependencies. " +
                "Immediate action is required." +
                "</p>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%); " +
                "border-radius: 16px; padding: 30px; margin: 40px 0; border: 2px solid #fca5a5;'>" +
                "<div style='text-align: center;'>" +
                "<div style='font-size: 24px; color: #dc2626; margin-bottom: 15px;'>⏰</div>" +
                "<h3 style='color: #dc2626; margin-bottom: 20px; font-size: 18px; font-weight: 600;'>" +
                "Task Status Alert" +
                "</h3>" +
                "<div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px;'>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border: 2px solid #ef4444;'>" +
                "<div style='font-size: 20px; color: #ef4444; margin-bottom: 10px;'>📅</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Original Due Date</div>" +
                "<div style='color: #ef4444; font-size: 16px; font-weight: 700; text-decoration: line-through;'>" + formattedDate + "</div>" +
                "</div>" +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border: 2px solid #ef4444;'>" +
                "<div style='font-size: 20px; color: #ef4444; margin-bottom: 10px;'>⏰</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Time Overdue</div>" +
                "<div style='color: #ef4444; font-size: 16px; font-weight: 700;'>" + daysText + "</div>" +
                "</div>" +

                (task.getPriority() != null ?
                        "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border: 2px solid " + priorityColor + ";'>" +
                                "<div style='font-size: 20px; margin-bottom: 10px;'>" + priorityIcon + "</div>" +
                                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Priority</div>" +
                                "<div style='color: " + priorityColor + "; font-size: 16px; font-weight: 700; text-transform: uppercase;'>" +
                                task.getPriority().toString() + "</div>" +
                                "</div>" : "") +

                "<div style='background: white; padding: 20px; border-radius: 12px; text-align: center; border: 2px solid #ef4444;'>" +
                "<div style='font-size: 20px; color: #ef4444; margin-bottom: 10px;'>⚡</div>" +
                "<div style='color: #475569; font-weight: 600; font-size: 14px; margin-bottom: 5px;'>Current Status</div>" +
                "<div style='color: #ef4444; font-size: 16px; font-weight: 700;'>OVERDUE</div>" +
                "</div>" +

                "</div>" +
                "<div style='color: #b91c1c; font-size: 14px; margin-top: 20px; font-weight: 600; padding: 10px; background: white; border-radius: 8px;'>" +
                "⚠️ This task delay may impact project progress and team dependencies" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0; border-left: 4px solid #f59e0b;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #d97706; font-size: 24px;'>📋</div>" +
                "<div>" +
                "<div style='font-weight: 700; color: #92400e; margin-bottom: 8px; font-size: 16px;'>" +
                "Required Immediate Actions" +
                "</div>" +
                "<div style='color: #78350f; font-size: 14px; line-height: 1.6;'>" +
                "1. Contact the task assigner immediately<br>" +
                "2. Provide a new completion estimate<br>" +
                "3. Submit any completed work immediately<br>" +
                "4. Explain reasons for the delay<br>" +
                "5. Request formal deadline extension if needed" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); " +
                "border-radius: 16px; padding: 25px; margin: 30px 0;'>" +
                "<div style='display: flex; align-items: flex-start; gap: 15px;'>" +
                "<div style='color: #0369a1; font-size: 24px;'>💡</div>" +
                "<div>" +
                "<div style='font-weight: 600; color: #0369a1; margin-bottom: 5px; font-size: 16px;'>" +
                "Quick Recovery Plan" +
                "</div>" +
                "<div style='color: #475569; font-size: 14px; line-height: 1.6;'>" +
                "• Focus on completing the most critical parts first<br>" +
                "• Work extra hours if necessary and feasible<br>" +
                "• Delegate subtasks if possible<br>" +
                "• Communicate progress updates regularly" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 40px; padding-top: 25px; border-top: 1px solid #e2e8f0;'>" +
                "<div style='text-align: center;'>" +
                "<h4 style='color: #475569; margin-bottom: 15px; font-size: 16px; font-weight: 600;'>" +
                "Address This Overdue Task Now" +
                "</h4>" +
                "<div style='color: #64748b; font-size: 14px; line-height: 1.6; margin-bottom: 20px;'>" +
                "Click below to update the task status and communicate with your team." +
                "</div>" +
                "<div style='display: inline-flex; gap: 15px; flex-wrap: wrap; justify-content: center;'>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>Update Task Status</div>" +
                "<div style='display: inline-block; background: #f1f5f9; color: #475569; " +
                "padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px; border: 1px solid #cbd5e1;'>Contact Assigner</div>" +
                "<div style='display: inline-block; background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); " +
                "color: white; padding: 12px 24px; border-radius: 8px; font-weight: 600; " +
                "font-size: 14px;'>Submit Work</div>" +
                "</div>" +
                "</div>" +
                "</div>" +

                "<div style='margin-top: 30px; text-align: center; color: #64748b; font-size: 13px; line-height: 1.6;'>" +
                "This is a critical overdue task notification from NOVA SPACE.<br>" +
                "Immediate action is required to address this task delay." +
                "</div>";

        emailService.sendEmail(user.getEmail(), title, getStyledHtml(title, body));
    }

    // You'll need to add this helper method to get the styled HTML template
    private String getStyledHtml(String title, String body) {
        // This should call your existing getStyledHtml method from EmailService
        // or you can move that method to this class
        // For now, I'll provide a simple wrapper - you should integrate with your existing method
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            background-color: #f4f4f4;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 20px auto;\n" +
                "            background-color: #ffffff;\n" +
                "            padding: 20px;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\n" +
                "        }\n" +
                "        .header {\n" +
                "            background-color: #667eea;\n" +
                "            color: #ffffff;\n" +
                "            padding: 10px;\n" +
                "            text-align: center;\n" +
                "            border-radius: 8px 8px 0 0;\n" +
                "        }\n" +
                "        .content {\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            text-align: center;\n" +
                "            padding: 10px;\n" +
                "            font-size: 12px;\n" +
                "            color: #888888;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'><h2>" + title + "</h2></div>\n" +
                "        <div class='content'>" + body + "</div>\n" +
                "        <div class='footer'><p>&copy; 2024 NOVA SPACE</p></div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}