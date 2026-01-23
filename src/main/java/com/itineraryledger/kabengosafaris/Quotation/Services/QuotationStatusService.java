package com.itineraryledger.kabengosafaris.Quotation.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quotation.DTOs.QuotationDTO;
import com.itineraryledger.kabengosafaris.Quotation.Entity.Quotation;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import com.itineraryledger.kabengosafaris.Quotation.Repository.QuotationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * QuotationStatusService - Service for managing quotation status transitions
 */
@Service
@Slf4j
@Transactional
public class QuotationStatusService {

    private final QuotationRepository quotationRepository;
    private final QuotationGetService quotationGetService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuotationStatusService(
        QuotationRepository quotationRepository,
        QuotationGetService quotationGetService,
        IdObfuscator idObfuscator
    ) {
        this.quotationRepository = quotationRepository;
        this.quotationGetService = quotationGetService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Send quotation to customer
     */
    @AuditLogAnnotation(
        action = "SEND_QUOTATION",
        description = "Sending quotation to customer",
        entityType = "Quotation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> sendQuotation(String idObfuscated) {
        log.info("Sending quotation: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quotation ID", "INVALID_QUOTATION_ID")
                );
            }

            Quotation quotation = quotationRepository.findById(id).orElse(null);
            if (quotation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            if (!quotation.canSend()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quotation cannot be sent in current status or is expired", "QUOTATION_CANNOT_BE_SENT")
                );
            }

            quotation.setStatus(QuotationStatus.SENT);
            quotation.setSentAt(LocalDateTime.now());
            quotation = quotationRepository.save(quotation);

            log.info("Quotation sent successfully: {}", quotation.getCode());

            // TODO: Trigger email notification to customer

            QuotationDTO dto = quotationGetService.convertToDTO(quotation);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation sent successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error sending quotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to send quotation", "QUOTATION_SEND_FAILED")
            );
        }
    }

    /**
     * Mark quotation as viewed by customer
     */
    @AuditLogAnnotation(
        action = "VIEW_QUOTATION",
        description = "Marking quotation as viewed",
        entityType = "Quotation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> markAsViewed(String idObfuscated) {
        log.info("Marking quotation as viewed: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quotation ID", "INVALID_QUOTATION_ID")
                );
            }

            Quotation quotation = quotationRepository.findById(id).orElse(null);
            if (quotation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            if (quotation.getStatus() != QuotationStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only sent quotations can be marked as viewed", "INVALID_STATUS_TRANSITION")
                );
            }

            quotation.setStatus(QuotationStatus.VIEWED);
            quotation.setViewedAt(LocalDateTime.now());
            quotation = quotationRepository.save(quotation);

            log.info("Quotation marked as viewed: {}", quotation.getCode());

            QuotationDTO dto = quotationGetService.convertToDTO(quotation);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation marked as viewed", dto)
            );

        } catch (Exception e) {
            log.error("Error marking quotation as viewed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to mark quotation as viewed", "QUOTATION_VIEW_FAILED")
            );
        }
    }

    /**
     * Accept quotation
     */
    @AuditLogAnnotation(
        action = "ACCEPT_QUOTATION",
        description = "Accepting quotation",
        entityType = "Quotation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> acceptQuotation(String idObfuscated) {
        log.info("Accepting quotation: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quotation ID", "INVALID_QUOTATION_ID")
                );
            }

            Quotation quotation = quotationRepository.findById(id).orElse(null);
            if (quotation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            if (!quotation.canAccept()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quotation cannot be accepted (invalid status or expired)", "QUOTATION_CANNOT_BE_ACCEPTED")
                );
            }

            quotation.setStatus(QuotationStatus.ACCEPTED);
            quotation.setAcceptedAt(LocalDateTime.now());
            quotation.setRespondedAt(LocalDateTime.now());
            quotation = quotationRepository.save(quotation);

            log.info("Quotation accepted: {}", quotation.getCode());

            QuotationDTO dto = quotationGetService.convertToDTO(quotation);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation accepted successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error accepting quotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to accept quotation", "QUOTATION_ACCEPT_FAILED")
            );
        }
    }

    /**
     * Reject quotation
     */
    @AuditLogAnnotation(
        action = "REJECT_QUOTATION",
        description = "Rejecting quotation",
        entityType = "Quotation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> rejectQuotation(String idObfuscated, String rejectionReason) {
        log.info("Rejecting quotation: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quotation ID", "INVALID_QUOTATION_ID")
                );
            }

            Quotation quotation = quotationRepository.findById(id).orElse(null);
            if (quotation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            if (quotation.getStatus() != QuotationStatus.SENT && quotation.getStatus() != QuotationStatus.VIEWED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only sent or viewed quotations can be rejected", "INVALID_STATUS_TRANSITION")
                );
            }

            quotation.setStatus(QuotationStatus.REJECTED);
            quotation.setRejectedAt(LocalDateTime.now());
            quotation.setRespondedAt(LocalDateTime.now());
            quotation.setRejectionReason(rejectionReason);
            quotation = quotationRepository.save(quotation);

            log.info("Quotation rejected: {}", quotation.getCode());

            QuotationDTO dto = quotationGetService.convertToDTO(quotation);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation rejected", dto)
            );

        } catch (Exception e) {
            log.error("Error rejecting quotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reject quotation", "QUOTATION_REJECT_FAILED")
            );
        }
    }

    /**
     * Cancel quotation
     */
    @AuditLogAnnotation(
        action = "CANCEL_QUOTATION",
        description = "Cancelling quotation",
        entityType = "Quotation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> cancelQuotation(String idObfuscated) {
        log.info("Cancelling quotation: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quotation ID", "INVALID_QUOTATION_ID")
                );
            }

            Quotation quotation = quotationRepository.findById(id).orElse(null);
            if (quotation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            // Cannot cancel accepted or already cancelled quotations
            if (quotation.getStatus() == QuotationStatus.ACCEPTED ||
                quotation.getStatus() == QuotationStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quotation cannot be cancelled in current status", "QUOTATION_CANNOT_BE_CANCELLED")
                );
            }

            quotation.setStatus(QuotationStatus.CANCELLED);
            quotation = quotationRepository.save(quotation);

            log.info("Quotation cancelled: {}", quotation.getCode());

            QuotationDTO dto = quotationGetService.convertToDTO(quotation);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation cancelled successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error cancelling quotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to cancel quotation", "QUOTATION_CANCEL_FAILED")
            );
        }
    }

    /**
     * Create a revision of an existing quotation
     */
    @AuditLogAnnotation(
        action = "REVISE_QUOTATION",
        description = "Creating quotation revision",
        entityType = "Quotation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> reviseQuotation(String idObfuscated) {
        log.info("Creating revision for quotation: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quotation ID", "INVALID_QUOTATION_ID")
                );
            }

            Quotation original = quotationRepository.findById(id).orElse(null);
            if (original == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            if (!original.canRevise()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Quotation cannot be revised in current status", "QUOTATION_CANNOT_BE_REVISED")
                );
            }

            // Mark original as revised
            original.setStatus(QuotationStatus.REVISED);
            quotationRepository.save(original);

            // Create new revision
            Quotation revision = Quotation.builder()
                .code("TEMP")
                .name(original.getName())
                .customer(original.getCustomer())
                .itinerary(original.getItinerary())
                .parentQuotation(original)
                .status(QuotationStatus.DRAFT)
                .version(original.getVersion() + 1)
                .startDate(original.getStartDate())
                .endDate(original.getEndDate())
                .totalDays(original.getTotalDays())
                .totalNights(original.getTotalNights())
                .totalPax(original.getTotalPax())
                .currency(original.getCurrency())
                .exchangeRate(original.getExchangeRate())
                .subtotal(original.getSubtotal())
                .discountType(original.getDiscountType())
                .discountValue(original.getDiscountValue())
                .discountReason(original.getDiscountReason())
                .taxRate(original.getTaxRate())
                .taxAmount(original.getTaxAmount())
                .totalAmount(original.getTotalAmount())
                .depositRequired(original.getDepositRequired())
                .depositPercentage(original.getDepositPercentage())
                .termsAndConditions(original.getTermsAndConditions())
                .inclusions(original.getInclusions())
                .exclusions(original.getExclusions())
                .internalNotes(original.getInternalNotes())
                .customerNotes(original.getCustomerNotes())
                .createdBy(original.getCreatedBy())
                .assignedTo(original.getAssignedTo())
                .build();

            revision = quotationRepository.save(revision);
            revision.setCode(revision.generateCode());
            revision = quotationRepository.save(revision);

            // TODO: Copy pax and line items from original

            log.info("Quotation revision created: {} (version {})", revision.getCode(), revision.getVersion());

            QuotationDTO dto = quotationGetService.convertToDTO(revision);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Quotation revision created successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error creating quotation revision", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create quotation revision", "QUOTATION_REVISE_FAILED")
            );
        }
    }
}
