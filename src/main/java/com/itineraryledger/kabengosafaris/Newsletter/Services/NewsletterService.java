package com.itineraryledger.kabengosafaris.Newsletter.Services;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Newsletter.DTOs.NewsletterSubscribeRequest;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import com.itineraryledger.kabengosafaris.Newsletter.Repository.NewsletterSubscriptionRepository;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NewsletterService {

    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final CustomerEmailRepository customerEmailRepository;
    private final NotificationSettingGetterServices notificationSettingGetterServices;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;

    public NewsletterService(NewsletterSubscriptionRepository subscriptionRepository,
                            CustomerEmailRepository customerEmailRepository,
                            NotificationSettingGetterServices notificationSettingGetterServices,
                            EmailTemplateRenderer emailTemplateRenderer,
                            EmailSendingService emailSendingService) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerEmailRepository = customerEmailRepository;
        this.notificationSettingGetterServices = notificationSettingGetterServices;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.emailSendingService = emailSendingService;
    }

    private static final Map<String, Object> GENERIC_SUCCESS =
            Map.of("status", "subscribed", "message", "Thank you for subscribing!");

    @Transactional
    public Map<String, Object> subscribe(NewsletterSubscribeRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Optional<NewsletterSubscription> existing = subscriptionRepository.findByEmailIgnoreCase(email);

        if (existing.isPresent()) {
            NewsletterSubscription sub = existing.get();
            if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
                // Re-subscribe silently
                sub.setStatus(SubscriptionStatus.ACTIVE);
                sub.setUnsubscribedAt(null);
            }
            if (request.getName() != null && !request.getName().isBlank()) {
                sub.setName(request.getName().trim());
            }
            if (request.getLocale() != null && !request.getLocale().isBlank()) {
                sub.setPreferredLocale(request.getLocale());
            }
            subscriptionRepository.save(sub);
            sendSubscriptionNotification(sub, true);
            return GENERIC_SUCCESS;
        }

        NewsletterSubscription subscription = new NewsletterSubscription();
        subscription.setEmail(email);
        subscription.setPreferredLocale(request.getLocale() != null ? request.getLocale() : "en");
        subscription.setSource("WEBSITE");

        if (request.getName() != null && !request.getName().isBlank()) {
            subscription.setName(request.getName().trim());
        }

        // Try to link to existing customer by email
        linkToCustomer(subscription, email);

        subscriptionRepository.save(subscription);
        sendSubscriptionNotification(subscription, false);
        return GENERIC_SUCCESS;
    }

    private void linkToCustomer(NewsletterSubscription subscription, String email) {
        try {
            customerEmailRepository.findByEmail(email).ifPresent(customerEmail ->
                subscription.setCustomer(customerEmail.getCustomer())
            );
        } catch (Exception e) {
            // Silently ignore - customer linking is optional
        }
    }

    @Transactional
    public Map<String, Object> unsubscribe(String email) {
        // Always return same response to prevent email enumeration
        subscriptionRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .ifPresent(sub -> {
                    sub.setStatus(SubscriptionStatus.UNSUBSCRIBED);
                    sub.setUnsubscribedAt(LocalDateTime.now());
                    subscriptionRepository.save(sub);
                });

        return Map.of("status", "unsubscribed", "message", "If this email was subscribed, it has been removed");
    }

    private void sendSubscriptionNotification(NewsletterSubscription subscription, boolean isResubscription) {
        try {
            if (!Boolean.TRUE.equals(notificationSettingGetterServices.isNewsletterNotificationEnabled())) {
                log.debug("Newsletter notification is disabled, skipping");
                return;
            }

            List<String> recipientEmails = notificationSettingGetterServices.getNewsletterNotificationEmails();
            if (recipientEmails.isEmpty()) {
                log.debug("No recipient emails configured for newsletter notification");
                return;
            }

            // Pre-extract all entity data synchronously (avoids lazy loading issues in async thread)
            long totalActiveSubscribers = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

            Map<String, String> variables = new HashMap<>();
            variables.put("subscriberEmail", subscription.getEmail());
            variables.put("subscriberName", subscription.getName() != null ? subscription.getName() : "");
            variables.put("subscriptionDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));
            variables.put("preferredLocale", subscription.getPreferredLocale());
            variables.put("source", subscription.getSource() != null ? subscription.getSource() : "WEBSITE");
            variables.put("isResubscription", String.valueOf(isResubscription));
            variables.put("linkedCustomerName", subscription.getCustomer() != null
                    ? subscription.getCustomer().getFirstName() + " " + subscription.getCustomer().getLastName()
                    : "");
            variables.put("totalActiveSubscribers", String.valueOf(totalActiveSubscribers));

            String subject = isResubscription
                    ? "Newsletter Re-subscription: " + subscription.getEmail()
                    : "New Newsletter Subscription: " + subscription.getEmail();

            // Dispatch template rendering + sending asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    String renderedHtml = emailTemplateRenderer.renderTemplate("NEWSLETTER_SUBSCRIPTION", variables);
                    for (String recipientEmail : recipientEmails) {
                        emailSendingService.sendHtmlEmail(recipientEmail, subject, renderedHtml);
                    }
                    log.info("Newsletter subscription notification sent to {} recipients", recipientEmails.size());
                } catch (Exception e) {
                    log.warn("Failed to send newsletter subscription notification for {}: {}",
                            variables.get("subscriberEmail"), e.getMessage());
                }
            });

        } catch (Exception e) {
            log.warn("Failed to prepare newsletter subscription notification for {}: {}",
                    subscription.getEmail(), e.getMessage());
        }
    }
}
