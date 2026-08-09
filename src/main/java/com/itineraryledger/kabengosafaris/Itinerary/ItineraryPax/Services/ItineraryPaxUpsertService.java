package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository.ItineraryPaxRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.DTOs.ItineraryPaxDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.DTOs.UpsertItineraryPaxDTO;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryPaxUpsertService - Service for creating/updating itinerary pax entries
 */
@Service
@Slf4j
@Transactional
public class ItineraryPaxUpsertService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryPaxRepository itineraryPaxRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final PaxAgeCategoryRepository ageCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryPaxUpsertService(
        ItineraryRepository itineraryRepository,
        ItineraryPaxRepository itineraryPaxRepository,
        PaxNationCategoryRepository nationCategoryRepository,
        PaxAgeCategoryRepository ageCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryPaxRepository = itineraryPaxRepository;
        this.nationCategoryRepository = nationCategoryRepository;
        this.ageCategoryRepository = ageCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Bulk upsert pax entries for an itinerary
     * Creates new entries or updates existing ones based on nation/age category combination
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param upsertDTOs List of pax data to upsert
     * @return ResponseEntity with ApiResponse containing the upserted pax entries
     */
    @AuditLogAnnotation(action = "UPSERT_ITINERARY_PAX", description = "Upserting itinerary pax entries", entityType = "ItineraryPax")
    public ResponseEntity<ApiResponse<?>> upsertItineraryPax(
        String itineraryIdObfuscated,
        List<UpsertItineraryPaxDTO> upsertDTOs
    ) {
        log.info("Upserting {} pax entries for itinerary: {}", upsertDTOs.size(), itineraryIdObfuscated);

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

            // Find itinerary
            Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            List<ItineraryPaxDTO> resultDTOs = new ArrayList<>();
            int createdCount = 0;
            int updatedCount = 0;

            /*
             * Bean validation does not cascade into a @RequestBody List, so @Min(1)
             * on the DTO never runs and a zero reached the entity's @PrePersist —
             * which throws, and an IllegalArgumentException reads to the caller as
             * "an unexpected error occurred". Check it here, where the answer can
             * name the band.
             */
            for (UpsertItineraryPaxDTO dto : upsertDTOs) {
                if (dto.getCount() == null || dto.getCount() < 1) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Every pax band needs at least one guest. Remove the band instead of setting it to "
                                + (dto.getCount() == null ? "nothing" : dto.getCount()) + ".",
                            "INVALID_PAX_COUNT")
                    );
                }
            }

            for (UpsertItineraryPaxDTO dto : upsertDTOs) {
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
                    ItineraryPax existingPax = itineraryPaxRepository
                        .findByItineraryIdAndNationCategoryIdAndAgeCategoryId(itineraryId, nationCategoryId, ageCategoryId)
                        .orElse(null);

                    ItineraryPax pax;
                    if (existingPax != null) {
                        // Update existing
                        existingPax.setCount(dto.getCount());
                        existingPax.setNotes(dto.getNotes());
                        pax = itineraryPaxRepository.save(existingPax);
                        updatedCount++;
                    } else {
                        // Create new
                        pax = ItineraryPax.builder()
                            .itinerary(itinerary)
                            .nationCategory(nationCategory)
                            .ageCategory(ageCategory)
                            .count(dto.getCount())
                            .notes(dto.getNotes())
                            .build();
                        pax = itineraryPaxRepository.save(pax);
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
            log.error("Error upserting itinerary pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upsert itinerary pax", "ITINERARY_PAX_UPSERT_FAILED")
            );
        }
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
