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
}
