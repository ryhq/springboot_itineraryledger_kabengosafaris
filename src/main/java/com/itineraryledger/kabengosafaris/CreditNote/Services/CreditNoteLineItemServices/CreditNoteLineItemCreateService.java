package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteLineItemServices;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CloneInvoiceLineItemsDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreateCreditNoteLineItemDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteLineItemDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteItemType;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteLineItemRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteCreateService;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteTotalsAggregationService;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating credit note line items
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CreditNoteLineItemCreateService {

    private final CreditNoteLineItemRepository creditNoteLineItemRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final IdObfuscator idObfuscator;
    private final CreditNoteCreateService creditNoteCreateService;
    private final CreditNoteTotalsAggregationService totalsAggregationService;

    @AuditLogAnnotation(
        action = "CREATE_CREDIT_NOTE_LINE_ITEM",
        description = "Creating a new credit note line item",
        entityType = "CreditNoteLineItem"
    )
    public ResponseEntity<ApiResponse<?>> createCreditNoteLineItem(String creditNoteId, CreateCreditNoteLineItemDTO createDTO) {
        log.info("Creating new credit note line item");

        try {
            // Validate and decode credit note ID
            Long decodedCreditNoteId;
            try {
                decodedCreditNoteId = idObfuscator.decodeId(creditNoteId);
            } catch (Exception e) {
                log.warn("Failed to decode credit note ID: {}", creditNoteId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid credit note ID", "INVALID_CREDIT_NOTE_ID")
                );
            }

            CreditNote creditNote = creditNoteRepository.findById(decodedCreditNoteId).orElse(null);
            if (creditNote == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            // Check if credit note is editable (DRAFT only)
            if (!creditNote.isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Credit note is not editable. Only DRAFT credit notes can be modified.", "CREDIT_NOTE_NOT_EDITABLE")
                );
            }

            // Automatically determine display order based on existing count + 1
            long existingCount = creditNoteLineItemRepository.countByCreditNoteId(decodedCreditNoteId);
            int nextDisplayOrder = (int) existingCount + 1;

            // Resolve optional InvoiceLineItem link — must belong to the credit note's invoice
            InvoiceLineItem linkedInvoiceLineItem = null;
            if (createDTO.getInvoiceLineItemId() != null && !createDTO.getInvoiceLineItemId().isBlank()) {
                Long invoiceLineItemId;
                try {
                    invoiceLineItemId = idObfuscator.decodeId(createDTO.getInvoiceLineItemId());
                } catch (Exception e) {
                    log.warn("Failed to decode invoice line item ID: {}", createDTO.getInvoiceLineItemId(), e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid invoice line item ID", "INVALID_INVOICE_LINE_ITEM_ID")
                    );
                }

                linkedInvoiceLineItem = invoiceLineItemRepository.findById(invoiceLineItemId).orElse(null);
                if (linkedInvoiceLineItem == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invoice line item not found", "INVOICE_LINE_ITEM_NOT_FOUND")
                    );
                }

                // Guard: the referenced line item must belong to this credit note's invoice
                if (creditNote.getInvoice() == null
                        || linkedInvoiceLineItem.getInvoice() == null
                        || !creditNote.getInvoice().getId().equals(linkedInvoiceLineItem.getInvoice().getId())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Invoice line item does not belong to this credit note's invoice",
                            "INVOICE_LINE_ITEM_MISMATCH")
                    );
                }
            }

            // Validate prices
            if (createDTO.getPrices() == null || createDTO.getPrices().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "At least one price is required", "PRICE_REQUIRED")
                );
            }

            // Validate for duplicate currencies
            Set<String> currencies = new HashSet<>();
            for (CreateCreditNoteLineItemDTO.PriceInput priceInput : createDTO.getPrices()) {
                String currency = priceInput.getCurrency().toUpperCase();
                if (!currencies.add(currency)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Duplicate currency detected: " + currency + ". Please merge prices for the same currency before submitting.",
                            "DUPLICATE_CURRENCY")
                    );
                }
            }

            // Convert PriceInput objects to Price objects with computed totalPrice
            List<Price> prices = createDTO.getPrices().stream()
                .map(priceInput -> {
                    BigDecimal totalPrice = priceInput.getUnitPrice()
                        .multiply(BigDecimal.valueOf(priceInput.getQuantity()));

                    return Price.builder()
                        .currency(priceInput.getCurrency().toUpperCase())
                        .quantity(priceInput.getQuantity())
                        .unitPrice(priceInput.getUnitPrice())
                        .totalPrice(totalPrice)
                        .build();
                })
                .collect(Collectors.toList());

            // Over-credit guard: would adding this line push total credits past the invoice grand total?
            if (!Boolean.TRUE.equals(createDTO.getForce()) && creditNote.getInvoice() != null) {
                java.util.Map<String, BigDecimal> newContribution =
                    totalsAggregationService.contributionForNewLineItem(creditNote, prices);
                CreditNoteTotalsAggregationService.OverCreditCheck check =
                    totalsAggregationService.checkOverCredit(creditNote.getInvoice(), newContribution);
                if (check.overCredit()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, check.message(), "OVER_CREDIT")
                    );
                }
            }

            // Build credit note line item entity
            CreditNoteLineItem lineItem = CreditNoteLineItem.builder()
                .creditNote(creditNote)
                .invoiceLineItem(linkedInvoiceLineItem)
                .itemType(createDTO.getItemType())
                .itemName(createDTO.getItemName())
                .description(createDTO.getDescription())
                .displayOrder(nextDisplayOrder)
                .prices(prices)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save line item
            lineItem = creditNoteLineItemRepository.save(lineItem);

            log.info("Credit note line item created successfully: {}", lineItem.getItemName());

            // Convert to DTO
            CreditNoteLineItemDTO lineItemDTO = creditNoteCreateService.convertLineItemToDTO(lineItem);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Credit note line item created successfully", lineItemDTO)
            );

        } catch (Exception e) {
            log.error("Error creating credit note line item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create credit note line item", "CREDIT_NOTE_LINE_ITEM_CREATE_FAILED")
            );
        }
    }

    /**
     * Clone selected InvoiceLineItems into this credit note as CreditNoteLineItems.
     *
     * For each invoiceLineItemId:
     *   - must decode successfully, exist, and belong to the credit note's invoice;
     *   - itemName, description, prices (deep copy), invoiceLineItem FK are copied;
     *   - itemType is mapped from InvoiceItemType to CreditNoteItemType (non-matching types fall back to OTHER).
     *
     * The over-credit guard runs once across ALL clones combined; set force=true to override.
     */
    @AuditLogAnnotation(
        action = "CLONE_INVOICE_LINE_ITEMS_TO_CREDIT_NOTE",
        description = "Cloning invoice line items into a credit note",
        entityType = "CreditNote",
        entityIdParamName = "creditNoteId"
    )
    public ResponseEntity<ApiResponse<?>> cloneInvoiceLineItems(String creditNoteId, CloneInvoiceLineItemsDTO dto) {
        log.info("Cloning {} invoice line items into credit note {}",
            dto.getInvoiceLineItemIds() != null ? dto.getInvoiceLineItemIds().size() : 0, creditNoteId);

        try {
            // Decode credit note id
            Long decodedCreditNoteId;
            try {
                decodedCreditNoteId = idObfuscator.decodeId(creditNoteId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid credit note ID", "INVALID_CREDIT_NOTE_ID")
                );
            }

            CreditNote creditNote = creditNoteRepository.findById(decodedCreditNoteId).orElse(null);
            if (creditNote == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            if (!creditNote.isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Credit note is not editable. Only DRAFT credit notes can be modified.", "CREDIT_NOTE_NOT_EDITABLE")
                );
            }

            if (creditNote.getInvoice() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Credit note has no linked invoice", "CREDIT_NOTE_NO_INVOICE")
                );
            }

            if (dto.getInvoiceLineItemIds() == null || dto.getInvoiceLineItemIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "At least one invoice line item ID is required", "INVOICE_LINE_ITEM_IDS_REQUIRED")
                );
            }

            // Resolve & validate all referenced invoice line items first (fail fast)
            Long invoiceId = creditNote.getInvoice().getId();
            List<InvoiceLineItem> resolved = new ArrayList<>();
            for (String obfuscatedId : dto.getInvoiceLineItemIds()) {
                if (obfuscatedId == null || obfuscatedId.isBlank()) continue;
                Long decoded;
                try {
                    decoded = idObfuscator.decodeId(obfuscatedId);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid invoice line item ID: " + obfuscatedId, "INVALID_INVOICE_LINE_ITEM_ID")
                    );
                }
                InvoiceLineItem src = invoiceLineItemRepository.findById(decoded).orElse(null);
                if (src == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invoice line item not found: " + obfuscatedId, "INVOICE_LINE_ITEM_NOT_FOUND")
                    );
                }
                if (src.getInvoice() == null || !invoiceId.equals(src.getInvoice().getId())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Invoice line item " + obfuscatedId + " does not belong to this credit note's invoice",
                            "INVOICE_LINE_ITEM_MISMATCH")
                    );
                }
                resolved.add(src);
            }

            // Combined over-credit check across all clones
            if (!Boolean.TRUE.equals(dto.getForce())) {
                Map<String, BigDecimal> combined = new HashMap<>();
                for (InvoiceLineItem src : resolved) {
                    Map<String, BigDecimal> itemContribution =
                        totalsAggregationService.contributionForNewLineItem(creditNote, src.getPrices());
                    for (Map.Entry<String, BigDecimal> e : itemContribution.entrySet()) {
                        combined.merge(e.getKey(), e.getValue(), BigDecimal::add);
                    }
                }
                CreditNoteTotalsAggregationService.OverCreditCheck check =
                    totalsAggregationService.checkOverCredit(creditNote.getInvoice(), combined);
                if (check.overCredit()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, check.message(), "OVER_CREDIT")
                    );
                }
            }

            // Persist clones
            long existingCount = creditNoteLineItemRepository.countByCreditNoteId(decodedCreditNoteId);
            int nextDisplayOrder = (int) existingCount + 1;

            List<CreditNoteLineItemDTO> created = new ArrayList<>();
            for (InvoiceLineItem src : resolved) {
                // Deep-copy prices so the credit note's prices aren't shared with invoice state
                List<Price> clonedPrices = src.getPrices() == null ? new ArrayList<>() :
                    src.getPrices().stream()
                        .map(p -> Price.builder()
                            .currency(p.getCurrency())
                            .quantity(p.getQuantity())
                            .unitPrice(p.getUnitPrice())
                            .totalPrice(p.getTotalPrice())
                            .breakdown(p.getBreakdown())
                            .build())
                        .collect(Collectors.toList());

                CreditNoteLineItem lineItem = CreditNoteLineItem.builder()
                    .creditNote(creditNote)
                    .invoiceLineItem(src)
                    .itemType(mapItemType(src.getItemType()))
                    .itemName(src.getItemName())
                    .description(src.getDescription())
                    .displayOrder(nextDisplayOrder++)
                    .prices(clonedPrices)
                    .isActive(true)
                    .build();

                lineItem = creditNoteLineItemRepository.save(lineItem);
                created.add(creditNoteCreateService.convertLineItemToDTO(lineItem));
            }

            log.info("Cloned {} invoice line items into credit note {}", created.size(), creditNote.getCreditNoteCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201,
                    String.format("%d invoice line items cloned into credit note", created.size()),
                    created)
            );

        } catch (Exception e) {
            log.error("Error cloning invoice line items into credit note", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to clone invoice line items", "CLONE_INVOICE_LINE_ITEMS_FAILED")
            );
        }
    }

    /**
     * Map InvoiceItemType to CreditNoteItemType by name. Types that exist in
     * InvoiceItemType but not in CreditNoteItemType (GUIDE, MEALS, EQUIPMENT,
     * INSURANCE, VISA) fall back to OTHER.
     */
    private CreditNoteItemType mapItemType(InvoiceItemType invoiceType) {
        if (invoiceType == null) return CreditNoteItemType.OTHER;
        try {
            return CreditNoteItemType.valueOf(invoiceType.name());
        } catch (IllegalArgumentException e) {
            return CreditNoteItemType.OTHER;
        }
    }
}
