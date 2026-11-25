package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import com.itineraryledger.kabengosafaris.Attribution.AttributionService;

import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.BookingInquiryRequest;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripInterest;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class BookingInquiryService {

    private static final Map<String, Object> GENERIC_SUCCESS =
            Map.of("status", "received", "message", "Thank you! We'll get back to you within 24 hours.");

    /** How many times to re-read the month's count when two leads race for the same code. */
    private static final int CODE_ATTEMPTS = 5;

    private final BookingInquiryRepository inquiryRepository;
    private final CustomerEmailRepository customerEmailRepository;
    private final IdObfuscator idObfuscator;
    private final ItineraryRepository itineraryRepository;
    private final ParkRepository parkRepository;
    private final NotificationSettingGetterServices notificationSettingGetterServices;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;
    private final CustomerAcknowledgementSender acknowledgements;
    private final AttributionService attributionService;

    public BookingInquiryService(BookingInquiryRepository inquiryRepository,
                                 CustomerEmailRepository customerEmailRepository,
                                 IdObfuscator idObfuscator,
                                 ItineraryRepository itineraryRepository,
                                 ParkRepository parkRepository,
                                 NotificationSettingGetterServices notificationSettingGetterServices,
                                 EmailTemplateRenderer emailTemplateRenderer,
                                 EmailSendingService emailSendingService,
                                 CustomerAcknowledgementSender acknowledgements,
                                 AttributionService attributionService) {
        this.inquiryRepository = inquiryRepository;
        this.customerEmailRepository = customerEmailRepository;
        this.idObfuscator = idObfuscator;
        this.itineraryRepository = itineraryRepository;
        this.parkRepository = parkRepository;
        this.notificationSettingGetterServices = notificationSettingGetterServices;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.emailSendingService = emailSendingService;
        this.acknowledgements = acknowledgements;
        this.attributionService = attributionService;
    }

    /**
     * The whole enquiry in one request, as the form has always sent it.
     *
     * <p>Kept exactly as it was so that a website still running the old build goes on working
     * while the new one is deployed. New traffic arrives through {@link #startDraft} instead.
     */
    @Transactional
    public Map<String, Object> submitInquiry(BookingInquiryRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setEmail(email);
        inquiry.setSource("WEBSITE");
        /*
         * `source` stays the literal it has always been: it is parsed into CustomerSource
         * when this inquiry is converted, so a channel name written into it would fall back
         * to WEBSITE and lose exactly what we are trying to record. The channel goes beside
         * it, derived on the server from tags the browser merely carried.
         */
        inquiry.setAttribution(attributionService.from(request.getAttribution()));

        applyRequest(inquiry, request);
        linkToCustomer(inquiry, email);
        inquiry.setSubmittedAt(LocalDateTime.now());

        saveWithCode(inquiry);
        sendInquiryNotification(inquiry);
        /*
         * And tell THEM. Somebody who fills in ten steps of a planner and receives nothing has no
         * way of knowing the form worked, and the natural next move is to try a competitor's.
         */
        sendInquiryAcknowledgement(inquiry);
        return GENERIC_SUCCESS;
    }

    /**
     * Copy whatever the request actually carries onto the inquiry, and nothing else.
     *
     * <p>Every field is guarded, because this runs for a step as well as for a whole form. A
     * request that says nothing about the dates must leave the dates alone rather than clear
     * them: the visitor answering "how many of you?" has not changed their mind about when.
     */
    private void applyRequest(BookingInquiry inquiry, BookingInquiryRequest request) {
        if (notBlank(request.getFirstName())) inquiry.setFirstName(request.getFirstName().trim());
        if (notBlank(request.getLastName())) inquiry.setLastName(request.getLastName().trim());
        if (notBlank(request.getLocale())) inquiry.setPreferredLocale(request.getLocale());
        if (notBlank(request.getPhone())) inquiry.setPhone(request.getPhone().trim());
        if (notBlank(request.getCountry())) inquiry.setCountry(request.getCountry().trim());

        if (request.getAdults() != null) inquiry.setAdults(request.getAdults());
        if (request.getChildren() != null) inquiry.setChildren(request.getChildren());

        LocalDate start = parseDate(request.getPreferredStartDate());
        if (start != null) inquiry.setPreferredStartDate(start);
        LocalDate end = parseDate(request.getPreferredEndDate());
        if (end != null) inquiry.setPreferredEndDate(end);

        BudgetCategory budget = parseEnum(BudgetCategory.class, request.getBudgetCategory());
        if (budget != null) inquiry.setBudgetCategory(budget);
        TripType tripType = parseEnum(TripType.class, request.getTripType());
        if (tripType != null) inquiry.setTripType(tripType);

        // Structured interests (planner step 1, multi-select)
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            java.util.Set<TripInterest> interests = new java.util.HashSet<>();
            for (String raw : request.getInterests()) {
                TripInterest interest = parseEnum(TripInterest.class, raw);
                if (interest != null) interests.add(interest);
            }
            inquiry.setInterests(interests);
        }

        // Preferred trip length (planner step)
        if (request.getPreferredDurationDays() != null && request.getPreferredDurationDays() > 0) {
            inquiry.setPreferredDurationDays(request.getPreferredDurationDays());
        }

        // Destination parks (planner "Where" step) - resolve identifiers to Park entities
        if (request.getDestinationParkIds() != null && !request.getDestinationParkIds().isEmpty()) {
            java.util.Set<Park> parks = new java.util.HashSet<>();
            for (String ident : request.getDestinationParkIds()) {
                Park park = resolvePark(ident);
                if (park != null) parks.add(park);
            }
            inquiry.setDestinationParks(parks);
        }

        if (notBlank(request.getSpecialRequests())) {
            inquiry.setSpecialRequests(request.getSpecialRequests().trim());
        }
        if (notBlank(request.getMessage())) inquiry.setMessage(request.getMessage().trim());

        // Resolve safari/itinerary
        resolveItinerary(inquiry, request.getSafariIdentifier());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Give the inquiry a code nothing else is using, then save it.
     *
     * <p>Catching the constraint violation and retrying is <b>not</b> an option here: these
     * callers are all {@code @Transactional}, and a {@code DataIntegrityViolationException}
     * marks the transaction rollback-only, so the retry would fail at commit having looked
     * like it worked. So the free number is found before the write, not after it.
     *
     * <p>The remaining race - two requests picking the same free number in the same instant -
     * is what the unique key is for, and is no more likely than it was before the form started
     * saving as it goes, because only the first step of a form allocates a code at all.
     */
    private void saveWithCode(BookingInquiry inquiry) {
        YearMonth now = YearMonth.now();
        int sequence = inquiryRepository.highestSequenceInMonth(monthPattern(now)) + 1;

        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++, sequence++) {
            String candidate = formatCode(sequence, now);
            if (!inquiryRepository.existsByCode(candidate)) {
                inquiry.setCode(candidate);
                inquiryRepository.save(inquiry);
                return;
            }
        }
        throw new IllegalStateException("Could not allocate an inquiry code for " + now);
    }

    /**
     * The highest number used this month, plus one.
     *
     * <p>It used to count the month's rows instead, which is a different number as soon as one
     * is deleted: the count drops, the next lead is handed a code that already exists, and it
     * collides every time from then on rather than once.
     */
    private String generateCode() {
        YearMonth now = YearMonth.now();
        return formatCode(inquiryRepository.highestSequenceInMonth(monthPattern(now)) + 1, now);
    }

    private static String formatCode(int sequence, YearMonth month) {
        return String.format("INQ-%04d-%02d-%02d", sequence, month.getMonthValue(), month.getYear() % 100);
    }

    private static String monthPattern(YearMonth month) {
        return String.format("INQ-%%-%02d-%02d", month.getMonthValue(), month.getYear() % 100);
    }

    /* ------------------------------------------------------- a lead, one step at a time */

    /**
     * Write the lead down the moment we know who it is.
     *
     * <p>A first name and an email is a lead. Everything after it is detail, and detail is
     * what the old form spent ten questions collecting before it saved anything - so somebody
     * who stopped at question six left nothing behind at all, not even a trace that they had
     * been interested.
     *
     * <p><b>No email is sent here.</b> Acknowledging an enquiry that has not been made yet
     * would be worse than saying nothing: the visitor is still filling the form in.
     */
    @Transactional
    public Map<String, Object> startDraft(BookingInquiryRequest request) {
        if (notBlank(request.getWebsite())) {
            /*
             * The honeypot was filled, so nothing human sent this. Answer exactly as a real
             * save does - a bot told it has been caught comes back having learned something,
             * and the token is useless because no row exists behind it.
             */
            log.info("Discarding a booking inquiry draft that filled the honeypot");
            return Map.of("status", "saved", "draftToken", UUID.randomUUID().toString(), "code", "");
        }

        String email = request.getEmail().trim().toLowerCase();

        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setEmail(email);
        inquiry.setSource("WEBSITE");
        inquiry.setAttribution(attributionService.from(request.getAttribution()));
        inquiry.setDraftToken(UUID.randomUUID().toString());
        inquiry.setLastStep(request.getStep() != null ? request.getStep() : 1);
        inquiry.setLastStepAt(LocalDateTime.now());

        applyRequest(inquiry, request);
        linkToCustomer(inquiry, email);

        saveWithCode(inquiry);
        log.info("Lead {} started by {}", inquiry.getCode(), email);

        return Map.of("status", "saved", "draftToken", inquiry.getDraftToken(), "code", inquiry.getCode());
    }

    /**
     * Add what the visitor has just answered to the lead that is already there.
     *
     * <p>Only the fields the request actually carries are touched, so a step that asks about
     * dates cannot wipe the party size answered on the step before it.
     */
    @Transactional
    public Map<String, Object> appendToDraft(String draftToken, BookingInquiryRequest request) {
        BookingInquiry inquiry = openDraft(draftToken);

        applyRequest(inquiry, request);
        if (request.getStep() != null && (inquiry.getLastStep() == null || request.getStep() > inquiry.getLastStep())) {
            inquiry.setLastStep(request.getStep());
        }
        inquiry.setLastStepAt(LocalDateTime.now());
        inquiryRepository.save(inquiry);

        return Map.of("status", "saved", "code", inquiry.getCode());
    }

    /**
     * The visitor has finished. This is the call that tells anybody.
     *
     * <p>Both emails hang off this one method and off nothing else, which is the guard that
     * matters: without it every abandoned first step would thank somebody for an enquiry they
     * never sent.
     */
    @Transactional
    public Map<String, Object> completeDraft(String draftToken, BookingInquiryRequest request) {
        BookingInquiry inquiry = openDraft(draftToken);

        applyRequest(inquiry, request);
        refreshLastTouch(inquiry, request);
        linkToCustomer(inquiry, inquiry.getEmail());

        inquiry.setSubmittedAt(LocalDateTime.now());
        inquiry.setLastStepAt(inquiry.getSubmittedAt());
        if (request.getStep() != null) inquiry.setLastStep(request.getStep());
        inquiryRepository.save(inquiry);

        sendInquiryNotification(inquiry);
        sendInquiryAcknowledgement(inquiry);

        return Map.of("status", "received", "code", inquiry.getCode(),
                "message", "Thank you! We'll get back to you within 24 hours.");
    }

    /**
     * Find the lead this browser is partway through, and refuse it if it is already finished.
     *
     * <p>Refusing rather than reopening is what makes a retried request safe: a flaky
     * connection that sends the final step twice gets one lead and one acknowledgement, not
     * two of each.
     */
    private BookingInquiry openDraft(String draftToken) {
        BookingInquiry inquiry = inquiryRepository.findByDraftToken(draftToken)
                .orElseThrow(() -> new IllegalArgumentException("That form has expired. Please start again."));
        if (inquiry.isSubmitted()) {
            throw new IllegalStateException("This enquiry has already been sent to us.");
        }
        return inquiry;
    }

    /**
     * If they came back through a different ad before finishing, credit the one that closed it.
     *
     * <p>{@link AttributionService#merge} already keeps the first touch and takes the last,
     * which is exactly the rule here, so this only decides whether there is anything to merge.
     */
    private void refreshLastTouch(BookingInquiry inquiry, BookingInquiryRequest request) {
        if (request.getAttribution() == null || inquiry.getAttribution() == null) return;

        String arriving = request.getAttribution().getClickId();
        String stored = inquiry.getAttribution().getClickId();
        if (arriving == null || arriving.equals(stored)) return;

        inquiry.setAttribution(
                attributionService.merge(inquiry.getAttribution(),
                        attributionService.from(request.getAttribution())));
    }

    private void resolveItinerary(BookingInquiry inquiry, String safariIdentifier) {
        if (safariIdentifier == null || safariIdentifier.isBlank()) return;
        try {
            // Try decode as obfuscated ID first
            Long id = idObfuscator.decodeId(safariIdentifier.trim());
            if (id != null) {
                itineraryRepository.findById(id).ifPresent(itinerary -> {
                    inquiry.setItinerary(itinerary);
                    inquiry.setItineraryName(itinerary.getName());
                });
                return;
            }
        } catch (Exception ignored) {}
        try {
            // Try by code
            itineraryRepository.findByCodeIgnoreCase(safariIdentifier.trim()).ifPresent(itinerary -> {
                inquiry.setItinerary(itinerary);
                inquiry.setItineraryName(itinerary.getName());
            });
        } catch (Exception ignored) {}
    }

    /** Resolve a park by obfuscated id or slug (mirrors the public entity resolver). */
    private Park resolvePark(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        String id = identifier.trim();
        try {
            Long decoded = idObfuscator.decodeId(id);
            if (decoded != null) {
                Park byId = parkRepository.findById(decoded).orElse(null);
                if (byId != null) return byId;
            }
        } catch (Exception ignored) {}
        try {
            return parkRepository.findBySlug(id).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void linkToCustomer(BookingInquiry inquiry, String email) {
        try {
            customerEmailRepository.findByEmail(email).ifPresent(customerEmail ->
                inquiry.setCustomer(customerEmail.getCustomer())
            );
        } catch (Exception ignored) {}
    }

    private void sendInquiryNotification(BookingInquiry inquiry) {
        try {
            if (!Boolean.TRUE.equals(notificationSettingGetterServices.isBookingInquiryNotificationEnabled())) {
                log.debug("Booking inquiry notification is disabled, skipping");
                return;
            }

            List<String> recipientEmails = notificationSettingGetterServices.getBookingInquiryNotificationEmails();
            if (recipientEmails.isEmpty()) {
                log.debug("No recipient emails configured for booking inquiry notification");
                return;
            }

            Map<String, String> variables = buildNotificationVariables(inquiry);

            String subject = "New Booking Inquiry: " + inquiry.getCode() + " - " + inquiry.getDisplayName();
            String inquiryCode = inquiry.getCode();

            // Dispatch template rendering + sending asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    String renderedHtml = emailTemplateRenderer.renderTemplate("BOOKING_INQUIRY", variables);
                    for (String recipientEmail : recipientEmails) {
                        emailSendingService.sendHtmlEmail(recipientEmail, subject, renderedHtml);
                    }
                    log.info("Booking inquiry notification sent to {} recipients for inquiry {}",
                            recipientEmails.size(), inquiryCode);
                } catch (Exception e) {
                    log.warn("Failed to send booking inquiry notification for {}: {}",
                            inquiryCode, e.getMessage());
                }
            });

        } catch (Exception e) {
            log.warn("Failed to prepare booking inquiry notification for {}: {}",
                    inquiry.getCode(), e.getMessage());
        }
    }

    /**
     * Everything the BOOKING_INQUIRY template is entitled to ask for.
     *
     * <p>Package-private and separated from the send so it can be checked against the event's own
     * schema. The renderer rejects a template render when a REQUIRED variable is missing or blank,
     * and it does so inside an async block whose only reaction is a logged warning. A name that
     * drifts between this map and the schema therefore stops every inquiry email silently, which
     * is how no booking inquiry notification was ever delivered.
     *
     * <p>Entity data is read here, on the caller's thread, because the async send has no session
     * and a lazy collection would fail to load in it.
     */
    Map<String, String> buildNotificationVariables(BookingInquiry inquiry) {
        // Pre-extract all entity data synchronously (avoids lazy loading issues in async thread)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        Map<String, String> variables = new HashMap<>();
        variables.put("inquiryCode", inquiry.getCode());
        variables.put("firstName", inquiry.getFirstName());
        variables.put("lastName", inquiry.getLastName());
        variables.put("email", inquiry.getEmail());
        variables.put("phone", inquiry.getPhone() != null ? inquiry.getPhone() : "");
        variables.put("country", inquiry.getCountry() != null ? inquiry.getCountry() : "");
        variables.put("adults", String.valueOf(inquiry.getAdults()));
        variables.put("children", String.valueOf(inquiry.getChildren()));
        variables.put("totalTravelers", String.valueOf(inquiry.getTotalTravelers()));
        variables.put("preferredStartDate", inquiry.getPreferredStartDate() != null
                ? inquiry.getPreferredStartDate().format(dateFormatter) : "");
        variables.put("preferredEndDate", inquiry.getPreferredEndDate() != null
                ? inquiry.getPreferredEndDate().format(dateFormatter) : "");
        variables.put("budgetCategory", inquiry.getBudgetCategory() != null
                ? inquiry.getBudgetCategory().name() : "");
        variables.put("tripType", inquiry.getTripType() != null
                ? inquiry.getTripType().name() : "");
        variables.put("interests", inquiry.getInterests() != null && !inquiry.getInterests().isEmpty()
                ? inquiry.getInterests().stream().map(TripInterest::getDisplayName)
                    .collect(java.util.stream.Collectors.joining(", "))
                : "");
        variables.put("preferredDurationDays", inquiry.getPreferredDurationDays() != null
                ? String.valueOf(inquiry.getPreferredDurationDays()) : "");
        variables.put("destinations", inquiry.getDestinationParks() != null && !inquiry.getDestinationParks().isEmpty()
                ? inquiry.getDestinationParks().stream().map(Park::getName)
                    .collect(java.util.stream.Collectors.joining(", "))
                : "");
        variables.put("specialRequests", inquiry.getSpecialRequests() != null
                ? inquiry.getSpecialRequests() : "");
        variables.put("message", inquiry.getMessage() != null ? inquiry.getMessage() : "");
        variables.put("source", inquiry.getSource() != null ? inquiry.getSource() : "WEBSITE");
        variables.put("preferredLocale", inquiry.getPreferredLocale() != null
                ? inquiry.getPreferredLocale() : "en");
        variables.put("inquiryDate", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

        Itinerary itinerary = inquiry.getItinerary();
        if (itinerary != null) {
            variables.put("itineraryName", itinerary.getName() != null ? itinerary.getName() : "");
            variables.put("itineraryCode", itinerary.getCode() != null ? itinerary.getCode() : "");
            variables.put("itineraryTotalDays", itinerary.getTotalDays() != null
                    ? String.valueOf(itinerary.getTotalDays()) : "");
            variables.put("itineraryTotalNights", itinerary.getTotalNights() != null
                    ? String.valueOf(itinerary.getTotalNights()) : "");
            variables.put("itineraryStartLocation", itinerary.getStartLocation() != null
                    ? itinerary.getStartLocation() : "");
            variables.put("itineraryEndLocation", itinerary.getEndLocation() != null
                    ? itinerary.getEndLocation() : "");
            variables.put("itineraryDescription", itinerary.getDescription() != null
                    ? itinerary.getDescription() : "");
        } else {
            variables.put("itineraryName", "");
            variables.put("itineraryCode", "");
            variables.put("itineraryTotalDays", "");
            variables.put("itineraryTotalNights", "");
            variables.put("itineraryStartLocation", "");
            variables.put("itineraryEndLocation", "");
            variables.put("itineraryDescription", "");
        }


        return variables;
    }

    /**
     * Tell the enquirer we have it, and read their answers back.
     *
     * <p>Reading it back is the useful part rather than politeness: dates, party size and comfort
     * level are exactly what is cheap to correct today and expensive to correct after a quote has
     * been built on them.
     */
    private void sendInquiryAcknowledgement(BookingInquiry inquiry) {
        try {
            /*
             * The locale arrived with the form and was then dropped: a German family filled in the
             * German site, "de" was stored on this inquiry, and the acknowledgement went out in
             * English. It is translated on the way out now, the same way a PDF is, and falls back
             * to English whenever that cannot be done.
             */
            acknowledgements.send(
                "BOOKING_INQUIRY_RECEIVED",
                inquiry.getEmail(),
                "We have your safari enquiry (" + inquiry.getCode() + ")",
                buildAcknowledgementVariables(inquiry),
                inquiry.getPreferredLocale());
        } catch (Exception e) {
            log.error("Could not acknowledge inquiry {}: {}", inquiry.getCode(), e.getMessage(), e);
        }
    }

    /**
     * Everything the BOOKING_INQUIRY_RECEIVED template is entitled to ask for.
     *
     * <p>Every answer the planner captures is supplied whether or not the default template prints
     * it, so a different template can be written for a different tone without touching any Java.
     * Read on the caller's thread, because the send has no session and a lazy collection would
     * fail to load inside it.
     */
    Map<String, String> buildAcknowledgementVariables(BookingInquiry inquiry) {
        DateTimeFormatter readable = DateTimeFormatter.ofPattern("d MMMM yyyy");
        Map<String, String> v = new HashMap<>(buildNotificationVariables(inquiry));

        v.put("greetingName", acknowledgements.greetingName(
            inquiry.getFirstName(),
            inquiry.getDisplayName()));

        // the party in one phrase, because "2 / 1" needs a caption and "2 adults and 1 child" does not
        v.put("travellersSummary", travellersSummary(inquiry));

        String start = inquiry.getPreferredStartDate() != null
            ? inquiry.getPreferredStartDate().format(readable) : "";
        String end = inquiry.getPreferredEndDate() != null
            ? inquiry.getPreferredEndDate().format(readable) : "";
        v.put("preferredDatesSummary",
            !start.isEmpty() && !end.isEmpty() ? start + " to " + end
                : !start.isEmpty() ? "from " + start
                : end.isEmpty() ? "" : "until " + end);

        v.put("budgetCategoryLabel", inquiry.getBudgetCategory() != null
            ? readableEnum(inquiry.getBudgetCategory().name()) : "");
        v.put("tripTypeLabel", inquiry.getTripType() != null
            ? readableEnum(inquiry.getTripType().name()) : "");

        Itinerary itinerary = inquiry.getItinerary();
        v.put("itineraryUrl", itinerary != null
            ? acknowledgements.itineraryUrl(itinerary.getCode()) : "");

        return v;
    }

    /** "2 adults and 1 child", or "1 adult" - never a pair of bare numbers. */
    private String travellersSummary(BookingInquiry inquiry) {
        int adults = inquiry.getAdults() != null ? inquiry.getAdults() : 0;
        int children = inquiry.getChildren() != null ? inquiry.getChildren() : 0;
        StringBuilder out = new StringBuilder();
        if (adults > 0) out.append(adults).append(adults == 1 ? " adult" : " adults");
        if (children > 0) {
            if (out.length() > 0) out.append(" and ");
            out.append(children).append(children == 1 ? " child" : " children");
        }
        return out.toString();
    }

    /** MID_RANGE becomes "Mid range": the enum names are ours, not the customer's. */
    private String readableEnum(String name) {
        String spaced = name.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
