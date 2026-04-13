package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreateCreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteLineItemDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating credit notes
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CreditNoteCreateService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final CreditNoteTotalsAggregationService totalsAggregationService;

    @AuditLogAnnotation(
        action = "CREATE_CREDIT_NOTE",
        description = "Creating a new credit note",
        entityType = "CreditNote"
    )
    public ResponseEntity<ApiResponse<?>> createCreditNote(CreateCreditNoteDTO createDTO) {
        log.info("Creating new credit note");

        try {
            // Invoice ID is REQUIRED
            if (createDTO.getInvoiceId() == null || createDTO.getInvoiceId().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice ID is required", "INVOICE_ID_REQUIRED")
                );
            }

            // Validate and decode invoice ID
            Invoice invoice;
            try {
                Long invoiceId = idObfuscator.decodeId(createDTO.getInvoiceId());
                invoice = invoiceRepository.findById(invoiceId).orElse(null);
                if (invoice == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invoice not found", "INVOICE_NOT_FOUND")
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", createDTO.getInvoiceId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            // Status guard: only invoices that have been issued to the customer
            // can have credit notes created against them.
            //   - DRAFT:     edit the invoice directly instead.
            //   - CANCELLED: invoice is void — nothing to credit.
            if (invoice.getStatus() == InvoiceStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot credit a DRAFT invoice — edit the invoice directly instead",
                        "INVOICE_NOT_ISSUED")
                );
            }
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot create a credit note against a cancelled invoice",
                        "INVOICE_CANCELLED")
                );
            }

            // Derive customer from invoice (REQUIRED)
            if (invoice.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot create credit note: Invoice has no customer linked.",
                        "INVOICE_NO_CUSTOMER")
                );
            }

            Customer customer = invoice.getCustomer();
            log.info("Customer derived from invoice: {}", customer.getDisplayName());

            // Get current user for audit tracking
            User currentUser = getCurrentUser();

            // Default title if not provided
            String title = createDTO.getTitle();
            if (title == null || title.isBlank()) {
                title = "Credit Note - " + invoice.getInvoiceCode();
            }

            // Build credit note entity (creditNoteCode will be set after save)
            CreditNote creditNote = CreditNote.builder()
                .creditNoteCode("TEMP") // Temporary code, will be updated after save
                .title(title)
                .description(createDTO.getDescription())
                .invoice(invoice)
                .customer(customer)
                .lineItems(new ArrayList<>())
                .taxPercentage(createDTO.getTaxPercentage())
                .issueDate(createDTO.getIssueDate())
                .reason(createDTO.getReason())
                .status(CreditNoteStatus.DRAFT)
                .createdBy(currentUser)
                .internalNotes(createDTO.getInternalNotes())
                .customerNotes(createDTO.getCustomerNotes())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save credit note to get ID
            creditNote = creditNoteRepository.save(creditNote);

            // Generate credit note code based on ID
            String creditNoteCode = creditNote.generateCode();
            creditNote.setCreditNoteCode(creditNoteCode);

            // Save again with the generated code
            creditNote = creditNoteRepository.save(creditNote);

            log.info("Credit note created successfully with code: {}", creditNoteCode);

            // Convert to DTO
            CreditNoteDTO creditNoteDTO = convertToDTO(creditNote);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Credit note created successfully", creditNoteDTO)
            );

        } catch (Exception e) {
            log.error("Error creating credit note", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create credit note", "CREDIT_NOTE_CREATE_FAILED")
            );
        }
    }

    /**
     * Convert CreditNote entity to CreditNoteDTO
     */
    public CreditNoteDTO convertToDTO(CreditNote creditNote) {
        // Derive subtotals/taxes/totals from active line items + taxPercentage —
        // they are not stored on the CreditNote entity.
        CreditNoteTotalsAggregationService.ComputedTotals computed =
            totalsAggregationService.compute(creditNote);

        CreditNoteDTO dto = CreditNoteDTO.builder()
            .id(idObfuscator.encodeId(creditNote.getId()))
            .creditNoteCode(creditNote.getCreditNoteCode())
            .title(creditNote.getTitle())
            .description(creditNote.getDescription())
            .status(creditNote.getStatus())
            .statusDisplayName(creditNote.getStatus().getDisplayName())
            .subtotals(convertPriceList(computed.subtotals()))
            .taxes(convertPriceList(computed.taxes()))
            .totals(convertPriceList(computed.totals()))
            .taxPercentage(creditNote.getTaxPercentage())
            .issueDate(creditNote.getIssueDate())
            .sentDate(creditNote.getSentDate())
            .consumedDate(creditNote.getConsumedDate())
            .consumptionMethod(creditNote.getConsumptionMethod())
            .consumptionMethodDisplayName(
                creditNote.getConsumptionMethod() != null
                    ? creditNote.getConsumptionMethod().getDisplayName()
                    : null
            )
            .consumptionNotes(creditNote.getConsumptionNotes())
            .reason(creditNote.getReason())
            .internalNotes(creditNote.getInternalNotes())
            .customerNotes(creditNote.getCustomerNotes())
            .lineItemCount(creditNote.getLineItems() != null ? creditNote.getLineItems().size() : 0)
            .isActive(creditNote.getIsActive())
            .createdAt(creditNote.getCreatedAt())
            .updatedAt(creditNote.getUpdatedAt())
            .build();

        // Set invoice if present
        if (creditNote.getInvoice() != null) {
            dto.setInvoiceId(idObfuscator.encodeId(creditNote.getInvoice().getId()));
            dto.setInvoiceCode(creditNote.getInvoice().getInvoiceCode());
        }

        // Set customer if present
        if (creditNote.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(creditNote.getCustomer().getId()));
            dto.setCustomerName(creditNote.getCustomer().getDisplayName());
        }

        // Set created by if present
        if (creditNote.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(creditNote.getCreatedBy().getId()));
            dto.setCreatedByName(creditNote.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (creditNote.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(creditNote.getUpdatedBy().getId()));
            dto.setUpdatedByName(creditNote.getUpdatedBy().getUsername());
        }

        return dto;
    }

    /**
     * Convert CreditNoteLineItem entity to CreditNoteLineItemDTO
     */
    public CreditNoteLineItemDTO convertLineItemToDTO(CreditNoteLineItem lineItem) {
        return CreditNoteLineItemDTO.builder()
            .id(idObfuscator.encodeId(lineItem.getId()))
            .creditNoteId(idObfuscator.encodeId(lineItem.getCreditNote().getId()))
            .invoiceLineItemId(lineItem.getInvoiceLineItem() != null
                ? idObfuscator.encodeId(lineItem.getInvoiceLineItem().getId())
                : null)
            .itemType(lineItem.getItemType())
            .itemTypeDisplayName(lineItem.getItemType().getDisplayName())
            .itemName(lineItem.getItemName())
            .description(lineItem.getDescription())
            .displayOrder(lineItem.getDisplayOrder())
            .prices(convertPriceList(lineItem.getPrices()))
            .isActive(lineItem.getIsActive())
            .createdAt(lineItem.getCreatedAt())
            .updatedAt(lineItem.getUpdatedAt())
            .build();
    }

    /**
     * Convert a list of Price embeddables to PriceDTO list
     */
    private List<CreditNoteDTO.PriceDTO> convertPriceList(List<Price> prices) {
        if (prices == null || prices.isEmpty()) {
            return new ArrayList<>();
        }
        return prices.stream()
            .map(price -> CreditNoteDTO.PriceDTO.builder()
                .currency(price.getCurrency())
                .quantity(price.getQuantity())
                .unitPrice(price.getUnitPrice())
                .totalPrice(price.getTotalPrice())
                .breakdown(price.getBreakdown())
                .formattedUnitPrice(
                    price.getCurrency() != null && price.getUnitPrice() != null
                        ? String.format("%s %,.2f", price.getCurrency(), price.getUnitPrice())
                        : null
                )
                .formattedTotalPrice(
                    price.getCurrency() != null && price.getTotalPrice() != null
                        ? String.format("%s %,.2f", price.getCurrency(), price.getTotalPrice())
                        : null
                )
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                // Fetch from repository to ensure it's a managed entity
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
