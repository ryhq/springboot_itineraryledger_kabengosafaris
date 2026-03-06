package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository.ItineraryDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs.ItineraryDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkTariffGetService - Service for retrieving park tariffs within a park visit
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayParkTariffGetService {

    private final ItineraryDayParkTariffRepository parkTariffRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "isIncludedInPrice", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ItineraryDayParkTariffGetService(
        ItineraryDayParkTariffRepository parkTariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkTariffRepository = parkTariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all tariffs for a park visit
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing list of tariffs
     */
    public ResponseEntity<ApiResponse<?>> getParkTariffs(
            String parkVisitIdObfuscated,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching tariffs for park visit: {}", parkVisitIdObfuscated);

        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            List<ItineraryDayParkTariff> tariffs = parkTariffRepository.findByItineraryDayParkId(parkVisitId);
            List<ItineraryDayParkTariffDTO> dtos = tariffs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("parkTariffs", dtos);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park tariffs retrieved", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park tariffs", "FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single tariff by ID
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param tariffIdObfuscated The obfuscated tariff entry ID
     * @return ResponseEntity with ApiResponse containing the tariff
     */
    public ResponseEntity<ApiResponse<?>> getParkTariff(
        String parkVisitIdObfuscated,
        String tariffIdObfuscated
    ) {
        log.info("Fetching tariff {} for park visit: {}", tariffIdObfuscated, parkVisitIdObfuscated);

        try {
            Long parkVisitId;
            Long tariffId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
                tariffId = idObfuscator.decodeId(tariffIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            ItineraryDayParkTariff tariff = parkTariffRepository.findById(tariffId).orElse(null);
            if (tariff == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park tariff not found", "PARK_TARIFF_NOT_FOUND")
                );
            }

            // Verify ownership
            if (!tariff.getItineraryDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Tariff does not belong to this park visit", "OWNERSHIP_MISMATCH")
                );
            }

            // Convert to DTO
            ItineraryDayParkTariffDTO tariffDTO = convertToDTO(tariff);

            // Parent-scoped circular navigation (scoped to park visit)
            Long parentId = tariff.getItineraryDayPark().getId();
            Long nextId = parkTariffRepository.findNextIdInParent(parentId, tariffId).orElse(null);
            Long previousId = parkTariffRepository.findPreviousIdInParent(parentId, tariffId).orElse(null);
            if (nextId == null) nextId = parkTariffRepository.findFirstIdInParent(parentId).orElse(null);
            if (previousId == null) previousId = parkTariffRepository.findLastIdInParent(parentId).orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("parkTariff", tariffDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park tariff retrieved", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park tariff", "FETCH_FAILED")
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
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
