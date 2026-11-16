package com.itineraryledger.kabengosafaris.NotificationSetting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationSettingGetterServices {

    @Value("${notification.newsletter.enabled:true}")
    private Boolean defaultNewsletterEnabled;

    @Value("${notification.newsletter.emails:}")
    private String defaultNewsletterEmails;

    @Value("${notification.booking_inquiry.enabled:true}")
    private Boolean defaultBookingInquiryEnabled;

    @Value("${notification.booking_inquiry.emails:}")
    private String defaultBookingInquiryEmails;

    @Value("${notification.contact_us.enabled:true}")
    private Boolean defaultContactUsEnabled;

    @Value("${notification.contact_us.emails:}")
    private String defaultContactUsEmails;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    private String getSettingValue(String key, String defaultValue) {
        NotificationSetting setting = notificationSettingRepository.findBySettingKey(key).orElse(null);
        if (setting == null || !setting.getActive()) {
            return defaultValue;
        }
        return setting.getSettingValue();
    }

    private Boolean getBooleanSetting(String key, Boolean defaultValue) {
        try {
            return Boolean.parseBoolean(getSettingValue(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ==================== NEWSLETTER SETTINGS ====================

    public Boolean isNewsletterNotificationEnabled() {
        return getBooleanSetting("notification.newsletter.enabled", defaultNewsletterEnabled);
    }

    public List<String> getNewsletterNotificationEmails() {
        String emailsString = getSettingValue("notification.newsletter.emails", defaultNewsletterEmails);
        if (emailsString == null || emailsString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(emailsString.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== BOOKING INQUIRY SETTINGS ====================

    public Boolean isBookingInquiryNotificationEnabled() {
        return getBooleanSetting("notification.booking_inquiry.enabled", defaultBookingInquiryEnabled);
    }

    public List<String> getBookingInquiryNotificationEmails() {
        String emailsString = getSettingValue("notification.booking_inquiry.emails", defaultBookingInquiryEmails);
        if (emailsString == null || emailsString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(emailsString.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== CONTACT US SETTINGS ====================

    public Boolean isContactMessageNotificationEnabled() {
        return getBooleanSetting("notification.contact_us.enabled", defaultContactUsEnabled);
    }

    public List<String> getContactMessageNotificationEmails() {
        String emailsString = getSettingValue("notification.contact_us.emails", defaultContactUsEmails);
        if (emailsString == null || emailsString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(emailsString.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== CLIENT PORTAL ====================

    public Boolean isClientPortalNotificationEnabled() {
        return getBooleanSetting("notification.client_portal.enabled", true);
    }

    /**
     * Who hears that a customer has asked us to look, accepted, or declined.
     *
     * <p>Falls back to the contact-us addresses, for the same reason the bill-due reminders do
     * and with more at stake. Pressing Accept in the portal deliberately does not move the
     * quotation's own status, so if this reaches nobody then somebody who has said yes is in a
     * state nothing in the panel watches. An alert nobody receives is indistinguishable from one
     * that was never built.
     */
    public List<String> getClientPortalNotificationEmails() {
        String configured = getSettingValue("notification.client_portal.emails", null);
        if (configured == null || configured.trim().isEmpty()) {
            return getContactMessageNotificationEmails();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== BILL DUE REMINDERS ====================

    public Boolean isBillDueReminderEnabled() {
        return getBooleanSetting("notification.bill_due.enabled", true);
    }

    /**
     * Who hears about a bill falling due.
     *
     * Falls back to the contact-us addresses rather than to nobody: this setting is newer than most
     * installations, and a reminder nobody receives is indistinguishable from a reminder that was
     * never built.
     */
    public List<String> getBillDueReminderEmails() {
        String configured = getSettingValue("notification.bill_due.emails", null);
        if (configured == null || configured.trim().isEmpty()) {
            return getContactMessageNotificationEmails();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * How many days ahead to warn, largest first.
     *
     * Defaults to 7, 3 and 0 - early warning, last chance, and the day itself. Parsed defensively
     * because it is a free-text field somebody can put anything in, and a bad character here must
     * not stop every reminder for the whole company.
     */
    public List<Integer> getBillDueLeadDays() {
        String configured = getSettingValue("notification.bill_due.lead_days", "7,3,0");
        List<Integer> days = new java.util.ArrayList<>();
        for (String part : (configured == null ? "7,3,0" : configured).split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 0 && value <= 365 && !days.contains(value)) days.add(value);
            } catch (NumberFormatException ignored) {
                /* one unreadable entry should not silence the readable ones */
            }
        }
        if (days.isEmpty()) days = List.of(7, 3, 0);
        return days.stream().sorted(java.util.Comparator.reverseOrder()).collect(Collectors.toList());
    }

    public Boolean isBillDueDraftsIncluded() {
        return getBooleanSetting("notification.bill_due.include_drafts", true);
    }

    // ==================== SAFARI OPERATIONS ====================

    public Boolean isSafariOperationsNotificationEnabled() {
        return getBooleanSetting("notification.safari_operations.enabled", true);
    }

    /**
     * Who hears that a trip is not ready, or has gone past its start date unpaid.
     *
     * <p>Falls back to the contact-us addresses, as the portal and bill-due alerts do. It used to
     * fall back to nothing of the sort: it read the BACKUP alert list, so the people who hear that
     * a database dump failed were the people, and the only people, who heard that a safari leaves
     * on Friday with no vehicle.
     */
    public List<String> getSafariOperationsNotificationEmails() {
        String configured = getSettingValue("notification.safari_operations.emails", null);
        if (configured == null || configured.trim().isEmpty()) {
            return getContactMessageNotificationEmails();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * How many days before departure a readiness problem is reported, largest first.
     *
     * <p>The check used to sweep a window: days 7 down to 3, then 2 and 1, re-sending the same
     * list of unresolved issues on every one of those seven mornings. Seven copies of one problem
     * is how a reader learns to delete the subject line unread, and the eighth copy is the one
     * that mattered.
     */
    public List<Integer> getSafariReadinessLeadDays() {
        return dayList("notification.safari_operations.readiness_days", "7,3,1", true);
    }

    /**
     * How many days AFTER the start date an unpaid booking is reported.
     *
     * <p>Day 0 is the morning of departure. The list ends, and that is the point of it: the old
     * query had no lower bound and no memory, and the state machine guaranteed the safari could
     * never leave the set, so one booking that was never marked fully paid produced a mail
     * subject-lined CRITICAL every morning for the rest of the installation's life.
     */
    public List<Integer> getSafariPaymentGapDays() {
        return dayList("notification.safari_operations.payment_gap_days", "0,3,7,14", false);
    }

    /**
     * How many days after the end date the post-trip reminder is sent.
     *
     * <p>One day, not seven. The reminder carries a fixed list of tasks and checks none of them,
     * so a second copy of it tells the reader nothing the first did not.
     */
    public List<Integer> getSafariPostTripDays() {
        return dayList("notification.safari_operations.post_trip_days", "2", false);
    }

    /**
     * A comma-separated list of days, parsed defensively.
     *
     * <p>Blank means silent. That is deliberate and it is the difference between this and
     * {@link #getBillDueLeadDays()}, which fills an empty value with its default: there, emptying
     * the field by accident would silence a reminder nobody else sends, so the default is safer.
     * Here, emptying the field is how a channel is switched off, so it has to be honoured.
     *
     * <p>One unreadable entry never silences the readable ones.
     */
    private List<Integer> dayList(String key, String fallback, boolean largestFirst) {
        String configured = getSettingValue(key, fallback);
        if (configured == null) configured = fallback;
        if (configured.trim().isEmpty()) return Collections.emptyList();

        List<Integer> days = new java.util.ArrayList<>();
        for (String part : configured.split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 0 && value <= 365 && !days.contains(value)) days.add(value);
            } catch (NumberFormatException ignored) {
                /* one bad character must not stop the whole channel */
            }
        }
        days.sort(largestFirst ? java.util.Comparator.reverseOrder() : java.util.Comparator.naturalOrder());
        return days;
    }
}
