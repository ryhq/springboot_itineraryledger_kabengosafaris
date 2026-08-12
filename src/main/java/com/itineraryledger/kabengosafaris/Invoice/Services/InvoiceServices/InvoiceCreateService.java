package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating invoices
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceCreateService {

    private final InvoiceRepository invoiceRepository;
    private final SafariRepository safariRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceTotalsCalculationService totalsCalculationService;
    private final InvoicePaymentAggregationService paymentAggregationService;

    @AuditLogAnnotation(
        action = "CREATE_INVOICE",
        description = "Creating a new invoice",
        entityType = "Invoice"
    )
    public ResponseEntity<ApiResponse<?>> createInvoice(CreateInvoiceDTO createDTO) {
        log.info("Creating new invoice");

        try {
            // Safari ID is REQUIRED
            if (createDTO.getSafariId() == null || createDTO.getSafariId().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari ID is required", "SAFARI_ID_REQUIRED")
                );
            }

            // Validate and decode safari ID
            Safari safari;
            try {
                Long safariId = idObfuscator.decodeId(createDTO.getSafariId());
                safari = safariRepository.findById(safariId).orElse(null);
                if (safari == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Safari not found", "SAFARI_NOT_FOUND")
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to decode safari ID: {}", createDTO.getSafariId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            /*
             * One LIVE invoice per safari, unless this one says it is a
             * supplement.
             *
             * The rule exists to stop the same trip being invoiced twice by two
             * people. It must not stop the legitimate second demand: a trip that
             * changes after it has been part-paid leaves the customer owing the
             * difference, and the original invoice cannot be amended to say so —
             * they hold a copy of it and have paid against it.
             *
             * So the caller declares the intent. A supplement must also say what
             * changed: an unexplained second bill is the kind a customer disputes.
             */
            boolean isSupplement = Boolean.TRUE.equals(createDTO.getIsSupplement());
            if (invoiceRepository.existsBySafariIdAndStatusNot(safari.getId(), InvoiceStatus.CANCELLED)
                    && !isSupplement) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "An active invoice already exists for this safari. Raise a supplement if the "
                            + "trip has changed and the customer owes more, or cancel the original first.",
                        "INVOICE_ALREADY_EXISTS")
                );
            }
            if (isSupplement
                    && (createDTO.getSupplementReason() == null
                        || createDTO.getSupplementReason().isBlank())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "A supplementary invoice must say what changed. It is the answer to the "
                            + "customer's first question.",
                        "SUPPLEMENT_REASON_REQUIRED")
                );
            }

            // Derive customer from safari (REQUIRED)
            if (safari.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot create invoice: Safari has no customer linked. Please link a customer to the safari first.",
                        "SAFARI_NO_CUSTOMER")
                );
            }

            Customer customer = safari.getCustomer();
            log.info("Customer derived from safari: {}", customer.getDisplayName());

            // Validate due date is after issue date
            if (!createDTO.getIssueDate().isBefore(createDTO.getDueDate())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Due date must be after issue date", "INVALID_DATE_RANGE")
                );
            }

            // Get current user for audit tracking
            User currentUser = getCurrentUser();

            // Build invoice entity (invoiceCode will be set after save)
            Invoice invoice = Invoice.builder()
                .invoiceCode("TEMP") // Temporary code, will be updated after save
                .title(createDTO.getTitle())
                .description(isSupplement
                    ? "Supplement — " + createDTO.getSupplementReason()
                        + (createDTO.getDescription() != null && !createDTO.getDescription().isBlank()
                            ? ". " + createDTO.getDescription() : "")
                    : createDTO.getDescription())
                .customer(customer)
                .safari(safari)
                .lineItems(new ArrayList<>())
                .subtotals(new ArrayList<>())
                .taxes(new ArrayList<>())
                .discounts(new ArrayList<>())
                .grandTotals(new ArrayList<>())
                .taxPercentage(createDTO.getTaxPercentage())
                .discountPercentage(createDTO.getDiscountPercentage())
                .discountReason(createDTO.getDiscountReason())
                .agentCommissionPercentage(createDTO.getAgentCommissionPercentage())
                .agentCommissionReason(createDTO.getAgentCommissionReason())
                .marginUpliftPercentage(createDTO.getMarginUpliftPercentage())
                .marginUpliftReason(createDTO.getMarginUpliftReason())
                .issueDate(createDTO.getIssueDate())
                .dueDate(createDTO.getDueDate())
                .status(InvoiceStatus.DRAFT)
                .createdBy(currentUser)
                .internalNotes(createDTO.getInternalNotes())
                .customerNotes(createDTO.getCustomerNotes())
                .paymentTerms(createDTO.getPaymentTerms())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save invoice to get ID
            invoice = invoiceRepository.save(invoice);

            // Generate invoice code based on ID
            String invoiceCode = invoice.generateCode();
            invoice.setInvoiceCode(invoiceCode);

            // Save again with the generated code
            invoice = invoiceRepository.save(invoice);

            // Calculate and update totals (will be zero for new invoice with no line items)
            totalsCalculationService.recalculateTotals(invoice.getId());

            // Reload invoice to get updated totals
            invoice = invoiceRepository.findById(invoice.getId()).orElse(invoice);

            log.info("Invoice created successfully with code: {}", invoiceCode);

            // Convert to DTO
            InvoiceDTO invoiceDTO = convertToDTO(invoice);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Invoice created successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error creating invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create invoice", "INVOICE_CREATE_FAILED")
            );
        }
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
            .agentCommissionPercentage(invoice.getAgentCommissionPercentage())
            .agentCommissionReason(invoice.getAgentCommissionReason())
            .marginUpliftPercentage(invoice.getMarginUpliftPercentage())
            .marginUpliftReason(invoice.getMarginUpliftReason())
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
