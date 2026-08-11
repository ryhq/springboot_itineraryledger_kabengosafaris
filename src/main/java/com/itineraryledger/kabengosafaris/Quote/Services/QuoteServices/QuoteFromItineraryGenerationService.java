package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.DTOs.CreateQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.CreateQuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices.QuoteItemCreateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QuoteFromItineraryGenerationService - Generates a quote from an itinerary cost estimation
 *
 * This service bridges the Itinerary and Quote modules by:
 * 1. Using ItineraryCostEstimationService to calculate costs
 * 2. Creating a new Quote entity
 * 3. Converting cost line items to QuoteItems
 * 4. Setting up proper relationships and totals
 */
@Service
@Slf4j
@Transactional
public class QuoteFromItineraryGenerationService {

    private final ItineraryCostEstimationService costEstimationService;
    private final QuoteCreateService quoteCreateService;
    private final QuoteItemCreateService quoteItemCreateService;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final ItineraryRepository itineraryRepository;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuoteFromItineraryGenerationService(
            ItineraryCostEstimationService costEstimationService,
            QuoteCreateService quoteCreateService,
            QuoteItemCreateService quoteItemCreateService,
            QuoteCostEstimationService quoteCostEstimationService,
            ItineraryRepository itineraryRepository,
            CustomerRepository customerRepository,
            QuoteRepository quoteRepository,
            IdObfuscator idObfuscator
    ) {
        this.costEstimationService = costEstimationService;
        this.quoteCreateService = quoteCreateService;
        this.quoteItemCreateService = quoteItemCreateService;
        this.quoteCostEstimationService = quoteCostEstimationService;
        this.itineraryRepository = itineraryRepository;
        this.customerRepository = customerRepository;
        this.quoteRepository = quoteRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Generate a quote from an itinerary for a specific customer
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param customerIdObfuscated The obfuscated customer ID
     * @param startDate Optional start date for cost estimation (defaults to today)
     * @param currency Preferred currency for cost estimation (default: USD)
     * @param useStoRate Whether to use STO rates (true) or RACK rates (false) - default: false
     * @param validityDays Number of days the quote should be valid (default: 30)
     * @param taxPercentage Optional tax percentage to apply (can be null)
     * @param discountPercentage Optional discount percentage to apply
     * @param discountReason Optional reason for discount
     * @return ResponseEntity with the created quote
     */
    public ResponseEntity<ApiResponse<?>> generateQuoteFromItinerary(
            String itineraryIdObfuscated,
            String customerIdObfuscated,
            LocalDate startDate,
            String currency,
            Boolean useStoRate,
            Integer validityDays,
            BigDecimal taxPercentage,
            BigDecimal discountPercentage,
            String discountReason,
            BigDecimal agentCommissionPercentage,
            String agentCommissionReason,
            BigDecimal marginUpliftPercentage,
            String marginUpliftReason,
            Boolean condense
    ) {
        boolean condenseLineItems = Boolean.TRUE.equals(condense);
        log.info("Generating quote for itinerary: {} and customer: {}", itineraryIdObfuscated, customerIdObfuscated);

        // Default useStoRate to false if not provided
        boolean useRackRates = useStoRate != null ? useStoRate : false;

        try {
            // 1. Validate inputs and decode IDs
            Long itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            Long customerId = idObfuscator.decodeId(customerIdObfuscated);

            if (itineraryId == null || customerId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid itinerary or customer ID", "INVALID_ID")
                );
            }

            // Verify entities exist
            Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            /*
             * The gate belongs here, not at conversion.
             *
             * It used to sit on convert: you could quote a draft itinerary, the
             * customer could accept it, and only then were you told the product
             * was not published — an accepted quote that could not be fulfilled.
             * Worse, archiving an old itinerary broke every accepted quote
             * against it, months after anyone touched either record.
             *
             * Stopping it here stops it before the customer is involved. A quote
             * already carries its own copy of the days, so once it exists it
             * needs nothing further from the itinerary.
             */
            if (itinerary.getStatus() != Itinerary.ItineraryStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "This itinerary is " + itinerary.getStatus().name().toLowerCase()
                                        + ". Publish it before quoting it to a customer.",
                                "ITINERARY_NOT_PUBLISHED")
                );
            }

            if (!customerRepository.existsById(customerId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Customer not found", "CUSTOMER_NOT_FOUND")
                );
            }

            // 2. Get cost estimation from itinerary
            ResponseEntity<ApiResponse<?>> costResponse =
                    costEstimationService.estimateCosts(itineraryIdObfuscated, startDate, useRackRates, currency);

            if (!costResponse.getStatusCode().is2xxSuccessful() || costResponse.getBody() == null) {
                log.error("Failed to get cost estimation for itinerary: {}", itineraryIdObfuscated);
                return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Failed to estimate costs", "COST_ESTIMATION_FAILED")
                );
            }

            ItineraryCostEstimationDTO costEstimation = (ItineraryCostEstimationDTO) costResponse.getBody().getData();
            if (costEstimation == null) {
                return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Cost estimation returned null", "NULL_ESTIMATION")
                );
            }

            // 3. Create the Quote entity
            CreateQuoteDTO createQuoteDTO = buildCreateQuoteDTO(
                    itineraryIdObfuscated,
                    customerIdObfuscated,
                    itinerary,
                    costEstimation,
                    startDate,
                    validityDays != null ? validityDays : 30,
                    useRackRates,
                    taxPercentage,
                    discountPercentage,
                    discountReason,
                    agentCommissionPercentage,
                    agentCommissionReason,
                    marginUpliftPercentage,
                    marginUpliftReason,
                    condenseLineItems
            );

            ResponseEntity<ApiResponse<?>> quoteResponse = quoteCreateService.createQuote(createQuoteDTO);
            if (!quoteResponse.getStatusCode().is2xxSuccessful() || quoteResponse.getBody() == null) {
                log.error("Failed to create quote");
                return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Failed to create quote", "QUOTE_CREATION_FAILED")
                );
            }

            QuoteDTO quoteDTO = (QuoteDTO) quoteResponse.getBody().getData();
            String quoteId = quoteDTO.getId();

            // 4. Snapshot the itinerary's day-tree and pax mix onto the new
            //    Quote, so per-customer edits live on the Quote (not the
            //    template). Cascades persist the children.
            Long decodedQuoteId = idObfuscator.decodeId(quoteId);
            Quote quote = quoteRepository.findById(decodedQuoteId).orElse(null);
            int itemsCreated;
            if (quote != null) {
                copyPaxConfiguration(itinerary, quote);
                copyDaysStructure(itinerary, quote);
                quoteRepository.save(quote);
                // Items are derived from the new Quote tree × pax mix by the
                // shared cost-estimation engine, so any future edits to the
                // tree update totals through the same path.
                itemsCreated = quoteCostEstimationService.recalculate(decodedQuoteId);
            } else {
                log.warn("Quote {} not found immediately after creation; falling back to legacy items.", quoteId);
                itemsCreated = createQuoteItemsFromEstimation(quoteId, costEstimation, condenseLineItems);
            }

            log.info("Successfully generated quote: {} with {} items for itinerary: {} and customer: {}",
                    quoteDTO.getQuoteCode(), itemsCreated, itineraryIdObfuscated, customerIdObfuscated);

            // 5. Return the created quote (totals are automatically calculated)
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201,
                            "Quote generated successfully from itinerary",
                            quoteDTO)
            );

        } catch (Exception e) {
            log.error("Error generating quote from itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500,
                            "Failed to generate quote: " + e.getMessage(),
                            "QUOTE_GENERATION_FAILED")
            );
        }
    }

    /**
     * Build CreateQuoteDTO from itinerary and cost estimation
     */
    private CreateQuoteDTO buildCreateQuoteDTO(
            String itineraryIdObfuscated,
            String customerIdObfuscated,
            Itinerary itinerary,
            ItineraryCostEstimationDTO costEstimation,
            LocalDate safariStartDate,
            int validityDays,
            boolean useStoRate,
            BigDecimal taxPercentage,
            BigDecimal discountPercentage,
            String discountReason,
            BigDecimal agentCommissionPercentage,
            String agentCommissionReason,
            BigDecimal marginUpliftPercentage,
            String marginUpliftReason,
            boolean condenseLineItems
    ) {
        CreateQuoteDTO dto = new CreateQuoteDTO();

        // Basic information
        dto.setTitle(itinerary.getName() != null ? itinerary.getName() + " - Quote" : "Safari Quote");
        dto.setDescription(itinerary.getDescription());
        dto.setItineraryId(itineraryIdObfuscated);
        dto.setCustomerId(customerIdObfuscated);

        // Safari start date (the date used for cost estimation / season determination)
        dto.setSafariStartDate(safariStartDate != null ? safariStartDate : LocalDate.now());

        // Validity dates
        LocalDate today = LocalDate.now();
        dto.setValidFrom(today);
        dto.setValidTo(today.plusDays(validityDays));

        // Pricing details
        dto.setIsStoRate(useStoRate);
        dto.setTaxPercentage(taxPercentage); // Can be null

        if (discountPercentage != null) {
            dto.setDiscountPercentage(discountPercentage);
            dto.setDiscountReason(discountReason);
        }

        // Internal markup — bakes into every line item's stored unit price
        // by QuoteCostEstimationService. Customer never sees a separate
        // markup line.
        if (agentCommissionPercentage != null) {
            dto.setAgentCommissionPercentage(agentCommissionPercentage);
            dto.setAgentCommissionReason(agentCommissionReason);
        }
        if (marginUpliftPercentage != null) {
            dto.setMarginUpliftPercentage(marginUpliftPercentage);
            dto.setMarginUpliftReason(marginUpliftReason);
        }

        // Persist the user's per-line vs. condensed choice on the Quote so
        // future recalcs (from day/pax edits) keep the same shape.
        dto.setCondenseItems(condenseLineItems);

        // Default: active and in draft status
        dto.setIsActive(true);

        return dto;
    }

    /**
     * Create QuoteItems from cost estimation breakdown.
     *
     * <p>When {@code condense} is true, all line items belonging to the same
     * QuoteItemType (Accommodation, Park Fee, Activity, …) are merged into a
     * single QuoteItem per type, with prices summed per currency. The merged
     * row's description preserves the underlying breakdown so the customer
     * still sees what is included on the PDF.
     *
     * <p>When {@code condense} is false, the original per-line behaviour is
     * preserved (one QuoteItem per accommodation per day, one per park per
     * day, etc.).
     *
     * @param quoteId The obfuscated quote ID
     * @param costEstimation The cost estimation DTO
     * @param condense Whether to collapse line items by type
     * @return Number of items created
     */
    private int createQuoteItemsFromEstimation(
            String quoteId,
            ItineraryCostEstimationDTO costEstimation,
            boolean condense
    ) {
        int itemsCreated = 0;

        List<ItineraryCostEstimationDTO.CostLineItem> accommodation = items(costEstimation.getAccommodationCosts());
        List<ItineraryCostEstimationDTO.CostLineItem> parkFees = items(costEstimation.getParkFeeCosts());
        List<ItineraryCostEstimationDTO.CostLineItem> activities = items(costEstimation.getActivityCosts());

        if (condense) {
            if (createCondensedItem(quoteId, accommodation, QuoteItemType.ACCOMMODATION, "Accommodation")) itemsCreated++;
            if (createCondensedItem(quoteId, parkFees, QuoteItemType.PARK_FEE, "Park Fees")) itemsCreated++;
            if (createCondensedItem(quoteId, activities, QuoteItemType.ACTIVITY, "Activities")) itemsCreated++;
        } else {
            for (ItineraryCostEstimationDTO.CostLineItem li : accommodation) {
                createQuoteItemFromLineItem(quoteId, li, QuoteItemType.ACCOMMODATION);
                itemsCreated++;
            }
            for (ItineraryCostEstimationDTO.CostLineItem li : parkFees) {
                createQuoteItemFromLineItem(quoteId, li, QuoteItemType.PARK_FEE);
                itemsCreated++;
            }
            for (ItineraryCostEstimationDTO.CostLineItem li : activities) {
                createQuoteItemFromLineItem(quoteId, li, QuoteItemType.ACTIVITY);
                itemsCreated++;
            }
        }

        return itemsCreated;
    }

    private List<ItineraryCostEstimationDTO.CostLineItem> items(ItineraryCostEstimationDTO.CostBreakdown breakdown) {
        if (breakdown == null || breakdown.getItems() == null) return List.of();
        return breakdown.getItems();
    }

    /**
     * Build and persist one merged QuoteItem from a list of cost line items
     * of the same type. Returns true if a row was created, false if the input
     * list was empty.
     */
    private boolean createCondensedItem(
            String quoteId,
            List<ItineraryCostEstimationDTO.CostLineItem> lineItems,
            QuoteItemType itemType,
            String displayName
    ) {
        if (lineItems == null || lineItems.isEmpty()) return false;

        // Sum totals per currency. Linked map preserves first-seen order.
        Map<String, BigDecimal> totalsByCurrency = new LinkedHashMap<>();
        boolean anyRateMissing = false;
        for (ItineraryCostEstimationDTO.CostLineItem li : lineItems) {
            if (li.getCurrency() == null) continue;
            BigDecimal total = li.getTotalPrice() != null
                    ? li.getTotalPrice()
                    : (li.getUnitPrice() != null && li.getQuantity() != null
                        ? li.getUnitPrice().multiply(BigDecimal.valueOf(li.getQuantity()))
                        : BigDecimal.ZERO);
            totalsByCurrency.merge(li.getCurrency(), total, BigDecimal::add);
            if (Boolean.FALSE.equals(li.getRateFound())) anyRateMissing = true;
        }

        if (totalsByCurrency.isEmpty()) return false;

        // Build the breakdown text — distinct itemNames in their original
        // order. This keeps the merged row's description honest for the PDF.
        String breakdown = lineItems.stream()
                .map(ItineraryCostEstimationDTO.CostLineItem::getItemName)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        CreateQuoteItemDTO itemDTO = new CreateQuoteItemDTO();
        itemDTO.setQuoteId(quoteId);
        itemDTO.setItemType(itemType);
        itemDTO.setItemName(displayName);
        if (!breakdown.isEmpty()) itemDTO.setDescription(breakdown);
        itemDTO.setIsActive(true);

        List<CreateQuoteItemDTO.PriceInput> prices = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totalsByCurrency.entrySet()) {
            CreateQuoteItemDTO.PriceInput price = new CreateQuoteItemDTO.PriceInput();
            price.setCurrency(entry.getKey());
            price.setQuantity(1);
            price.setUnitPrice(entry.getValue());
            if (anyRateMissing) {
                price.setBreakdown("Some rates were estimated");
            }
            prices.add(price);
        }
        itemDTO.setPrices(prices);

        try {
            quoteItemCreateService.createQuoteItem(itemDTO);
            return true;
        } catch (Exception e) {
            log.error("Failed to create condensed quote item for type {}: {}", itemType, e.getMessage());
            return false;
        }
    }

    /**
     * Create a single QuoteItem from a cost estimation line item
     */
    private void createQuoteItemFromLineItem(
            String quoteId,
            ItineraryCostEstimationDTO.CostLineItem lineItem,
            QuoteItemType itemType
    ) {
        CreateQuoteItemDTO itemDTO = new CreateQuoteItemDTO();
        itemDTO.setQuoteId(quoteId);
        itemDTO.setItemType(itemType);
        itemDTO.setItemName(lineItem.getItemName());

        // Build description from available information
        StringBuilder description = new StringBuilder();
        if (lineItem.getDayNumber() != null) {
            description.append("Day ").append(lineItem.getDayNumber());
        }
        if (lineItem.getPaxCategory() != null && !lineItem.getPaxCategory().isEmpty()) {
            if (description.length() > 0) description.append(" - ");
            description.append(lineItem.getPaxCategory());
        }
        if (description.length() > 0) {
            itemDTO.setDescription(description.toString());
        }

        // Convert price to QuoteItem price format
        List<CreateQuoteItemDTO.PriceInput> prices = new ArrayList<>();
        CreateQuoteItemDTO.PriceInput price = new CreateQuoteItemDTO.PriceInput();
        price.setCurrency(lineItem.getCurrency());
        price.setQuantity(lineItem.getQuantity());
        price.setUnitPrice(lineItem.getUnitPrice());

        // Add breakdown if rate was found or missing
        if (lineItem.getRateFound() != null && !lineItem.getRateFound()) {
            price.setBreakdown("Rate not found - estimated");
        }

        prices.add(price);
        itemDTO.setPrices(prices);
        itemDTO.setIsActive(true);

        // Create the item
        try {
            quoteItemCreateService.createQuoteItem(itemDTO);
        } catch (Exception e) {
            log.error("Failed to create quote item: {} - {}", lineItem.getItemName(), e.getMessage());
            // Continue creating other items even if one fails
        }
    }

    // =====================================================================
    // Itinerary → Quote deep-copy helpers
    //
    // Mirror SafariCreateService.copyPaxConfiguration / copyDaysStructure /
    // copyDayActivities / copyDayAccommodations / copyDayParks /
    // copyParkActivities / copyParkTariffs. The Quote-side entities drop
    // the operational fields, so the builder calls are slimmer.
    // =====================================================================

    /**
     * Snapshot the Itinerary's pax + day-tree onto the Quote in place. Used
     * by both the initial quote-creation path and
     * {@link QuoteResyncAndTemplateService#resyncFromItinerary}. Callers
     * must {@code save()} the Quote afterwards to let cascades persist.
     */
    public void snapshotItineraryIntoQuote(Itinerary itinerary, Quote quote) {
        copyPaxConfiguration(itinerary, quote);
        copyDaysStructure(itinerary, quote);
    }

    private void copyPaxConfiguration(Itinerary itinerary, Quote quote) {
        if (itinerary.getPaxList() == null || itinerary.getPaxList().isEmpty()) {
            return;
        }
        for (ItineraryPax itineraryPax : itinerary.getPaxList()) {
            QuotePax quotePax = QuotePax.builder()
                    .quote(quote)
                    .nationCategory(itineraryPax.getNationCategory())
                    .ageCategory(itineraryPax.getAgeCategory())
                    .count(itineraryPax.getCount())
                    .notes(itineraryPax.getNotes())
                    .build();
            quote.getPaxList().add(quotePax);
        }
    }

    private void copyDaysStructure(Itinerary itinerary, Quote quote) {
        if (itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
            return;
        }
        for (ItineraryDay itineraryDay : itinerary.getDays()) {
            QuoteDay quoteDay = QuoteDay.builder()
                    .dayNumber(itineraryDay.getDayNumber())
                    .dayTag(itineraryDay.getDayTag())
                    .title(itineraryDay.getTitle())
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

            copyDayActivities(itineraryDay, quoteDay);
            copyDayAccommodations(itineraryDay, quoteDay);
            copyDayParks(itineraryDay, quoteDay);

            quoteDay.setQuote(quote);
            quote.getDays().add(quoteDay);
        }
    }

    private void copyDayActivities(ItineraryDay itineraryDay, QuoteDay quoteDay) {
        if (itineraryDay.getActivities() == null || itineraryDay.getActivities().isEmpty()) {
            return;
        }
        for (ItineraryDayActivity itineraryActivity : itineraryDay.getActivities()) {
            QuoteDayActivity quoteActivity = QuoteDayActivity.builder()
                    .activity(itineraryActivity.getActivity())
                    .sortOrder(itineraryActivity.getSortOrder())
                    .durationHours(itineraryActivity.getDurationHours())
                    .startTime(itineraryActivity.getStartTime())
                    .endTime(itineraryActivity.getEndTime())
                    .notes(itineraryActivity.getNotes())
                    .isIncludedInPrice(itineraryActivity.getIsIncludedInPrice())
                    .isOptional(itineraryActivity.getIsOptional())
                    .build();
            quoteDay.addActivity(quoteActivity);
        }
    }

    private void copyDayAccommodations(ItineraryDay itineraryDay, QuoteDay quoteDay) {
        if (itineraryDay.getAccommodations() == null || itineraryDay.getAccommodations().isEmpty()) {
            return;
        }
        for (ItineraryDayAccommodation itineraryAccommodation : itineraryDay.getAccommodations()) {
            QuoteDayAccommodation quoteAccommodation = QuoteDayAccommodation.builder()
                    .accommodation(itineraryAccommodation.getAccommodation())
                    .roomType(itineraryAccommodation.getRoomType())
                    .roomStandard(itineraryAccommodation.getRoomStandard())
                    .boardType(itineraryAccommodation.getBoardType())
                    .roomCount(itineraryAccommodation.getRoomCount())
                    .isAlternative(itineraryAccommodation.getIsAlternative())
                    .notes(itineraryAccommodation.getNotes())
                    .build();
            quoteDay.addAccommodation(quoteAccommodation);
        }
    }

    private void copyDayParks(ItineraryDay itineraryDay, QuoteDay quoteDay) {
        if (itineraryDay.getParks() == null || itineraryDay.getParks().isEmpty()) {
            return;
        }
        for (ItineraryDayPark itineraryPark : itineraryDay.getParks()) {
            QuoteDayPark quotePark = QuoteDayPark.builder()
                    .park(itineraryPark.getPark())
                    .entryType(itineraryPark.getEntryType())
                    .sortOrder(itineraryPark.getSortOrder())
                    .arrivalTime(itineraryPark.getArrivalTime())
                    .departureTime(itineraryPark.getDepartureTime())
                    .notes(itineraryPark.getNotes())
                    .build();

            copyParkActivities(itineraryPark, quotePark);
            copyParkTariffs(itineraryPark, quotePark);

            quoteDay.addPark(quotePark);
        }
    }

    private void copyParkActivities(ItineraryDayPark itineraryPark, QuoteDayPark quotePark) {
        if (itineraryPark.getParkActivities() == null || itineraryPark.getParkActivities().isEmpty()) {
            return;
        }
        for (ItineraryDayParkActivity itineraryParkActivity : itineraryPark.getParkActivities()) {
            QuoteDayParkActivity quoteParkActivity = QuoteDayParkActivity.builder()
                    .parkActivity(itineraryParkActivity.getParkActivity())
                    .sortOrder(itineraryParkActivity.getSortOrder())
                    .durationHours(itineraryParkActivity.getDurationHours())
                    .notes(itineraryParkActivity.getNotes())
                    .isIncludedInPrice(itineraryParkActivity.getIsIncludedInPrice())
                    .build();
            quotePark.addParkActivity(quoteParkActivity);
        }
    }

    private void copyParkTariffs(ItineraryDayPark itineraryPark, QuoteDayPark quotePark) {
        if (itineraryPark.getParkTariffs() == null || itineraryPark.getParkTariffs().isEmpty()) {
            return;
        }
        for (ItineraryDayParkTariff itineraryParkTariff : itineraryPark.getParkTariffs()) {
            QuoteDayParkTariff quoteParkTariff = QuoteDayParkTariff.builder()
                    .parkTariff(itineraryParkTariff.getParkTariff())
                    .notes(itineraryParkTariff.getNotes())
                    .isIncludedInPrice(itineraryParkTariff.getIsIncludedInPrice())
                    .build();
            quotePark.addParkTariff(quoteParkTariff);
        }
    }
}
