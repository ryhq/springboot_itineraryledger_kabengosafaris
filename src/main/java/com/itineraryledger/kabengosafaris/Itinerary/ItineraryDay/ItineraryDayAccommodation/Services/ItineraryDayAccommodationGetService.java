package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services;

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

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.DTOs.ItineraryDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayAccommodationGetService - Service for retrieving itinerary day accommodations
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayAccommodationGetService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final ItineraryDayAccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "roomCount", "isAlternative", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ItineraryDayAccommodationGetService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayAccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get all accommodations for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing list of accommodations
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayAccommodations(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching accommodations for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Verify day exists and belongs to itinerary
            var day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
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

            // Fetch accommodations
            List<ItineraryDayAccommodation> accommodations = accommodationRepository.findByItineraryDayId(dayId);

            // Convert to DTOs
            List<ItineraryDayAccommodationDTO> dtos = accommodations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("accommodations", dtos);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodations retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch accommodations", "ACCOMMODATIONS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single accommodation by ID with circular navigation scoped to parent day
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayAccommodation(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String accommodationIdObfuscated,
        /* the sort travels with the record so its arrows keep the list's order */
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching accommodation: {} for day: {}", accommodationIdObfuscated, dayIdObfuscated);

        try {
            Long itineraryId;
            Long dayId;
            Long accommodationId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                accommodationId = idObfuscator.decodeId(accommodationIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            var day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            var accommodation = accommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
                );
            }
            if (!accommodation.getItineraryDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Accommodation does not belong to this day", "ACCOMMODATION_DAY_MISMATCH")
                );
            }

            var dto = convertToDTO(accommodation);

            /*
             * Parent-scoped circular navigation, in the ORDER THE LIST USED. The repository
             * walk this replaces stepped by id whatever the sort was, so the arrows moved
             * through a different sequence from the one on screen — and could not say where
             * in it you were.
             */
            Long parentId = accommodation.getItineraryDay().getId();
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                ItineraryDayAccommodation.class,
                (root, query, cb) -> cb.equal(root.get("itineraryDay").get("id"), parentId),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                accommodationId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("accommodation", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodation retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch accommodation", "ACCOMMODATION_FETCH_FAILED")
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

    /**
     * Convert entity to DTO
     */
    private ItineraryDayAccommodationDTO convertToDTO(ItineraryDayAccommodation entity) {
        ItineraryDayAccommodationDTO dto = new ItineraryDayAccommodationDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setItineraryDayId(idObfuscator.encodeId(entity.getItineraryDay().getId()));
        dto.setAccommodationId(idObfuscator.encodeId(entity.getAccommodation().getId()));
        dto.setAccommodationName(entity.getAccommodation().getName());
        dto.setAccommodationSlug(entity.getAccommodation().getSlug());

        dto.setRoomTypeId(idObfuscator.encodeId(entity.getRoomType().getId()));
        dto.setRoomTypeName(entity.getRoomType().getName());
        dto.setRoomStandardId(idObfuscator.encodeId(entity.getRoomStandard().getId()));
        dto.setRoomStandardName(entity.getRoomStandard().getName());
        dto.setBoardTypeId(idObfuscator.encodeId(entity.getBoardType().getId()));
        dto.setBoardTypeName(entity.getBoardType().getName());

        dto.setRoomCount(entity.getRoomCount());
        dto.setIsAlternative(entity.getIsAlternative());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
