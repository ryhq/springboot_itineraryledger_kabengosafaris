package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.UpdateQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating quotes
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteUpdateService {

    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final QuoteTotalsCalculationService totalsCalculationService;

    @AuditLogAnnotation(
        action = "UPDATE_QUOTE",
        description = "Updating a quote",
        entityType = "Quote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateQuote(String idObfuscated, UpdateQuoteDTO updateDTO) {
        log.info("Updating quote with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode quote ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            // Find quote
            Quote quote = quoteRepository.findById(id).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // WORKFLOW ENFORCEMENT: Check status-based edit restrictions
            QuoteStatus status = quote.getStatus();

            // ACCEPTED, REJECTED, EXPIRED, CANCELLED, CONVERTED quotes cannot be edited at all
            if (status == QuoteStatus.ACCEPTED || status == QuoteStatus.REJECTED ||
                status == QuoteStatus.EXPIRED || status == QuoteStatus.CANCELLED ||
                status == QuoteStatus.CONVERTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        String.format("Cannot edit %s quote. Create a new version instead.", status.getDisplayName()),
                        "EDIT_BLOCKED"
                    )
                );
            }

            // SENT quotes can only edit display fields
            if (status == QuoteStatus.SENT) {
                List<String> blockedFields = new ArrayList<>();

                // Check if any non-display fields are being changed
                if (updateDTO.getTitle() != null && !updateDTO.getTitle().equals(quote.getTitle())) {
                    blockedFields.add("title");
                }
                if (updateDTO.getIsStoRate() != null && !updateDTO.getIsStoRate().equals(quote.getIsStoRate())) {
                    blockedFields.add("isStoRate");
                }
                if (updateDTO.getTaxPercentage() != null && !updateDTO.getTaxPercentage().equals(quote.getTaxPercentage())) {
                    blockedFields.add("taxPercentage");
                }
                if (updateDTO.getDiscountPercentage() != null && !updateDTO.getDiscountPercentage().equals(quote.getDiscountPercentage())) {
                    blockedFields.add("discountPercentage");
                }
                if (updateDTO.getValidFrom() != null && !updateDTO.getValidFrom().equals(quote.getValidFrom())) {
                    blockedFields.add("validFrom");
                }
                if (updateDTO.getValidTo() != null && !updateDTO.getValidTo().equals(quote.getValidTo())) {
                    blockedFields.add("validTo");
                }
                if (updateDTO.getDepositPercentage() != null && !updateDTO.getDepositPercentage().equals(quote.getDepositPercentage())) {
                    blockedFields.add("depositPercentage");
                }

                if (!blockedFields.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            String.format("Cannot modify fields (%s) on SENT quote. Only description and customerNotes can be edited. Revert to DRAFT for other changes.",
                                String.join(", ", blockedFields)),
                            "SENT_EDIT_BLOCKED"
                        )
                    );
                }

                log.info("Allowing display-only field updates to SENT quote: {}", quote.getQuoteCode());
            }

            // READY quotes can only edit non-critical fields
            if (status == QuoteStatus.READY) {
                List<String> blockedFields = new ArrayList<>();

                // Check if any critical fields are being changed
                if (updateDTO.getIsStoRate() != null && !updateDTO.getIsStoRate().equals(quote.getIsStoRate())) {
                    blockedFields.add("isStoRate");
                }
                if (updateDTO.getTaxPercentage() != null && !updateDTO.getTaxPercentage().equals(quote.getTaxPercentage())) {
                    blockedFields.add("taxPercentage");
                }
                if (updateDTO.getDiscountPercentage() != null && !updateDTO.getDiscountPercentage().equals(quote.getDiscountPercentage())) {
                    blockedFields.add("discountPercentage");
                }

                if (!blockedFields.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            String.format("Cannot modify critical fields (%s) on READY quote. Revert to DRAFT first.",
                                String.join(", ", blockedFields)),
                            "READY_CRITICAL_EDIT_BLOCKED"
                        )
                    );
                }

                log.info("Allowing non-critical field updates to READY quote: {}", quote.getQuoteCode());
            }

            // Block direct status updates (must use workflow endpoints)
            if (updateDTO.getStatus() != null && updateDTO.getStatus() != status) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot change quote status directly. Use the workflow endpoints (e.g., /mark-ready, /send, /accept).",
                        "DIRECT_STATUS_CHANGE_BLOCKED"
                    )
                );
            }

            // Update title
            if (updateDTO.getTitle() != null) {
                quote.setTitle(updateDTO.getTitle());
            }

            // Update description
            if (updateDTO.getDescription() != null) {
                quote.setDescription(updateDTO.getDescription());
            }

            // Update pricing fields
            if (updateDTO.getIsStoRate() != null) {
                quote.setIsStoRate(updateDTO.getIsStoRate());
            }
            if (updateDTO.getTaxPercentage() != null) {
                quote.setTaxPercentage(updateDTO.getTaxPercentage());
            }
            if (updateDTO.getDiscountPercentage() != null) {
                quote.setDiscountPercentage(updateDTO.getDiscountPercentage());
            }
            if (updateDTO.getDiscountReason() != null) {
                quote.setDiscountReason(updateDTO.getDiscountReason());
            }

            // Note: Status updates are blocked - must use workflow endpoints
            // (Already validated above)

            // Update dates (validFrom, validTo, isValid only - sentDate is set by system)
            if (updateDTO.getValidFrom() != null) {
                quote.setValidFrom(updateDTO.getValidFrom());
            }
            if (updateDTO.getValidTo() != null) {
                quote.setValidTo(updateDTO.getValidTo());
            }
            if (updateDTO.getIsValid() != null) {
                quote.setIsValid(updateDTO.getIsValid());
            }

            // Update payment terms
            if (updateDTO.getDepositPercentage() != null) {
                quote.setDepositPercentage(updateDTO.getDepositPercentage());
            }
            if (updateDTO.getDepositDueDate() != null) {
                quote.setDepositDueDate(updateDTO.getDepositDueDate());
            }
            if (updateDTO.getFullPaymentDueDate() != null) {
                quote.setFullPaymentDueDate(updateDTO.getFullPaymentDueDate());
            }

            // Update approval metadata (approvalNotes only - approvedAt is set by system)
            if (updateDTO.getApprovalNotes() != null) {
                quote.setApprovalNotes(updateDTO.getApprovalNotes());
            }

            // Update version fields (versionNotes only - version is managed by system)
            if (updateDTO.getVersionNotes() != null) {
                quote.setVersionNotes(updateDTO.getVersionNotes());
            }

            // Update notes
            if (updateDTO.getInternalNotes() != null) {
                quote.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getCustomerNotes() != null) {
                quote.setCustomerNotes(updateDTO.getCustomerNotes());
            }

            // Update isActive
            if (updateDTO.getIsActive() != null) {
                quote.setIsActive(updateDTO.getIsActive());
            }

            // Set updated by current user
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                quote.setUpdatedBy(currentUser);
            }

            // Save updated quote
            quote = quoteRepository.save(quote);

            // Recalculate totals (especially if tax/discount percentages changed)
            totalsCalculationService.recalculateTotals(quote.getId());

            // Reload quote to get updated totals
            quote = quoteRepository.findById(quote.getId()).orElse(quote);

            // Convert to DTO
            QuoteDTO quoteDTO = convertToDTO(quote);

            log.info("Quote updated successfully: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote updated successfully", quoteDTO)
            );

        } catch (Exception e) {
            log.error("Error updating quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update quote", "QUOTE_UPDATE_FAILED")
            );
        }
    }

    /**
     * Convert Quote entity to QuoteDTO
     */
    public QuoteDTO convertToDTO(Quote quote) {
        QuoteDTO dto = QuoteDTO.builder()
            .id(idObfuscator.encodeId(quote.getId()))
            .quoteCode(quote.getQuoteCode())
            .title(quote.getTitle())
            .description(quote.getDescription())
            .itineraryId(idObfuscator.encodeId(quote.getItinerary().getId()))
            .itineraryCode(quote.getItinerary().getCode())
            .itineraryName(quote.getItinerary().getName())
            .customerId(idObfuscator.encodeId(quote.getCustomer().getId()))
            .customerName(quote.getCustomer().getDisplayName())
            .customerEmail(quote.getCustomer().getPrimaryEmail())
            .subtotals(quote.getSubtotals())
            .taxes(quote.getTaxes())
            .discounts(quote.getDiscounts())
            .grandTotals(quote.getGrandTotals())
            .isStoRate(quote.getIsStoRate())
            .taxPercentage(quote.getTaxPercentage())
            .discountPercentage(quote.getDiscountPercentage())
            .discountReason(quote.getDiscountReason())
            .version(quote.getVersion())
            .status(quote.getStatus())
            .sentDate(quote.getSentDate())
            .validFrom(quote.getValidFrom())
            .validTo(quote.getValidTo())
            .isValid(quote.getIsValid())
            .depositPercentage(quote.getDepositPercentage())
            .depositDueDate(quote.getDepositDueDate())
            .fullPaymentDueDate(quote.getFullPaymentDueDate())
            .internalNotes(quote.getInternalNotes())
            .customerNotes(quote.getCustomerNotes())
            .versionNotes(quote.getVersionNotes())
            .isActive(quote.getIsActive())
            .itemCount(quote.getItems() != null ? (long) quote.getItems().size() : 0L)
            .documentCount(quote.getDocuments() != null ? (long) quote.getDocuments().size() : 0L)
            .createdAt(quote.getCreatedAt())
            .updatedAt(quote.getUpdatedAt())
            .build();

        // Set approver if present
        if (quote.getApprover() != null) {
            dto.setApproverId(idObfuscator.encodeId(quote.getApprover().getId()));
            dto.setApproverName(quote.getApprover().getUsername());
        }

        // Set approved by if present
        if (quote.getApprovedBy() != null) {
            dto.setApprovedById(idObfuscator.encodeId(quote.getApprovedBy().getId()));
            dto.setApprovedByName(quote.getApprovedBy().getUsername());
            dto.setApprovedAt(quote.getApprovedAt());
            dto.setApprovalNotes(quote.getApprovalNotes());
        }

        // Set previous version if present
        if (quote.getPreviousVersion() != null) {
            dto.setPreviousVersionId(idObfuscator.encodeId(quote.getPreviousVersion().getId()));
            dto.setPreviousVersionCode(quote.getPreviousVersion().getQuoteCode());
        }

        // Set next version if present
        if (quote.getNextVersion() != null) {
            dto.setNextVersionId(idObfuscator.encodeId(quote.getNextVersion().getId()));
            dto.setNextVersionCode(quote.getNextVersion().getQuoteCode());
        }

        // Set created by if present
        if (quote.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(quote.getCreatedBy().getId()));
            dto.setCreatedByName(quote.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (quote.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(quote.getUpdatedBy().getId()));
            dto.setUpdatedByName(quote.getUpdatedBy().getUsername());
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
