package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.BookingInquiryRequest;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripInterest;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class BookingInquiryService {

    private static final Map<String, Object> GENERIC_SUCCESS =
            Map.of("status", "received", "message", "Thank you! We'll get back to you within 24 hours.");

    private final BookingInquiryRepository inquiryRepository;
    private final CustomerEmailRepository customerEmailRepository;
    private final IdObfuscator idObfuscator;
    private final ItineraryRepository itineraryRepository;
    private final NotificationSettingGetterServices notificationSettingGetterServices;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSendingService emailSendingService;

    public BookingInquiryService(BookingInquiryRepository inquiryRepository,
                                 CustomerEmailRepository customerEmailRepository,
                                 IdObfuscator idObfuscator,
                                 ItineraryRepository itineraryRepository,
                                 NotificationSettingGetterServices notificationSettingGetterServices,
                                 EmailTemplateRenderer emailTemplateRenderer,
                                 EmailSendingService emailSendingService) {
        this.inquiryRepository = inquiryRepository;
        this.customerEmailRepository = customerEmailRepository;
        this.idObfuscator = idObfuscator;
        this.itineraryRepository = itineraryRepository;
        this.notificationSettingGetterServices = notificationSettingGetterServices;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.emailSendingService = emailSendingService;
    }

    @Transactional
    public Map<String, Object> submitInquiry(BookingInquiryRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setFirstName(request.getFirstName().trim());
        inquiry.setLastName(request.getLastName().trim());
        inquiry.setEmail(email);
        inquiry.setSource("WEBSITE");
        inquiry.setPreferredLocale(request.getLocale() != null ? request.getLocale() : "en");

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            inquiry.setPhone(request.getPhone().trim());
        }
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            inquiry.setCountry(request.getCountry().trim());
        }

        inquiry.setAdults(request.getAdults() != null ? request.getAdults() : 1);
        inquiry.setChildren(request.getChildren() != null ? request.getChildren() : 0);

        // Parse dates
        inquiry.setPreferredStartDate(parseDate(request.getPreferredStartDate()));
        inquiry.setPreferredEndDate(parseDate(request.getPreferredEndDate()));

        // Parse enums
        inquiry.setBudgetCategory(parseEnum(BudgetCategory.class, request.getBudgetCategory()));
        inquiry.setTripType(parseEnum(TripType.class, request.getTripType()));

        // Structured interests (planner step 1, multi-select)
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            java.util.Set<TripInterest> interests = new java.util.HashSet<>();
            for (String raw : request.getInterests()) {
                TripInterest interest = parseEnum(TripInterest.class, raw);
                if (interest != null) interests.add(interest);
            }
            inquiry.setInterests(interests);
        }

        // Preferred trip length (planner step 2)
        if (request.getPreferredDurationDays() != null && request.getPreferredDurationDays() > 0) {
            inquiry.setPreferredDurationDays(request.getPreferredDurationDays());
        }

        if (request.getSpecialRequests() != null && !request.getSpecialRequests().isBlank()) {
            inquiry.setSpecialRequests(request.getSpecialRequests().trim());
        }
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            inquiry.setMessage(request.getMessage().trim());
        }

        // Resolve safari/itinerary
        resolveItinerary(inquiry, request.getSafariIdentifier());

        // Link to existing customer
        linkToCustomer(inquiry, email);

        // Generate inquiry code: INQ-{####}-{MM}-{YY}
        inquiry.setCode(generateCode());

        inquiryRepository.save(inquiry);
        sendInquiryNotification(inquiry);
        return GENERIC_SUCCESS;
    }

    private String generateCode() {
        YearMonth now = YearMonth.now();
        LocalDateTime startOfMonth = now.atDay(1).atStartOfDay();
        LocalDateTime startOfNextMonth = now.plusMonths(1).atDay(1).atStartOfDay();

        long count = inquiryRepository.countByMonth(startOfMonth, startOfNextMonth);
        int sequenceNumber = (int) count + 1;

        return String.format("INQ-%04d-%02d-%02d", sequenceNumber, now.getMonthValue(), now.getYear() % 100);
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
