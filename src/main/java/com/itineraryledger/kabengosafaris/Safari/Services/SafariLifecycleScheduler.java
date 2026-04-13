package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SafariLifecycleScheduler - Automatically transitions safari states based on dates
 * and sends email alerts for critical events.
 *
 * Runs daily at 6:00 AM:
 * 1. AUTO-START: FULLY_PAID → IN_PROGRESS when startDate <= today
 * 2. AUTO-COMPLETE: IN_PROGRESS → COMPLETED when endDate < today
 * 3. AUTO-CLOSE: COMPLETED → CLOSED after grace period
 * 4. PAYMENT GAP: Flag safaris not FULLY_PAID on startDate (critical email alert)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafariLifecycleScheduler {

    private final SafariRepository safariRepository;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final BackupSettingsGetterServices backupSettings;

    @Value("${safari.lifecycle.auto-close.grace-days:14}")
    private int autoCloseGraceDays;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    @Scheduled(cron = "${safari.lifecycle.schedule.cron:0 0 6 * * ?}")
    @Transactional
    public void processLifecycleTransitions() {
        log.info("=== Safari Lifecycle Scheduler START ===");

        LocalDate today = LocalDate.now();

        int started = autoStartSafaris(today);
        int completed = autoCompleteSafaris(today);
        int closed = autoCloseSafaris();
        int paymentGaps = detectPaymentGaps(today);

        log.info("=== Safari Lifecycle Scheduler END === (started: {}, completed: {}, closed: {}, payment gaps: {})",
            started, completed, closed, paymentGaps);
    }

    /**
     * AUTO-START: FULLY_PAID → IN_PROGRESS
     */
    private int autoStartSafaris(LocalDate today) {
        List<Safari> readyToStart = safariRepository.findReadyToStart(today);
        if (readyToStart.isEmpty()) return 0;

        int count = 0;
        for (Safari safari : readyToStart) {
            safari.changeState(SafariState.IN_PROGRESS, "Auto-started by system on " + today);
            safariRepository.save(safari);
            count++;

            log.info("AUTO-START: {} ({}) — {} to {} — IN_PROGRESS",
                safari.getCode(), safari.getName(), safari.getStartDate(), safari.getEndDate());

            sendAlert("SAFARI_STARTED", buildStartedVariables(safari),
                "Safari Started: " + safari.getCode() + " — " + safari.getName());
        }

        return count;
    }

    /**
     * AUTO-COMPLETE: IN_PROGRESS → COMPLETED
     */
    private int autoCompleteSafaris(LocalDate today) {
        List<Safari> readyToComplete = safariRepository.findReadyToComplete(today);
        if (readyToComplete.isEmpty()) return 0;

        int count = 0;
        for (Safari safari : readyToComplete) {
            safari.changeState(SafariState.COMPLETED, "Auto-completed by system — ended on " + safari.getEndDate());
            safariRepository.save(safari);
            count++;

            log.info("AUTO-COMPLETE: {} ({}) — ended {} — COMPLETED",
                safari.getCode(), safari.getName(), safari.getEndDate());

            sendAlert("SAFARI_COMPLETED", buildCompletedVariables(safari),
                "Safari Completed: " + safari.getCode() + " — " + safari.getName());
        }

        return count;
    }

    /**
     * AUTO-CLOSE: COMPLETED → CLOSED after grace period
     */
    private int autoCloseSafaris() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(autoCloseGraceDays);
        List<Safari> readyToClose = safariRepository.findReadyToClose(cutoff);
        if (readyToClose.isEmpty()) return 0;

        int count = 0;
        for (Safari safari : readyToClose) {
            safari.changeState(SafariState.CLOSED,
                "Auto-closed — completed " + autoCloseGraceDays + " days ago");
            safariRepository.save(safari);
            count++;

            log.info("AUTO-CLOSE: {} ({}) — CLOSED", safari.getCode(), safari.getName());
        }

        return count;
    }

    /**
     * PAYMENT GAP: Flag safaris not FULLY_PAID on startDate — sends critical email
     */
    private int detectPaymentGaps(LocalDate today) {
        List<SafariState> unpaidStates = List.of(
            SafariState.DRAFT, SafariState.PENDING_APPROVAL, SafariState.APPROVED,
            SafariState.CONFIRMED, SafariState.PENDING_PAYMENT
        );

        List<Safari> gaps = safariRepository.findPaymentGapSafaris(today, unpaidStates);
        if (gaps.isEmpty()) return 0;

        for (Safari safari : gaps) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(safari.getStartDate(), today);

            log.error("PAYMENT GAP: {} ({}) — startDate {} is {} day(s) overdue — state: {} — customer: {}",
                safari.getCode(), safari.getName(), safari.getStartDate(), daysOverdue,
                safari.getState().getDisplayName(),
                safari.getCustomer() != null ? safari.getCustomer().getDisplayName() : "N/A");

            sendAlert("SAFARI_PAYMENT_GAP", buildPaymentGapVariables(safari, daysOverdue),
                "CRITICAL: Payment Gap — " + safari.getCode() + " — " + safari.getName());
        }

        log.warn("PAYMENT GAP ALERT: {} safari(s) past start date without full payment", gaps.size());
        return gaps.size();
    }

    // ============================================================
    // Variable builders
    // ============================================================

    private Map<String, String> buildStartedVariables(Safari s) {
        Map<String, String> v = new HashMap<>();
        v.put("safariCode", s.getCode());
        v.put("safariName", s.getName());
        v.put("customerName", s.getCustomer() != null ? s.getCustomer().getDisplayName() : "N/A");
        v.put("startDate", s.getStartDate().format(DATE_FMT));
        v.put("endDate", s.getEndDate().format(DATE_FMT));
        v.put("totalDays", String.valueOf(s.getTotalDays()));
        v.put("totalNights", String.valueOf(s.getTotalNights()));
        v.put("startLocation", s.getStartLocation() != null ? s.getStartLocation() : "");
        v.put("alertDate", LocalDate.now().format(DATE_FMT));
        return v;
    }

    private Map<String, String> buildCompletedVariables(Safari s) {
        Map<String, String> v = new HashMap<>();
        v.put("safariCode", s.getCode());
        v.put("safariName", s.getName());
        v.put("customerName", s.getCustomer() != null ? s.getCustomer().getDisplayName() : "N/A");
        v.put("startDate", s.getStartDate().format(DATE_FMT));
        v.put("endDate", s.getEndDate().format(DATE_FMT));
        v.put("totalDays", String.valueOf(s.getTotalDays()));
        v.put("alertDate", LocalDate.now().format(DATE_FMT));
        return v;
    }

    private Map<String, String> buildPaymentGapVariables(Safari s, long daysOverdue) {
        Map<String, String> v = new HashMap<>();
        v.put("safariCode", s.getCode());
        v.put("safariName", s.getName());
        v.put("customerName", s.getCustomer() != null ? s.getCustomer().getDisplayName() : "N/A");
        v.put("startDate", s.getStartDate().format(DATE_FMT));
        v.put("endDate", s.getEndDate().format(DATE_FMT));
        v.put("currentState", s.getState().getDisplayName());
        v.put("daysOverdue", String.valueOf(daysOverdue));
        v.put("totalDays", String.valueOf(s.getTotalDays()));
        v.put("alertDate", LocalDate.now().format(DATE_FMT));
        return v;
    }

    // ============================================================
    // Email dispatch
    // ============================================================

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
