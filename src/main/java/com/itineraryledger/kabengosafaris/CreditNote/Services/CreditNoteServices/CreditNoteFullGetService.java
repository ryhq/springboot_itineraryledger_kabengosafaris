package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices;

import com.itineraryledger.kabengosafaris.CreditNote.DTOs.FullCreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.FullCreditNoteDTO.CustomerDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.FullCreditNoteDTO.LineItemDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.FullCreditNoteDTO.PriceDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteLineItemRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CreditNoteFullGetService - Complete credit note with everything a document needs
 *
 * Mirrors InvoiceFullGetService: the entity's own fields, the customer, the invoice the credit
 * comes off, the line items in display order, and the totals computed the same way the API
 * computes them, so the PDF and the screen can never quote different figures.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditNoteFullGetService {

    private final CreditNoteRepository creditNoteRepository;
    private final CreditNoteLineItemRepository creditNoteLineItemRepository;
    private final CreditNoteTotalsAggregationService totalsAggregationService;
    private final IdObfuscator idObfuscator;

    /**
     * Get the complete credit note by obfuscated id, as an API response.
     */
    public ResponseEntity<ApiResponse<?>> getFullCreditNote(String idObfuscated) {
        FullCreditNoteDTO dto = fetch(idObfuscated);
        if (dto == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
            );
        }
        return ResponseEntity.ok(
            ApiResponse.success(200, "Credit note retrieved successfully", dto)
        );
    }

    /**
     * Get the complete credit note, or null when there is no such record.
     * This is the entry point the PDF generator uses.
     */
    public FullCreditNoteDTO fetch(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            return creditNoteRepository.findById(id).map(this::build).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to load credit note {}", idObfuscated, e);
            return null;
        }
    }

    private FullCreditNoteDTO build(CreditNote creditNote) {
        FullCreditNoteDTO dto = new FullCreditNoteDTO();

        dto.setId(idObfuscator.encodeId(creditNote.getId()));
        dto.setCreditNoteCode(creditNote.getCreditNoteCode());
        dto.setTitle(creditNote.getTitle());
        dto.setDescription(creditNote.getDescription());
        dto.setStatus(creditNote.getStatus());
        dto.setStatusDisplayName(creditNote.getStatus() != null ? creditNote.getStatus().getDisplayName() : null);
        dto.setTaxPercentage(creditNote.getTaxPercentage());
        dto.setIssueDate(creditNote.getIssueDate());
        dto.setSentDate(creditNote.getSentDate());
        dto.setConsumedDate(creditNote.getConsumedDate());
        dto.setReason(creditNote.getReason());
        dto.setCustomerNotes(creditNote.getCustomerNotes());
        dto.setInternalNotes(creditNote.getInternalNotes());
        dto.setIsActive(creditNote.getIsActive());
        if (creditNote.getCreatedBy() != null) {
            dto.setCreatedByName(creditNote.getCreatedBy().getUsername());
        }
        if (creditNote.getUpdatedBy() != null) {
            dto.setUpdatedByName(creditNote.getUpdatedBy().getUsername());
        }
        dto.setCreatedAt(creditNote.getCreatedAt());
        dto.setUpdatedAt(creditNote.getUpdatedAt());

        // ========================
        // CUSTOMER
        // ========================
        Customer customer = creditNote.getCustomer();
        if (customer != null) {
            dto.setCustomer(CustomerDTO.builder()
                .id(idObfuscator.encodeId(customer.getId()))
                .customerCode(customer.getCode())
                .customerName(displayName(customer))
                .email(customer.getPrimaryEmail())
                .phone(customer.getPrimaryPhone())
                .nationality(customer.getNationality())
                .address(customer.getAddress())
                .city(customer.getCity())
                .country(customer.getCountry())
                .build());
        }

        // ========================
        // THE INVOICE IT COMES OFF
        // ========================
        Invoice invoice = creditNote.getInvoice();
        if (invoice != null) {
            dto.setInvoice(FullCreditNoteDTO.InvoiceDTO.builder()
                .id(idObfuscator.encodeId(invoice.getId()))
                .invoiceCode(invoice.getInvoiceCode())
                .title(invoice.getTitle())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .statusDisplayName(invoice.getStatus() != null ? invoice.getStatus().getDisplayName() : null)
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .grandTotals(toPriceDTOs(invoice.getGrandTotals()))
                .build());
        }

        // ========================
        // LINE ITEMS
        // ========================
        List<CreditNoteLineItem> lineItems =
            creditNoteLineItemRepository.findByCreditNoteIdOrderByDisplayOrderAsc(creditNote.getId());
        List<LineItemDTO> lineItemDTOs = lineItems.stream()
            .map(this::toLineItemDTO)
            .collect(Collectors.toList());
        dto.setLineItems(lineItemDTOs);
        dto.setTotalLineItemsCount(lineItemDTOs.size());

        // ========================
        // TOTALS
        // ========================
        CreditNoteTotalsAggregationService.ComputedTotals totals = totalsAggregationService.compute(creditNote);
        dto.setSubtotals(toPriceDTOs(totals.subtotals()));
        dto.setTaxes(toPriceDTOs(totals.taxes()));
        dto.setTotals(toPriceDTOs(totals.totals()));

        /*
         * The template builds a column per currency and reads every price through that list, so an
         * empty one renders a credit note with no money on it. Taking the currencies from the line
         * items as well as the totals means a note whose totals have not been computed yet still
         * prints its lines.
         */
        Set<String> currencies = new LinkedHashSet<>();
        for (LineItemDTO item : lineItemDTOs) {
            if (item.getPrices() == null) continue;
            for (PriceDTO price : item.getPrices()) {
                if (price.getCurrency() != null) currencies.add(price.getCurrency());
            }
        }
        if (dto.getTotals() != null) {
            for (PriceDTO total : dto.getTotals()) {
                if (total.getCurrency() != null) currencies.add(total.getCurrency());
            }
        }
        dto.setCurrencies(new ArrayList<>(currencies));
        dto.setTotalCurrenciesCount(currencies.size());

        return dto;
    }

    private LineItemDTO toLineItemDTO(CreditNoteLineItem item) {
        return LineItemDTO.builder()
            .id(idObfuscator.encodeId(item.getId()))
            .itemType(item.getItemType() != null ? item.getItemType().name() : null)
            .itemTypeDisplayName(item.getItemType() != null ? item.getItemType().getDisplayName() : null)
            .itemName(item.getItemName())
            .description(item.getDescription())
            .displayOrder(item.getDisplayOrder())
            .prices(toPriceDTOs(item.getPrices()))
            .isActive(item.getIsActive())
            .build();
    }

    private List<PriceDTO> toPriceDTOs(List<Price> prices) {
        if (prices == null) {
            return List.of();
        }
        return prices.stream().map(this::toPriceDTO).collect(Collectors.toList());
    }

    private PriceDTO toPriceDTO(Price price) {
        return PriceDTO.builder()
            .currency(price.getCurrency())
            .quantity(price.getQuantity())
            .unitPrice(price.getUnitPrice())
            .totalPrice(price.getTotalPrice())
            .breakdown(price.getBreakdown())
            .formattedUnitPrice(format(price.getCurrency(), price.getUnitPrice()))
            .formattedTotalPrice(format(price.getCurrency(), price.getTotalPrice()))
            .build();
    }

    /** Same formatting as the invoice, so the two documents never disagree about how money looks. */
    private String format(String currencyCode, BigDecimal amount) {
        if (amount == null || currencyCode == null) {
            return null;
        }
        try {
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            formatter.setCurrency(currency);
            return formatter.format(amount);
        } catch (Exception e) {
            return currencyCode + " " + amount.toPlainString();
        }
    }

    private String displayName(Customer customer) {
        if (customer.getCustomerType() == CustomerType.CORPORATE
            || customer.getCustomerType() == CustomerType.TRAVEL_AGENT) {
            return customer.getCompanyName();
        }
        String first = customer.getFirstName() != null ? customer.getFirstName() : "";
        String last = customer.getLastName() != null ? customer.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? customer.getCompanyName() : full;
    }
}
