package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;
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
 * Runs daily at 7:00 AM (after SafariLifecycleScheduler at 6:00 AM).
 *
 * <p>It speaks on named days, not across windows. It used to sweep days 7 down to 3 and then 2
 * and 1, re-sending the same unresolved list on all seven mornings, and to send the post-trip
 * reminder on every one of the seven days after a trip ended. That reminder carries a fixed list
 * of tasks and checks none of them, so copies two through seven told the reader nothing the first
 * had not. Seven copies of one problem is how a person learns to delete a subject line unread.
 *
 * <p>Which days, and whether a channel speaks at all, is configured under Settings →
 * Notification. An empty day list means that channel is silent. Inside two days of departure the
 * readiness alert is marked critical and adds the payment state, which is the one piece the
 * window form was carrying that the day form has to keep.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafariPhaseAlertScheduler {

    private final SafariRepository safariRepository;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final NotificationSettingGetterServices notificationSettings;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    private static final List<SafariState> ACTIVE_BOOKING_STATES = List.of(
        SafariState.APPROVED, SafariState.CONFIRMED,
        SafariState.PENDING_PAYMENT, SafariState.FULLY_PAID
    );

    @Scheduled(cron = "${safari.phase-alert.schedule.cron:0 0 7 * * ?}")
    public void checkPhaseAlerts() {
        log.info("=== Safari Phase Alert Scheduler START ===");

        if (!Boolean.TRUE.equals(notificationSettings.isSafariOperationsNotificationEnabled())) {
            log.debug("Safari operations alerts are switched off");
            return;
        }

        LocalDate today = LocalDate.now();

        checkReadiness(today);
        checkJustEnded(today);

        log.info("=== Safari Phase Alert Scheduler END ===");
    }

    /**
     * Readiness, on the days we were told to speak.
     *
     * <p>Inside two days the phase is Imminent: the subject is marked critical and an incomplete
     * payment counts as an issue in its own right. Further out it is a heads-up about something
     * there is still time to fix.
     */
    private void checkReadiness(LocalDate today) {
        List<Integer> leadDays = notificationSettings.getSafariReadinessLeadDays();
        if (leadDays.isEmpty()) {
            log.debug("Readiness alerts have no lead days configured");
            return;
        }

        for (Integer daysUntil : leadDays) {
            LocalDate departure = today.plusDays(daysUntil);
            List<Safari> safaris = safariRepository.findUpcomingInWindow(
                departure, departure, ACTIVE_BOOKING_STATES);
            if (safaris.isEmpty()) continue;

            boolean imminent = daysUntil <= 2;
            log.info("READINESS: {} safari(s) starting in {} day(s)", safaris.size(), daysUntil);

            for (Safari safari : safaris) {
                List<String> issues = validateReadiness(safari);

                if (imminent && safari.getState() != SafariState.FULLY_PAID) {
                    issues.add("Payment not complete, current state: "
                        + safari.getState().getDisplayName());
                }

                if (issues.isEmpty()) {
                    log.info("  [OK] {} starts in {} day(s), all checks passed",
                        safari.getCode(), daysUntil);
                    continue;
                }

                if (imminent) {
                    log.error("  [CRITICAL] {} starts in {} day(s), UNRESOLVED: {}",
                        safari.getCode(), daysUntil, String.join("; ", issues));
                } else {
                    log.warn("  [ISSUES] {} starts in {} day(s), {}: {}",
                        safari.getCode(), daysUntil, issues.size(), String.join("; ", issues));
                }

                sendReadinessAlert(safari, imminent ? "Imminent" : "Starting Soon", daysUntil, issues);
            }
        }
    }

    /**
     * The post-trip reminder, once.
     *
     * <p>The task list it carries is fixed text and nothing here checks whether any of it is
     * outstanding, so there is no honest reason to send a second one.
     */
    private void checkJustEnded(LocalDate today) {
        List<Integer> afterDays = notificationSettings.getSafariPostTripDays();
        if (afterDays.isEmpty()) {
            log.debug("Post-trip reminders have no days configured");
            return;
        }

        for (Integer daysSinceEnd : afterDays) {
            LocalDate ended = today.minusDays(daysSinceEnd);
            List<Safari> completed = safariRepository.findByEndDateBetween(ended, ended).stream()
                .filter(s -> s.getState() == SafariState.COMPLETED)
                .toList();
            if (completed.isEmpty()) continue;

            log.info("POST-TRIP: {} safari(s) ended {} day(s) ago", completed.size(), daysSinceEnd);
            for (Safari safari : completed) {
                sendPostTripReminder(safari, daysSinceEnd);
            }
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

    private void sendReadinessAlert(Safari safari, String phase, int daysUntil, List<String> issues) {
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
            String subject = urgency + "Safari Readiness Issues - " + safari.getCode() + " starts in " + daysUntil + " day(s)";

            sendAlert("SAFARI_READINESS_ALERT", variables, subject);
        } catch (Exception e) {
            log.warn("Failed to send readiness alert for {}: {}", safari.getCode(), e.getMessage());
        }
    }

    private void sendPostTripReminder(Safari safari, int daysSinceEnd) {
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

            String subject = "Post-Trip Tasks Pending - " + safari.getCode() + " - ended " + daysSinceEnd + " day(s) ago";

            sendAlert("SAFARI_POST_TRIP_REMINDER", variables, subject);
        } catch (Exception e) {
            log.warn("Failed to send post-trip reminder for {}: {}", safari.getCode(), e.getMessage());
        }
    }

    private void sendAlert(String eventName, Map<String, String> variables, String subject) {
        try {
            List<String> recipients = notificationSettings.getSafariOperationsNotificationEmails();
            if (recipients.isEmpty()) {
                log.warn("No safari operations recipients configured, skipping {} alert", eventName);
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
