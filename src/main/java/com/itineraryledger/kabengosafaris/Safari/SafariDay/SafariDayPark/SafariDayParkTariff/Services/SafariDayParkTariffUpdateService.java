package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Repository.SafariDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs.SafariDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs.UpdateSafariDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkTariffUpdateService - Service for updating safari park tariffs
 *
 * Implements dual update modes:
 * - Planning updates (notes, isIncludedInPrice) require editable safari state
 * - Operational updates (payment details, waiver) allowed anytime
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkTariffUpdateService {

    private final SafariDayParkTariffRepository parkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkTariffUpdateService(
        SafariDayParkTariffRepository parkTariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkTariffRepository = parkTariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a safari park tariff
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param tariffIdObfuscated The obfuscated tariff ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated tariff
     */
    @AuditLogAnnotation(action = "UPDATE_SAFARI_PARK_TARIFF", description = "Updating safari park tariff", entityType = "SafariDayParkTariff")
    public ResponseEntity<ApiResponse<?>> updateParkTariff(
        String parkVisitIdObfuscated,
        String tariffIdObfuscated,
        UpdateSafariDayParkTariffDTO updateDTO
    ) {
        log.info("Updating safari park tariff: {} in park visit: {}", tariffIdObfuscated, parkVisitIdObfuscated);

        try {
            // Decode IDs
            Long parkVisitId;
            Long tariffId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
                tariffId = idObfuscator.decodeId(tariffIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Find tariff
            SafariDayParkTariff tariff = parkTariffRepository.findById(tariffId).orElse(null);
            if (tariff == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari park tariff not found", "SAFARI_PARK_TARIFF_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!tariff.getSafariDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Tariff does not belong to this park visit", "TARIFF_PARK_VISIT_MISMATCH")
                );
            }

            // Determine if this is an operational update
            boolean isOperationalUpdate = updateDTO.getIsPaid() != null ||
                updateDTO.getReceiptNumber() != null ||
                updateDTO.getPaymentNotes() != null ||
                updateDTO.getPaxCount() != null ||
                updateDTO.getIsWaived() != null ||
                updateDTO.getWaiverReason() != null;

            // Check if safari is editable for planning updates
            Safari safari = tariff.getSafariDayPark().getSafariDay().getSafari();
            if (!isOperationalUpdate && !safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
                );
            }

            // Update planning fields if provided
            if (updateDTO.getNotes() != null) {
                tariff.setNotes(updateDTO.getNotes());
            }
            if (updateDTO.getIsIncludedInPrice() != null) {
                tariff.setIsIncludedInPrice(updateDTO.getIsIncludedInPrice());
            }

            // Update Safari-specific operational fields
            if (updateDTO.getIsPaid() != null) {
                tariff.setIsPaid(updateDTO.getIsPaid());
                // Auto-set paidAt timestamp when marking as paid
                if (Boolean.TRUE.equals(updateDTO.getIsPaid())) {
                    tariff.setPaidAt(LocalDateTime.now());
                } else {
                    tariff.setPaidAt(null);
                }
            }

            if (updateDTO.getReceiptNumber() != null) {
                tariff.setReceiptNumber(updateDTO.getReceiptNumber());
            }

            if (updateDTO.getPaymentNotes() != null) {
                tariff.setPaymentNotes(updateDTO.getPaymentNotes());
            }

            if (updateDTO.getPaxCount() != null) {
                tariff.setPaxCount(updateDTO.getPaxCount());
            }

            if (updateDTO.getIsWaived() != null) {
                tariff.setIsWaived(updateDTO.getIsWaived());
            }

            if (updateDTO.getWaiverReason() != null) {
                tariff.setWaiverReason(updateDTO.getWaiverReason());
            }

            // Save
            tariff = parkTariffRepository.save(tariff);

            // Convert to DTO
            SafariDayParkTariffDTO dto = convertToDTO(tariff);

            log.info("Safari park tariff updated successfully: {}", tariffId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari park tariff updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating safari park tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update safari park tariff", "SAFARI_DAY_PARK_TARIFF_UPDATE_FAILED")
            );
        }
    }

    /**
     * Convert SafariDayParkTariff entity to DTO
     */
    private SafariDayParkTariffDTO convertToDTO(SafariDayParkTariff entity) {
        SafariDayParkTariffDTO dto = new SafariDayParkTariffDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setSafariDayParkId(idObfuscator.encodeId(entity.getSafariDayPark().getId()));
        dto.setParkId(idObfuscator.encodeId(entity.getParkTariff().getPark().getId()));
        dto.setParkName(entity.getParkTariff().getPark().getName());
        dto.setTariffId(idObfuscator.encodeId(entity.getParkTariff().getTariff().getId()));
        dto.setTariffName(entity.getParkTariff().getTariff().getName());
        dto.setNotes(entity.getNotes());
        dto.setIsIncludedInPrice(entity.getIsIncludedInPrice());

        // Safari-specific fields
        dto.setIsPaid(entity.getIsPaid());
        dto.setPaidAt(entity.getPaidAt());
        dto.setReceiptNumber(entity.getReceiptNumber());
        dto.setPaymentNotes(entity.getPaymentNotes());
        dto.setPaxCount(entity.getPaxCount());
        dto.setIsWaived(entity.getIsWaived());
        dto.setWaiverReason(entity.getWaiverReason());

        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
