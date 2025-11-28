// package com.itineraryledger.kabengosafaris.EmailAccount.Components;

// import org.springframework.boot.context.event.ApplicationReadyEvent;
// import org.springframework.context.event.EventListener;
// import org.springframework.core.annotation.Order;
// import org.springframework.stereotype.Component;

// import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
// import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
// import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// /**
//  * EmailAccountInitializer - Initializes default email accounts on startup
//  *
//  * This component:
//  * - Creates default email accounts if they don't exist
//  * - Sets up placeholder configurations for common email providers
//  * - Runs after other initializers (AuditLogSettingInitializer,
//  * SecuritySettingsInitializer)
//  *
//  * Default configurations created:
//  * - Default SMTP: Generic configuration for custom SMTP servers
//  * - Gmail: Pre-configured for Gmail SMTP
//  * - Outlook: Pre-configured for Outlook SMTP
//  *
//  * NOTE: These are PLACEHOLDER configurations - they must be updated with actual
//  * credentials
//  */
// @Component
// @RequiredArgsConstructor
// @Slf4j
// @Order(30) // Run after other initializers (SecuritySettingsInitializer=10,
//            // AuditLogSettingInitializer=20)
// public class EmailAccountInitializer {

//     private final EmailAccountRepository emailAccountRepository;

//     @EventListener(ApplicationReadyEvent.class)
//     public void initializeEmailAccounts() {
//         log.info("\n\n\n");
//         log.info("=============================================");
//         log.info("Email Account initialization...");
//         log.info("=============================================");
//         log.info("\n");

//         try {
//             // Create Gmail configuration template if it doesn't exist
//             if (!emailAccountRepository.existsByProviderType(EmailAccountProvider.GMAIL)) {
//                 createEmailAccount("GMAIL", "example@gmail.com", EmailAccountProvider.GMAIL);
//             }
//             // Create OUTLOOK configuration template if it doesn't exist
//             if (!emailAccountRepository.existsByProviderType(EmailAccountProvider.OUTLOOK)) {
//                 createEmailAccount("OUTLOOK", "example@outlook.com", EmailAccountProvider.OUTLOOK);
//             }
//             // Create OUTLOOK configuration template if it doesn't exist
//             if (!emailAccountRepository.existsByProviderType(EmailAccountProvider.CUSTOM)) {
//                 createEmailAccount("CUSTOM", "example@domain.com", EmailAccountProvider.CUSTOM);
//             }
//         } catch (Exception e) {
//             log.error("Error initializing email configurations", e);
//         }

//         log.info("\n\n\n");
//         log.info("=============================================");
//         log.info("Email Account initialization Complete.");
//         log.info("=============================================");
//         log.info("\n");

//     }

//     private void createEmailAccount(String name, String email, EmailAccountProvider emailAccountProvider) {
//         log.info("Creating email account template for {} ", emailAccountProvider);

//         EmailAccount emailAccount = EmailAccount.builder()
//             .email(email)
//             .name(name)
//             .providerType(emailAccountProvider)
//             .description(emailAccountProvider.getDisplayName() + " Account - Update with your credentials")
//             .enabled(false) // Disabled by default until configured
//             .isDefault(false)
//             .rateLimitPerMinute(0)
//             .maxRetryAttempts(3)
//             .retryDelaySeconds(5)
//             .emailsSentCount(0L)
//             .emailsFailedCount(0L)
//             .createdBy("SYSTEM")
//             .updatedBy("SYSTEM")
//             // Placeholder password - MUST BE UPDATED
//             .smtpPassword(EncryptionUtil.encrypt("your-app-password"))
//             .useTls(true)
//             .useSsl(false)
//             .smtpHost("smtp.domain")
//             .smtpPort(587)
//             .smtpUsername("your-email@domain.com")
//             .build();

//         emailAccountRepository.save(emailAccount);
//     }
// }
