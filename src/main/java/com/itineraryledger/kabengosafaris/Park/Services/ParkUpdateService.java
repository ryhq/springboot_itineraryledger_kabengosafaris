package com.itineraryledger.kabengosafaris.Park.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.UpdateParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ParkUpdateService - Service for updating parks
 */
@Service
@Slf4j
@Transactional
public class ParkUpdateService {

    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository vendorRepository;

    @Autowired
    public ParkUpdateService(
        ParkRepository parkRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository vendorRepository
    ) {
        this.parkRepository = parkRepository;
        this.idObfuscator = idObfuscator;
        this.vendorRepository = vendorRepository;
    }

    /**
     * Update a park by obfuscated ID
     *
     * @param idObfuscated The obfuscated park ID
     * @param updateParkDTO The updated park data
     * @return ResponseEntity with ApiResponse containing the updated park
     */
    @AuditLogAnnotation(action = "UPDATE_PARK", description = "Updating park", entityType = "Park", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updatePark(String idObfuscated, UpdateParkDTO updateParkDTO) {
        log.info("Updating park with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode park ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid park ID",
                        "INVALID_PARK_ID"
                    )
                );
            }

            return updateParkById(id, updateParkDTO);

        } catch (Exception e) {
            log.error("Error updating park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update park",
                    "PARK_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update a park by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateParkById(Long id, UpdateParkDTO updateParkDTO) {
        // Find park
        Park park = parkRepository.findById(id).orElse(null);
        if (park == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(
                    404,
                    "Park not found",
                    "PARK_NOT_FOUND"
                )
            );
        }

        // Check if name is being changed and if it's unique
        if (updateParkDTO.getName() != null && !updateParkDTO.getName().equals(park.getName())) {
            if (parkRepository.existsByName(updateParkDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park with name '" + updateParkDTO.getName() + "' already exists",
                        "PARK_NAME_EXISTS"
                    )
                );
            }
            park.setName(updateParkDTO.getName());
        }

        // Check if slug is being changed and if it's unique
        if (updateParkDTO.getSlug() != null && !updateParkDTO.getSlug().equals(park.getSlug())) {
            if (parkRepository.existsBySlug(updateParkDTO.getSlug())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park with slug '" + updateParkDTO.getSlug() + "' already exists",
                        "PARK_SLUG_EXISTS"
                    )
                );
            }
            park.setSlug(updateParkDTO.getSlug());
        }

        // Update other fields if provided
        if (updateParkDTO.getParkType() != null) {
            park.setParkType(updateParkDTO.getParkType().isBlank() ? null : com.itineraryledger.kabengosafaris.Park.ParkType.valueOf(updateParkDTO.getParkType().trim()));
        }
        if (updateParkDTO.getRegion() != null) {
            park.setRegion(updateParkDTO.getRegion());
        }
        if (updateParkDTO.getDistrict() != null) {
            park.setDistrict(updateParkDTO.getDistrict());
        }
        if (updateParkDTO.getLocation() != null) {
            park.setLocation(updateParkDTO.getLocation());
        }
        if (updateParkDTO.getLatitude() != null) {
            park.setLatitude(updateParkDTO.getLatitude());
        }
        if (updateParkDTO.getLongitude() != null) {
            park.setLongitude(updateParkDTO.getLongitude());
        }
        if (updateParkDTO.getElevation() != null) {
            park.setElevation(updateParkDTO.getElevation());
        }
        if (updateParkDTO.getSize() != null) {
            park.setSize(updateParkDTO.getSize());
        }
        if (updateParkDTO.getShortDescription() != null) {
            park.setShortDescription(updateParkDTO.getShortDescription());
        }
        if (updateParkDTO.getFullDescription() != null) {
            park.setFullDescription(updateParkDTO.getFullDescription());
        }
        if (updateParkDTO.getHistory() != null) {
            park.setHistory(updateParkDTO.getHistory());
        }
        if (updateParkDTO.getEcosystem() != null) {
            park.setEcosystem(updateParkDTO.getEcosystem());
        }
        if (updateParkDTO.getWildlife() != null) {
            park.setWildlife(updateParkDTO.getWildlife());
        }
        if (updateParkDTO.getVegetation() != null) {
            park.setVegetation(updateParkDTO.getVegetation());
        }
        if (updateParkDTO.getPrimaryImage() != null) {
            park.setPrimaryImage(updateParkDTO.getPrimaryImage());
        }
        if (updateParkDTO.getBestTimeToVisit() != null) {
            park.setBestTimeToVisit(updateParkDTO.getBestTimeToVisit());
        }
        if (updateParkDTO.getOpeningHours() != null) {
            park.setOpeningHours(updateParkDTO.getOpeningHours());
        }
        if (updateParkDTO.getAccessInformation() != null) {
            park.setAccessInformation(updateParkDTO.getAccessInformation());
        }
        if (updateParkDTO.getTags() != null) {
            park.setTags(updateParkDTO.getTags());
        }
        /*
         * Who charges to enter. Set once — usually the first time a park fee is
         * billed — and remembered from then on, so nobody is asked twice.
         *
         * An empty string clears it; null leaves it alone, as everywhere else.
         */
        if (updateParkDTO.getVendorId() != null) {
            if (updateParkDTO.getVendorId().isBlank()) {
                park.setVendor(null);
            } else {
                try {
                    Long vendorId = idObfuscator.decodeId(updateParkDTO.getVendorId());
                    park.setVendor(vendorRepository.findById(vendorId).orElse(null));
                } catch (Exception e) {
                    log.warn("Unreadable vendor id on park update: {}", updateParkDTO.getVendorId());
                }
            }
        }

        if (updateParkDTO.getIsActive() != null) {
            park.setIsActive(updateParkDTO.getIsActive());
        }
        if (updateParkDTO.getIsWebActive() != null) {
            park.setIsWebActive(updateParkDTO.getIsWebActive());
        }

        // Save updated park
        park = parkRepository.save(park);

        // Convert to DTO
        ParkDTO parkDTO = convertToDTO(park);

        log.info("Park updated successfully: {}", park.getName());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Park updated successfully",
                parkDTO
            )
        );
    }

    /**
     * Convert Park entity to ParkDTO
     */
    private ParkDTO convertToDTO(Park park) {
        ParkDTO dto = new ParkDTO();
        dto.setId(idObfuscator.encodeId(park.getId()));
        dto.setName(park.getName());
        dto.setSlug(park.getSlug());
        dto.setParkType(park.getParkType());
        dto.setRegion(park.getRegion());
        dto.setDistrict(park.getDistrict());
        dto.setLocation(park.getLocation());
        dto.setLatitude(park.getLatitude());
        dto.setLongitude(park.getLongitude());
        dto.setElevation(park.getElevation());
        dto.setSize(park.getSize());
        dto.setShortDescription(park.getShortDescription());
        dto.setFullDescription(park.getFullDescription());
        dto.setHistory(park.getHistory());
        dto.setEcosystem(park.getEcosystem());
        dto.setWildlife(park.getWildlife());
        dto.setVegetation(park.getVegetation());
        dto.setPrimaryImage(park.getPrimaryImage());
        dto.setBestTimeToVisit(park.getBestTimeToVisit());
        dto.setOpeningHours(park.getOpeningHours());
        dto.setAccessInformation(park.getAccessInformation());
        dto.setTags(park.getTags());
        dto.setIsActive(park.getIsActive());
        if (park.getVendor() != null) {
            dto.setVendorId(idObfuscator.encodeId(park.getVendor().getId()));
            dto.setVendorName(park.getVendor().getName());
        }
        dto.setIsWebActive(park.getIsWebActive());
        dto.setCreatedAt(park.getCreatedAt());
        dto.setUpdatedAt(park.getUpdatedAt());
        return dto;
    }
}
