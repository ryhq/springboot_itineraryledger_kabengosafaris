package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.CreateSafariFromItineraryDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * SafariCreateService - Service for creating Safari bookings from Itinerary templates
 *
 * This service performs a deep copy of an Itinerary structure to create a new Safari.
 * The Safari will have actual dates calculated from the startDate.
 */
@Service
@Slf4j
public class SafariCreateService {

    private final SafariRepository safariRepository;
    private final ItineraryRepository itineraryRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final QuoteRepository quoteRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariCreateService(
            SafariRepository safariRepository,
            ItineraryRepository itineraryRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            QuoteRepository quoteRepository,
            IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.itineraryRepository = itineraryRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.quoteRepository = quoteRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new Safari from an Itinerary template
     *
     * @param dto The creation request with itinerary ID and start date
     * @return ResponseEntity with ApiResponse containing the created Safari
     */
    @Transactional
    @AuditLogAnnotation(action = "CREATE_SAFARI", description = "Creating a new safari from itinerary", entityType = "Safari")
    public ResponseEntity<ApiResponse<?>> createSafariFromItinerary(CreateSafariFromItineraryDTO dto) {
        log.info("Creating Safari from Itinerary: {}", dto.getItineraryId());

        try {
            // Decode and validate itinerary ID
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(dto.getItineraryId());
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", dto.getItineraryId(), e);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // Find the source itinerary
            Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Validate itinerary status - only PUBLISHED itineraries can be used
            if (itinerary.getStatus() != Itinerary.ItineraryStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Cannot create safari from itinerary with status: " + itinerary.getStatus().getDisplayName() +
                                        ". Only published itineraries can be used.",
                                "ITINERARY_NOT_PUBLISHED")
                );
            }

            // Decode and validate customer ID
            Long customerId;
            try {
                customerId = idObfuscator.decodeId(dto.getCustomerId());
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", dto.getCustomerId(), e);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            // Find the customer
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Customer not found", "CUSTOMER_NOT_FOUND")
                );
            }

            // Validate customer can book safaris
            if (!customer.canBook()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Customer cannot book safaris. Customer may be inactive or blacklisted.",
                                "CUSTOMER_CANNOT_BOOK")
                );
            }

            // Validate start date
            if (dto.getStartDate() == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Start date is required", "START_DATE_REQUIRED")
                );
            }

            // Validate start date is not in the past
            if (dto.getStartDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Start date cannot be in the past. Provided: " + dto.getStartDate() + ", Today: " + LocalDate.now(),
                                "START_DATE_IN_PAST")
                );
            }

            // Calculate end date based on itinerary totalDays
            LocalDate endDate = dto.getStartDate().plusDays(itinerary.getTotalDays() - 1);

            // Get current user for audit tracking
            User currentUser = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String username = authentication.getName();
                currentUser = userRepository.findByUsername(username).orElse(null);
            }

            // Create the Safari entity (code will be generated after save)
            Safari safari = Safari.builder()
                    .itinerary(itinerary)
                    .customer(customer)
                    .name(dto.getName() != null ? dto.getName() : itinerary.getName())
                    .slug(generateSlug(dto.getName() != null ? dto.getName() : itinerary.getName()))
                    .startDate(dto.getStartDate())
                    .endDate(endDate)
                    .state(SafariState.DRAFT)
                    .totalDays(itinerary.getTotalDays())
                    .totalNights(itinerary.getTotalNights())
                    .carCount(itinerary.getCarCount())
                    .description(dto.getDescription() != null ? dto.getDescription() : itinerary.getDescription())
                    .highlights(itinerary.getHighlights())
                    .startLocation(itinerary.getStartLocation())
                    .endLocation(itinerary.getEndLocation())
                    .specialRequests(dto.getSpecialRequests())
                    .dietaryRequirements(dto.getDietaryRequirements())
                    .emergencyContact(dto.getEmergencyContact())
                    .createdBy(currentUser)
                    .isActive(true)
                    .build();

            // Deep copy pax configuration
            copyPaxConfiguration(itinerary, safari);

            // Deep copy days with all nested entities
            copyDaysStructure(itinerary, safari, dto.getStartDate());

            // Save the Safari to get the ID (cascade will save all nested entities)
            Safari savedSafari = safariRepository.save(safari);

            // Generate and set the code using entity method (requires ID)
            savedSafari.setCode(savedSafari.generateCode());
            savedSafari = safariRepository.save(savedSafari);

            log.info("Safari created successfully with ID: {} and code: {}", savedSafari.getId(), savedSafari.getCode());

            // Convert to DTO and return
            SafariDTO safariDTO = convertToDTO(savedSafari);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Safari created successfully", safariDTO)
            );

        } catch (Exception e) {
            log.error("Error creating Safari from Itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create Safari: " + e.getMessage(), "SAFARI_CREATE_FAILED")
            );
        }
    }

    /**
     * Generate unique URL-friendly slug from name
     * If slug already exists, append a counter to make it unique
     */
    private String generateSlug(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        // Check if slug is unique
        String slug = baseSlug;
        int counter = 1;
        while (safariRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    /**
     * Deep copy pax configuration from Itinerary to Safari
     */
    private void copyPaxConfiguration(Itinerary itinerary, Safari safari) {
        if (itinerary.getPaxList() == null || itinerary.getPaxList().isEmpty()) {
            return;
        }

        for (ItineraryPax itineraryPax : itinerary.getPaxList()) {
            SafariPax safariPax = SafariPax.builder()
                    .nationCategory(itineraryPax.getNationCategory())
                    .ageCategory(itineraryPax.getAgeCategory())
                    .count(itineraryPax.getCount())
                    .notes(itineraryPax.getNotes())
                    .build();

            safari.addPax(safariPax);
        }
    }

    /**
     * Deep copy day structure from Itinerary to Safari
     */
    private void copyDaysStructure(Itinerary itinerary, Safari safari, LocalDate startDate) {
        if (itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
            return;
        }

        for (ItineraryDay itineraryDay : itinerary.getDays()) {
            // Calculate actual date for this day
            LocalDate actualDate = startDate.plusDays(itineraryDay.getDayNumber() - 1);

            SafariDay safariDay = SafariDay.builder()
                    .dayNumber(itineraryDay.getDayNumber())
                    .dayTag(itineraryDay.getDayTag())
                    .title(itineraryDay.getTitle())
                    .actualDate(actualDate)
                    .description(itineraryDay.getDescription())
                    .morningActivities(itineraryDay.getMorningActivities())
                    .afternoonActivities(itineraryDay.getAfternoonActivities())
                    .eveningActivities(itineraryDay.getEveningActivities())
                    .wildlifeHighlights(itineraryDay.getWildlifeHighlights())
                    .scenicHighlights(itineraryDay.getScenicHighlights())
                    .specialNotes(itineraryDay.getSpecialNotes())
                    .startLocation(itineraryDay.getStartLocation())
                    .endLocation(itineraryDay.getEndLocation())
                    .distanceKm(itineraryDay.getDistanceKm())
                    .isOvernight(itineraryDay.getIsOvernight())
                    .mealsIncluded(itineraryDay.getMealsIncluded())
                    .internalNotes(itineraryDay.getInternalNotes())
                    .build();

            // Copy activities
            copyDayActivities(itineraryDay, safariDay);

            // Copy accommodations
            copyDayAccommodations(itineraryDay, safariDay);

            // Copy parks (with nested activities and tariffs)
            copyDayParks(itineraryDay, safariDay);

            safari.addDay(safariDay);
        }
    }

    /**
     * Copy day activities from ItineraryDay to SafariDay
     */
    private void copyDayActivities(ItineraryDay itineraryDay, SafariDay safariDay) {
        if (itineraryDay.getActivities() == null || itineraryDay.getActivities().isEmpty()) {
            return;
        }

        for (ItineraryDayActivity itineraryActivity : itineraryDay.getActivities()) {
            SafariDayActivity safariActivity = SafariDayActivity.builder()
                    .activity(itineraryActivity.getActivity())
                    .sortOrder(itineraryActivity.getSortOrder())
                    .durationHours(itineraryActivity.getDurationHours())
                    .startTime(itineraryActivity.getStartTime())
                    .endTime(itineraryActivity.getEndTime())
                    .notes(itineraryActivity.getNotes())
                    .isIncludedInPrice(itineraryActivity.getIsIncludedInPrice())
                    .isOptional(itineraryActivity.getIsOptional())
                    .isCompleted(false)
                    .isSkipped(false)
                    .build();

            safariDay.addActivity(safariActivity);
        }
    }

    /**
     * Copy day accommodations from ItineraryDay to SafariDay
     */
    private void copyDayAccommodations(ItineraryDay itineraryDay, SafariDay safariDay) {
        if (itineraryDay.getAccommodations() == null || itineraryDay.getAccommodations().isEmpty()) {
            return;
        }

        for (ItineraryDayAccommodation itineraryAccommodation : itineraryDay.getAccommodations()) {
            SafariDayAccommodation safariAccommodation = SafariDayAccommodation.builder()
                    .accommodation(itineraryAccommodation.getAccommodation())
                    .roomType(itineraryAccommodation.getRoomType())
                    .roomStandard(itineraryAccommodation.getRoomStandard())
                    .boardType(itineraryAccommodation.getBoardType())
                    .roomCount(itineraryAccommodation.getRoomCount())
                    .isAlternative(itineraryAccommodation.getIsAlternative())
                    .notes(itineraryAccommodation.getNotes())
                    .bookingStatus(SafariDayAccommodation.BookingStatus.PENDING)
                    .build();

            safariDay.addAccommodation(safariAccommodation);
        }
    }

    /**
     * Copy day parks from ItineraryDay to SafariDay (including nested activities and tariffs)
     */
    private void copyDayParks(ItineraryDay itineraryDay, SafariDay safariDay) {
        if (itineraryDay.getParks() == null || itineraryDay.getParks().isEmpty()) {
            return;
        }

        for (ItineraryDayPark itineraryPark : itineraryDay.getParks()) {
            SafariDayPark safariPark = SafariDayPark.builder()
                    .park(itineraryPark.getPark())
                    .entryType(itineraryPark.getEntryType())
                    .sortOrder(itineraryPark.getSortOrder())
                    .arrivalTime(itineraryPark.getArrivalTime())
                    .departureTime(itineraryPark.getDepartureTime())
                    .notes(itineraryPark.getNotes())
                    .feesPaid(false)
                    .build();

            // Copy park activities
            copyParkActivities(itineraryPark, safariPark);

            // Copy park tariffs
            copyParkTariffs(itineraryPark, safariPark);

            safariDay.addPark(safariPark);
        }
    }

    /**
     * Copy park activities from ItineraryDayPark to SafariDayPark
     */
    private void copyParkActivities(ItineraryDayPark itineraryPark, SafariDayPark safariPark) {
        if (itineraryPark.getParkActivities() == null || itineraryPark.getParkActivities().isEmpty()) {
            return;
        }

        for (ItineraryDayParkActivity itineraryParkActivity : itineraryPark.getParkActivities()) {
            SafariDayParkActivity safariParkActivity = SafariDayParkActivity.builder()
                    .parkActivity(itineraryParkActivity.getParkActivity())
                    .sortOrder(itineraryParkActivity.getSortOrder())
                    .durationHours(itineraryParkActivity.getDurationHours())
                    .notes(itineraryParkActivity.getNotes())
                    .isIncludedInPrice(itineraryParkActivity.getIsIncludedInPrice())
                    .isCompleted(false)
                    .isSkipped(false)
                    .build();

            safariPark.addParkActivity(safariParkActivity);
        }
    }

    /**
     * Copy park tariffs from ItineraryDayPark to SafariDayPark
     */
    private void copyParkTariffs(ItineraryDayPark itineraryPark, SafariDayPark safariPark) {
        if (itineraryPark.getParkTariffs() == null || itineraryPark.getParkTariffs().isEmpty()) {
            return;
        }

        for (ItineraryDayParkTariff itineraryParkTariff : itineraryPark.getParkTariffs()) {
            SafariDayParkTariff safariParkTariff = SafariDayParkTariff.builder()
                    .parkTariff(itineraryParkTariff.getParkTariff())
                    .notes(itineraryParkTariff.getNotes())
                    .isIncludedInPrice(itineraryParkTariff.getIsIncludedInPrice())
                    .isPaid(false)
                    .isWaived(false)
                    .build();

            safariPark.addParkTariff(safariParkTariff);
        }
    }

    /**
     * Create a new Safari from a Quote, deep-copying the Quote's day-tree and
     * pax mix (snapshotted from the Itinerary at quote creation, then edited
     * during negotiation). This is the path used by
     * {@code QuoteStatusService.convertQuote()} for any non-legacy quote.
     *
     * @param quoteId   the decoded primary-key id of the source Quote
     * @param startDate the safari start date (used to compute actualDate per day)
     * @return ResponseEntity with ApiResponse containing the created Safari
     */
    @Transactional
    @AuditLogAnnotation(action = "CREATE_SAFARI", description = "Creating a new safari from quote", entityType = "Safari")
    public ResponseEntity<ApiResponse<?>> createSafariFromQuote(Long quoteId, LocalDate startDate) {
        log.info("Creating Safari from Quote: {}", quoteId);

        try {
            if (quoteId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Quote ID is required", "INVALID_QUOTE_ID"));
            }

            Quote quote = quoteRepository.findById(quoteId).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }

            if (quote.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Quote has no customer", "QUOTE_MISSING_CUSTOMER"));
            }
            if (!quote.getCustomer().canBook()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Customer cannot book safaris. Customer may be inactive or blacklisted.",
                                "CUSTOMER_CANNOT_BOOK"));
            }

            if (startDate == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Start date is required", "START_DATE_REQUIRED"));
            }
            if (startDate.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Start date cannot be in the past. Provided: " + startDate + ", Today: " + LocalDate.now(),
                                "START_DATE_IN_PAST"));
            }

            // Quote must have its own snapshot to use this path. Legacy quotes
            // (no day-tree, no pax) should be converted via
            // createSafariFromItinerary instead.
            if (quote.getDays() == null || quote.getDays().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Quote has no day snapshot. Use createSafariFromItinerary for legacy quotes.",
                                "QUOTE_HAS_NO_DAY_SNAPSHOT"));
            }

            Itinerary itinerary = quote.getItinerary();
            /*
             * The safari describes the QUOTE it was sold from, not the itinerary
             * the quote was priced from. The quote has been negotiated — a day
             * dropped, a route changed — and a booking whose header disagreed
             * with its own days is one the office operates from and gets wrong.
             */
            List<QuoteDay> orderedDays = quote.getDays().stream()
                    .sorted(java.util.Comparator.comparing(
                            QuoteDay::getDayNumber,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .collect(java.util.stream.Collectors.toList());

            int totalDays = orderedDays.size();
            // the days say where the guests sleep; a fly-out ending is not a night
            int totalNights = (int) orderedDays.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsOvernight()))
                    .count();
            LocalDate endDate = startDate.plusDays(totalDays - 1);

            String quoteStartLocation = orderedDays.stream()
                    .map(QuoteDay::getStartLocation)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst()
                    .orElse(itinerary != null ? itinerary.getStartLocation() : null);

            String quoteEndLocation = orderedDays.stream()
                    .map(QuoteDay::getEndLocation)
                    .filter(v -> v != null && !v.isBlank())
                    .reduce((first, second) -> second)
                    .orElse(itinerary != null ? itinerary.getEndLocation() : null);

            User currentUser = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            }

            String safariName = quote.getTitle() != null && !quote.getTitle().isBlank()
                    ? quote.getTitle()
                    : (itinerary != null ? itinerary.getName() : "Safari");

            Safari safari = Safari.builder()
                    .itinerary(itinerary)
                    .customer(quote.getCustomer())
                    .name(safariName)
                    .slug(generateSlug(safariName))
                    .startDate(startDate)
                    .endDate(endDate)
                    .state(SafariState.DRAFT)
                    .totalDays(totalDays)
                    .totalNights(totalNights)
                    .carCount(itinerary != null ? itinerary.getCarCount() : null)
                    .description(quote.getDescription() != null
                            ? quote.getDescription()
                            : (itinerary != null ? itinerary.getDescription() : null))
                    .highlights(itinerary != null ? itinerary.getHighlights() : null)
                    .startLocation(quoteStartLocation)
                    .endLocation(quoteEndLocation)
                    .createdBy(currentUser)
                    .isActive(true)
                    .build();

            // Deep-copy Quote pax → SafariPax
            copyQuotePaxConfiguration(quote, safari);

            // Deep-copy Quote day-tree → SafariDay tree, computing actualDate
            copyQuoteDaysStructure(quote, safari, startDate);

            Safari savedSafari = safariRepository.save(safari);
            savedSafari.setCode(savedSafari.generateCode());
            savedSafari = safariRepository.save(savedSafari);

            log.info("Safari created from Quote {} with ID: {} and code: {}",
                    quote.getId(), savedSafari.getId(), savedSafari.getCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Safari created successfully", convertToDTO(savedSafari)));

        } catch (Exception e) {
            log.error("Error creating Safari from Quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create Safari: " + e.getMessage(), "SAFARI_CREATE_FAILED"));
        }
    }

    // =====================================================================
    // Quote → Safari deep-copy helpers (mirror the Itinerary → Safari helpers
    // above, reading from the Quote tree instead).
    // =====================================================================

    private void copyQuotePaxConfiguration(Quote quote, Safari safari) {
        if (quote.getPaxList() == null || quote.getPaxList().isEmpty()) {
            return;
        }
        for (QuotePax quotePax : quote.getPaxList()) {
            SafariPax safariPax = SafariPax.builder()
                    .nationCategory(quotePax.getNationCategory())
                    .ageCategory(quotePax.getAgeCategory())
                    .count(quotePax.getCount())
                    .notes(quotePax.getNotes())
                    .build();
            safari.addPax(safariPax);
        }
    }

    private void copyQuoteDaysStructure(Quote quote, Safari safari, LocalDate startDate) {
        if (quote.getDays() == null || quote.getDays().isEmpty()) {
            return;
        }
        for (QuoteDay quoteDay : quote.getDays()) {
            LocalDate actualDate = startDate.plusDays(quoteDay.getDayNumber() - 1);

            SafariDay safariDay = SafariDay.builder()
                    .dayNumber(quoteDay.getDayNumber())
                    .dayTag(quoteDay.getDayTag())
                    .title(quoteDay.getTitle())
                    .actualDate(actualDate)
                    .description(quoteDay.getDescription())
                    .morningActivities(quoteDay.getMorningActivities())
                    .afternoonActivities(quoteDay.getAfternoonActivities())
                    .eveningActivities(quoteDay.getEveningActivities())
                    .wildlifeHighlights(quoteDay.getWildlifeHighlights())
                    .scenicHighlights(quoteDay.getScenicHighlights())
                    .specialNotes(quoteDay.getSpecialNotes())
                    .startLocation(quoteDay.getStartLocation())
                    .endLocation(quoteDay.getEndLocation())
                    .distanceKm(quoteDay.getDistanceKm())
                    .isOvernight(quoteDay.getIsOvernight())
                    .mealsIncluded(quoteDay.getMealsIncluded())
                    .internalNotes(quoteDay.getInternalNotes())
                    .build();

            copyQuoteDayActivities(quoteDay, safariDay);
            copyQuoteDayAccommodations(quoteDay, safariDay);
            copyQuoteDayParks(quoteDay, safariDay);

            safari.addDay(safariDay);
        }
    }

    private void copyQuoteDayActivities(QuoteDay quoteDay, SafariDay safariDay) {
        if (quoteDay.getActivities() == null || quoteDay.getActivities().isEmpty()) {
            return;
        }
        for (QuoteDayActivity quoteActivity : quoteDay.getActivities()) {
            SafariDayActivity safariActivity = SafariDayActivity.builder()
                    .activity(quoteActivity.getActivity())
                    .sortOrder(quoteActivity.getSortOrder())
                    .durationHours(quoteActivity.getDurationHours())
                    .startTime(quoteActivity.getStartTime())
                    .endTime(quoteActivity.getEndTime())
                    .notes(quoteActivity.getNotes())
                    .isIncludedInPrice(quoteActivity.getIsIncludedInPrice())
                    .isOptional(quoteActivity.getIsOptional())
                    .isCompleted(false)
                    .isSkipped(false)
                    .build();
            safariDay.addActivity(safariActivity);
        }
    }

    private void copyQuoteDayAccommodations(QuoteDay quoteDay, SafariDay safariDay) {
        if (quoteDay.getAccommodations() == null || quoteDay.getAccommodations().isEmpty()) {
            return;
        }
        for (QuoteDayAccommodation quoteAccommodation : quoteDay.getAccommodations()) {
            SafariDayAccommodation safariAccommodation = SafariDayAccommodation.builder()
                    .accommodation(quoteAccommodation.getAccommodation())
                    .roomType(quoteAccommodation.getRoomType())
                    .roomStandard(quoteAccommodation.getRoomStandard())
                    .boardType(quoteAccommodation.getBoardType())
                    .roomCount(quoteAccommodation.getRoomCount())
                    .isAlternative(quoteAccommodation.getIsAlternative())
                    .notes(quoteAccommodation.getNotes())
                    .bookingStatus(SafariDayAccommodation.BookingStatus.PENDING)
                    .build();
            safariDay.addAccommodation(safariAccommodation);
        }
    }

    private void copyQuoteDayParks(QuoteDay quoteDay, SafariDay safariDay) {
        if (quoteDay.getParks() == null || quoteDay.getParks().isEmpty()) {
            return;
        }
        for (QuoteDayPark quotePark : quoteDay.getParks()) {
            SafariDayPark safariPark = SafariDayPark.builder()
                    .park(quotePark.getPark())
                    .entryType(quotePark.getEntryType())
                    .sortOrder(quotePark.getSortOrder())
                    .arrivalTime(quotePark.getArrivalTime())
                    .departureTime(quotePark.getDepartureTime())
                    .notes(quotePark.getNotes())
                    .feesPaid(false)
                    .build();

            copyQuoteParkActivities(quotePark, safariPark);
            copyQuoteParkTariffs(quotePark, safariPark);

            safariDay.addPark(safariPark);
        }
    }

    private void copyQuoteParkActivities(QuoteDayPark quotePark, SafariDayPark safariPark) {
        if (quotePark.getParkActivities() == null || quotePark.getParkActivities().isEmpty()) {
            return;
        }
        for (QuoteDayParkActivity quoteParkActivity : quotePark.getParkActivities()) {
            SafariDayParkActivity safariParkActivity = SafariDayParkActivity.builder()
                    .parkActivity(quoteParkActivity.getParkActivity())
                    .sortOrder(quoteParkActivity.getSortOrder())
                    .durationHours(quoteParkActivity.getDurationHours())
                    .notes(quoteParkActivity.getNotes())
                    .isIncludedInPrice(quoteParkActivity.getIsIncludedInPrice())
                    .isCompleted(false)
                    .isSkipped(false)
                    .build();
            safariPark.addParkActivity(safariParkActivity);
        }
    }

    private void copyQuoteParkTariffs(QuoteDayPark quotePark, SafariDayPark safariPark) {
        if (quotePark.getParkTariffs() == null || quotePark.getParkTariffs().isEmpty()) {
            return;
        }
        for (QuoteDayParkTariff quoteParkTariff : quotePark.getParkTariffs()) {
            SafariDayParkTariff safariParkTariff = SafariDayParkTariff.builder()
                    .parkTariff(quoteParkTariff.getParkTariff())
                    .notes(quoteParkTariff.getNotes())
                    .isIncludedInPrice(quoteParkTariff.getIsIncludedInPrice())
                    .isPaid(false)
                    .isWaived(false)
                    .build();
            safariPark.addParkTariff(safariParkTariff);
        }
    }

    /**
     * Convert Safari entity to SafariDTO
     */
    private SafariDTO convertToDTO(Safari safari) {
        SafariDTO dto = new SafariDTO();
        dto.setId(idObfuscator.encodeId(safari.getId()));
        dto.setName(safari.getName());
        dto.setCode(safari.getCode());
        dto.setSlug(safari.getSlug());

        // Itinerary reference
        if (safari.getItinerary() != null) {
            dto.setItineraryId(idObfuscator.encodeId(safari.getItinerary().getId()));
            dto.setItineraryName(safari.getItinerary().getName());
            dto.setItineraryCode(safari.getItinerary().getCode());
        }

        // Customer reference
        if (safari.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(safari.getCustomer().getId()));
            dto.setCustomerName(safari.getCustomer().getDisplayName());
            dto.setCustomerCode(safari.getCustomer().getCode());
        }

        // State information (booking/operational)
        dto.setState(safari.getState());
        dto.setStateDisplayName(safari.getState().getDisplayName());
        dto.setStateDescription(safari.getState().getDescription());
        dto.setStateReason(safari.getStateReason());
        dto.setStateChangedAt(safari.getStateChangedAt());

        // Phase information (time-based)
        var phase = safari.getCurrentPhase();
        dto.setPhase(phase);
        dto.setPhaseDisplayName(phase.getDisplayName());
        dto.setPhaseDescription(phase.getDescription());
        dto.setPhaseUrgencyLevel(phase.getUrgencyLevel());
        dto.setPhaseColorCode(phase.getColorCode());

        // Dates
        dto.setStartDate(safari.getStartDate());
        dto.setEndDate(safari.getEndDate());

        // Duration
        dto.setTotalDays(safari.getTotalDays());
        dto.setTotalNights(safari.getTotalNights());
        dto.setCarCount(safari.getCarCount());

        // Descriptions
        dto.setDescription(safari.getDescription());
        dto.setHighlights(safari.getHighlights());
        dto.setStartLocation(safari.getStartLocation());
        dto.setEndLocation(safari.getEndLocation());

        // Safari-specific
        dto.setSpecialRequests(safari.getSpecialRequests());
        dto.setDietaryRequirements(safari.getDietaryRequirements());
        dto.setEmergencyContact(safari.getEmergencyContact());

        // Status flags
        dto.setIsActive(safari.getIsActive());
        dto.setIsEditable(safari.isEditable());
        dto.setIsCancellable(safari.isCancellable());
        dto.setHasStarted(safari.hasStarted());
        dto.setHasEnded(safari.hasEnded());
        dto.setIsInProgress(safari.isInProgress());
        dto.setIsUrgentPhase(safari.isUrgentPhase());

        // Time calculations
        dto.setDaysUntilStart(safari.getDaysUntilStart());
        dto.setDaysSinceEnd(safari.getDaysSinceEnd());
        dto.setCurrentDayNumber(safari.getCurrentDayNumber());

        // Counts
        dto.setTotalPaxCount(safari.getTotalPaxCount());
        dto.setTotalDaysCount(safari.getDays() != null ? safari.getDays().size() : 0);

        // Audit information
        if (safari.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(safari.getCreatedBy().getId()));
            dto.setCreatedByUsername(safari.getCreatedBy().getUsername());
            dto.setCreatedByFullName(safari.getCreatedBy().getUsername());
        }
        if (safari.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(safari.getUpdatedBy().getId()));
            dto.setUpdatedByUsername(safari.getUpdatedBy().getUsername());
            dto.setUpdatedByFullName(safari.getUpdatedBy().getUsername());
        }

        // Metadata
        dto.setCreatedAt(safari.getCreatedAt());
        dto.setUpdatedAt(safari.getUpdatedAt());

        return dto;
    }
}
