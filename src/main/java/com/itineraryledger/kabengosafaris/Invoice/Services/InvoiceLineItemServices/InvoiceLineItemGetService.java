package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.InvoiceLineItemSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceLineItemGetService {

    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getInvoiceLineItemById(String invoiceId, String itemId) {
        log.info("Fetching invoice line item with ID: {}", itemId);

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

            InvoiceLineItemDTO lineItemDTO = convertToDTO(lineItem);
            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice line item retrieved successfully", lineItemDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice line item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice line item", "INVOICE_LINE_ITEM_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllInvoiceLineItems(
        String invoiceId,
        String itemName,
        String description,
        Boolean isActive,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching invoice line items for invoice ID: {}", invoiceId);

        try {
            Long decodedInvoiceId;
            try {
                decodedInvoiceId = idObfuscator.decodeId(invoiceId);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", invoiceId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            Specification<InvoiceLineItem> spec = InvoiceLineItemSpecification.byInvoiceId(decodedInvoiceId);

            if (itemName != null && !itemName.isEmpty()) {
                spec = spec.and(InvoiceLineItemSpecification.byItemName(itemName));
            }
            if (description != null && !description.isEmpty()) {
                spec = spec.and(InvoiceLineItemSpecification.byDescription(description));
            }
            if (isActive != null) {
                spec = spec.and(InvoiceLineItemSpecification.byIsActive(isActive));
            }

            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "displayOrder"));
            Page<InvoiceLineItem> lineItemPage = invoiceLineItemRepository.findAll(spec, pageable);

            List<InvoiceLineItemDTO> lineItemDTOs = lineItemPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("lineItems", lineItemDTOs);
            response.put("currentPage", lineItemPage.getNumber());
            response.put("totalItems", lineItemPage.getTotalElements());
            response.put("totalPages", lineItemPage.getTotalPages());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice line items retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice line items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice line items", "INVOICE_LINE_ITEMS_FETCH_FAILED")
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
