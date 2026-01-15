package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository.ItineraryDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs.CreateItineraryDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs.ItineraryDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkTariffService - Service for managing park tariffs within a park visit
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayParkTariffService {

    private final ItineraryDayParkRepository dayParkRepository;
    private final ItineraryDayParkTariffRepository parkTariffRepository;
    private final ParkTariffRepository baseParkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkTariffService(
        ItineraryDayParkRepository dayParkRepository,
        ItineraryDayParkTariffRepository parkTariffRepository,
        ParkTariffRepository baseParkTariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.dayParkRepository = dayParkRepository;
        this.parkTariffRepository = parkTariffRepository;
        this.baseParkTariffRepository = baseParkTariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Add tariffs to a park visit
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param createDTOs List of tariffs to add
     * @return ResponseEntity with ApiResponse
     */
    @AuditLogAnnotation(action = "ADD_PARK_TARIFFS", description = "Adding tariffs to park visit", entityType = "ItineraryDayParkTariff")
    public ResponseEntity<ApiResponse<?>> addParkTariffs(
        String parkVisitIdObfuscated,
        List<CreateItineraryDayParkTariffDTO> createDTOs
    ) {
        log.info("Adding {} tariffs to park visit: {}", createDTOs.size(), parkVisitIdObfuscated);

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
            ItineraryDayPark parkVisit = dayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            Long parkId = parkVisit.getPark().getId();
            List<ItineraryDayParkTariffDTO> resultDTOs = new ArrayList<>();

            for (CreateItineraryDayParkTariffDTO dto : createDTOs) {
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
                    ItineraryDayParkTariff entry = ItineraryDayParkTariff.builder()
                        .itineraryDayPark(parkVisit)
                        .parkTariff(parkTariff)
                        .notes(dto.getNotes())
                        .isIncludedInPrice(dto.getIsIncludedInPrice() != null ? dto.getIsIncludedInPrice() : true)
                        .build();

                    entry = parkTariffRepository.save(entry);
                    resultDTOs.add(convertToDTO(entry));

                } catch (Exception e) {
                    log.error("Error adding park tariff", e);
                }
            }

            log.info("Added {} park tariffs", resultDTOs.size());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, resultDTOs.size() + " tariffs added", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error adding park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to add park tariffs", "PARK_TARIFFS_ADD_FAILED")
            );
        }
    }

    /**
     * Get all tariffs for a park visit
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getParkTariffs(String parkVisitIdObfuscated) {
        log.info("Fetching tariffs for park visit: {}", parkVisitIdObfuscated);

        try {
            Long parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);

            List<ItineraryDayParkTariff> tariffs = parkTariffRepository.findByItineraryDayParkId(parkVisitId);
            List<ItineraryDayParkTariffDTO> dtos = tariffs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park tariffs retrieved", dtos)
            );

        } catch (Exception e) {
            log.error("Error fetching park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park tariffs", "FETCH_FAILED")
            );
        }
    }

    /**
     * Delete park tariffs
     */
    public ResponseEntity<ApiResponse<?>> deleteParkTariffs(
        String parkVisitIdObfuscated,
        List<String> tariffIdObfuscatedList
    ) {
        log.info("Deleting {} park tariffs", tariffIdObfuscatedList.size());

        try {
            Long parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);

            int deletedCount = 0;
            for (String idObfuscated : tariffIdObfuscatedList) {
                try {
                    Long tariffEntryId = idObfuscator.decodeId(idObfuscated);
                    ItineraryDayParkTariff entry = parkTariffRepository.findById(tariffEntryId).orElse(null);

                    if (entry == null || !entry.getItineraryDayPark().getId().equals(parkVisitId)) {
                        continue;
                    }

                    ((ItineraryDayParkTariffService) AopContext.currentProxy()).deleteTariff(tariffEntryId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting park tariff", e);
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " tariffs deleted", null)
            );

        } catch (Exception e) {
            log.error("Error deleting park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park tariffs", "DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_PARK_TARIFF", description = "Deleting park tariff", entityType = "ItineraryDayParkTariff", entityIdParamName = "id")
    public void deleteTariff(Long id) {
        parkTariffRepository.deleteById(id);
    }

    private ItineraryDayParkTariffDTO convertToDTO(ItineraryDayParkTariff entity) {
        ItineraryDayParkTariffDTO dto = new ItineraryDayParkTariffDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setItineraryDayParkId(idObfuscator.encodeId(entity.getItineraryDayPark().getId()));
        dto.setParkId(idObfuscator.encodeId(entity.getParkTariff().getPark().getId()));
        dto.setParkName(entity.getParkTariff().getPark().getName());
        dto.setTariffId(idObfuscator.encodeId(entity.getParkTariff().getTariff().getId()));
        dto.setTariffName(entity.getParkTariff().getTariff().getName());
        dto.setNotes(entity.getNotes());
        dto.setIsIncludedInPrice(entity.getIsIncludedInPrice());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
