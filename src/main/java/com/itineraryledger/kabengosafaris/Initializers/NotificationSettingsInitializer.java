package com.itineraryledger.kabengosafaris.Initializers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSetting;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingRepository;

/**
 * Initializer for Notification Settings.
 * Runs at application startup and initializes default notification settings in the database.
 *
 * Seeds settings for Newsletter and Booking Inquiry notification configuration.
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsInitializer implements ApplicationRunner, Ordered {

    private final NotificationSettingRepository notificationSettingRepository;

    // Newsletter Settings
    @Value("${notification.newsletter.enabled:true}")
    private Boolean newsletterEnabled;

    @Value("${notification.newsletter.emails:admin@kabengosafaris.com}")
    private String newsletterEmails;

    // Booking Inquiry Settings
    @Value("${notification.booking_inquiry.enabled:true}")
    private Boolean bookingInquiryEnabled;

    @Value("${notification.booking_inquiry.emails:admin@kabengosafaris.com}")
    private String bookingInquiryEmails;

    // Contact Us Settings
    @Value("${notification.contact_us.enabled:true}")
    private Boolean contactUsEnabled;

    @Value("${notification.contact_us.emails:admin@kabengosafaris.com}")
    private String contactUsEmails;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 18;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();

        try {
            initializeNotificationSettings();
            printEndBanner(true);
        } catch (Exception e) {
            log.error("Error during Notification Settings initialization", e);
            printEndBanner(false);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║          NOTIFICATION SETTINGS INITIALIZER - START                 ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║     ✓ NOTIFICATION SETTINGS INITIALIZER - COMPLETED                ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║     ✗ NOTIFICATION SETTINGS INITIALIZER - FAILED                   ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializeNotificationSettings() {
        // Newsletter Settings
        createOrUpdateSetting(
                "notification.newsletter.enabled",
                String.valueOf(newsletterEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable email notifications for new newsletter subscriptions",
                NotificationSetting.Category.NEWSLETTER,
                false
        );

        createOrUpdateSetting(
                "notification.newsletter.emails",
                newsletterEmails,
                SettingDataType.STRING,
                "Email addresses to receive newsletter subscription notifications (comma-separated: email1@example.com,email2@example.com)",
                NotificationSetting.Category.NEWSLETTER,
                false
        );

        // Booking Inquiry Settings
        createOrUpdateSetting(
                "notification.booking_inquiry.enabled",
                String.valueOf(bookingInquiryEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable email notifications for new booking inquiries",
                NotificationSetting.Category.BOOKING_INQUIRY,
                false
        );

        createOrUpdateSetting(
                "notification.booking_inquiry.emails",
                bookingInquiryEmails,
                SettingDataType.STRING,
                "Email addresses to receive booking inquiry notifications (comma-separated: email1@example.com,email2@example.com)",
                NotificationSetting.Category.BOOKING_INQUIRY,
                false
        );

        // Contact Us Settings
        createOrUpdateSetting(
                "notification.contact_us.enabled",
                String.valueOf(contactUsEnabled),
                SettingDataType.BOOLEAN,
                "Enable or disable email notifications for new contact form messages",
                NotificationSetting.Category.CONTACT_US,
                false
        );

        createOrUpdateSetting(
                "notification.contact_us.emails",
                contactUsEmails,
                SettingDataType.STRING,
                "Email addresses to receive contact form notifications (comma-separated: email1@example.com,email2@example.com)",
                NotificationSetting.Category.CONTACT_US,
                false
        );

        log.info("All notification settings have been initialized");
    }

    private void createOrUpdateSetting(String settingKey, String settingValue,
                                        SettingDataType dataType,
                                        String description, NotificationSetting.Category category,
                                        Boolean requiresRestart) {
        try {
            if (notificationSettingRepository.existsBySettingKey(settingKey)) {
                log.debug("⊘ Setting already exists, skipping: {}", settingKey);
                return;
            }

            NotificationSetting setting = NotificationSetting.builder()
                    .settingKey(settingKey)
                    .settingValue(settingValue)
                    .dataType(dataType)
                    .description(description)
                    .category(category)
                    .active(true)
                    .isSystemDefault(true)
                    .requiresRestart(requiresRestart)
                    .build();

            notificationSettingRepository.save(setting);
            log.info("✓ Notification setting initialized: {} = {} (category: {})", settingKey, settingValue, category);

        } catch (Exception e) {
            log.warn("✗ Failed to initialize notification setting {}: {}", settingKey, e.getMessage());
        }
    }
}
