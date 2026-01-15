package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Services;

import java.util.List;
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
    private final ItineraryPaxRepository itineraryPaxRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryPaxGetService(
        ItineraryRepository itineraryRepository,
        ItineraryPaxRepository itineraryPaxRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryPaxRepository = itineraryPaxRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all pax entries for an itinerary
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing list of pax entries
     */
    public ResponseEntity<ApiResponse<?>> getItineraryPax(String itineraryIdObfuscated) {
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

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Retrieved " + paxDTOs.size() + " pax categories (total: " + totalPax + " passengers)",
                    paxDTOs
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
