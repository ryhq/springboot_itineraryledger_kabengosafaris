package com.itineraryledger.kabengosafaris.Quotation.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quotation.DTOs.QuotationDTO;
import com.itineraryledger.kabengosafaris.Quotation.DTOs.UpdateQuotationDTO;
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

/**
 * QuotationUpdateService - Service for updating quotations
 */
@Service
@Slf4j
@Transactional
public class QuotationUpdateService {

    private final QuotationRepository quotationRepository;
    private final QuotationGetService quotationGetService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuotationUpdateService(
        QuotationRepository quotationRepository,
        QuotationGetService quotationGetService,
        IdObfuscator idObfuscator
    ) {
        this.quotationRepository = quotationRepository;
        this.quotationGetService = quotationGetService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a quotation
     */
    @AuditLogAnnotation(
        action = "UPDATE_QUOTATION",
        description = "Updating quotation",
        entityType = "Quotation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateQuotation(String idObfuscated, UpdateQuotationDTO updateDTO) {
        log.info("Updating quotation: {}", idObfuscated);

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

            // Only DRAFT quotations can be updated
            if (quotation.getStatus() != QuotationStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only draft quotations can be updated", "QUOTATION_NOT_EDITABLE")
                );
            }

            // Update fields if provided
            if (updateDTO.getName() != null) {
                quotation.setName(updateDTO.getName());
            }
            if (updateDTO.getStartDate() != null) {
                quotation.setStartDate(updateDTO.getStartDate());
            }
            if (updateDTO.getEndDate() != null) {
                quotation.setEndDate(updateDTO.getEndDate());
            }
            if (updateDTO.getTotalDays() != null) {
                quotation.setTotalDays(updateDTO.getTotalDays());
            }
            if (updateDTO.getTotalNights() != null) {
                quotation.setTotalNights(updateDTO.getTotalNights());
            }
            if (updateDTO.getCurrency() != null) {
                quotation.setCurrency(updateDTO.getCurrency());
            }
            if (updateDTO.getExchangeRate() != null) {
                quotation.setExchangeRate(updateDTO.getExchangeRate());
            }
            if (updateDTO.getDiscountType() != null) {
                quotation.setDiscountType(updateDTO.getDiscountType());
            }
            if (updateDTO.getDiscountValue() != null) {
                quotation.setDiscountValue(updateDTO.getDiscountValue());
            }
            if (updateDTO.getDiscountReason() != null) {
                quotation.setDiscountReason(updateDTO.getDiscountReason());
            }
            if (updateDTO.getTaxRate() != null) {
                quotation.setTaxRate(updateDTO.getTaxRate());
            }
            if (updateDTO.getDepositPercentage() != null) {
                quotation.setDepositPercentage(updateDTO.getDepositPercentage());
            }
            if (updateDTO.getValidUntil() != null) {
                quotation.setValidUntil(updateDTO.getValidUntil());
            }
            if (updateDTO.getTermsAndConditions() != null) {
                quotation.setTermsAndConditions(updateDTO.getTermsAndConditions());
            }
            if (updateDTO.getInclusions() != null) {
                quotation.setInclusions(updateDTO.getInclusions());
            }
            if (updateDTO.getExclusions() != null) {
                quotation.setExclusions(updateDTO.getExclusions());
            }
            if (updateDTO.getInternalNotes() != null) {
                quotation.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getCustomerNotes() != null) {
                quotation.setCustomerNotes(updateDTO.getCustomerNotes());
            }
            if (updateDTO.getAssignedToId() != null) {
                try {
                    Long assignedToId = idObfuscator.decodeId(updateDTO.getAssignedToId());
                    quotation.setAssignedTo(assignedToId);
                } catch (Exception e) {
                    log.warn("Failed to decode assigned to ID: {}", updateDTO.getAssignedToId());
                }
            }

            // Recalculate end date if needed
            if (quotation.getEndDate() == null && quotation.getStartDate() != null && quotation.getTotalDays() != null) {
                quotation.setEndDate(quotation.getStartDate().plusDays(quotation.getTotalDays() - 1));
            }

            // Recalculate totals
            quotation.calculateTotals();

            quotation = quotationRepository.save(quotation);

            log.info("Quotation updated successfully: {}", quotation.getId());

            QuotationDTO dto = quotationGetService.convertToDTO(quotation);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating quotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update quotation", "QUOTATION_UPDATE_FAILED")
            );
        }
    }
}
