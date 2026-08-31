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
     * Defaults to 7, 3 and 0 — early warning, last chance, and the day itself. Parsed defensively
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
}
