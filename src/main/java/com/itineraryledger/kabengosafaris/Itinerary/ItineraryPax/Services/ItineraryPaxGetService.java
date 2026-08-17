package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Services;

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

import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository.ItineraryPaxRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.DTOs.ItineraryPaxDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryPaxGetService - Service for retrieving itinerary pax entries
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryPaxGetService {

    private final ItineraryRepository itineraryRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final ItineraryPaxRepository itineraryPaxRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "count", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ItineraryPaxGetService(
        ItineraryRepository itineraryRepository,
        ItineraryPaxRepository itineraryPaxRepository,
        IdObfuscator idObfuscator
    ,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryPaxRepository = itineraryPaxRepository;
        this.idObfuscator = idObfuscator;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get all pax entries for an itinerary
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing list of pax entries
     */
    public ResponseEntity<ApiResponse<?>> getItineraryPax(
            String itineraryIdObfuscated,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching pax entries for itinerary: {}", itineraryIdObfuscated);

        try {
            // Decode itinerary ID
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // Verify itinerary exists
            if (!itineraryRepository.existsById(itineraryId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
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

            // Fetch pax entries
            List<ItineraryPax> paxList = itineraryPaxRepository.findByItineraryId(itineraryId);

            // Convert to DTOs
            List<ItineraryPaxDTO> paxDTOs = paxList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Calculate totals
            int totalPax = paxList.stream()
                .mapToInt(p -> p.getCount() != null ? p.getCount() : 0)
                .sum();

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("paxEntries", paxDTOs);
            response.put("totalPax", totalPax);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Retrieved " + paxDTOs.size() + " pax categories (total: " + totalPax + " passengers)",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary pax", "ITINERARY_PAX_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single pax entry by ID with circular navigation scoped to parent itinerary
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param paxIdObfuscated The obfuscated pax ID
     * @return ResponseEntity with ApiResponse containing the pax entry with nextId/previousId
     */
    public ResponseEntity<ApiResponse<?>> getItineraryPaxById(
            String itineraryIdObfuscated,
            String paxIdObfuscated,
        /* the sort travels with the record so its arrows keep the list's order */
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching pax entry: {} for itinerary: {}", paxIdObfuscated, itineraryIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long paxId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                paxId = idObfuscator.decodeId(paxIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Verify itinerary exists
            if (!itineraryRepository.existsById(itineraryId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Fetch pax entry
            var pax = itineraryPaxRepository.findById(paxId).orElse(null);
            if (pax == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Pax entry not found", "PAX_NOT_FOUND")
                );
            }
            if (!pax.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Pax entry does not belong to this itinerary", "PAX_ITINERARY_MISMATCH")
                );
            }

            // Convert to DTO
            ItineraryPaxDTO dto = convertToDTO(pax);

            /*
             * Parent-scoped circular navigation, in the ORDER THE LIST USED. The repository
             * walk this replaces stepped by id whatever the sort was, so the arrows moved
             * through a different sequence from the one on screen — and could not say where
             * in it you were.
             */
            Long parentId = pax.getItinerary().getId();
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                ItineraryPax.class,
                (root, query, cb) -> cb.equal(root.get("itinerary").get("id"), parentId),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                paxId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("paxEntry", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Pax entry retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary pax by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary pax", "ITINERARY_PAX_FETCH_FAILED")
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
     * Convert ItineraryPax entity to ItineraryPaxDTO
     */
    private ItineraryPaxDTO convertToDTO(ItineraryPax pax) {
        ItineraryPaxDTO dto = new ItineraryPaxDTO();
        dto.setId(idObfuscator.encodeId(pax.getId()));
        dto.setItineraryId(idObfuscator.encodeId(pax.getItinerary().getId()));
        dto.setNationCategoryId(idObfuscator.encodeId(pax.getNationCategory().getId()));
        dto.setNationCategoryName(pax.getNationCategory().getName());
        dto.setAgeCategoryId(idObfuscator.encodeId(pax.getAgeCategory().getId()));
        dto.setAgeCategoryName(pax.getAgeCategory().getName());
        dto.setCount(pax.getCount());
        dto.setNotes(pax.getNotes());
        dto.setCreatedAt(pax.getCreatedAt());
        dto.setUpdatedAt(pax.getUpdatedAt());
        return dto;
    }
}
