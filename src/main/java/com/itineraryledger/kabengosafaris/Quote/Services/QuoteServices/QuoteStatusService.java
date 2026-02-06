package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QuoteStatusService - Service for managing quote status workflow transitions
 *
 * Implements the complete quote lifecycle:
 * DRAFT → READY → SENT → [ACCEPTED/REJECTED/EXPIRED] → [CONVERTED/CANCELLED]
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteStatusService {

    private final QuoteRepository quoteRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Mark quote as READY (ready to send to customer)
     * Only allowed from DRAFT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "READY_QUOTE", description = "Marking quote as ready", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> markAsReady(String idObfuscated) {
        log.info("Marking quote as ready: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate current status
            if (quote.getStatus() != QuoteStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only DRAFT quotes can be marked as READY", "INVALID_STATUS_TRANSITION")
                );
            }

            // Validate completeness
            if (!canMarkAsReady(quote)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Quote does not meet requirements. Must have itinerary, customer, and line items.",
                        "INCOMPLETE_QUOTE"
                    )
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.READY);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as ready: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as ready", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error marking quote as ready", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to mark quote as ready", "READY_FAILED")
            );
        }
    }

    /**
     * Send quote to customer
     * Allowed from DRAFT (if ready), READY, SENT (resend), ACCEPTED (resend), CONVERTED (resend)
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "SEND_QUOTE", description = "Sending quote to customer", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> sendQuote(String idObfuscated) {
        log.info("Sending quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate status - cannot send REJECTED, EXPIRED, or CANCELLED quotes
            if (quote.getStatus() == QuoteStatus.REJECTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send REJECTED quote", "INVALID_STATUS_TRANSITION")
                );
            }
            if (quote.getStatus() == QuoteStatus.EXPIRED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send EXPIRED quote", "INVALID_STATUS_TRANSITION")
                );
            }
            if (quote.getStatus() == QuoteStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send CANCELLED quote", "INVALID_STATUS_TRANSITION")
                );
            }

            // Validate completeness if not already sent
            if (quote.getStatus() == QuoteStatus.DRAFT && !canMarkAsReady(quote)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Quote does not meet requirements to send. Must have itinerary, customer, and line items.",
                        "INCOMPLETE_QUOTE"
                    )
                );
            }

            // Update status and sent date
            quote.setStatus(QuoteStatus.SENT);
            if (quote.getSentDate() == null) {
                quote.setSentDate(LocalDate.now());
            }
            quote = quoteRepository.save(quote);

            log.info("Quote sent to customer: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote sent to customer successfully", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error sending quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to send quote", "SEND_FAILED")
            );
        }
    }

    /**
     * Mark quote as ACCEPTED by customer
     * Only allowed from SENT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "ACCEPT_QUOTE", description = "Marking quote as accepted", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> acceptQuote(String idObfuscated) {
        log.info("Accepting quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate current status
            if (quote.getStatus() != QuoteStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only SENT quotes can be marked as ACCEPTED", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.ACCEPTED);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as accepted: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as accepted", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error accepting quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to accept quote", "ACCEPT_FAILED")
            );
        }
    }

    /**
     * Mark quote as REJECTED by customer
     * Only allowed from SENT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "REJECT_QUOTE", description = "Marking quote as rejected", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> rejectQuote(String idObfuscated) {
        log.info("Rejecting quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate current status
            if (quote.getStatus() != QuoteStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only SENT quotes can be marked as REJECTED", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.REJECTED);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as rejected: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as rejected", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error rejecting quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reject quote", "REJECT_FAILED")
            );
        }
    }

    /**
     * Mark quote as EXPIRED
     * Allowed from SENT or READY status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "EXPIRE_QUOTE", description = "Marking quote as expired", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> expireQuote(String idObfuscated) {
        log.info("Expiring quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate current status - can only expire SENT or READY quotes
            if (quote.getStatus() != QuoteStatus.SENT && quote.getStatus() != QuoteStatus.READY) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only SENT or READY quotes can be marked as EXPIRED", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.EXPIRED);
            quote = quoteRepository.save(quote);

            log.info("Quote marked as expired: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote marked as expired", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error expiring quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to expire quote", "EXPIRE_FAILED")
            );
        }
    }

    /**
     * Cancel a quote
     * Allowed from any status except CONVERTED
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "CANCEL_QUOTE", description = "Cancelling quote", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> cancelQuote(String idObfuscated) {
        log.info("Cancelling quote: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Cannot cancel CONVERTED quotes
            if (quote.getStatus() == QuoteStatus.CONVERTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot cancel CONVERTED quote", "INVALID_STATUS_TRANSITION")
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.CANCELLED);
            quote = quoteRepository.save(quote);

            log.info("Quote cancelled: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote cancelled successfully", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error cancelling quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to cancel quote", "CANCEL_FAILED")
            );
        }
    }

    /**
     * Revert quote to DRAFT status
     * Allowed from READY or SENT status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "REVERT_QUOTE_TO_DRAFT", description = "Reverting quote to draft", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> revertToDraft(String idObfuscated) {
        log.info("Reverting quote to draft: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate current status
            if (quote.getStatus() == QuoteStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quote is already in DRAFT status", "ALREADY_DRAFT")
                );
            }

            // Only READY and SENT can be reverted to DRAFT
            if (quote.getStatus() != QuoteStatus.READY && quote.getStatus() != QuoteStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot revert " + quote.getStatus().getDisplayName() + " quote to DRAFT. Create a new version instead.",
                        "INVALID_STATUS_TRANSITION"
                    )
                );
            }

            // Update status to DRAFT
            quote.setStatus(QuoteStatus.DRAFT);
            quote = quoteRepository.save(quote);

            log.info("Quote reverted to draft: {}", quote.getQuoteCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote reverted to draft successfully", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error reverting quote to draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to revert quote to draft", "REVERT_TO_DRAFT_FAILED")
            );
        }
    }

    /**
     * Convert quote to booking/safari
     * Only allowed from ACCEPTED status
     *
     * @param idObfuscated The obfuscated quote ID
     * @return ResponseEntity with ApiResponse containing the updated quote
     */
    @AuditLogAnnotation(action = "CONVERT_QUOTE", description = "Converting quote to booking", entityType = "Quote", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> convertQuote(String idObfuscated) {
        log.info("Converting quote to booking: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Quote quote = quoteRepository.findById(id).orElse(null);

            if (quote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            // Validate current status
            if (quote.getStatus() != QuoteStatus.ACCEPTED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only ACCEPTED quotes can be converted to bookings", "INVALID_STATUS_TRANSITION")
                );
            }

            // Validate itinerary is published
            if (quote.getItinerary() != null && quote.getItinerary().getStatus() != ItineraryStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot convert quote - associated itinerary is not PUBLISHED",
                        "ITINERARY_NOT_PUBLISHED"
                    )
                );
            }

            // Update status
            quote.setStatus(QuoteStatus.CONVERTED);
            quote = quoteRepository.save(quote);

            log.info("Quote converted to booking: {}", quote.getQuoteCode());

            // TODO: Create Safari/Booking entity here in future enhancement

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote converted to booking successfully", convertToDTO(quote))
            );

        } catch (Exception e) {
            log.error("Error converting quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to convert quote", "CONVERT_FAILED")
            );
        }
    }

    /**
     * Check if quote meets requirements to be marked as READY
     */
    private boolean canMarkAsReady(Quote quote) {
        return quote.getItinerary() != null
            && quote.getCustomer() != null
            && quote.getItems() != null
            && !quote.getItems().isEmpty()
            && quote.getGrandTotals() != null
            && !quote.getGrandTotals().isEmpty();
    }

    /**
     * Convert Quote entity to QuoteDTO
     */
    private QuoteDTO convertToDTO(Quote quote) {
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
}
