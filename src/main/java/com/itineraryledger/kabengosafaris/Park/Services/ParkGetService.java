package com.itineraryledger.kabengosafaris.Park.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ParkGetService - Service for retrieving parks with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ParkGetService {

    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkGetService(
        ParkRepository parkRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkRepository = parkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single park by obfuscated ID
     *
     * @param idObfuscated The obfuscated park ID
     * @return ResponseEntity with ApiResponse containing the park
     */
    public ResponseEntity<ApiResponse<?>> getParkById(String idObfuscated) {
        log.info("Fetching park with ID: {}", idObfuscated);

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

            // Convert to DTO
            ParkDTO parkDTO = convertToDTO(park);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Park retrieved successfully",
                    parkDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch park",
                    "PARK_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get a single park by slug
     *
     * @param slug The park slug
     * @return ResponseEntity with ApiResponse containing the park
     */
    public ResponseEntity<ApiResponse<?>> getParkBySlug(String slug) {
        log.info("Fetching park with slug: {}", slug);

        try {
            // Find park
            Park park = parkRepository.findBySlug(slug).orElse(null);
            if (park == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Park not found",
                        "PARK_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            ParkDTO parkDTO = convertToDTO(park);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Park retrieved successfully",
                    parkDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch park",
                    "PARK_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all parks with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param parkType Filter by park type
     * @param region Filter by region (partial match)
     * @param district Filter by district (partial match)
     * @param location Filter by location (partial match)
     * @param parkSize Filter by park size (partial match)
     * @param isActive Filter by active status
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated parks
     */
    public ResponseEntity<ApiResponse<?>> getAllParks(
        String name,
        String slug,
        ParkType parkType,
        String region,
        String district,
        String location,
        String parkSize,
        Boolean isActive,
        String keyword,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching all parks with filters");

        try {
            // Build specification for filtering
            Specification<Park> spec = Specification.unrestricted();

            if (name != null && !name.isEmpty()) {
                spec = spec.and(ParkSpecification.nameLike(name));
            }
            if (slug != null && !slug.isEmpty()) {
                spec = spec.and(ParkSpecification.slugLike(slug));
            }
            if (parkType != null) {
                spec = spec.and(ParkSpecification.hasParkType(parkType));
            }
            if (region != null && !region.isEmpty()) {
                spec = spec.and(ParkSpecification.regionLike(region));
            }
            if (district != null && !district.isEmpty()) {
                spec = spec.and(ParkSpecification.districtLike(district));
            }
            if (location != null && !location.isEmpty()) {
                spec = spec.and(ParkSpecification.locationLike(location));
            }
            if (isActive != null) {
                spec = spec.and(ParkSpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(ParkSpecification.searchKeyword(keyword));
            }
            if (parkSize != null && !parkSize.isEmpty()) {
                spec = spec.and(ParkSpecification.sizeLike(parkSize));
            }

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set default sorting (always by createdAt)
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));

            // Fetch parks
            Page<Park> parkPage = parkRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<ParkDTO> parkDTOs = parkPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("parks", parkDTOs);
            response.put("currentPage", parkPage.getNumber());
            response.put("totalItems", parkPage.getTotalElements());
            response.put("totalPages", parkPage.getTotalPages());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Parks retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching parks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch parks",
                    "PARKS_FETCH_FAILED"
                )
            );
        }
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
