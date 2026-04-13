package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SafariPhaseAlertScheduler - Runs daily phase-driven checks on safaris
 * and sends email alerts for readiness issues and post-trip reminders.
 *
 * Runs daily at 7:00 AM (after SafariLifecycleScheduler at 6:00 AM):
 *
 * STARTING_SOON (3-7 days): Readiness check — vehicles, accommodations, pax
 * IMMINENT (1-2 days): Final check — all issues logged as CRITICAL
 * JUST_ENDED (1-7 days): Post-trip task reminders
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafariPhaseAlertScheduler {

    private final SafariRepository safariRepository;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final BackupSettingsGetterServices backupSettings;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    private static final List<SafariState> ACTIVE_BOOKING_STATES = List.of(
        SafariState.APPROVED, SafariState.CONFIRMED,
        SafariState.PENDING_PAYMENT, SafariState.FULLY_PAID
    );

    @Scheduled(cron = "${safari.phase-alert.schedule.cron:0 0 7 * * ?}")
    public void checkPhaseAlerts() {
        log.info("=== Safari Phase Alert Scheduler START ===");

        LocalDate today = LocalDate.now();

        checkStartingSoon(today);
        checkImminent(today);
        checkJustEnded(today);

        log.info("=== Safari Phase Alert Scheduler END ===");
    }

    /**
     * STARTING_SOON (3-7 days out): Readiness check — sends email if issues found
     */
    private void checkStartingSoon(LocalDate today) {
        List<Safari> safaris = safariRepository.findUpcomingInWindow(today.plusDays(3), today.plusDays(7), ACTIVE_BOOKING_STATES);
        if (safaris.isEmpty()) return;

        log.info("STARTING_SOON: {} safari(s) starting in 3-7 days", safaris.size());

        for (Safari safari : safaris) {
            long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, safari.getStartDate());
            List<String> issues = validateReadiness(safari);

            if (issues.isEmpty()) {
                log.info("  [OK] {} — starts in {} days — all checks passed", safari.getCode(), daysUntil);
            } else {
                log.warn("  [ISSUES] {} — starts in {} days — {}: {}", safari.getCode(), daysUntil, issues.size(), String.join("; ", issues));

                sendReadinessAlert(safari, "Starting Soon", daysUntil, issues);
            }
        }
    }

    /**
     * IMMINENT (1-2 days out): Final check — sends critical email for ANY unresolved issue
     */
    private void checkImminent(LocalDate today) {
        List<Safari> safaris = safariRepository.findUpcomingInWindow(today.plusDays(1), today.plusDays(2), ACTIVE_BOOKING_STATES);
        if (safaris.isEmpty()) return;

        log.info("IMMINENT: {} safari(s) starting in 1-2 days", safaris.size());

        for (Safari safari : safaris) {
            long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, safari.getStartDate());
            List<String> issues = validateReadiness(safari);

            // Also flag payment issues
            if (safari.getState() != SafariState.FULLY_PAID) {
                issues.add("Payment not complete — current state: " + safari.getState().getDisplayName());
            }

            if (!issues.isEmpty()) {
                log.error("  [CRITICAL] {} — starts in {} day(s) — UNRESOLVED: {}", safari.getCode(), daysUntil, String.join("; ", issues));

                sendReadinessAlert(safari, "Imminent", daysUntil, issues);
            }
        }
    }

    /**
     * JUST_ENDED (1-7 days after end): Post-trip task reminders
     */
    private void checkJustEnded(LocalDate today) {
        List<Safari> recentlyEnded = safariRepository.findByEndDateBetween(today.minusDays(7), today.minusDays(1));

        List<Safari> completedSafaris = recentlyEnded.stream()
            .filter(s -> s.getState() == SafariState.COMPLETED)
            .toList();

        if (completedSafaris.isEmpty()) return;

        log.info("JUST_ENDED: {} safari(s) completed in last 7 days", completedSafaris.size());

        for (Safari safari : completedSafaris) {
            long daysSinceEnd = java.time.temporal.ChronoUnit.DAYS.between(safari.getEndDate(), today);

            log.info("  [POST-TRIP] {} — ended {} day(s) ago", safari.getCode(), daysSinceEnd);

            sendPostTripReminder(safari, daysSinceEnd);
        }
    }

    // ============================================================
    // Readiness validation
    // ============================================================

    private List<String> validateReadiness(Safari safari) {
        List<String> issues = new ArrayList<>();

        if (safari.getSafariVehicles() == null || safari.getSafariVehicles().isEmpty()) {
            issues.add("No vehicles assigned");
        }

        if (safari.getPaxList() == null || safari.getPaxList().isEmpty()) {
            issues.add("No passenger categories defined");
        }

        if (safari.getDays() == null || safari.getDays().isEmpty()) {
            issues.add("No safari days defined");
        } else {
            long daysWithoutAccommodation = safari.getDays().stream()
                .filter(day -> Boolean.TRUE.equals(day.getIsOvernight()))
                .filter(day -> day.getAccommodations() == null || day.getAccommodations().isEmpty())
                .count();

            if (daysWithoutAccommodation > 0) {
                issues.add(daysWithoutAccommodation + " overnight day(s) missing accommodation");
            }
        }

        if (safari.getCustomer() == null) {
            issues.add("No customer linked");
        }

        return issues;
    }

    // ============================================================
    // Email dispatchers
    // ============================================================

    private void sendReadinessAlert(Safari safari, String phase, long daysUntil, List<String> issues) {
        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("safariCode", safari.getCode());
            variables.put("safariName", safari.getName());
            variables.put("customerName", safari.getCustomer() != null ? safari.getCustomer().getDisplayName() : "N/A");
            variables.put("startDate", safari.getStartDate().format(DATE_FMT));
            variables.put("daysUntilStart", String.valueOf(daysUntil));
            variables.put("phase", phase);
            variables.put("issueCount", String.valueOf(issues.size()));
            variables.put("alertDate", LocalDate.now().format(DATE_FMT));

            // Build HTML list of issues
            StringBuilder issuesHtml = new StringBuilder();
            for (String issue : issues) {
                issuesHtml.append("<li style=\"margin: 4px 0;\">").append(issue).append("</li>");
            }
            variables.put("issuesList", issuesHtml.toString());

            String urgency = "Imminent".equals(phase) ? "CRITICAL: " : "";
            String subject = urgency + "Safari Readiness Issues — " + safari.getCode() + " starts in " + daysUntil + " day(s)";

            sendAlert("SAFARI_READINESS_ALERT", variables, subject);
        } catch (Exception e) {
            log.warn("Failed to send readiness alert for {}: {}", safari.getCode(), e.getMessage());
        }
    }

    private void sendPostTripReminder(Safari safari, long daysSinceEnd) {
        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("safariCode", safari.getCode());
            variables.put("safariName", safari.getName());
            variables.put("customerName", safari.getCustomer() != null ? safari.getCustomer().getDisplayName() : "N/A");
            variables.put("endDate", safari.getEndDate().format(DATE_FMT));
            variables.put("daysSinceEnd", String.valueOf(daysSinceEnd));
            variables.put("alertDate", LocalDate.now().format(DATE_FMT));

            // Pending tasks
            String tasks = "<li style=\"margin: 4px 0;\">Collect guest feedback and reviews</li>"
                + "<li style=\"margin: 4px 0;\">Reconcile expenses and receipts</li>"
                + "<li style=\"margin: 4px 0;\">Update driver/guide logs</li>"
                + "<li style=\"margin: 4px 0;\">Close out any open invoices</li>"
                + "<li style=\"margin: 4px 0;\">File park entry receipts</li>";
            variables.put("pendingTasks", tasks);

            String subject = "Post-Trip Tasks Pending — " + safari.getCode() + " — ended " + daysSinceEnd + " day(s) ago";

            sendAlert("SAFARI_POST_TRIP_REMINDER", variables, subject);
        } catch (Exception e) {
            log.warn("Failed to send post-trip reminder for {}: {}", safari.getCode(), e.getMessage());
        }
    }

    private void sendAlert(String eventName, Map<String, String> variables, String subject) {
        try {
            List<String> recipients = backupSettings.getNotificationEmails();
            if (recipients.isEmpty()) {
                log.warn("No notification email recipients configured — skipping {} alert", eventName);
                return;
            }

            String html = emailTemplateRenderer.renderTemplate(eventName, variables);

            for (String email : recipients) {
                try {
                    emailSendingService.sendHtmlEmail(email, subject, html);
                } catch (Exception e) {
                    log.warn("Failed to send {} alert to {}: {}", eventName, email, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to prepare {} email alert: {}", eventName, e.getMessage());
        }
    }
}
