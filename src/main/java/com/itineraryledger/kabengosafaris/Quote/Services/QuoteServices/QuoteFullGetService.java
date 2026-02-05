package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO.*;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * QuoteFullGetService - Service for retrieving complete quote with all nested data
 *
 * Returns the full quote structure including:
 * - Quote base data
 * - Customer information
 * - Itinerary summary
 * - Quote items with multi-currency prices
 * - Totals (subtotals, taxes, discounts, grand totals) by currency
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class QuoteFullGetService {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuoteFullGetService(
        QuoteRepository quoteRepository,
        QuoteItemRepository quoteItemRepository,
        IdObfuscator idObfuscator
    ) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get complete quote with all nested data by obfuscated ID
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the full quote
     */
    public ResponseEntity<ApiResponse<?>> getFullQuote(String idObfuscated) {
        log.info("Fetching full quote with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode quote ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            // Find quote
            Quote quote = quoteRepository.findById(id).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Build full DTO
            FullQuoteDTO fullDTO = buildFullQuoteDTO(quote);

            log.info("Full quote retrieved successfully: {} with {} items",
                quote.getQuoteCode(),
                fullDTO.getTotalItemsCount());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Full quote retrieved successfully", fullDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching full quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch full quote", "FULL_QUOTE_FETCH_FAILED")
            );
        }
    }

    /**
     * Build the complete FullQuoteDTO with all nested data
     */
    private FullQuoteDTO buildFullQuoteDTO(Quote quote) {
        FullQuoteDTO dto = new FullQuoteDTO();

        // ========================
        // QUOTE BASE FIELDS
        // ========================
        dto.setId(idObfuscator.encodeId(quote.getId()));
        dto.setQuoteCode(quote.getQuoteCode());
        dto.setTitle(quote.getTitle());
        dto.setDescription(quote.getDescription());
        dto.setStatus(quote.getStatus());
        dto.setStatusDisplayName(quote.getStatus() != null ? quote.getStatus().getDisplayName() : null);
        dto.setVersion(quote.getVersion());
        dto.setVersionNotes(quote.getVersionNotes());

        // ========================
        // PRICING DETAILS
        // ========================
        dto.setIsStoRate(quote.getIsStoRate());
        dto.setTaxPercentage(quote.getTaxPercentage());
        dto.setDiscountPercentage(quote.getDiscountPercentage());
        dto.setDiscountReason(quote.getDiscountReason());

        // ========================
        // VALIDITY AND DATES
        // ========================
        dto.setSentDate(quote.getSentDate());
        dto.setValidFrom(quote.getValidFrom());
        dto.setValidTo(quote.getValidTo());
        dto.setIsValid(quote.getIsValid());

        // Calculate validity status message
        if (quote.getValidFrom() != null && quote.getValidTo() != null) {
            LocalDate now = LocalDate.now();
            if (now.isBefore(quote.getValidFrom())) {
                dto.setValidityStatusMessage("Not yet valid");
            } else if (now.isAfter(quote.getValidTo())) {
                dto.setValidityStatusMessage("Expired");
            } else {
                dto.setValidityStatusMessage("Valid");
            }
        }

        // ========================
        // PAYMENT TERMS
        // ========================
        dto.setDepositPercentage(quote.getDepositPercentage());
        dto.setDepositDueDate(quote.getDepositDueDate());
        dto.setFullPaymentDueDate(quote.getFullPaymentDueDate());

        // ========================
        // NOTES
        // ========================
        dto.setCustomerNotes(quote.getCustomerNotes());
        dto.setInternalNotes(quote.getInternalNotes());

        // ========================
        // APPROVAL
        // ========================
        if (quote.getApprover() != null) {
            dto.setApproverName(quote.getApprover().getUsername());
        }
        if (quote.getApprovedBy() != null) {
            dto.setApprovedByName(quote.getApprovedBy().getUsername());
        }
        dto.setApprovedAt(quote.getApprovedAt());
        dto.setApprovalNotes(quote.getApprovalNotes());

        // ========================
        // AUDIT
        // ========================
        dto.setIsActive(quote.getIsActive());
        if (quote.getCreatedBy() != null) {
            dto.setCreatedByName(quote.getCreatedBy().getUsername());
        }
        if (quote.getUpdatedBy() != null) {
            dto.setUpdatedByName(quote.getUpdatedBy().getUsername());
        }
        dto.setCreatedAt(quote.getCreatedAt());
        dto.setUpdatedAt(quote.getUpdatedAt());

        // ========================
        // CUSTOMER INFORMATION
        // ========================
        if (quote.getCustomer() != null) {
            String customerName = getCustomerDisplayName(quote.getCustomer());
            CustomerDTO customerDTO = CustomerDTO.builder()
                .id(idObfuscator.encodeId(quote.getCustomer().getId()))
                .customerName(customerName)
                .email(quote.getCustomer().getPrimaryEmail())
                .phone(quote.getCustomer().getPrimaryPhone())
                .nationality(quote.getCustomer().getNationality())
                .address(quote.getCustomer().getAddress())
                .city(quote.getCustomer().getCity())
                .country(quote.getCustomer().getCountry())
                .build();
            dto.setCustomer(customerDTO);
        }

        // ========================
        // ITINERARY SUMMARY
        // ========================
        if (quote.getItinerary() != null) {
            ItineraryDTO itineraryDTO = ItineraryDTO.builder()
                .id(idObfuscator.encodeId(quote.getItinerary().getId()))
                .name(quote.getItinerary().getName())
                .code(quote.getItinerary().getCode())
                .status(quote.getItinerary().getStatus() != null ? quote.getItinerary().getStatus().name() : null)
                .statusDisplayName(quote.getItinerary().getStatus() != null ? quote.getItinerary().getStatus().getDisplayName() : null)
                .tripType(quote.getItinerary().getTripType() != null ? quote.getItinerary().getTripType().name() : null)
                .tripTypeDisplayName(quote.getItinerary().getTripType() != null ? quote.getItinerary().getTripType().getDisplayName() : null)
                .budgetCategory(quote.getItinerary().getBudgetCategory() != null ? quote.getItinerary().getBudgetCategory().name() : null)
                .budgetCategoryDisplayName(quote.getItinerary().getBudgetCategory() != null ? quote.getItinerary().getBudgetCategory().getDisplayName() : null)
                .totalDays(quote.getItinerary().getTotalDays())
                .totalNights(quote.getItinerary().getTotalNights())
                .description(quote.getItinerary().getDescription())
                .startLocation(quote.getItinerary().getStartLocation())
                .endLocation(quote.getItinerary().getEndLocation())
                .build();
            dto.setItinerary(itineraryDTO);
        }

        // ========================
        // QUOTE ITEMS
        // ========================
        List<QuoteItem> items = quoteItemRepository.findByQuoteIdOrderByDisplayOrder(quote.getId());
        List<QuoteItemDTO> itemDTOs = items.stream()
            .map(this::convertItemToDTO)
            .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        // ========================
        // TOTALS (SUBTOTALS, TAXES, DISCOUNTS, GRAND TOTALS)
        // ========================
        if (quote.getSubtotals() != null) {
            List<PriceDTO> subtotalDTOs = quote.getSubtotals().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setSubtotals(subtotalDTOs);
        }

        if (quote.getTaxes() != null) {
            List<PriceDTO> taxDTOs = quote.getTaxes().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setTaxes(taxDTOs);
        }

        if (quote.getDiscounts() != null) {
            List<PriceDTO> discountDTOs = quote.getDiscounts().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setDiscounts(discountDTOs);
        }

        if (quote.getGrandTotals() != null) {
            List<PriceDTO> grandTotalDTOs = quote.getGrandTotals().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setGrandTotals(grandTotalDTOs);
        }

        // ========================
        // SUMMARY STATISTICS
        // ========================
        dto.setTotalItemsCount(items.size());
        dto.setTotalCurrenciesCount(quote.getGrandTotals() != null ? quote.getGrandTotals().size() : 0);

        // Extract unique currencies from grand totals
        if (quote.getGrandTotals() != null && !quote.getGrandTotals().isEmpty()) {
            List<String> currencies = quote.getGrandTotals().stream()
                .map(Price::getCurrency)
                .distinct()
                .collect(Collectors.toList());
            dto.setCurrencies(currencies);
        }

        return dto;
    }

    /**
     * Convert QuoteItem entity to QuoteItemDTO
     */
    private QuoteItemDTO convertItemToDTO(QuoteItem item) {
        List<PriceDTO> priceDTOs = item.getPrices().stream()
            .map(this::convertPriceToDTO)
            .collect(Collectors.toList());

        return QuoteItemDTO.builder()
            .id(idObfuscator.encodeId(item.getId()))
            .itemType(item.getItemType())
            .itemTypeDisplayName(item.getItemType() != null ? item.getItemType().getDisplayName() : null)
            .itemName(item.getItemName())
            .description(item.getDescription())
            .displayOrder(item.getDisplayOrder())
            .prices(priceDTOs)
            .isActive(item.getIsActive())
            .build();
    }

    /**
     * Convert Price embeddable to PriceDTO with formatted values
     */
    private PriceDTO convertPriceToDTO(Price price) {
        String formattedUnitPrice = formatPrice(price.getCurrency(), price.getUnitPrice());
        String formattedTotalPrice = formatPrice(price.getCurrency(), price.getTotalPrice());

        return PriceDTO.builder()
            .currency(price.getCurrency())
            .quantity(price.getQuantity())
            .unitPrice(price.getUnitPrice())
            .totalPrice(price.getTotalPrice())
            .breakdown(price.getBreakdown())
            .formattedUnitPrice(formattedUnitPrice)
            .formattedTotalPrice(formattedTotalPrice)
            .build();
    }

    /**
     * Format price with currency symbol
     */
    private String formatPrice(String currencyCode, BigDecimal amount) {
        if (amount == null || currencyCode == null) {
            return null;
        }

        try {
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            formatter.setCurrency(currency);
            return formatter.format(amount);
        } catch (Exception e) {
            // Fallback: simple format
            return currencyCode + " " + amount.toString();
        }
    }

    /**
     * Get customer display name based on customer type
     */
    private String getCustomerDisplayName(Customer customer) {
        if (customer == null) {
            return null;
        }

        // For corporate/travel agent, use company name
        if (customer.getCustomerType() == CustomerType.CORPORATE ||
            customer.getCustomerType() == CustomerType.TRAVEL_AGENT) {
            return customer.getCompanyName();
        }

        // For individual, combine first and last name
        StringBuilder name = new StringBuilder();
        if (customer.getTitle() != null && !customer.getTitle().isEmpty()) {
            name.append(customer.getTitle()).append(" ");
        }
        if (customer.getFirstName() != null && !customer.getFirstName().isEmpty()) {
            name.append(customer.getFirstName());
        }
        if (customer.getLastName() != null && !customer.getLastName().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(customer.getLastName());
        }

        return name.length() > 0 ? name.toString() : customer.getCode();
    }
}
