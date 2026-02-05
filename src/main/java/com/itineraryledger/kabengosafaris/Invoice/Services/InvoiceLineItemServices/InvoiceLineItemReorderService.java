package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.ReorderInvoiceItemsDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.ReorderInvoiceItemsDTO.ItemOrder;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class InvoiceLineItemReorderService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public InvoiceLineItemReorderService(
        InvoiceRepository invoiceRepository,
        InvoiceLineItemRepository invoiceLineItemRepository,
        IdObfuscator idObfuscator
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(action = "REORDER_INVOICE_LINE_ITEMS", description = "Reordering invoice line items", entityType = "InvoiceLineItem")
    public ResponseEntity<ApiResponse<?>> reorderInvoiceLineItems(
        String invoiceIdObfuscated,
        ReorderInvoiceItemsDTO reorderDTO
    ) {
        log.info("Reordering items for invoice: {}", invoiceIdObfuscated);

        try {
            Long invoiceId;
            try {
                invoiceId = idObfuscator.decodeId(invoiceIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid invoice ID format: {}", invoiceIdObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID format", "INVALID_INVOICE_ID")
                );
            }

            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                log.warn("Invoice not found: {}", invoiceId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Check if invoice is editable
            if (!invoice.isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice is not editable. Only DRAFT invoices can be modified.", "INVOICE_NOT_EDITABLE")
                );
            }

            List<InvoiceLineItem> existingItems = invoiceLineItemRepository.findByInvoiceIdOrderByDisplayOrderAsc(invoiceId);

            if (existingItems.isEmpty()) {
                log.warn("No items found for invoice: {}", invoiceId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice has no items to reorder", "NO_ITEMS_TO_REORDER")
                );
            }

            List<ItemOrder> itemOrders = reorderDTO.getItemOrders();

            if (itemOrders.size() != existingItems.size()) {
                log.warn("Item order count mismatch. Expected: {}, Received: {}", existingItems.size(), itemOrders.size());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Item order list must contain exactly " + existingItems.size() + " items",
                        "ITEM_COUNT_MISMATCH"
                    )
                );
            }

            Map<Long, InvoiceLineItem> itemMap = existingItems.stream()
                .collect(Collectors.toMap(InvoiceLineItem::getId, item -> item));

            List<Long> decodedItemIds = new ArrayList<>();
            for (ItemOrder itemOrder : itemOrders) {
                try {
                    Long itemId = idObfuscator.decodeId(itemOrder.getItemId());
                    decodedItemIds.add(itemId);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid item ID: " + itemOrder.getItemId(), "INVALID_ITEM_ID")
                    );
                }
            }

            for (Long itemId : decodedItemIds) {
                if (!itemMap.containsKey(itemId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Item " + itemId + " does not belong to this invoice", "ITEM_NOT_IN_INVOICE")
                    );
                }
            }

            Set<Long> uniqueIds = new HashSet<>(decodedItemIds);
            if (uniqueIds.size() != decodedItemIds.size()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Duplicate item IDs found in reorder list", "DUPLICATE_ITEM_IDS")
                );
            }

            // First pass: set temporary high display orders
            for (InvoiceLineItem item : existingItems) {
                item.setDisplayOrder(item.getDisplayOrder() + 10000);
            }
            invoiceLineItemRepository.saveAll(existingItems);
            invoiceLineItemRepository.flush();

            // Second pass: set final display orders
            for (int i = 0; i < itemOrders.size(); i++) {
                Long itemId = decodedItemIds.get(i);
                InvoiceLineItem item = itemMap.get(itemId);
                item.setDisplayOrder(i);
            }
            invoiceLineItemRepository.saveAll(existingItems);

            List<InvoiceLineItemDTO> reorderedDTOs = existingItems.stream()
                .sorted(Comparator.comparing(InvoiceLineItem::getDisplayOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} items for invoice: {}", reorderedDTOs.size(), invoiceIdObfuscated);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice line items reordered successfully", reorderedDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering invoice line items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder invoice line items", "REORDER_FAILED")
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
