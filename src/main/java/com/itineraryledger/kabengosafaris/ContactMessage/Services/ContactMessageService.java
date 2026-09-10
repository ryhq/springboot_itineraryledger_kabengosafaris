package com.itineraryledger.kabengosafaris.ContactMessage.Services;

import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.ContactMessageRequest;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessage;
import com.itineraryledger.kabengosafaris.ContactMessage.Repository.ContactMessageRepository;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ContactMessageService {

    private static final Map<String, Object> GENERIC_SUCCESS =
            Map.of("status", "received", "message", "Thank you for your message! We'll get back to you shortly.");

    private final ContactMessageRepository contactMessageRepository;
    private final CustomerEmailRepository customerEmailRepository;
    private final NotificationSettingGetterServices notificationSettingGetterServices;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final CustomerAcknowledgementSender acknowledgements;

    public ContactMessageService(ContactMessageRepository contactMessageRepository,
                                 CustomerEmailRepository customerEmailRepository,
                                 NotificationSettingGetterServices notificationSettingGetterServices,
                                 EmailTemplateRenderer emailTemplateRenderer,
                                 EmailSendingService emailSendingService,
                                 CustomerAcknowledgementSender acknowledgements) {
        this.contactMessageRepository = contactMessageRepository;
        this.customerEmailRepository = customerEmailRepository;
        this.notificationSettingGetterServices = notificationSettingGetterServices;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.emailSendingService = emailSendingService;
        this.acknowledgements = acknowledgements;
    }

    @Transactional
    public Map<String, Object> submitContactMessage(ContactMessageRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setName(request.getName().trim());
        contactMessage.setEmail(email);
        contactMessage.setSource("WEBSITE");
        contactMessage.setPreferredLocale(request.getLocale() != null ? request.getLocale() : "en");

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            contactMessage.setPhone(request.getPhone().trim());
        }
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            contactMessage.setSubject(request.getSubject().trim());
        }

        contactMessage.setMessage(request.getMessage().trim());

        // Link to existing customer by email
        linkToCustomer(contactMessage, email);

        // Generate contact message code: MSG-{####}-{MM}-{YY}
        contactMessage.setCode(generateCode());

        contactMessageRepository.save(contactMessage);
        sendContactNotification(contactMessage);
        /*
         * And answer THEM. The page promises a reply from a real specialist rather than an
         * auto-responder, so this one is careful not to pose as the answer: it confirms the
         * message arrived, quotes it back, and names the deadline the page named.
         */
        sendContactAcknowledgement(contactMessage);
        return GENERIC_SUCCESS;
    }

    private String generateCode() {
        YearMonth now = YearMonth.now();
        LocalDateTime startOfMonth = now.atDay(1).atStartOfDay();
        LocalDateTime startOfNextMonth = now.plusMonths(1).atDay(1).atStartOfDay();

        long count = contactMessageRepository.countByMonth(startOfMonth, startOfNextMonth);
        int sequenceNumber = (int) count + 1;

        return String.format("MSG-%04d-%02d-%02d", sequenceNumber, now.getMonthValue(), now.getYear() % 100);
    }

    private void linkToCustomer(ContactMessage contactMessage, String email) {
        try {
            customerEmailRepository.findByEmail(email).ifPresent(customerEmail ->
                contactMessage.setCustomer(customerEmail.getCustomer())
            );
        } catch (Exception ignored) {}
    }

    /** Tell the sender we have their message, and show them what we received. */
    private void sendContactAcknowledgement(ContactMessage contactMessage) {
        try {
            acknowledgements.send(
                "CONTACT_US_RECEIVED",
                contactMessage.getEmail(),
                "We have your message (" + contactMessage.getCode() + ")",
                buildAcknowledgementVariables(contactMessage));
        } catch (Exception e) {
            log.error("Could not acknowledge contact message {}: {}",
                contactMessage.getCode(), e.getMessage(), e);
        }
    }

    /**
     * Everything the CONTACT_US_RECEIVED template is entitled to ask for.
     *
     * <p>Their own message is quoted back in full. It is the only proof they have that the form
     * worked, and a truncated version invites a second submission of the same enquiry.
     */
    Map<String, String> buildAcknowledgementVariables(ContactMessage contactMessage) {
        Map<String, String> v = new HashMap<>();
        v.put("contactCode", contactMessage.getCode());
        v.put("name", nullToEmpty(contactMessage.getName()));
        v.put("greetingName", acknowledgements.greetingName(null, contactMessage.getName()));
        v.put("email", nullToEmpty(contactMessage.getEmail()));
        v.put("phone", nullToEmpty(contactMessage.getPhone()));
        v.put("subject", nullToEmpty(contactMessage.getSubject()));
        v.put("message", nullToEmpty(contactMessage.getMessage()));
        v.put("messageExcerpt", excerpt(contactMessage.getMessage()));
        v.put("source", contactMessage.getSource() != null ? contactMessage.getSource() : "WEBSITE");
        v.put("preferredLocale", contactMessage.getPreferredLocale() != null
            ? contactMessage.getPreferredLocale() : "en");
        v.put("contactDate", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("d MMMM yyyy 'at' HH:mm")));
        return v;
    }

    /** The opening of a long message, for a template that wants a line rather than a page. */
    private String excerpt(String message) {
        if (message == null || message.isBlank()) return "";
        String flat = message.trim().replaceAll("\\s+", " ");
        return flat.length() <= 180 ? flat : flat.substring(0, 179).trim() + "…";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void sendContactNotification(ContactMessage contactMessage) {
        try {
            if (!Boolean.TRUE.equals(notificationSettingGetterServices.isContactMessageNotificationEnabled())) {
                log.debug("Contact message notification is disabled, skipping");
                return;
            }

            List<String> recipientEmails = notificationSettingGetterServices.getContactMessageNotificationEmails();
            if (recipientEmails.isEmpty()) {
                log.debug("No recipient emails configured for contact message notification");
                return;
            }

            // Pre-extract all entity data synchronously (avoids lazy loading issues in async thread)
            Map<String, String> variables = new HashMap<>();
            variables.put("contactCode", contactMessage.getCode());
            variables.put("name", contactMessage.getName());
            variables.put("email", contactMessage.getEmail());
            variables.put("phone", contactMessage.getPhone() != null ? contactMessage.getPhone() : "");
            variables.put("subject", contactMessage.getSubject() != null ? contactMessage.getSubject() : "");
            variables.put("message", contactMessage.getMessage());
            variables.put("source", contactMessage.getSource() != null ? contactMessage.getSource() : "WEBSITE");
            variables.put("preferredLocale", contactMessage.getPreferredLocale() != null
                    ? contactMessage.getPreferredLocale() : "en");
            variables.put("contactDate", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

            String subject = "New Contact Message: " + contactMessage.getCode()
                    + (contactMessage.getSubject() != null ? " - " + contactMessage.getSubject() : "");
            String contactCode = contactMessage.getCode();

            // Dispatch template rendering + sending asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    String renderedHtml = emailTemplateRenderer.renderTemplate("CONTACT_US", variables);
                    for (String recipientEmail : recipientEmails) {
                        emailSendingService.sendHtmlEmail(recipientEmail, subject, renderedHtml);
                    }
                    log.info("Contact message notification sent to {} recipients for message {}",
                            recipientEmails.size(), contactCode);
                } catch (Exception e) {
                    log.warn("Failed to send contact message notification for {}: {}",
                            contactCode, e.getMessage());
                }
            });

        } catch (Exception e) {
            log.warn("Failed to prepare contact message notification for {}: {}",
                    contactMessage.getCode(), e.getMessage());
        }
    }
}
