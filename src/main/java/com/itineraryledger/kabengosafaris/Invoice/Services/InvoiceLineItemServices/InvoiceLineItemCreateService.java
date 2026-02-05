package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating invoice line items
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceLineItemCreateService {

    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceTotalsCalculationService totalsCalculationService;

    @AuditLogAnnotation(
        action = "CREATE_INVOICE_LINE_ITEM",
        description = "Creating a new invoice line item",
        entityType = "InvoiceLineItem"
    )
    public ResponseEntity<ApiResponse<?>> createInvoiceLineItem(String invoiceId, CreateInvoiceLineItemDTO createDTO) {
        log.info("Creating new invoice line item");

        try {
            // Validate and decode invoice ID
            Long decodedInvoiceId;
            try {
                decodedInvoiceId = idObfuscator.decodeId(invoiceId);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", invoiceId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            Invoice invoice = invoiceRepository.findById(decodedInvoiceId).orElse(null);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Check if invoice is editable (DRAFT only)
            if (!invoice.isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice is not editable. Only DRAFT invoices can be modified.", "INVOICE_NOT_EDITABLE")
                );
            }

            // Automatically determine display order (max + 1, or 1 if no items exist)
            Integer maxDisplayOrder = invoiceLineItemRepository.findMaxDisplayOrderByInvoiceId(decodedInvoiceId);
            int nextDisplayOrder = (maxDisplayOrder != null) ? maxDisplayOrder + 1 : 1;

            // Validate prices
            if (createDTO.getPrices() == null || createDTO.getPrices().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "At least one price is required", "PRICE_REQUIRED")
                );
            }

            // Validate for duplicate currencies
            Set<String> currencies = new HashSet<>();
            for (CreateInvoiceLineItemDTO.PriceInput priceInput : createDTO.getPrices()) {
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
                        .currency(priceInput.getCurrency())
                        .quantity(priceInput.getQuantity())
                        .unitPrice(priceInput.getUnitPrice())
                        .totalPrice(totalPrice)
                        .breakdown(priceInput.getBreakdown())
                        .build();
                })
                .collect(Collectors.toList());

            // Build invoice line item entity
            InvoiceLineItem lineItem = InvoiceLineItem.builder()
                .invoice(invoice)
                .itemType(createDTO.getItemType())
                .itemName(createDTO.getItemName())
                .description(createDTO.getDescription())
                .displayOrder(nextDisplayOrder)
                .prices(prices)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save line item
            lineItem = invoiceLineItemRepository.save(lineItem);

            // Recalculate invoice totals after adding item
            totalsCalculationService.recalculateTotals(decodedInvoiceId);

            log.info("Invoice line item created successfully: {}", lineItem.getItemName());

            // Convert to DTO
            InvoiceLineItemDTO lineItemDTO = convertToDTO(lineItem);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Invoice line item created successfully", lineItemDTO)
            );

        } catch (Exception e) {
            log.error("Error creating invoice line item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create invoice line item", "INVOICE_LINE_ITEM_CREATE_FAILED")
            );
        }
    }

    /**
     * Convert InvoiceLineItem entity to InvoiceLineItemDTO
     */
    private InvoiceLineItemDTO convertToDTO(InvoiceLineItem lineItem) {
        return InvoiceLineItemDTO.builder()
            .id(idObfuscator.encodeId(lineItem.getId()))
            .invoiceId(idObfuscator.encodeId(lineItem.getInvoice().getId()))
            .invoiceCode(lineItem.getInvoice().getInvoiceCode())
            .itemType(lineItem.getItemType())
            .itemTypeDisplayName(lineItem.getItemType().getDisplayName())
            .itemName(lineItem.getItemName())
            .description(lineItem.getDescription())
            .displayOrder(lineItem.getDisplayOrder())
            .prices(lineItem.getPrices())
            .isActive(lineItem.getIsActive())
            .createdAt(lineItem.getCreatedAt())
            .updatedAt(lineItem.getUpdatedAt())
            .build();
    }
}
