package com.itineraryledger.kabengosafaris.Safari.SafariPax.Services;

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

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.DTOs.SafariPaxDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository.SafariPaxRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariPaxGetService - Service for retrieving safari pax entries
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariPaxGetService {

    private final SafariRepository safariRepository;
    private final SafariPaxRepository safariPaxRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "count", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public SafariPaxGetService(
        SafariRepository safariRepository,
        SafariPaxRepository safariPaxRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.safariPaxRepository = safariPaxRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all pax entries for a safari
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @return ResponseEntity with ApiResponse containing list of pax entries
     */
    public ResponseEntity<ApiResponse<?>> getSafariPax(String safariIdObfuscated, String sortBy, String sortDirection) {
        log.info("Fetching pax entries for safari: {}", safariIdObfuscated);

        try {
            // Decode safari ID
            Long safariId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // Verify safari exists
            if (!safariRepository.existsById(safariId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
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
            List<SafariPax> paxList = safariPaxRepository.findBySafariId(safariId);

            // Convert to DTOs
            List<SafariPaxDTO> paxDTOs = paxList.stream()
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
            log.error("Error fetching safari pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari pax", "SAFARI_PAX_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single pax entry by ID with circular navigation scoped to parent safari
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param paxIdObfuscated The obfuscated pax ID
     * @return ResponseEntity with ApiResponse containing the pax entry with nextId/previousId
     */
    public ResponseEntity<ApiResponse<?>> getSafariPaxById(
            String safariIdObfuscated,
            String paxIdObfuscated
    ) {
        log.info("Fetching pax entry: {} for safari: {}", paxIdObfuscated, safariIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long paxId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                paxId = idObfuscator.decodeId(paxIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Verify safari exists
            if (!safariRepository.existsById(safariId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Fetch pax entry
            var pax = safariPaxRepository.findById(paxId).orElse(null);
            if (pax == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Pax entry not found", "PAX_NOT_FOUND")
                );
            }
            if (!pax.getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Pax entry does not belong to this safari", "PAX_SAFARI_MISMATCH")
                );
            }

            // Convert to DTO
            SafariPaxDTO dto = convertToDTO(pax);

            // Parent-scoped circular navigation
            Long parentId = pax.getSafari().getId();
            Long nextId = safariPaxRepository.findNextIdInParent(parentId, paxId).orElse(null);
            Long previousId = safariPaxRepository.findPreviousIdInParent(parentId, paxId).orElse(null);
            if (nextId == null) nextId = safariPaxRepository.findFirstIdInParent(parentId).orElse(null);
            if (previousId == null) previousId = safariPaxRepository.findLastIdInParent(parentId).orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("paxEntry", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Pax entry retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching safari pax by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari pax", "SAFARI_PAX_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert SafariPax entity to SafariPaxDTO
     */
    private SafariPaxDTO convertToDTO(SafariPax pax) {
        SafariPaxDTO dto = new SafariPaxDTO();
        dto.setId(idObfuscator.encodeId(pax.getId()));
        dto.setSafariId(idObfuscator.encodeId(pax.getSafari().getId()));
        dto.setNationCategoryId(idObfuscator.encodeId(pax.getNationCategory().getId()));
        dto.setNationCategoryName(pax.getNationCategory().getName());
        dto.setAgeCategoryId(idObfuscator.encodeId(pax.getAgeCategory().getId()));
        dto.setAgeCategoryName(pax.getAgeCategory().getName());
        dto.setCount(pax.getCount());
        dto.setSpecialRequirements(pax.getSpecialRequirements());
        dto.setNotes(pax.getNotes());
        dto.setCreatedAt(pax.getCreatedAt());
        dto.setUpdatedAt(pax.getUpdatedAt());
        return dto;
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}
