package com.itineraryledger.kabengosafaris.Newsletter.Services;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Newsletter.DTOs.NewsletterSubscribeRequest;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import com.itineraryledger.kabengosafaris.Newsletter.Repository.NewsletterSubscriptionRepository;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final CustomerAcknowledgementSender acknowledgements;

    /**
     * Where the confirm and unsubscribe links point.
     *
     * <p>The API's own address, not the website's. The website is a separate application whose
     * routes this service cannot know, and a confirmation link is the one link that has to work
     * on the first click: it is the only thing standing between a typed address and a list we are
     * allowed to write to. The endpoints answer with a small page a person can read.
     */
    @Value("${app.base.url:}")
    private String apiBaseUrl;

    /** How long a confirmation link stays good for. */
    @Value("${app.newsletter.confirm-expiry-days:14}")
    private int confirmExpiryDays;

    public NewsletterService(NewsletterSubscriptionRepository subscriptionRepository,
                            CustomerEmailRepository customerEmailRepository,
                            NotificationSettingGetterServices notificationSettingGetterServices,
                            EmailTemplateRenderer emailTemplateRenderer,
                            EmailSendingService emailSendingService,
                            CustomerAcknowledgementSender acknowledgements) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerEmailRepository = customerEmailRepository;
        this.notificationSettingGetterServices = notificationSettingGetterServices;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.emailSendingService = emailSendingService;
        this.acknowledgements = acknowledgements;
    }

    /*
     * "Check your email", not "subscribed". They are not subscribed until they confirm, and a form
     * that says otherwise trains people to ignore the email that actually matters.
     *
     * The same answer comes back whether the address was new, already on the list, or previously
     * unsubscribed. Anything else turns the form into a way of asking whether a given address is
     * one of our subscribers.
     */
    private static final Map<String, Object> GENERIC_SUCCESS =
            Map.of("status", "confirmation_sent",
                   "message", "Almost there. Please check your email and confirm the address.");

    @Transactional
    public Map<String, Object> subscribe(NewsletterSubscribeRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Optional<NewsletterSubscription> existing = subscriptionRepository.findByEmailIgnoreCase(email);

        if (existing.isPresent()) {
            NewsletterSubscription sub = existing.get();
            boolean alreadyConfirmed = sub.getStatus() == SubscriptionStatus.ACTIVE;
            if (!alreadyConfirmed) {
                /*
                 * Somebody who unsubscribed, bounced, or never confirmed is asked again rather
                 * than quietly switched back on. Re-subscribing silently is how a person who
                 * opted out starts receiving mail again without ever agreeing to it.
                 */
                sub.setStatus(SubscriptionStatus.PENDING_CONFIRMATION);
                sub.setUnsubscribedAt(null);
                sub.setConfirmedAt(null);
            }
            if (request.getName() != null && !request.getName().isBlank()) {
                sub.setName(request.getName().trim());
            }
            if (request.getLocale() != null && !request.getLocale().isBlank()) {
                sub.setPreferredLocale(request.getLocale());
            }
            if (sub.getConfirmToken() == null || sub.getConfirmToken().isBlank()) {
                sub.setConfirmToken(newToken());
            }
            subscriptionRepository.save(sub);
            sendSubscriptionNotification(sub, true);
            if (!alreadyConfirmed) {
                sendConfirmationRequest(sub);
            }
            return GENERIC_SUCCESS;
        }

        NewsletterSubscription subscription = new NewsletterSubscription();
        subscription.setEmail(email);
        subscription.setStatus(SubscriptionStatus.PENDING_CONFIRMATION);
        subscription.setConfirmToken(newToken());
        subscription.setPreferredLocale(request.getLocale() != null ? request.getLocale() : "en");
        subscription.setSource("WEBSITE");

        if (request.getName() != null && !request.getName().isBlank()) {
            subscription.setName(request.getName().trim());
        }

        // Try to link to existing customer by email
        linkToCustomer(subscription, email);

        subscriptionRepository.save(subscription);
        sendSubscriptionNotification(subscription, false);
        sendConfirmationRequest(subscription);
        return GENERIC_SUCCESS;
    }

    /**
     * The click that turns a typed address into a subscriber.
     *
     * <p>Idempotent: a second click on the same link, from a mail client that prefetches or from
     * somebody who forwarded it to themselves, says the same thing rather than failing. An
     * unknown token says so plainly, because the alternative is a page that looks like it worked.
     */
    @Transactional
    public Map<String, Object> confirm(String token) {
        if (token == null || token.isBlank()) {
            return Map.of("status", "invalid", "message", "That confirmation link is not valid.");
        }

        NewsletterSubscription sub = subscriptionRepository.findByConfirmToken(token.trim()).orElse(null);
        if (sub == null) {
            return Map.of("status", "invalid", "message", "That confirmation link is not valid.");
        }

        if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getConfirmedAt() != null) {
            return Map.of("status", "already_confirmed",
                          "message", "This address is already confirmed. Nothing more to do.",
                          "email", sub.getEmail());
        }

        /*
         * An expired link is refused but a fresh one is sent, so somebody who comes back to an old
         * email is not simply turned away with nothing to click.
         */
        if (sub.getSubscribedAt() != null
                && sub.getSubscribedAt().plusDays(confirmExpiryDays).isBefore(LocalDateTime.now())) {
            sub.setConfirmToken(newToken());
            subscriptionRepository.save(sub);
            sendConfirmationRequest(sub);
            return Map.of("status", "expired",
                          "message", "That link had expired, so we have sent you a fresh one.",
                          "email", sub.getEmail());
        }

        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setConfirmedAt(LocalDateTime.now());
        sub.setUnsubscribedAt(null);
        subscriptionRepository.save(sub);

        sendWelcome(sub);
        return Map.of("status", "confirmed",
                      "message", "Thank you. Your subscription is confirmed.",
                      "email", sub.getEmail());
    }

    /**
     * Unsubscribe from the link in a newsletter, which carries a token rather than an address.
     *
     * <p>Kept alongside the email-based method: that one is what the panel and the public form
     * use, this one is what a person in their inbox uses, and only this one proves the person
     * clicking is the person who was written to.
     */
    @Transactional
    public Map<String, Object> unsubscribeByToken(String token) {
        // the same answer either way, so a token cannot be used to test whether an address is on the list
        Map<String, Object> answer = Map.of("status", "unsubscribed",
            "message", "You have been removed. We will not write again.");
        if (token == null || token.isBlank()) return answer;

        subscriptionRepository.findByConfirmToken(token.trim()).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.UNSUBSCRIBED);
            sub.setUnsubscribedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        });
        return answer;
    }

    /** A token nobody can guess. */
    private String newToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String confirmUrl(NewsletterSubscription sub) {
        return link("/api/public/newsletter/confirm?token=", sub);
    }

    private String unsubscribeUrl(NewsletterSubscription sub) {
        return link("/api/public/newsletter/unsubscribe?token=", sub);
    }

    private String link(String path, NewsletterSubscription sub) {
        if (apiBaseUrl == null || apiBaseUrl.isBlank() || sub.getConfirmToken() == null) return "";
        String base = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        return base + path + sub.getConfirmToken();
    }

    /** The one email an unconfirmed address is allowed to receive. */
    private void sendConfirmationRequest(NewsletterSubscription sub) {
        Map<String, String> v = new HashMap<>();
        v.put("email", sub.getEmail());
        v.put("name", sub.getName() != null ? sub.getName() : "");
        v.put("greetingName", acknowledgements.greetingName(null, sub.getName()));
        v.put("confirmUrl", confirmUrl(sub));
        v.put("unsubscribeUrl", unsubscribeUrl(sub));
        v.put("preferredLocale", sub.getPreferredLocale() != null ? sub.getPreferredLocale() : "en");
        v.put("source", sub.getSource() != null ? sub.getSource() : "WEBSITE");
        v.put("subscribedAt", sub.getSubscribedAt() != null
            ? sub.getSubscribedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy")) : "");
        v.put("expiresInDays", String.valueOf(confirmExpiryDays));

        if (v.get("confirmUrl").isBlank()) {
            // a confirmation email with no link is worse than none: it cannot be acted on
            log.error("No app.base.url is configured, so {} cannot be sent a confirmation link "
                + "and can never be added to the list", sub.getEmail());
            return;
        }
        acknowledgements.send("NEWSLETTER_CONFIRM", sub.getEmail(),
            "Please confirm your subscription", v);
    }

    /** Sent once, the moment they confirm. */
    private void sendWelcome(NewsletterSubscription sub) {
        Map<String, String> v = new HashMap<>();
        v.put("email", sub.getEmail());
        v.put("name", sub.getName() != null ? sub.getName() : "");
        v.put("greetingName", acknowledgements.greetingName(null, sub.getName()));
        v.put("confirmedAt", sub.getConfirmedAt() != null
            ? sub.getConfirmedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy")) : "");
        v.put("preferredLocale", sub.getPreferredLocale() != null ? sub.getPreferredLocale() : "en");
        v.put("unsubscribeUrl", unsubscribeUrl(sub));
        acknowledgements.send("NEWSLETTER_WELCOME", sub.getEmail(),
            "Welcome to our safari news", v);
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
