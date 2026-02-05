package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdateInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating invoices
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceUpdateService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceTotalsCalculationService totalsCalculationService;

    @AuditLogAnnotation(
        action = "UPDATE_INVOICE",
        description = "Updating an invoice",
        entityType = "Invoice",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateInvoice(String idObfuscated, UpdateInvoiceDTO updateDTO) {
        log.info("Updating invoice with ID: {}", idObfuscated);

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

            // Update title
            if (updateDTO.getTitle() != null) {
                invoice.setTitle(updateDTO.getTitle());
            }

            // Update description
            if (updateDTO.getDescription() != null) {
                invoice.setDescription(updateDTO.getDescription());
            }

            // Update pricing fields
            if (updateDTO.getTaxPercentage() != null) {
                invoice.setTaxPercentage(updateDTO.getTaxPercentage());
            }
            if (updateDTO.getDiscountPercentage() != null) {
                invoice.setDiscountPercentage(updateDTO.getDiscountPercentage());
            }
            if (updateDTO.getDiscountReason() != null) {
                invoice.setDiscountReason(updateDTO.getDiscountReason());
            }

            // Update status
            if (updateDTO.getStatus() != null) {
                invoice.setStatus(updateDTO.getStatus());
            }

            // Update payment status
            if (updateDTO.getPaymentStatus() != null) {
                invoice.setPaymentStatus(updateDTO.getPaymentStatus());
            }

            // Update dates
            if (updateDTO.getIssueDate() != null) {
                invoice.setIssueDate(updateDTO.getIssueDate());
            }
            if (updateDTO.getDueDate() != null) {
                invoice.setDueDate(updateDTO.getDueDate());
            }
            if (updateDTO.getSentDate() != null) {
                invoice.setSentDate(updateDTO.getSentDate());
            }
            if (updateDTO.getPaidDate() != null) {
                invoice.setPaidDate(updateDTO.getPaidDate());
            }

            // Update notes
            if (updateDTO.getInternalNotes() != null) {
                invoice.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getCustomerNotes() != null) {
                invoice.setCustomerNotes(updateDTO.getCustomerNotes());
            }
            if (updateDTO.getPaymentTerms() != null) {
                invoice.setPaymentTerms(updateDTO.getPaymentTerms());
            }

            // Update isActive
            if (updateDTO.getIsActive() != null) {
                invoice.setIsActive(updateDTO.getIsActive());
            }

            // Set updated by current user
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                invoice.setUpdatedBy(currentUser);
            }

            // Save updated invoice
            invoice = invoiceRepository.save(invoice);

            // Recalculate totals (especially if tax/discount percentages changed)
            totalsCalculationService.recalculateTotals(invoice.getId());

            // Reload invoice to get updated totals
            invoice = invoiceRepository.findById(invoice.getId()).orElse(invoice);

            // Convert to DTO
            InvoiceDTO invoiceDTO = convertToDTO(invoice);

            log.info("Invoice updated successfully: {}", invoice.getInvoiceCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice updated successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error updating invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update invoice", "INVOICE_UPDATE_FAILED")
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
            .amountsPaid(invoice.getAmountsPaid())
            .balances(invoice.getBalances())
            .taxPercentage(invoice.getTaxPercentage())
            .discountPercentage(invoice.getDiscountPercentage())
            .discountReason(invoice.getDiscountReason())
            .issueDate(invoice.getIssueDate())
            .dueDate(invoice.getDueDate())
            .sentDate(invoice.getSentDate())
            .paidDate(invoice.getPaidDate())
            .status(invoice.getStatus())
            .statusDisplayName(invoice.getStatus().getDisplayName())
            .paymentStatus(invoice.getPaymentStatus())
            .paymentStatusDisplayName(invoice.getPaymentStatus().getDisplayName())
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
