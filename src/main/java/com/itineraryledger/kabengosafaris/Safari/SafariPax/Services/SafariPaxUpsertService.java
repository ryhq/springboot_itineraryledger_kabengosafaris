package com.itineraryledger.kabengosafaris.Safari.SafariPax.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.DTOs.SafariPaxDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.DTOs.UpsertSafariPaxDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository.SafariPaxRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariPaxUpsertService - Service for creating/updating safari pax entries
 */
@Service
@Slf4j
@Transactional
public class SafariPaxUpsertService {

    private final SafariRepository safariRepository;
    private final SafariPaxRepository safariPaxRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final PaxAgeCategoryRepository ageCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariPaxUpsertService(
        SafariRepository safariRepository,
        SafariPaxRepository safariPaxRepository,
        PaxNationCategoryRepository nationCategoryRepository,
        PaxAgeCategoryRepository ageCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.safariPaxRepository = safariPaxRepository;
        this.nationCategoryRepository = nationCategoryRepository;
        this.ageCategoryRepository = ageCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Bulk upsert pax entries for a safari
     * Creates new entries or updates existing ones based on nation/age category combination
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param upsertDTOs List of pax data to upsert
     * @return ResponseEntity with ApiResponse containing the upserted pax entries
     */
    @AuditLogAnnotation(action = "UPSERT_SAFARI_PAX", description = "Upserting safari pax entries", entityType = "SafariPax")
    public ResponseEntity<ApiResponse<?>> upsertSafariPax(
        String safariIdObfuscated,
        List<UpsertSafariPaxDTO> upsertDTOs
    ) {
        log.info("Upserting {} pax entries for safari: {}", upsertDTOs.size(), safariIdObfuscated);

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

            // Find safari
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Check if safari is editable
            if (!safari.isEditable()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari cannot be edited in state: " + safari.getState().getDisplayName(), "SAFARI_NOT_EDITABLE")
                );
            }

            List<SafariPaxDTO> resultDTOs = new ArrayList<>();
            int createdCount = 0;
            int updatedCount = 0;

            for (UpsertSafariPaxDTO dto : upsertDTOs) {
                try {
                    // Decode category IDs
                    Long nationCategoryId = idObfuscator.decodeId(dto.getNationCategoryId());
                    Long ageCategoryId = idObfuscator.decodeId(dto.getAgeCategoryId());

                    // Find categories
                    PaxNationCategory nationCategory = nationCategoryRepository.findById(nationCategoryId).orElse(null);
                    if (nationCategory == null) {
                        log.warn("Nation category not found: {}", dto.getNationCategoryId());
                        continue;
                    }

                    PaxAgeCategory ageCategory = ageCategoryRepository.findById(ageCategoryId).orElse(null);
                    if (ageCategory == null) {
                        log.warn("Age category not found: {}", dto.getAgeCategoryId());
                        continue;
                    }

                    // Check if entry already exists
                    SafariPax existingPax = safariPaxRepository
                        .findBySafariIdAndNationCategoryIdAndAgeCategoryId(safariId, nationCategoryId, ageCategoryId)
                        .orElse(null);

                    SafariPax pax;
                    if (existingPax != null) {
                        // Update existing
                        existingPax.setCount(dto.getCount());
                        existingPax.setSpecialRequirements(dto.getSpecialRequirements());
                        existingPax.setNotes(dto.getNotes());
                        pax = safariPaxRepository.save(existingPax);
                        updatedCount++;
                    } else {
                        // Create new
                        pax = SafariPax.builder()
                            .safari(safari)
                            .nationCategory(nationCategory)
                            .ageCategory(ageCategory)
                            .count(dto.getCount())
                            .specialRequirements(dto.getSpecialRequirements())
                            .notes(dto.getNotes())
                            .build();
                        pax = safariPaxRepository.save(pax);
                        createdCount++;
                    }

                    resultDTOs.add(convertToDTO(pax));

                } catch (Exception e) {
                    log.error("Error processing pax entry: {}", dto, e);
                }
            }

            log.info("Pax upsert complete: {} created, {} updated", createdCount, updatedCount);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    createdCount + " created, " + updatedCount + " updated",
                    resultDTOs
                )
            );

        } catch (Exception e) {
            log.error("Error upserting safari pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upsert safari pax", "SAFARI_PAX_UPSERT_FAILED")
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
