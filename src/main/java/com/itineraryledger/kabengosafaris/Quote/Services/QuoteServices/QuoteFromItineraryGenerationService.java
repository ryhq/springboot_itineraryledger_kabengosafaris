package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.DTOs.CreateQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.CreateQuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
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
    private final ItineraryRepository itineraryRepository;
    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuoteFromItineraryGenerationService(
            ItineraryCostEstimationService costEstimationService,
            QuoteCreateService quoteCreateService,
            QuoteItemCreateService quoteItemCreateService,
            ItineraryRepository itineraryRepository,
            CustomerRepository customerRepository,
            IdObfuscator idObfuscator
    ) {
        this.costEstimationService = costEstimationService;
        this.quoteCreateService = quoteCreateService;
        this.quoteItemCreateService = quoteItemCreateService;
        this.itineraryRepository = itineraryRepository;
        this.customerRepository = customerRepository;
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
                    discountReason
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

            // 4. Create QuoteItems from cost estimation line items
            int itemsCreated = createQuoteItemsFromEstimation(quoteId, costEstimation, condenseLineItems);

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
            String discountReason
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
}
