package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdateInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating invoices (metadata only)
 *
 * NOTE: Customer and Safari relationships cannot be updated after creation.
 * These relationships are established at creation time and remain fixed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceUpdateService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceTotalsCalculationService totalsCalculationService;
    private final InvoicePaymentAggregationService paymentAggregationService;

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

            InvoiceStatus currentStatus = invoice.getStatus();

            // ========================
            // WORKFLOW ENFORCEMENT
            // ========================

            // 1. Block direct status changes - must use workflow endpoints
            if (updateDTO.getStatus() != null && updateDTO.getStatus() != currentStatus) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot change invoice status directly. Use the workflow endpoints at /api/invoices/{id}/state/* " +
                        "(e.g., /send, /record-payment, /cancel).",
                        "DIRECT_STATUS_CHANGE_BLOCKED")
                );
            }

            // 2. Block ALL edits to payment and final state invoices
            if (currentStatus == InvoiceStatus.PARTIALLY_PAID ||
                currentStatus == InvoiceStatus.PAID ||
                currentStatus == InvoiceStatus.OVERDUE ||
                currentStatus == InvoiceStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot edit %s invoice. Payment and final state invoices are read-only.",
                            currentStatus.getDisplayName()),
                        "EDIT_BLOCKED")
                );
            }

            // 3. SENT invoices can only edit non-critical fields
            if (currentStatus == InvoiceStatus.SENT) {
                List<String> blockedFields = new ArrayList<>();

                // Check if critical fields are being changed
                if (updateDTO.getTitle() != null && !updateDTO.getTitle().equals(invoice.getTitle())) {
                    blockedFields.add("title");
                }
                if (updateDTO.getTaxPercentage() != null && !updateDTO.getTaxPercentage().equals(invoice.getTaxPercentage())) {
                    blockedFields.add("taxPercentage");
                }
                if (updateDTO.getDiscountPercentage() != null && !updateDTO.getDiscountPercentage().equals(invoice.getDiscountPercentage())) {
                    blockedFields.add("discountPercentage");
                }
                if (updateDTO.getAgentCommissionPercentage() != null && !updateDTO.getAgentCommissionPercentage().equals(invoice.getAgentCommissionPercentage())) {
                    blockedFields.add("agentCommissionPercentage");
                }
                if (updateDTO.getMarginUpliftPercentage() != null && !updateDTO.getMarginUpliftPercentage().equals(invoice.getMarginUpliftPercentage())) {
                    blockedFields.add("marginUpliftPercentage");
                }
                if (updateDTO.getIssueDate() != null && !updateDTO.getIssueDate().equals(invoice.getIssueDate())) {
                    blockedFields.add("issueDate");
                }
                if (updateDTO.getDueDate() != null && !updateDTO.getDueDate().equals(invoice.getDueDate())) {
                    blockedFields.add("dueDate");
                }
                if (updateDTO.getSentDate() != null && !updateDTO.getSentDate().equals(invoice.getSentDate())) {
                    blockedFields.add("sentDate");
                }
                if (updateDTO.getPaidDate() != null && !updateDTO.getPaidDate().equals(invoice.getPaidDate())) {
                    blockedFields.add("paidDate");
                }

                if (!blockedFields.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            String.format("Cannot modify critical fields (%s) on %s invoice. " +
                                         "Only description, customerNotes, internalNotes, paymentTerms, and isActive can be edited.",
                                String.join(", ", blockedFields),
                                currentStatus.getDisplayName()),
                            "SENT_EDIT_BLOCKED")
                    );
                }
            }

            // ========================
            // UPDATE ALLOWED FIELDS
            // ========================

            // DRAFT: Can update all fields
            // SENT/VIEWED: Can only update non-critical fields (enforced above)

            // Capture pre-update markup multiplier so we can re-scale line
            // item unit prices by the *ratio* of new/old when commission or
            // uplift changes. Without this, switching from 10% to 20% would
            // double-count the original 10% already baked in.
            BigDecimal oldMultiplier = computeMarkupMultiplier(
                    invoice.getAgentCommissionPercentage(), invoice.getMarginUpliftPercentage());
            boolean markupChanged = false;

            if (currentStatus == InvoiceStatus.DRAFT) {
                // Update title (DRAFT only)
                if (updateDTO.getTitle() != null) {
                    invoice.setTitle(updateDTO.getTitle());
                }

                // Update pricing fields (DRAFT only)
                if (updateDTO.getTaxPercentage() != null) {
                    invoice.setTaxPercentage(updateDTO.getTaxPercentage());
                }
                if (updateDTO.getDiscountPercentage() != null) {
                    invoice.setDiscountPercentage(updateDTO.getDiscountPercentage());
                }

                // Markup fields (DRAFT only) — bake into existing line item
                // unit prices when commission or uplift changes.
                if (updateDTO.getAgentCommissionPercentage() != null
                        && !updateDTO.getAgentCommissionPercentage().equals(invoice.getAgentCommissionPercentage())) {
                    invoice.setAgentCommissionPercentage(updateDTO.getAgentCommissionPercentage());
                    markupChanged = true;
                }
                if (updateDTO.getAgentCommissionReason() != null) {
                    invoice.setAgentCommissionReason(updateDTO.getAgentCommissionReason());
                }
                if (updateDTO.getMarginUpliftPercentage() != null
                        && !updateDTO.getMarginUpliftPercentage().equals(invoice.getMarginUpliftPercentage())) {
                    invoice.setMarginUpliftPercentage(updateDTO.getMarginUpliftPercentage());
                    markupChanged = true;
                }
                if (updateDTO.getMarginUpliftReason() != null) {
                    invoice.setMarginUpliftReason(updateDTO.getMarginUpliftReason());
                }

                // Update dates (DRAFT only)
                if (updateDTO.getIssueDate() != null) {
                    invoice.setIssueDate(updateDTO.getIssueDate());
                }
                if (updateDTO.getDueDate() != null) {
                    invoice.setDueDate(updateDTO.getDueDate());
                }
            }

            // Update description (allowed in DRAFT, SENT, VIEWED)
            if (updateDTO.getDescription() != null) {
                invoice.setDescription(updateDTO.getDescription());
            }

            // Update discount reason (allowed in DRAFT)
            if (updateDTO.getDiscountReason() != null && currentStatus == InvoiceStatus.DRAFT) {
                invoice.setDiscountReason(updateDTO.getDiscountReason());
            }

            // Update notes (allowed in DRAFT, SENT, VIEWED)
            if (updateDTO.getInternalNotes() != null) {
                invoice.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getCustomerNotes() != null) {
                invoice.setCustomerNotes(updateDTO.getCustomerNotes());
            }
            if (updateDTO.getPaymentTerms() != null) {
                invoice.setPaymentTerms(updateDTO.getPaymentTerms());
            }

            // Update isActive (allowed in DRAFT, SENT, VIEWED)
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

            // If markup changed, re-scale every line item's per-currency unit
            // price by (newMultiplier / oldMultiplier). This keeps prices in
            // sync with the new percentages without re-deriving from the
            // safari (invoices are not derived continuously like quotes).
            if (markupChanged) {
                BigDecimal newMultiplier = computeMarkupMultiplier(
                        invoice.getAgentCommissionPercentage(), invoice.getMarginUpliftPercentage());
                if (oldMultiplier.signum() != 0 && newMultiplier.compareTo(oldMultiplier) != 0) {
                    BigDecimal ratio = newMultiplier.divide(oldMultiplier, 8, RoundingMode.HALF_UP);
                    List<InvoiceLineItem> lineItems =
                            invoiceLineItemRepository.findByInvoiceIdOrderByDisplayOrderAsc(invoice.getId());
                    for (InvoiceLineItem item : lineItems) {
                        if (item.getPrices() == null) continue;
                        for (Price p : item.getPrices()) {
                            if (p.getUnitPrice() == null) continue;
                            BigDecimal scaled = p.getUnitPrice().multiply(ratio)
                                    .setScale(2, RoundingMode.HALF_UP);
                            p.setUnitPrice(scaled);
                            if (p.getQuantity() != null) {
                                p.setTotalPrice(scaled.multiply(BigDecimal.valueOf(p.getQuantity())));
                            }
                        }
                        invoiceLineItemRepository.save(item);
                    }
                }
            }

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
     * (1 + (commission% + uplift%) / 100). Returns {@link BigDecimal#ONE} when
     * both are null/zero so callers can divide safely.
     */
    private BigDecimal computeMarkupMultiplier(BigDecimal commissionPct, BigDecimal upliftPct) {
        BigDecimal c = commissionPct != null ? commissionPct : BigDecimal.ZERO;
        BigDecimal u = upliftPct != null ? upliftPct : BigDecimal.ZERO;
        BigDecimal total = c.add(u);
        if (total.signum() == 0) return BigDecimal.ONE;
        return BigDecimal.ONE.add(total.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
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
