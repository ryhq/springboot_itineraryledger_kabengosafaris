package com.itineraryledger.kabengosafaris.Park.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.DTOs.CreateParkDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ParkCreateService - Service for creating parks
 */
@Service
@Slf4j
@Transactional
public class ParkCreateService {

    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkCreateService(
        ParkRepository parkRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkRepository = parkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new park
     *
     * @param createParkDTO The park data
     * @return ResponseEntity with ApiResponse containing the created park
     */
    @AuditLogAnnotation(action = "CREATE_PARK", description = "Creating a new park", entityType = "Park")
    public ResponseEntity<ApiResponse<?>> createPark(CreateParkDTO createParkDTO) {
        log.info("Creating new park: {}", createParkDTO.getName());

        try {
            // Validate name uniqueness
            if (parkRepository.existsByName(createParkDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park with name '" + createParkDTO.getName() + "' already exists",
                        "PARK_NAME_EXISTS"
                    )
                );
            }

            // Generate slug if not provided
            String slug = createParkDTO.getSlug();
            if (slug == null || slug.isEmpty()) {
                slug = generateSlug(createParkDTO.getName());
            }

            // Validate slug uniqueness
            if (parkRepository.existsBySlug(slug)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park with slug '" + slug + "' already exists",
                        "PARK_SLUG_EXISTS"
                    )
                );
            }

            // Create park entity
            Park park = Park.builder()
                .name(createParkDTO.getName())
                .slug(slug)
                .parkType(createParkDTO.getParkType())
                .region(createParkDTO.getRegion())
                .district(createParkDTO.getDistrict())
                .location(createParkDTO.getLocation())
                .latitude(createParkDTO.getLatitude())
                .longitude(createParkDTO.getLongitude())
                .elevation(createParkDTO.getElevation())
                .size(createParkDTO.getSize())
                .shortDescription(createParkDTO.getShortDescription())
                .fullDescription(createParkDTO.getFullDescription())
                .history(createParkDTO.getHistory())
                .ecosystem(createParkDTO.getEcosystem())
                .wildlife(createParkDTO.getWildlife())
                .vegetation(createParkDTO.getVegetation())
                .primaryImage(createParkDTO.getPrimaryImage())
                .bestTimeToVisit(createParkDTO.getBestTimeToVisit())
                .openingHours(createParkDTO.getOpeningHours())
                .accessInformation(createParkDTO.getAccessInformation())
                .tags(createParkDTO.getTags())
                .isActive(createParkDTO.getIsActive() != null ? createParkDTO.getIsActive() : true)
                .build();

            // Save park
            park = parkRepository.save(park);

            // Convert to DTO
            ParkDTO parkDTO = convertToDTO(park);

            log.info("Park created successfully: {}", park.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Park created successfully",
                    parkDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create park",
                    "PARK_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Generate URL-friendly slug from park name
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
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
        dto.setCreatedAt(park.getCreatedAt());
        dto.setUpdatedAt(park.getUpdatedAt());
        return dto;
    }
}
