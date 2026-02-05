package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Repository.SafariDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs.CreateSafariDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs.SafariDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkTariffCreateService - Service for creating park tariffs within a safari park visit
 *
 * Implements Safari state validation - only editable safaris can have tariffs added.
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkTariffCreateService {

    private final SafariDayParkRepository dayParkRepository;
    private final SafariDayParkTariffRepository parkTariffRepository;
    private final ParkTariffRepository baseParkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkTariffCreateService(
        SafariDayParkRepository dayParkRepository,
        SafariDayParkTariffRepository parkTariffRepository,
        ParkTariffRepository baseParkTariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.dayParkRepository = dayParkRepository;
        this.parkTariffRepository = parkTariffRepository;
        this.baseParkTariffRepository = baseParkTariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Add tariffs to a safari park visit
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param createDTOs List of tariffs to add
     * @return ResponseEntity with ApiResponse
     */
    @AuditLogAnnotation(action = "ADD_SAFARI_PARK_TARIFFS", description = "Adding tariffs to safari park visit", entityType = "SafariDayParkTariff")
    public ResponseEntity<ApiResponse<?>> addParkTariffs(
        String parkVisitIdObfuscated,
        List<CreateSafariDayParkTariffDTO> createDTOs
    ) {
        log.info("Adding {} tariffs to safari park visit: {}", createDTOs.size(), parkVisitIdObfuscated);

        try {
            // Decode park visit ID
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            // Find park visit
            SafariDayPark parkVisit = dayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            // Check if safari is editable
            Safari safari = parkVisit.getSafariDay().getSafari();
            if (!safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
                );
            }

            Long parkId = parkVisit.getPark().getId();
            List<SafariDayParkTariffDTO> resultDTOs = new ArrayList<>();

            for (CreateSafariDayParkTariffDTO dto : createDTOs) {
                try {
                    // Decode tariff ID
                    Long tariffId = idObfuscator.decodeId(dto.getTariffId());

                    // Verify parkId matches (must be same park)
                    Long dtoParkId = idObfuscator.decodeId(dto.getParkId());
                    if (!dtoParkId.equals(parkId)) {
                        log.warn("Park ID mismatch for tariff: expected {}, got {}", parkId, dtoParkId);
                        continue;
                    }

                    // Find ParkTariff by park and tariff IDs
                    ParkTariff parkTariff = baseParkTariffRepository.findByParkIdAndTariffId(parkId, tariffId).orElse(null);
                    if (parkTariff == null) {
                        log.warn("ParkTariff not found: park={}, tariff={}", parkId, tariffId);
                        continue;
                    }

                    // Create entry
                    SafariDayParkTariff entry = SafariDayParkTariff.builder()
                        .safariDayPark(parkVisit)
                        .parkTariff(parkTariff)
                        .notes(dto.getNotes())
                        .isIncludedInPrice(dto.getIsIncludedInPrice() != null ? dto.getIsIncludedInPrice() : true)
                        .isPaid(false)
                        .isWaived(false)
                        .build();

                    entry = parkTariffRepository.save(entry);
                    resultDTOs.add(convertToDTO(entry));

                } catch (Exception e) {
                    log.error("Error adding safari park tariff", e);
                }
            }

            log.info("Added {} safari park tariffs", resultDTOs.size());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, resultDTOs.size() + " tariffs added", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error adding safari park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to add safari park tariffs", "SAFARI_PARK_TARIFFS_ADD_FAILED")
            );
        }
    }

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
