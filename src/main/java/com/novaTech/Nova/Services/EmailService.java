package com.novaTech.Nova.Services;

import com.novaTech.Nova.Entities.Enums.Priority;
import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@novaspace.com", "NOVA SPACE");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("✅ Email sent successfully to: {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("❌ Failed to send email to: {}. Error: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("✅ Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to: {}", to, e);
        }
    }

    private String getStyledHtml(String title, String body) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;\n" +
                "            background-color: #f5f5f5;\n" +
                "            color: #1a1a1a;\n" +
                "            line-height: 1.6;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            background-color: #ffffff;\n" +
                "            border-radius: 8px;\n" +
                "            overflow: hidden;\n" +
                "            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);\n" +
                "        }\n" +
                "        .header {\n" +
                "            background-color: #1a1a1a;\n" +
                "            color: #ffffff;\n" +
                "            padding: 30px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .header .logo {\n" +
                "            font-size: 24px;\n" +
                "            font-weight: 700;\n" +
                "            letter-spacing: 2px;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            font-size: 24px;\n" +
                "            font-weight: 600;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "        .content {\n" +
                "            padding: 40px 30px;\n" +
                "            color: #333333;\n" +
                "        }\n" +
                "        .content p {\n" +
                "            margin-bottom: 16px;\n" +
                "            font-size: 15px;\n" +
                "            line-height: 1.6;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            background-color: #f8f8f8;\n" +
                "            padding: 20px 30px;\n" +
                "            text-align: center;\n" +
                "            border-top: 1px solid #e0e0e0;\n" +
                "        }\n" +
                "        .footer p {\n" +
                "            color: #666666;\n" +
                "            font-size: 13px;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "        .badge {\n" +
                "            display: inline-block;\n" +
                "            background-color: #2563eb;\n" +
                "            color: #ffffff;\n" +
                "            padding: 8px 16px;\n" +
                "            border-radius: 4px;\n" +
                "            font-size: 13px;\n" +
                "            font-weight: 600;\n" +
                "            margin: 10px 0;\n" +
                "        }\n" +
                "        .info-box {\n" +
                "            background-color: #f8f9fa;\n" +
                "            border-left: 4px solid #2563eb;\n" +
                "            padding: 16px;\n" +
                "            margin: 20px 0;\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        .warning-box {\n" +
                "            background-color: #fff3cd;\n" +
                "            border-left: 4px solid #ffc107;\n" +
                "            padding: 16px;\n" +
                "            margin: 20px 0;\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        .danger-box {\n" +
                "            background-color: #f8d7da;\n" +
                "            border-left: 4px solid #dc3545;\n" +
                "            padding: 16px;\n" +
                "            margin: 20px 0;\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        .success-box {\n" +
                "            background-color: #d4edda;\n" +
                "            border-left: 4px solid #28a745;\n" +
                "            padding: 16px;\n" +
                "            margin: 20px 0;\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        .button {\n" +
                "            display: inline-block;\n" +
                "            background-color: #2563eb;\n" +
                "            color: #ffffff;\n" +
                "            padding: 12px 24px;\n" +
                "            text-decoration: none;\n" +
                "            border-radius: 4px;\n" +
                "            font-weight: 600;\n" +
                "            font-size: 14px;\n" +
                "            margin: 10px 5px;\n" +
                "        }\n" +
                "        .button-secondary {\n" +
                "            background-color: #ffffff;\n" +
                "            color: #1a1a1a;\n" +
                "            border: 1px solid #d0d0d0;\n" +
                "        }\n" +
                "        .otp-code {\n" +
                "            display: inline-block;\n" +
                "            background-color: #f8f9fa;\n" +
                "            border: 2px dashed #d0d0d0;\n" +
                "            padding: 20px 30px;\n" +
                "            border-radius: 8px;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .otp-code .code {\n" +
                "            font-size: 36px;\n" +
                "            font-weight: 700;\n" +
                "            letter-spacing: 6px;\n" +
                "            color: #2563eb;\n" +
                "            font-family: monospace;\n" +
                "        }\n" +
                "        .grid {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));\n" +
                "            gap: 15px;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .grid-item {\n" +
                "            background-color: #f8f9fa;\n" +
                "            padding: 20px;\n" +
                "            border-radius: 8px;\n" +
                "            text-align: center;\n" +
                "            border: 1px solid #e0e0e0;\n" +
                "        }\n" +
                "        .grid-item i {\n" +
                "            font-size: 24px;\n" +
                "            color: #2563eb;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .grid-item h4 {\n" +
                "            color: #1a1a1a;\n" +
                "            font-size: 14px;\n" +
                "            font-weight: 600;\n" +
                "            margin-bottom: 5px;\n" +
                "        }\n" +
                "        .grid-item p {\n" +
                "            color: #666666;\n" +
                "            font-size: 12px;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "        ul {\n" +
                "            list-style: none;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        ul li {\n" +
                "            padding: 8px 0;\n" +
                "            padding-left: 24px;\n" +
                "            position: relative;\n" +
                "        }\n" +
                "        ul li:before {\n" +
                "            content: '\\f00c';\n" +
                "            font-family: 'Font Awesome 6 Free';\n" +
                "            font-weight: 900;\n" +
                "            position: absolute;\n" +
                "            left: 0;\n" +
                "            color: #2563eb;\n" +
                "        }\n" +
                "        @media (max-width: 600px) {\n" +
                "            .container {\n" +
                "                margin: 10px;\n" +
                "            }\n" +
                "            .content {\n" +
                "                padding: 20px;\n" +
                "            }\n" +
                "            .grid {\n" +
                "                grid-template-columns: 1fr;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <div class='header'>\n" +
                "            <div class='logo'><i class='fas fa-rocket'></i> NOVA SPACE</div>\n" +
                "            <h1>" + title + "</h1>\n" +
                "        </div>\n" +
                "        <div class='content'>\n" +
                body +
                "        </div>\n" +
                "        <div class='footer'>\n" +
                "            <p>&copy; 2026 NOVA SPACE. All rights reserved.</p>\n" +
                "            <p style='margin-top: 8px;'><i class='fas fa-check-circle'></i> Professional Task Management Platform</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    @Async
    public void sendOtpEmail(String to, Integer otp) {
        String title = "Verification Code";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px; font-size: 20px;'>" +
                "<i class='fas fa-shield-alt'></i> Secure Login Verification" +
                "</h2>" +
                "<p style='color: #666666; margin-bottom: 24px;'>" +
                "Use the verification code below to complete your login to NOVA SPACE." +
                "</p>" +
                "</div>" +

                "<div style='text-align: center; margin: 30px 0;'>" +
                "<div class='otp-code'>" +
                "<div style='font-size: 12px; color: #666666; margin-bottom: 8px; font-weight: 600; letter-spacing: 1px;'>" +
                "VERIFICATION CODE" +
                "</div>" +
                "<div class='code'>" + otp + "</div>" +
                "</div>" +
                "</div>" +

                "<div class='info-box'>" +
                "<p style='margin: 0; color: #333333; font-size: 14px;'>" +
                "<i class='fas fa-clock'></i> <strong>Expires in 10 minutes</strong> — Use this code immediately to complete your authentication." +
                "</p>" +
                "</div>" +

                "<div class='warning-box'>" +
                "<p style='margin: 0; color: #856404; font-size: 14px;'>" +
                "<i class='fas fa-lock'></i> <strong>Security Notice:</strong> Keep this code confidential — never share it with anyone. " +
                "NOVA SPACE will never ask for your verification code via phone or email." +
                "</p>" +
                "</div>" +

                "<div style='margin-top: 30px; padding-top: 20px; border-top: 1px solid #e0e0e0; text-align: center;'>" +
                "<p style='color: #666666; font-size: 13px;'>" +
                "If you didn't request this code, please ignore this email or contact our support team immediately." +
                "</p>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void sendAccountCreationEmail(String to) {
        String title = "Welcome to NOVA SPACE";

        String[] quotes = {
                "\"Productivity is never an accident. It is always the result of a commitment to excellence, intelligent planning, and focused effort.\" — Paul J. Meyer",
                "\"The way to get started is to quit talking and begin doing.\" — Walt Disney",
                "\"Your work is going to fill a large part of your life, and the only way to be truly satisfied is to do what you believe is great work.\" — Steve Jobs",
                "\"The secret of getting ahead is getting started.\" — Mark Twain",
                "\"Efficiency is doing things right; effectiveness is doing the right things.\" — Peter Drucker"
        };

        String randomQuote = quotes[(int) (Math.random() * quotes.length)];

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-check-circle' style='font-size: 48px; color: #28a745; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Welcome to NOVA SPACE</h2>" +
                "<span class='badge'><i class='fas fa-user-check'></i> Account Successfully Created</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Hello and welcome to NOVA SPACE! We're excited to help you organize your tasks, manage your projects, " +
                "and achieve your goals more efficiently." +
                "</p>" +

                "<p style='color: #333333;'>" +
                "Your account is now active and ready to use. Start creating tasks, setting up projects, and organizing " +
                "your work in your personal productivity space." +
                "</p>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-rocket'></i> Ready to Get Started?</h3>" +
                "<p style='color: #333333; margin-bottom: 12px;'>Here are your first steps:</p>" +
                "<ul>" +
                "<li>Create your first project</li>" +
                "<li>Add tasks with deadlines</li>" +
                "<li>Set up your dashboard preferences</li>" +
                "</ul>" +
                "</div>" +

                "<div style='background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;'>" +
                "<i class='fas fa-quote-left' style='font-size: 24px; color: #2563eb; margin-bottom: 12px;'></i>" +
                "<p style='color: #333333; font-style: italic; margin-bottom: 8px;'>" +
                randomQuote.substring(0, randomQuote.indexOf("—")) +
                "</p>" +
                "<p style='color: #666666; font-size: 13px; margin: 0;'>" +
                randomQuote.substring(randomQuote.indexOf("—")) +
                "</p>" +
                "</div>" +

                "<div class='grid'>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-tasks'></i>" +
                "<h4>Task Management</h4>" +
                "<p>Organize daily tasks with deadlines</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-project-diagram'></i>" +
                "<h4>Project Tracking</h4>" +
                "<p>Monitor progress and milestones</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-users'></i>" +
                "<h4>Team Collaboration</h4>" +
                "<p>Work together on shared projects</p>" +
                "</div>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-play-circle'></i> Get Started</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-life-ring'></i> Contact Support</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void passwordUpdate(String to) {
        String title = "Password Changed Successfully";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-check-circle' style='font-size: 48px; color: #28a745; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Password Updated Successfully</h2>" +
                "<span class='badge' style='background-color: #28a745;'><i class='fas fa-lock'></i> Security Action Confirmed</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Your NOVA SPACE account password has been successfully updated. The change is now active across all your devices and sessions." +
                "</p>" +

                "<p style='color: #333333;'>" +
                "You can now use your new password to access your account. For security reasons, you may have been signed out of other active sessions." +
                "</p>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-shield-alt'></i> Security Best Practices</h3>" +
                "<ul>" +
                "<li>Use a unique password that you don't reuse elsewhere</li>" +
                "<li>Enable two-factor authentication for added security</li>" +
                "<li>Consider using a password manager for secure storage</li>" +
                "</ul>" +
                "</div>" +

                "<div class='danger-box'>" +
                "<p style='margin: 0; color: #721c24; font-size: 14px;'>" +
                "<i class='fas fa-exclamation-triangle'></i> <strong>Important Security Alert:</strong> " +
                "If you did NOT make this change, please contact our security team immediately at " +
                "<strong>support@novaspace.com</strong>." +
                "</p>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-cog'></i> Security Settings</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-history'></i> Account Activity</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void mfaVerificationSuccessful(String to) {
        String title = "MFA Verification Successful";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-shield-check' style='font-size: 48px; color: #28a745; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Multi-Factor Authentication Verified</h2>" +
                "<span class='badge' style='background-color: #6f42c1;'><i class='fas fa-lock'></i> Security Enhanced</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Your multi-factor authentication verification was successful. Your account security has been enhanced with an additional layer of protection." +
                "</p>" +

                "<div class='success-box'>" +
                "<h3 style='color: #155724; margin-bottom: 12px;'><i class='fas fa-check-double'></i> Your Security is Now Stronger</h3>" +
                "<ul style='color: #155724;'>" +
                "<li>Your account is now significantly more secure</li>" +
                "<li>Unauthorized access attempts are much less likely</li>" +
                "<li>You'll need your MFA method for future logins</li>" +
                "</ul>" +
                "</div>" +

                "<div class='warning-box'>" +
                "<p style='margin: 0; color: #856404; font-size: 14px;'>" +
                "<i class='fas fa-key'></i> <strong>Keep Your Recovery Codes Safe:</strong> " +
                "Save your backup recovery codes in a secure location. These are essential if you lose access to your MFA device." +
                "</p>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void mfaEnabledMail(String to) {
        String title = "MFA Enabled Successfully";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-shield-alt' style='font-size: 48px; color: #28a745; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Multi-Factor Authentication Enabled</h2>" +
                "<span class='badge' style='background-color: #28a745;'><i class='fas fa-lock'></i> Account Security Upgraded</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Great news! Multi-Factor Authentication (MFA) has been successfully enabled on your NOVA SPACE account. " +
                "Your account is now protected by an additional layer of security." +
                "</p>" +

                "<div class='grid'>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-chart-line'></i>" +
                "<h4>99.9% Protection</h4>" +
                "<p>Reduces account breaches</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-lock'></i>" +
                "<h4>Two-Step Access</h4>" +
                "<p>Password + Verification</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-shield-check'></i>" +
                "<h4>Enhanced Security</h4>" +
                "<p>Industry best practice</p>" +
                "</div>" +
                "</div>" +

                "<div class='warning-box'>" +
                "<p style='margin: 0; color: #856404; font-size: 14px;'>" +
                "<i class='fas fa-download'></i> <strong>Action Required:</strong> " +
                "Download and securely store your backup recovery codes. Store them in a password manager or print them for safe keeping." +
                "</p>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-sign-in-alt'></i> Next Time You Log In</h3>" +
                "<ul>" +
                "<li>Enter your username and password as usual</li>" +
                "<li>You'll be prompted for your MFA verification code</li>" +
                "<li>Open your authenticator app and enter the 6-digit code</li>" +
                "<li>Access your account with enhanced security</li>" +
                "</ul>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void mfaDisabled(String to) {
        String title = "MFA Disabled";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-exclamation-triangle' style='font-size: 48px; color: #dc3545; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Multi-Factor Authentication Disabled</h2>" +
                "<span class='badge' style='background-color: #dc3545;'><i class='fas fa-unlock'></i> Security Level Changed</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Multi-Factor Authentication (MFA) has been disabled on your NOVA SPACE account. " +
                "Your account will no longer require secondary verification during login." +
                "</p>" +

                "<div class='danger-box'>" +
                "<h3 style='color: #721c24; margin-bottom: 12px;'><i class='fas fa-shield-alt'></i> Important Security Implications</h3>" +
                "<p style='color: #721c24; margin-bottom: 0;'>Your account security has been reduced to password-only protection. " +
                "We strongly recommend re-enabling MFA as soon as possible.</p>" +
                "</div>" +

                "<div class='warning-box'>" +
                "<p style='margin: 0; color: #856404; font-size: 14px;'>" +
                "<i class='fas fa-exclamation-circle'></i> <strong>Critical: If This Wasn't You</strong><br>" +
                "If you did NOT disable MFA on your account:<br>" +
                "1. Change your password immediately<br>" +
                "2. Contact our security team at <strong>security@novaspace.com</strong><br>" +
                "3. Re-enable MFA after securing your account" +
                "</p>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button' style='background-color: #28a745;'><i class='fas fa-shield-alt'></i> Re-enable MFA</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-lock'></i> Change Password</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void teamCreatedSuccessfully(String to, String teamName, String description, String username, TeamStatus role) {
        String title = "New Team Created: " + teamName;

        String roleIcon = role == TeamStatus.ADMIN ? "fa-user-shield" : "fa-user";
        String roleName = role == TeamStatus.ADMIN ? "Team Admin" : "Team Member";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-users' style='font-size: 48px; color: #2563eb; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Welcome to " + teamName + "</h2>" +
                "<span class='badge'><i class='fas fa-check'></i> Team Created Successfully</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Hello <strong>" + username + "</strong>,<br>" +
                "You have been added to the new project team <strong>" + teamName + "</strong>. " +
                "This team is now active and ready for collaboration on NOVA SPACE." +
                "</p>" +

                (description != null && !description.trim().isEmpty() ?
                        "<div class='info-box'>" +
                                "<h3 style='color: #2563eb; margin-bottom: 8px;'><i class='fas fa-info-circle'></i> Team Description</h3>" +
                                "<p style='color: #333333; margin: 0;'>" + description + "</p>" +
                                "</div>" : "") +

                "<div style='background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;'>" +
                "<i class='fas " + roleIcon + "' style='font-size: 32px; color: #2563eb; margin-bottom: 12px;'></i>" +
                "<h3 style='color: #1a1a1a; margin-bottom: 8px;'>Your Role: " + roleName + "</h3>" +
                "<span class='badge'>" + roleName + "</span>" +
                "</div>" +

                "<div class='grid'>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-users'></i>" +
                "<h4>Team Members</h4>" +
                "<p>View and coordinate with your team</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-folder-open'></i>" +
                "<h4>Shared Resources</h4>" +
                "<p>Access team files and documents</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-chart-line'></i>" +
                "<h4>Project Dashboard</h4>" +
                "<p>Track progress and milestones</p>" +
                "</div>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-arrow-right'></i> Go to Team Dashboard</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void addMemberMail(String to, String teamName, String description, TeamStatus role) {
        String title = "Added to Team: " + teamName;

        String roleIcon = role == TeamStatus.ADMIN ? "fa-user-shield" : "fa-user";
        String roleName = role == TeamStatus.ADMIN ? "Team Admin" : "Team Member";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-user-plus' style='font-size: 48px; color: #28a745; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>You've Been Added to " + teamName + "</h2>" +
                "<span class='badge' style='background-color: #28a745;'><i class='fas fa-check'></i> Team Invitation Accepted</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "You have been successfully added to the team <strong>" + teamName + "</strong>. " +
                "You can now start collaborating with your team members on NOVA SPACE." +
                "</p>" +

                (description != null && !description.trim().isEmpty() ?
                        "<div class='info-box'>" +
                                "<h3 style='color: #2563eb; margin-bottom: 8px;'><i class='fas fa-info-circle'></i> Team Description</h3>" +
                                "<p style='color: #333333; margin: 0;'>" + description + "</p>" +
                                "</div>" : "") +

                "<div style='background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;'>" +
                "<i class='fas " + roleIcon + "' style='font-size: 32px; color: #2563eb; margin-bottom: 12px;'></i>" +
                "<h3 style='color: #1a1a1a; margin-bottom: 8px;'>Your Role: " + roleName + "</h3>" +
                "<span class='badge'>" + roleName + "</span>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-rocket'></i> Quick Start Guide</h3>" +
                "<ul>" +
                "<li>Visit the team dashboard to see active projects</li>" +
                "<li>Review your assigned tasks and responsibilities</li>" +
                "<li>Connect with other team members</li>" +
                "<li>Start contributing to team projects</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-arrow-right'></i> Go to Team Dashboard</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-users'></i> View Team Members</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void removeMemberMail(String to, String teamName, String adminName) {
        String title = "Removed from Team: " + teamName;

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-user-minus' style='font-size: 48px; color: #dc3545; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Team Access Update</h2>" +
                "<span class='badge' style='background-color: #dc3545;'><i class='fas fa-times'></i> Team Access Removed</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "You have been removed from the team <strong>" + teamName + "</strong>. " +
                "This action was performed by <strong>" + adminName + "</strong>." +
                "</p>" +

                "<p style='color: #333333;'>" +
                "Your access to this team's projects, tasks, and shared resources has been revoked. " +
                "You will no longer be able to view or edit team content." +
                "</p>" +

                "<div class='danger-box'>" +
                "<h3 style='color: #721c24; margin-bottom: 12px;'><i class='fas fa-info-circle'></i> What This Means</h3>" +
                "<p style='color: #721c24; margin: 0;'>" +
                "• Access to team projects has been revoked<br>" +
                "• Team files are no longer visible<br>" +
                "• Collaboration with this team has ended" +
                "</p>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-check-circle'></i> Your Remaining Access</h3>" +
                "<ul>" +
                "<li>Your personal projects and tasks are unaffected</li>" +
                "<li>You still have access to your NOVA SPACE account</li>" +
                "<li>You can join or create other teams</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-home'></i> Go to Dashboard</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void roleUpdated(String to, String teamName, TeamStatus role) {
        String title = "Role Updated in " + teamName;

        String roleIcon = role == TeamStatus.ADMIN ? "fa-user-shield" : "fa-user";
        String roleName = role == TeamStatus.ADMIN ? "Team Admin" : "Team Member";
        String roleChangeText = role == TeamStatus.ADMIN ? "You have been promoted to Team Admin." : "Your role has been updated to Team Member.";

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-sync-alt' style='font-size: 48px; color: #ffc107; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Role Updated in " + teamName + "</h2>" +
                "<span class='badge' style='background-color: #ffc107; color: #1a1a1a;'><i class='fas fa-edit'></i> Permissions Changed</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                roleChangeText + "<br>" +
                "Your role in the team <strong>" + teamName + "</strong> has been updated." +
                "</p>" +

                "<div style='background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;'>" +
                "<i class='fas " + roleIcon + "' style='font-size: 32px; color: #2563eb; margin-bottom: 12px;'></i>" +
                "<h3 style='color: #1a1a1a; margin-bottom: 8px;'>Your New Role: " + roleName + "</h3>" +
                "<span class='badge'>" + roleName + "</span>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-lightbulb'></i> What Happens Next</h3>" +
                "<ul>" +
                "<li>Your new permissions are effective immediately</li>" +
                "<li>Some features may now be accessible or restricted</li>" +
                "<li>Team members will see your updated role</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-arrow-right'></i> Go to Team Dashboard</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void deleteTeam(String to, String teamName) {
        String title = "Team Deleted: " + teamName;

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-trash-alt' style='font-size: 48px; color: #dc3545; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Team Has Been Deleted</h2>" +
                "<span class='badge' style='background-color: #dc3545;'><i class='fas fa-times-circle'></i> Team Permanently Removed</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "The team <strong>" + teamName + "</strong> has been permanently deleted from NOVA SPACE. " +
                "This action is irreversible and all team data has been removed." +
                "</p>" +

                "<div class='danger-box'>" +
                "<h3 style='color: #721c24; margin-bottom: 12px;'><i class='fas fa-exclamation-triangle'></i> What Has Been Removed</h3>" +
                "<p style='color: #721c24; margin: 0;'>" +
                "• All team projects have been deleted<br>" +
                "• Shared files have been removed<br>" +
                "• Team structure and member roles are gone<br>" +
                "<strong>⚠️ This action cannot be undone</strong>" +
                "</p>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-user-check'></i> Your Account Status</h3>" +
                "<ul>" +
                "<li>Your NOVA SPACE account remains active</li>" +
                "<li>Your personal projects are unaffected</li>" +
                "<li>You can join or create other teams</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-home'></i> Go to Dashboard</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-plus'></i> Create New Team</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void createProjectMail(String to, String projectName) {
        String title = "New Project Created: " + projectName;

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-project-diagram' style='font-size: 48px; color: #2563eb; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>New Project Created</h2>" +
                "<span class='badge'>" + projectName + "</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "A new project has been successfully created: <strong>" + projectName + "</strong>. " +
                "The project is now ready for you to start adding tasks, assigning team members, and tracking progress." +
                "</p>" +

                "<div class='grid'>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-tasks'></i>" +
                "<h4>Add Tasks</h4>" +
                "<p>Create project tasks</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-users'></i>" +
                "<h4>Invite Team</h4>" +
                "<p>Assign team members</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-calendar-alt'></i>" +
                "<h4>Set Timeline</h4>" +
                "<p>Define milestones</p>" +
                "</div>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-lightbulb'></i> Project Management Tips</h3>" +
                "<ul>" +
                "<li>Break down large goals into manageable tasks</li>" +
                "<li>Set clear deadlines and priorities</li>" +
                "<li>Regularly update task status and progress</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-arrow-right'></i> Open Project</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-plus'></i> Add First Task</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void updateProjectMail(String to, String projectName, ProjectStatus status) {
        String title = "Project Updated: " + projectName;

        String statusIcon = "";
        String statusMessage = "";
        String badgeColor = "";

        switch(status) {
            case ACTIVE:
                statusIcon = "fa-play-circle";
                statusMessage = "Project is now active and in progress.";
                badgeColor = "#28a745";
                break;
            case COMPLETED:
                statusIcon = "fa-check-circle";
                statusMessage = "Project has been marked as completed.";
                badgeColor = "#2563eb";
                break;
            case ARCHIVED:
                statusIcon = "fa-archive";
                statusMessage = "Project has been archived.";
                badgeColor = "#6c757d";
                break;
            default:
                statusIcon = "fa-edit";
                statusMessage = "Project status has been updated.";
                badgeColor = "#ffc107";
        }

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-sync-alt' style='font-size: 48px; color: #6f42c1; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Project Status Updated</h2>" +
                "<span class='badge'>" + projectName + "</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "The project <strong>" + projectName + "</strong> has been updated. " + statusMessage +
                "</p>" +

                "<div style='background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;'>" +
                "<i class='fas " + statusIcon + "' style='font-size: 48px; color: " + badgeColor + "; margin-bottom: 12px;'></i>" +
                "<h3 style='color: #1a1a1a; margin-bottom: 8px;'>New Project Status</h3>" +
                "<span class='badge' style='background-color: " + badgeColor + ";'>" + status.toString().replace("_", " ") + "</span>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-arrow-right'></i> View Project</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-chart-bar'></i> Project Reports</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void deleteProjectMail(String to, String projectName) {
        String title = "Project Deleted: " + projectName;

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-trash-alt' style='font-size: 48px; color: #dc3545; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Project Has Been Deleted</h2>" +
                "<span class='badge' style='background-color: #dc3545;'><i class='fas fa-times-circle'></i> Permanently Removed</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "The project <strong>" + projectName + "</strong> has been permanently deleted from NOVA SPACE. " +
                "All project data, including tasks, documents, and team assignments, has been removed." +
                "</p>" +

                "<div class='danger-box'>" +
                "<h3 style='color: #721c24; margin-bottom: 12px;'><i class='fas fa-exclamation-triangle'></i> What Has Been Removed</h3>" +
                "<p style='color: #721c24; margin: 0;'>" +
                "• All tasks have been deleted<br>" +
                "• Uploaded documents have been removed<br>" +
                "• Team member assignments are gone<br>" +
                "<strong>⚠️ This deletion cannot be undone</strong>" +
                "</p>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-check-circle'></i> Your Current Projects</h3>" +
                "<ul>" +
                "<li>Your other projects remain unaffected</li>" +
                "<li>You can create new projects anytime</li>" +
                "<li>Your account continues as normal</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-list'></i> View All Projects</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-plus'></i> Create New Project</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void uploadDocumentsToDocument(String to, String projectName) {
        String title = "Documents Uploaded to " + projectName;

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-file-upload' style='font-size: 48px; color: #28a745; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Documents Successfully Uploaded</h2>" +
                "<span class='badge' style='background-color: #28a745;'>" + projectName + "</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Documents have been successfully uploaded to the project <strong>" + projectName + "</strong>. " +
                "The files are now available to all team members and can be accessed from the project documents section." +
                "</p>" +

                "<div class='grid'>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-eye'></i>" +
                "<h4>View & Preview</h4>" +
                "<p>Preview documents online</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-download'></i>" +
                "<h4>Download</h4>" +
                "<p>Access files offline</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-comments'></i>" +
                "<h4>Collaborate</h4>" +
                "<p>Add comments & notes</p>" +
                "</div>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-lock'></i> Security & Organization Tips</h3>" +
                "<ul>" +
                "<li>Use clear, descriptive file names</li>" +
                "<li>Organize documents in folders</li>" +
                "<li>Set appropriate access permissions</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-folder-open'></i> View Documents</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-upload'></i> Upload More Files</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void taskUploadMail(String to, String taskName, Priority priority, TaskStatus status, LocalDate dueDate) {
        String title = "New Task Created: " + taskName;

        String priorityColor = "";
        String priorityIcon = "";
        String priorityText = "";

        switch(priority) {
            case HIGH:
                priorityColor = "#dc3545";
                priorityIcon = "fa-exclamation-circle";
                priorityText = "High Priority";
                break;
            case MEDIUM:
                priorityColor = "#ffc107";
                priorityIcon = "fa-exclamation-triangle";
                priorityText = "Medium Priority";
                break;
            case LOW:
                priorityColor = "#28a745";
                priorityIcon = "fa-info-circle";
                priorityText = "Low Priority";
                break;
            default:
                priorityColor = "#6c757d";
                priorityIcon = "fa-circle";
                priorityText = "Normal Priority";
        }

        String statusColor = "";
        String statusIcon = "";

        switch(status) {
            case TO_DO:
                statusColor = "#6c757d";
                statusIcon = "fa-circle";
                break;
            case IN_PROGRESS:
                statusColor = "#2563eb";
                statusIcon = "fa-spinner";
                break;
            case DONE:
                statusColor = "#28a745";
                statusIcon = "fa-check-circle";
                break;
            case BLOCKED:
                statusColor = "#dc3545";
                statusIcon = "fa-ban";
                break;
            default:
                statusColor = "#ffc107";
                statusIcon = "fa-question-circle";
        }

        String dueDateText = "";
        String dueDateColor = "";

        if (dueDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            dueDateText = dueDate.format(formatter);

            LocalDate today = LocalDate.now();
            if (dueDate.isBefore(today)) {
                dueDateColor = "#dc3545";
            } else if (dueDate.isEqual(today)) {
                dueDateColor = "#ffc107";
            } else {
                dueDateColor = "#28a745";
            }
        } else {
            dueDateText = "No due date set";
            dueDateColor = "#6c757d";
        }

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-tasks' style='font-size: 48px; color: #6f42c1; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>New Task Created</h2>" +
                "<span class='badge' style='background-color: #6f42c1;'>" + taskName + "</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "A new task has been created and assigned to you: <strong>" + taskName + "</strong>. " +
                "The task details are summarized below. Please review and start working on it." +
                "</p>" +

                "<div class='grid'>" +
                "<div class='grid-item' style='border-top: 4px solid " + priorityColor + ";'>" +
                "<i class='fas " + priorityIcon + "' style='color: " + priorityColor + ";'></i>" +
                "<h4>Priority</h4>" +
                "<p style='color: " + priorityColor + "; font-weight: 600;'>" + priorityText + "</p>" +
                "</div>" +
                "<div class='grid-item' style='border-top: 4px solid " + statusColor + ";'>" +
                "<i class='fas " + statusIcon + "' style='color: " + statusColor + ";'></i>" +
                "<h4>Status</h4>" +
                "<p style='color: " + statusColor + "; font-weight: 600;'>" + status.toString().replace("_", " ") + "</p>" +
                "</div>" +
                "<div class='grid-item' style='border-top: 4px solid " + dueDateColor + ";'>" +
                "<i class='fas fa-calendar'></i>" +
                "<h4>Due Date</h4>" +
                "<p style='color: " + dueDateColor + "; font-weight: 600;'>" + dueDateText + "</p>" +
                "</div>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-lightbulb'></i> Next Steps</h3>" +
                "<ul>" +
                "<li>Review the task requirements and details</li>" +
                "<li>Estimate the time needed for completion</li>" +
                "<li>Update the task status as you make progress</li>" +
                "<li>Mark as done when completed</li>" +
                "</ul>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-arrow-right'></i> View Task</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-edit'></i> Update Status</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }

    @Async
    public void documentUploadedSuccessfully(String to, String documentName, LocalDateTime uploadedAt) {
        String title = "Document Uploaded Successfully";

        // Format the upload timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        String formattedDateTime = uploadedAt.format(formatter);

        String body = "<div style='text-align: center; margin-bottom: 30px;'>" +
                "<i class='fas fa-cloud-upload-alt' style='font-size: 48px; color: #28a745; margin-bottom: 20px;'></i>" +
                "<h2 style='color: #1a1a1a; margin-bottom: 16px;'>Document Uploaded Successfully</h2>" +
                "<span class='badge' style='background-color: #28a745;'><i class='fas fa-check'></i> Upload Complete</span>" +
                "</div>" +

                "<p style='color: #333333;'>" +
                "Your document <strong>" + documentName + "</strong> has been successfully uploaded to NOVA SPACE." +
                "</p>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-info-circle'></i> Upload Details</h3>" +
                "<p style='color: #333333; margin: 0;'>" +
                "<strong>Document Name:</strong> " + documentName + "<br>" +
                "<strong>Uploaded On:</strong> " + formattedDateTime + "<br>" +
                "<strong>Status:</strong> <span style='color: #28a745; font-weight: 600;'>Successfully Uploaded</span>" +
                "</p>" +
                "</div>" +

                "<div class='grid'>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-eye'></i>" +
                "<h4>View Document</h4>" +
                "<p>Access and preview your file</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-share-alt'></i>" +
                "<h4>Share</h4>" +
                "<p>Collaborate with team members</p>" +
                "</div>" +
                "<div class='grid-item'>" +
                "<i class='fas fa-download'></i>" +
                "<h4>Download</h4>" +
                "<p>Save a local copy anytime</p>" +
                "</div>" +
                "</div>" +

                "<div class='success-box'>" +
                "<h3 style='color: #155724; margin-bottom: 12px;'><i class='fas fa-shield-alt'></i> Your Document is Secure</h3>" +
                "<p style='color: #155724; margin: 0;'>" +
                "Your file is safely stored with enterprise-grade encryption. " +
                "Only authorized team members can access this document." +
                "</p>" +
                "</div>" +

                "<div class='info-box'>" +
                "<h3 style='color: #2563eb; margin-bottom: 12px;'><i class='fas fa-lightbulb'></i> What You Can Do Next</h3>" +
                "<ul>" +
                "<li>Share the document with your team members</li>" +
                "<li>Add comments or annotations</li>" +
                "<li>Organize it into a project folder</li>" +
                "<li>Set access permissions for collaboration</li>" +
                "</ul>" +
                "</div>" +

                "<div class='warning-box'>" +
                "<p style='margin: 0; color: #856404; font-size: 14px;'>" +
                "<i class='fas fa-exclamation-circle'></i> <strong>Important:</strong> " +
                "If you did not upload this document, please contact our support team immediately at " +
                "<strong>support@novaspace.com</strong> to secure your account." +
                "</p>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 30px;'>" +
                "<a href='#' class='button'><i class='fas fa-folder-open'></i> View Document</a>" +
                "<a href='#' class='button button-secondary'><i class='fas fa-upload'></i> Upload More</a>" +
                "</div>";

        sendHtmlEmail(to, title, getStyledHtml(title, body));
    }
}