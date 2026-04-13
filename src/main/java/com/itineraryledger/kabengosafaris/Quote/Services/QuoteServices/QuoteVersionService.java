package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.CreateQuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices.QuoteItemCreateService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QuoteVersionService - Creates new versions of quotes with optional parameter overrides.
 *
 * Behavior:
 * - Unspecified params inherit from the original quote
 * - If startDate changes, items are re-generated from itinerary cost estimation (new season rates)
 * - If startDate is the same, items are deep-copied from the original
 * - Validity dates are always recalculated from today
 * - Auto-generates a changelog of what params changed
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuoteVersionService {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final QuoteItemCreateService quoteItemCreateService;
    private final QuoteTotalsCalculationService totalsCalculationService;
    private final ItineraryCostEstimationService costEstimationService;
    private final IdObfuscator idObfuscator;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Transactional
    @AuditLogAnnotation(action = "CREATE_QUOTE_VERSION", description = "Creating new quote version", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> createNewVersion(
            String idObfuscated, String versionNotes,
            LocalDate startDate, String currency, Boolean useStoRate,
            Integer validityDays, BigDecimal taxPercentage,
            BigDecimal discountPercentage, String discountReason) {

        log.info("Creating new version of quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote original = quoteRepository.findById(id).orElse(null);

            if (original == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate status
            QuoteStatus status = original.getStatus();
            if (status == QuoteStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quote is still in DRAFT. Edit it directly instead of creating a new version.", "ALREADY_DRAFT")
                );
            }
            if (status == QuoteStatus.READY) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quote is in READY status. Revert to DRAFT to make changes, or send it first.", "REVERT_INSTEAD")
                );
            }
            if (status == QuoteStatus.CONVERTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot create a new version of a CONVERTED quote. It has already been booked.", "CONVERTED_QUOTE")
                );
            }

            // Prevent branching — must version from latest in chain
            if (original.getNextVersion() != null) {
                String nextCode = original.getNextVersion().getQuoteCode();
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "This quote already has a newer version: " + nextCode + ". Create a new version from the latest one.",
                        "NEXT_VERSION_EXISTS")
                );
            }

            // ============================================================
            // Resolve params: override > original
            // ============================================================
            LocalDate resolvedStartDate = startDate != null ? startDate : original.getSafariStartDate();
            Boolean resolvedStoRate = useStoRate != null ? useStoRate : original.getIsStoRate();
            String resolvedCurrency = currency != null ? currency : "USD";
            BigDecimal resolvedTax = taxPercentage != null ? taxPercentage : original.getTaxPercentage();
            BigDecimal resolvedDiscount = discountPercentage != null ? discountPercentage : original.getDiscountPercentage();
            String resolvedDiscountReason = discountReason != null ? discountReason : original.getDiscountReason();
            int resolvedValidityDays = validityDays != null ? validityDays : 30;

            boolean dateChanged = resolvedStartDate != null && original.getSafariStartDate() != null
                    && !resolvedStartDate.equals(original.getSafariStartDate());
            // Also regenerate if original had no date but now one is specified
            if (resolvedStartDate != null && original.getSafariStartDate() == null) {
                dateChanged = true;
            }

            // ============================================================
            // Auto-generate changelog
            // ============================================================
            String changelog = buildChangelog(original, resolvedStartDate, resolvedStoRate,
                resolvedTax, resolvedDiscount, resolvedDiscountReason, versionNotes);

            // ============================================================
            // Step 1: Create new quote entity
            // ============================================================
            User currentUser = getCurrentUser();
            int newVersionNumber = original.getVersion() + 1;
            LocalDate today = LocalDate.now();

            Quote newQuote = Quote.builder()
                .title(original.getTitle())
                .description(original.getDescription())
                .itinerary(original.getItinerary())
                .customer(original.getCustomer())
                .items(new ArrayList<>())
                .documents(new ArrayList<>())
                .subtotals(new ArrayList<>())
                .taxes(new ArrayList<>())
                .discounts(new ArrayList<>())
                .grandTotals(new ArrayList<>())
                .safariStartDate(resolvedStartDate)
                .isStoRate(resolvedStoRate)
                .taxPercentage(resolvedTax)
                .discountPercentage(resolvedDiscount)
                .discountReason(resolvedDiscountReason)
                .version(newVersionNumber)
                .previousVersion(original)
                .status(QuoteStatus.DRAFT)
                .validFrom(today)
                .validTo(today.plusDays(resolvedValidityDays))
                .isValid(true)
                .depositPercentage(original.getDepositPercentage())
                .depositDueDate(original.getDepositDueDate())
                .fullPaymentDueDate(original.getFullPaymentDueDate())
                .internalNotes(original.getInternalNotes())
                .customerNotes(original.getCustomerNotes())
                .versionNotes(changelog)
                .createdBy(currentUser)
                .isActive(true)
                .build();

            // Save to get ID, then generate real code
            newQuote.setQuoteCode("TEMP-" + System.currentTimeMillis());
            newQuote = quoteRepository.save(newQuote);
            newQuote.setQuoteCode(newQuote.generateCode());
            newQuote = quoteRepository.save(newQuote);

            log.info("Created new quote version: {} (v{}) from {} (v{})",
                newQuote.getQuoteCode(), newVersionNumber, original.getQuoteCode(), original.getVersion());

            // ============================================================
            // Step 2: Items — re-generate if date changed, else deep copy
            // ============================================================
            int itemCount;
            if (dateChanged && original.getItinerary() != null) {
                log.info("Start date changed ({} -> {}), re-generating items from cost estimation",
                    original.getSafariStartDate(), resolvedStartDate);
                itemCount = regenerateItemsFromCostEstimation(newQuote, resolvedStartDate, resolvedStoRate, resolvedCurrency);
            } else {
                log.info("Start date unchanged, deep copying items from original quote");
                itemCount = deepCopyItems(original, newQuote);
            }

            log.info("{} items for new version {}", dateChanged ? "Re-generated " + itemCount : "Copied " + itemCount, newQuote.getQuoteCode());

            // ============================================================
            // Step 3: Recalculate totals
            // ============================================================
            totalsCalculationService.recalculateTotals(newQuote.getId());

            // ============================================================
            // Step 4: Link version chain
            // ============================================================
            original.setNextVersion(newQuote);
            quoteRepository.save(original);

            // Reload for updated totals
            newQuote = quoteRepository.findById(newQuote.getId()).orElse(newQuote);

            // ============================================================
            // Step 5: Response
            // ============================================================
            QuoteDTO dto = convertToDTO(newQuote);

            String message = dateChanged
                ? String.format("New version (v%d) created with new start date. %d items re-generated from cost estimation.", newVersionNumber, itemCount)
                : String.format("New version (v%d) created. %d items copied from %s.", newVersionNumber, itemCount, original.getQuoteCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, message, dto)
            );

        } catch (Exception e) {
            log.error("Error creating new quote version", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create new quote version: " + e.getMessage(), "VERSION_CREATION_FAILED")
            );
        }
    }

    // ============================================================
    // Item strategies
    // ============================================================

    private int regenerateItemsFromCostEstimation(Quote newQuote, LocalDate startDate, Boolean useStoRate, String currency) {
        String itineraryObfuscatedId = idObfuscator.encodeId(newQuote.getItinerary().getId());
        String quoteObfuscatedId = idObfuscator.encodeId(newQuote.getId());

        ResponseEntity<ApiResponse<?>> costResponse = costEstimationService.estimateCosts(
            itineraryObfuscatedId, startDate, useStoRate, currency
        );

        if (!costResponse.getStatusCode().is2xxSuccessful() || costResponse.getBody() == null) {
            log.warn("Cost estimation failed for version creation, falling back to item copy");
            return 0;
        }

        ItineraryCostEstimationDTO estimation = (ItineraryCostEstimationDTO) costResponse.getBody().getData();
        if (estimation == null) {
            log.warn("Cost estimation returned null, falling back to empty items");
            return 0;
        }

        int count = 0;

        // Accommodation costs
        if (estimation.getAccommodationCosts() != null && estimation.getAccommodationCosts().getItems() != null) {
            for (ItineraryCostEstimationDTO.CostLineItem item : estimation.getAccommodationCosts().getItems()) {
                createItemFromLineItem(quoteObfuscatedId, item, QuoteItemType.ACCOMMODATION);
                count++;
            }
        }

        // Park fee costs
        if (estimation.getParkFeeCosts() != null && estimation.getParkFeeCosts().getItems() != null) {
            for (ItineraryCostEstimationDTO.CostLineItem item : estimation.getParkFeeCosts().getItems()) {
                createItemFromLineItem(quoteObfuscatedId, item, QuoteItemType.PARK_FEE);
                count++;
            }
        }

        // Activity costs
        if (estimation.getActivityCosts() != null && estimation.getActivityCosts().getItems() != null) {
            for (ItineraryCostEstimationDTO.CostLineItem item : estimation.getActivityCosts().getItems()) {
                createItemFromLineItem(quoteObfuscatedId, item, QuoteItemType.ACTIVITY);
                count++;
            }
        }

        return count;
    }

    private void createItemFromLineItem(String quoteObfuscatedId, ItineraryCostEstimationDTO.CostLineItem lineItem, QuoteItemType itemType) {
        CreateQuoteItemDTO itemDTO = new CreateQuoteItemDTO();
        itemDTO.setQuoteId(quoteObfuscatedId);
        itemDTO.setItemType(itemType);
        itemDTO.setItemName(lineItem.getItemName());

        StringBuilder desc = new StringBuilder();
        if (lineItem.getDayNumber() != null) desc.append("Day ").append(lineItem.getDayNumber());
        if (lineItem.getPaxCategory() != null && !lineItem.getPaxCategory().isEmpty()) {
            if (desc.length() > 0) desc.append(" - ");
            desc.append(lineItem.getPaxCategory());
        }
        if (desc.length() > 0) itemDTO.setDescription(desc.toString());

        List<CreateQuoteItemDTO.PriceInput> prices = new ArrayList<>();
        CreateQuoteItemDTO.PriceInput price = new CreateQuoteItemDTO.PriceInput();
        price.setCurrency(lineItem.getCurrency());
        price.setQuantity(lineItem.getQuantity());
        price.setUnitPrice(lineItem.getUnitPrice());
        if (lineItem.getRateFound() != null && !lineItem.getRateFound()) {
            price.setBreakdown("Rate not found - estimated");
        }
        prices.add(price);
        itemDTO.setPrices(prices);
        itemDTO.setIsActive(true);

        try {
            quoteItemCreateService.createQuoteItem(itemDTO);
        } catch (Exception e) {
            log.error("Failed to create quote item: {} - {}", lineItem.getItemName(), e.getMessage());
        }
    }

    private int deepCopyItems(Quote original, Quote newQuote) {
        List<QuoteItem> originalItems = quoteItemRepository.findByQuoteIdOrderByDisplayOrder(original.getId());
        int count = 0;

        for (QuoteItem originalItem : originalItems) {
            List<Price> copiedPrices = new ArrayList<>();
            if (originalItem.getPrices() != null) {
                for (Price p : originalItem.getPrices()) {
                    copiedPrices.add(Price.builder()
                        .currency(p.getCurrency())
                        .quantity(p.getQuantity())
                        .unitPrice(p.getUnitPrice())
                        .totalPrice(p.getTotalPrice())
                        .breakdown(p.getBreakdown())
                        .build());
                }
            }

            quoteItemRepository.save(QuoteItem.builder()
                .quote(newQuote)
                .itemType(originalItem.getItemType())
                .itemName(originalItem.getItemName())
                .description(originalItem.getDescription())
                .displayOrder(originalItem.getDisplayOrder())
                .prices(copiedPrices)
                .isActive(originalItem.getIsActive())
                .build());
            count++;
        }

        return count;
    }

    // ============================================================
    // Changelog builder
    // ============================================================

    private String buildChangelog(Quote original, LocalDate newStartDate, Boolean newStoRate,
                                  BigDecimal newTax, BigDecimal newDiscount, String newDiscountReason,
                                  String userNotes) {
        List<String> changes = new ArrayList<>();

        if (newStartDate != null && original.getSafariStartDate() != null && !newStartDate.equals(original.getSafariStartDate())) {
            changes.add("Safari start date: " + original.getSafariStartDate().format(DATE_FMT) + " -> " + newStartDate.format(DATE_FMT));
        } else if (newStartDate != null && original.getSafariStartDate() == null) {
            changes.add("Safari start date set: " + newStartDate.format(DATE_FMT));
        }

        if (newStoRate != null && !newStoRate.equals(original.getIsStoRate())) {
            changes.add("Rate type: " + (original.getIsStoRate() ? "STO" : "RACK") + " -> " + (newStoRate ? "STO" : "RACK"));
        }

        if (newTax != null && !newTax.equals(original.getTaxPercentage())) {
            String oldTax = original.getTaxPercentage() != null ? original.getTaxPercentage().toPlainString() + "%" : "none";
            changes.add("Tax: " + oldTax + " -> " + newTax.toPlainString() + "%");
        }

        if (newDiscount != null && !newDiscount.equals(original.getDiscountPercentage())) {
            String oldDisc = original.getDiscountPercentage() != null ? original.getDiscountPercentage().toPlainString() + "%" : "none";
            changes.add("Discount: " + oldDisc + " -> " + newDiscount.toPlainString() + "%");
        }

        if (newDiscountReason != null && !newDiscountReason.equals(original.getDiscountReason())) {
            changes.add("Discount reason updated");
        }

        StringBuilder sb = new StringBuilder();

        if (!changes.isEmpty()) {
            sb.append("Changes: ").append(String.join("; ", changes));
        }

        if (userNotes != null && !userNotes.isBlank()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(userNotes);
        }

        if (sb.length() == 0) {
            sb.append("New version created from v").append(original.getVersion());
        }

        return sb.toString();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return userRepository.findByUsername(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not get current user: {}", e.getMessage());
        }
        return null;
    }

    private QuoteDTO convertToDTO(Quote quote) {
        QuoteDTO dto = QuoteDTO.builder()
            .id(idObfuscator.encodeId(quote.getId()))
            .quoteCode(quote.getQuoteCode())
            .title(quote.getTitle())
            .description(quote.getDescription())
            .itineraryId(idObfuscator.encodeId(quote.getItinerary().getId()))
            .itineraryCode(quote.getItinerary().getCode())
            .itineraryName(quote.getItinerary().getName())
            .customerId(idObfuscator.encodeId(quote.getCustomer().getId()))
            .customerName(quote.getCustomer().getDisplayName())
            .customerEmail(quote.getCustomer().getPrimaryEmail())
            .subtotals(quote.getSubtotals())
            .taxes(quote.getTaxes())
            .discounts(quote.getDiscounts())
            .grandTotals(quote.getGrandTotals())
            .isStoRate(quote.getIsStoRate())
            .taxPercentage(quote.getTaxPercentage())
            .discountPercentage(quote.getDiscountPercentage())
            .discountReason(quote.getDiscountReason())
            .version(quote.getVersion())
            .status(quote.getStatus())
            .safariStartDate(quote.getSafariStartDate())
            .sentDate(quote.getSentDate())
            .validFrom(quote.getValidFrom())
            .validTo(quote.getValidTo())
            .isValid(quote.getIsValid())
            .depositPercentage(quote.getDepositPercentage())
            .depositDueDate(quote.getDepositDueDate())
            .fullPaymentDueDate(quote.getFullPaymentDueDate())
            .internalNotes(quote.getInternalNotes())
            .customerNotes(quote.getCustomerNotes())
            .versionNotes(quote.getVersionNotes())
            .isActive(quote.getIsActive())
            .itemCount(quote.getItems() != null ? (long) quote.getItems().size() : 0L)
            .documentCount(0L)
            .createdAt(quote.getCreatedAt())
            .updatedAt(quote.getUpdatedAt())
            .build();

        if (quote.getPreviousVersion() != null) {
            dto.setPreviousVersionId(idObfuscator.encodeId(quote.getPreviousVersion().getId()));
            dto.setPreviousVersionCode(quote.getPreviousVersion().getQuoteCode());
        }

        if (quote.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(quote.getCreatedBy().getId()));
            dto.setCreatedByName(quote.getCreatedBy().getUsername());
        }

        return dto;
    }
}
