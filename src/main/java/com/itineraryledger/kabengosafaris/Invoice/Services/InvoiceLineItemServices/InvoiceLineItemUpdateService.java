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
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdateInvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceLineItemUpdateService {

    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceTotalsCalculationService totalsCalculationService;

    @AuditLogAnnotation(
        action = "UPDATE_INVOICE_LINE_ITEM",
        description = "Updating an invoice line item",
        entityType = "InvoiceLineItem",
        entityIdParamName = "itemId"
    )
    public ResponseEntity<ApiResponse<?>> updateInvoiceLineItem(String invoiceId, String itemId, UpdateInvoiceLineItemDTO updateDTO) {
        log.info("Updating invoice line item with ID: {}", itemId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(itemId);
            } catch (Exception e) {
                log.warn("Failed to decode invoice line item ID: {}", itemId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice line item ID", "INVALID_INVOICE_LINE_ITEM_ID")
                );
            }

            InvoiceLineItem lineItem = invoiceLineItemRepository.findById(id).orElse(null);
            if (lineItem == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice line item not found", "INVOICE_LINE_ITEM_NOT_FOUND")
                );
            }

            // Check if invoice is editable
            if (!lineItem.getInvoice().isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice is not editable. Only DRAFT invoices can be modified.", "INVOICE_NOT_EDITABLE")
                );
            }

            if (updateDTO.getItemType() != null) {
                lineItem.setItemType(updateDTO.getItemType());
            }
            if (updateDTO.getItemName() != null) {
                lineItem.setItemName(updateDTO.getItemName());
            }
            if (updateDTO.getDescription() != null) {
                lineItem.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getPrices() != null) {
                if (updateDTO.getPrices().isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "At least one price is required", "PRICE_REQUIRED")
                    );
                }

                Set<String> currencies = new HashSet<>();
                for (UpdateInvoiceLineItemDTO.PriceInput priceInput : updateDTO.getPrices()) {
                    String currency = priceInput.getCurrency().toUpperCase();
                    if (!currencies.add(currency)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Duplicate currency detected: " + currency, "DUPLICATE_CURRENCY")
                        );
                    }
                }

                List<Price> prices = updateDTO.getPrices().stream()
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
                lineItem.setPrices(prices);
            }
            if (updateDTO.getIsActive() != null) {
                lineItem.setIsActive(updateDTO.getIsActive());
            }

            lineItem = invoiceLineItemRepository.save(lineItem);
            totalsCalculationService.recalculateTotals(lineItem.getInvoice().getId());

            InvoiceLineItemDTO lineItemDTO = convertToDTO(lineItem);

            log.info("Invoice line item updated successfully: {}", lineItem.getItemName());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice line item updated successfully", lineItemDTO)
            );

        } catch (Exception e) {
            log.error("Error updating invoice line item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update invoice line item", "INVOICE_LINE_ITEM_UPDATE_FAILED")
            );
        }
    }

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
