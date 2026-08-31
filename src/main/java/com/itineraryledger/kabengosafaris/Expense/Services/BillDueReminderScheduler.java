package com.itineraryledger.kabengosafaris.Expense.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Saying that a supplier's bill is about to fall due, before it does.
 *
 * Everything else this system notifies about is somebody contacting us — a booking inquiry, a
 * contact form, a newsletter sign-up. This is the opposite and that is why it matters: nobody writes
 * in to say a provisional hold is about to lapse. A lodge holds rooms until a date, that date passes
 * in silence, and the first anyone knows is a reply saying the rooms are gone.
 *
 * Three warnings rather than one — a week out, three days out, and the morning of — because a single
 * reminder is a single chance to be looking at the inbox that day. They are separate emails on
 * purpose: a digest that says "3 bills coming up" is easy to leave for later, and one that says
 * "PAY TODAY: Tanganyika Wilderness Camps, USD 2,096" is not.
 *
 * Runs at 07:15, a quarter past the safari phase alerts, so the two do not compete for the mail
 * account and their failures are separable in the log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillDueReminderScheduler {

    private final ExpenseRepository expenses;
    private final NotificationSettingGetterServices settings;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailSendingService emailSendingService;

    private static final String EVENT = "BILL_DUE_REMINDER";

    @Scheduled(cron = "${bill.due-reminder.schedule.cron:0 15 7 * * ?}")
    public void remind() {
        if (!Boolean.TRUE.equals(settings.isBillDueReminderEnabled())) {
            log.debug("Bill due reminders are switched off");
            return;
        }

        List<String> recipients = settings.getBillDueReminderEmails();
        if (recipients.isEmpty()) {
            log.warn("Bill due reminders are on but nobody is configured to receive them");
            return;
        }

        LocalDate today = LocalDate.now();
        List<Integer> leadDays = settings.getBillDueLeadDays();

        /* which calendar day each lead time lands on, and how to describe it when it does */
        Map<LocalDate, Integer> wanted = new LinkedHashMap<>();
        for (Integer days : leadDays) wanted.putIfAbsent(today.plusDays(days), days);

        /*
         * Which bills count as still owing. Draft is included by default and deliberately: a
         * provisional booking is usually recorded as a draft, and it is the one that costs a room
         * rather than a late-payment note.
         */
        List<ExpenseStatus> statuses = new ArrayList<>(
            List.of(ExpenseStatus.RECORDED, ExpenseStatus.PARTIALLY_PAID));
        if (Boolean.TRUE.equals(settings.isBillDueDraftsIncluded())) statuses.add(ExpenseStatus.DRAFT);

        List<Expense> due = expenses.findDueOn(wanted.keySet(), statuses);
        if (due.isEmpty()) {
            log.debug("No bills fall due on {}", wanted.keySet());
            return;
        }

        int sent = 0;
        for (Expense bill : due) {
            Integer daysAway = wanted.get(bill.getDueDate());
            if (daysAway == null) continue;
            if (send(bill, daysAway, recipients)) sent++;
        }
        log.info("Bill due reminders: {} of {} bill(s) reported to {} recipient(s)",
            sent, due.size(), recipients.size());
    }

    private boolean send(Expense bill, int daysAway, List<String> recipients) {
        try {
            String urgency = daysAway == 0 ? "PAY TODAY"
                : daysAway == 1 ? "Due tomorrow"
                : "Due in " + daysAway + " days";

            Map<String, String> variables = new HashMap<>();
            variables.put("urgency", urgency);
            variables.put("daysAway", String.valueOf(daysAway));
            variables.put("billCode", text(bill.getExpenseCode()));
            variables.put("billTitle", text(bill.getTitle()));
            variables.put("vendorName", bill.getVendor() != null ? text(bill.getVendor().getName()) : "—");
            variables.put("dueDate", bill.getDueDate() == null ? "—" : bill.getDueDate().toString());
            variables.put("reference", text(bill.getReferenceNumber()));
            variables.put("status", bill.getStatus() == null ? "—" : bill.getStatus().name());
            variables.put("safariName", bill.getSafari() != null ? text(bill.getSafari().getName()) : "—");
            variables.put("safariCode", bill.getSafari() != null ? text(bill.getSafari().getCode()) : "");
            variables.put("amount", amountOf(bill));

            String subject = urgency + " · " + variables.get("vendorName")
                + " · " + variables.get("amount");

            String html = templateRenderer.renderTemplate(EVENT, variables);
            for (String email : recipients) {
                try {
                    emailSendingService.sendHtmlEmail(email, subject, html);
                } catch (Exception e) {
                    /* one bad address must not cost the others their warning */
                    log.warn("Could not send the {} reminder to {}: {}",
                        bill.getExpenseCode(), email, e.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Could not build the due reminder for {}: {}", bill.getExpenseCode(), e.getMessage());
            return false;
        }
    }

    /**
     * What is still owed, falling back to the total.
     *
     * A part-paid bill should say what is LEFT — telling somebody to pay 2,096 when 1,000 has gone
     * is how a supplier gets paid twice.
     */
    private String amountOf(Expense bill) {
        try {
            /*
             * The grand total, in the currency it was billed in. Not a balance: the expense carries
             * no outstanding figure of its own — what is left is derived from payments elsewhere —
             * and inventing one here would put a number in an email that no screen agrees with.
             */
            if (bill.getGrandTotals() != null && !bill.getGrandTotals().isEmpty()) {
                var total = bill.getGrandTotals().get(0);
                if (total.getTotalPrice() != null && total.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                    return (text(total.getCurrency()) + " " + total.getTotalPrice().toPlainString()).trim();
                }
            }
        } catch (Exception e) {
            log.debug("Could not read the amount on {}: {}", bill.getExpenseCode(), e.getMessage());
        }
        return "—";
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
