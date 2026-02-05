package com.itineraryledger.kabengosafaris.Safari.SafariPax.Services;

import java.util.List;
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
    public ResponseEntity<ApiResponse<?>> getSafariPax(String safariIdObfuscated) {
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

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Retrieved " + paxDTOs.size() + " pax categories (total: " + totalPax + " passengers)",
                    paxDTOs
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
}
