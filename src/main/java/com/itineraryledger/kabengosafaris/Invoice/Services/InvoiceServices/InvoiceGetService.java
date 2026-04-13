package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.time.LocalDate;
import java.util.Arrays;
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

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.InvoiceSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving invoices with filtering, pagination, and sorting
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "invoiceCode", "title", "status", "issueDate", "dueDate", "sentDate", "paidDate", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final InvoiceRepository invoiceRepository;
    private final IdObfuscator idObfuscator;
    private final InvoicePaymentAggregationService paymentAggregationService;

    /**
     * Get a single invoice by obfuscated ID
     *
     * @param idObfuscated The obfuscated invoice ID
     * @return ResponseEntity with ApiResponse containing the invoice
     */
    public ResponseEntity<ApiResponse<?>> getInvoiceById(String idObfuscated) {
        log.info("Fetching invoice with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            // Find invoice
            Invoice invoice = invoiceRepository.findById(id).orElse(null);
            if (invoice == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Convert to DTO
            InvoiceDTO invoiceDTO = convertToDTO(invoice);

            // Build navigation
            Long nextId = invoiceRepository.findNextId(id).orElse(null);
            Long previousId = invoiceRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = invoiceRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = invoiceRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("invoice", invoiceDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice", "INVOICE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single invoice by invoice code
     *
     * @param invoiceCode The invoice code
     * @return ResponseEntity with ApiResponse containing the invoice
     */
    public ResponseEntity<ApiResponse<?>> getInvoiceByCode(String invoiceCode) {
        log.info("Fetching invoice with code: {}", invoiceCode);

        try {
            // Find invoice
            Invoice invoice = invoiceRepository.findByInvoiceCode(invoiceCode).orElse(null);
            if (invoice == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Convert to DTO
            InvoiceDTO invoiceDTO = convertToDTO(invoice);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice retrieved successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice", "INVOICE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all invoices with pagination, sorting, and filtering
     *
     * @param invoiceCode Filter by invoice code (partial match)
     * @param title Filter by title (partial match)
     * @param status Filter by status
     * @param paymentStatus Filter by payment status
     * @param customerId Filter by customer ID (obfuscated)
     * @param safariId Filter by safari ID (obfuscated)
     * @param createdById Filter by created by ID (obfuscated)
     * @param updatedById Filter by updated by ID (obfuscated)
     * @param isActive Filter by active status
     * @param issueDateAfter Filter by issue date after
     * @param issueDateBefore Filter by issue date before
     * @param dueDateAfter Filter by due date after
     * @param dueDateBefore Filter by due date before
     * @param sentAfter Filter by sent after date
     * @param sentBefore Filter by sent before date
     * @param isOverdue Filter by overdue status
     * @param statusGroup Filter by status group (draft, unpaid, paid, closed)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated invoices
     */
    public ResponseEntity<ApiResponse<?>> getAllInvoices(
        String invoiceCode,
        String title,
        InvoiceStatus status,
        String customerId,
        String safariId,
        String createdById,
        String updatedById,
        Boolean isActive,
        LocalDate issueDateAfter,
        LocalDate issueDateBefore,
        LocalDate dueDateAfter,
        LocalDate dueDateBefore,
        LocalDate sentAfter,
        LocalDate sentBefore,
        Boolean isOverdue,
        String statusGroup,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all invoices with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Build specification for filtering
            Specification<Invoice> spec = Specification.unrestricted();

            if (invoiceCode != null && !invoiceCode.isEmpty()) {
                spec = spec.and(InvoiceSpecification.byInvoiceCode(invoiceCode));
            }
            if (title != null && !title.isEmpty()) {
                spec = spec.and(InvoiceSpecification.byTitle(title));
            }
            if (status != null) {
                spec = spec.and(InvoiceSpecification.byStatus(status));
            }
            if (isActive != null) {
                spec = spec.and(InvoiceSpecification.byIsActive(isActive));
            }
            if (issueDateAfter != null) {
                spec = spec.and(InvoiceSpecification.byIssueDateAfter(issueDateAfter));
            }
            if (issueDateBefore != null) {
                spec = spec.and(InvoiceSpecification.byIssueDateBefore(issueDateBefore));
            }
            if (dueDateAfter != null) {
                spec = spec.and(InvoiceSpecification.byDueDateAfter(dueDateAfter));
            }
            if (dueDateBefore != null) {
                spec = spec.and(InvoiceSpecification.byDueDateBefore(dueDateBefore));
            }
            if (sentAfter != null) {
                spec = spec.and(InvoiceSpecification.bySentAfter(sentAfter));
            }
            if (sentBefore != null) {
                spec = spec.and(InvoiceSpecification.bySentBefore(sentBefore));
            }
            if (Boolean.TRUE.equals(isOverdue)) {
                spec = spec.and(InvoiceSpecification.byOverdue());
            }

            // Apply relationship filters (decode IDs)
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(customerId);
                    spec = spec.and(InvoiceSpecification.byCustomerId(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode customer ID: {}", customerId, e);
                }
            }
            if (safariId != null && !safariId.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(safariId);
                    spec = spec.and(InvoiceSpecification.bySafariId(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode safari ID: {}", safariId, e);
                }
            }
            if (createdById != null && !createdById.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(createdById);
                    spec = spec.and(InvoiceSpecification.byCreatedById(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode created by ID: {}", createdById, e);
                }
            }
            if (updatedById != null && !updatedById.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(updatedById);
                    spec = spec.and(InvoiceSpecification.byUpdatedById(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode updated by ID: {}", updatedById, e);
                }
            }

            // Apply status group filter
            if (statusGroup != null && !statusGroup.isEmpty()) {
                switch (statusGroup.toLowerCase()) {
                    case "draft":
                        spec = spec.and(InvoiceSpecification.byDraftStatus());
                        break;
                    case "unpaid":
                        spec = spec.and(InvoiceSpecification.byUnpaidStatuses());
                        break;
                    case "paid":
                        spec = spec.and(InvoiceSpecification.byPaidStatuses());
                        break;
                    case "closed":
                        spec = spec.and(InvoiceSpecification.byClosedStatuses());
                        break;
                    default:
                        log.warn("Unknown status group: {}", statusGroup);
                }
            }

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set sorting (always sort by createdAt)
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch invoices
            Page<Invoice> invoicePage = invoiceRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<InvoiceDTO> invoiceDTOs = invoicePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("invoices", invoiceDTOs);
            response.put("currentPage", invoicePage.getNumber());
            response.put("totalItems", invoicePage.getTotalElements());
            response.put("totalPages", invoicePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoices retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching invoices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoices", "INVOICES_FETCH_FAILED")
            );
        }
    }

    /**
     * Validate and return the sort field, or null if invalid
     */
    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert Invoice entity to InvoiceDTO
     */
    public InvoiceDTO convertToDTO(Invoice invoice) {
        InvoiceDTO dto = InvoiceDTO.builder()
            .id(idObfuscator.encodeId(invoice.getId()))
            .invoiceCode(invoice.getInvoiceCode())
            .title(invoice.getTitle())
            .description(invoice.getDescription())
            .subtotals(invoice.getSubtotals())
            .taxes(invoice.getTaxes())
            .discounts(invoice.getDiscounts())
            .grandTotals(invoice.getGrandTotals())
            .amountsPaid(paymentAggregationService.computeAmountsPaid(invoice))
            .balances(paymentAggregationService.computeBalances(invoice))
            .taxPercentage(invoice.getTaxPercentage())
            .discountPercentage(invoice.getDiscountPercentage())
            .discountReason(invoice.getDiscountReason())
            .issueDate(invoice.getIssueDate())
            .dueDate(invoice.getDueDate())
            .sentDate(invoice.getSentDate())
            .paidDate(invoice.getPaidDate())
            .status(invoice.getStatus())
            .statusDisplayName(invoice.getStatus().getDisplayName())
            .internalNotes(invoice.getInternalNotes())
            .customerNotes(invoice.getCustomerNotes())
            .paymentTerms(invoice.getPaymentTerms())
            .isActive(invoice.getIsActive())
            .isOverdue(invoice.isOverdue())
            .lineItemCount(invoice.getLineItems() != null ? (long) invoice.getLineItems().size() : 0L)
            .createdAt(invoice.getCreatedAt())
            .updatedAt(invoice.getUpdatedAt())
            .build();

        // Set customer if present
        if (invoice.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(invoice.getCustomer().getId()));
            dto.setCustomerName(invoice.getCustomer().getDisplayName());
            dto.setCustomerEmail(invoice.getCustomer().getPrimaryEmail());
        }

        // Set safari if present
        if (invoice.getSafari() != null) {
            dto.setSafariId(idObfuscator.encodeId(invoice.getSafari().getId()));
            dto.setSafariCode(invoice.getSafari().getCode());
            dto.setSafariName(invoice.getSafari().getName());
        }

        // Set created by if present
        if (invoice.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(invoice.getCreatedBy().getId()));
            dto.setCreatedByName(invoice.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (invoice.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(invoice.getUpdatedBy().getId()));
            dto.setUpdatedByName(invoice.getUpdatedBy().getUsername());
        }

        return dto;
    }
}
