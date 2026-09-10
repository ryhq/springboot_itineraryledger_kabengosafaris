package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The reply that goes back to whoever filled in a form on the website.
 *
 * <p>Three public forms now answer the person who used them, and all three need the same few
 * things: the company's own contact details as links a customer can act on, the reply commitment
 * the website makes, and a send that does not take the web request down with it if the mail server
 * is having a bad afternoon. Written once here rather than three times, because three copies of a
 * WhatsApp link is three chances for one of them to be wrong.
 *
 * <p>Failures are logged at ERROR, not WARN. An unanswered enquiry is a lost booking, and the
 * booking inquiry notification spent its entire life failing at WARN with nobody reading it.
 */
@Service
@Slf4j
public class CustomerAcknowledgementSender {

    private final EmailTemplateRenderer renderer;
    private final EmailSendingService emailSendingService;
    private final CompanyIdentityService company;

    /**
     * How the public site addresses one itinerary, e.g. {@code /safaris/{code}}.
     *
     * <p>Empty by default, and empty means no link at all. The website is a separate application
     * and this service cannot know its routes; a guessed path in a customer's inbox is a 404 with
     * our name on it, which is worse than the paragraph simply not appearing. The templates wrap
     * every use in a conditional for exactly this reason.
     */
    @Value("${app.website.itinerary-path:}")
    private String itineraryPath;

    /** What the website promises, in hours, so the email cannot contradict the page it came from. */
    @Value("${app.website.reply-within-hours:24}")
    private String replyWithinHours;

    public CustomerAcknowledgementSender(EmailTemplateRenderer renderer,
                                        EmailSendingService emailSendingService,
                                        CompanyIdentityService company) {
        this.renderer = renderer;
        this.emailSendingService = emailSendingService;
        this.company = company;
    }

    /**
     * Render and send, off the request thread.
     *
     * <p>Everything the template needs must already be in {@code variables}: the send runs without
     * a persistence session, so a lazy association read in here would fail where nobody is looking.
     */
    public void send(String eventName, String toEmail, String subject, Map<String, String> variables) {
        if (toEmail == null || toEmail.isBlank()) {
            log.error("No address to acknowledge {} to; nobody will be told we received their submission",
                eventName);
            return;
        }
        Map<String, String> payload = new LinkedHashMap<>(commonVariables());
        payload.putAll(variables);

        CompletableFuture.runAsync(() -> {
            try {
                String html = renderer.renderTemplate(eventName, payload);
                emailSendingService.sendHtmlEmail(toEmail, subject, html);
                log.info("Acknowledged {} to {}", eventName, toEmail);
            } catch (Exception e) {
                log.error("Could not acknowledge {} to {}. They have no way of knowing we received it: {}",
                    eventName, toEmail, e.getMessage(), e);
            }
        });
    }

    /** The company's details in the forms a customer-facing template wants them. */
    public Map<String, String> commonVariables() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("replyWithinHours", replyWithinHours);
        map.put("websiteUrl", nullToEmpty(company.variables().get("companyWebsite")));
        map.put("whatsappUrl", whatsappUrl());
        return map;
    }

    /**
     * A wa.me link built from the company's own number.
     *
     * <p>wa.me takes digits only, with the country code and no plus, so a stored "+255 786 345 408"
     * has to be reduced to 255786345408. A leading zero is a local prefix rather than part of the
     * number, and wa.me cannot dial it, so a number that has not been stored in international form
     * gets no link instead of a broken one.
     */
    public String whatsappUrl() {
        String phone = company.variables().get("companyPhone");
        if (phone == null || phone.isBlank()) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.startsWith("0")) return "";
        return "https://wa.me/" + digits;
    }

    /**
     * The public page for one itinerary, or empty when the deployment has not said where that is.
     */
    public String itineraryUrl(String itineraryCode) {
        if (itineraryCode == null || itineraryCode.isBlank()) return "";
        if (itineraryPath == null || itineraryPath.isBlank()) return "";
        String website = nullToEmpty(company.variables().get("companyWebsite"));
        if (website.isBlank()) return "";
        String base = website.endsWith("/") ? website.substring(0, website.length() - 1) : website;
        String path = itineraryPath.startsWith("/") ? itineraryPath : "/" + itineraryPath;
        return base + path.replace("{code}", itineraryCode);
    }

    /**
     * What to put after "Hello".
     *
     * <p>A first name when we have one, the whole name when the form gave only that, and nothing
     * when we have neither. The point is that the line never reads "Hello ," which is the greeting
     * of an email that was not checked before it was sent.
     */
    public String greetingName(String firstName, String fullName) {
        if (firstName != null && !firstName.isBlank()) return firstName.trim();
        if (fullName == null || fullName.isBlank()) return "there";
        String first = fullName.trim().split("\\s+")[0];
        return first.isBlank() ? "there" : first;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
