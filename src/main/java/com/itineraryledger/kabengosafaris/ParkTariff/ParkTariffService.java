package com.itineraryledger.kabengosafaris.ParkTariff;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkWithNotesDTO;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.ParkTariff.DTOs.ParkTariffUpsertDTO;
import com.itineraryledger.kabengosafaris.ParkTariff.DTOs.ParkTariffUpsertResponseDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.TariffWithNotesDTO;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Specifications.TariffSpecification;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing park-tariff associations
 *
 * Provides functionality for:
 * - Getting tariffs for a park (assigned/unassigned)
 * - Getting parks for a tariff (assigned/unassigned)
 * - Bulk upsert operations for park-tariff relationships
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkTariffService {

    private final ParkTariffRepository parkTariffRepository;
    private final ParkRepository parkRepository;
    private final TariffRepository tariffRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Get all tariffs for a specific park with filtering, pagination, and sorting
     *
     * @param parkIdObfuscated Obfuscated park ID
     * @param assigned true = assigned tariffs, false = unassigned tariffs, null = assigned (default)
     * @param name Filter by tariff name
     * @param slug Filter by tariff slug
     * @param chargingBasis Filter by charging basis
     * @param isActive Filter by active status
     * @param isSystem Filter by system status
     * @param keyword Search keyword
     * @param page Page number
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with paginated tariffs
     */
    public ResponseEntity<ApiResponse<?>> getTariffsForPark(
        String parkIdObfuscated,
        Boolean assigned,
        String name,
        String slug,
        ChargingBasis chargingBasis,
        Boolean isActive,
        Boolean isSystem,
        String keyword,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching tariffs for park: {}, assigned: {}", parkIdObfuscated, assigned);

        try {
            // Decode park ID
            Long parkId = idObfuscator.decodeId(parkIdObfuscated);

            // Verify park exists
            if (!parkRepository.existsById(parkId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park not found", "PARK_NOT_FOUND")
                );
            }

            // Build specification - filter by park assignment
            Specification<Tariff> spec;
            if (assigned != null && !assigned) {
                // Get tariffs NOT assigned to this park
                spec = TariffSpecification.notByParkId(parkId);
            } else {
                // Get tariffs assigned to this park (default behavior)
                spec = TariffSpecification.byParkId(parkId);
            }

            // Add additional filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(TariffSpecification.nameLike(name));
            }
            if (slug != null && !slug.isEmpty()) {
                spec = spec.and(TariffSpecification.hasSlugLike(slug));
            }
            if (chargingBasis != null) {
                spec = spec.and(TariffSpecification.hasChargingBasis(chargingBasis));
            }
            if (isActive != null) {
                spec = spec.and(TariffSpecification.isActive(isActive));
            }
            if (isSystem != null) {
                spec = spec.and(TariffSpecification.isSystem(isSystem));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(TariffSpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting
            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "name"));

            // Fetch tariffs
            Page<Tariff> tariffPage = tariffRepository.findAll(spec, pageable);

            // Convert to DTOs with notes (only include notes if assigned=true)
            boolean includeNotes = assigned == null || assigned;
            List<TariffWithNotesDTO> tariffDTOs = tariffPage.getContent().stream()
                .map(tariff -> convertTariffToWithNotesDTO(tariff, includeNotes ? parkId : null))
                .collect(Collectors.toList());

            // Response with pagination
            Map<String, Object> response = new HashMap<>();
            response.put("tariffs", tariffDTOs);
            response.put("currentPage", tariffPage.getNumber());
            response.put("totalItems", tariffPage.getTotalElements());
            response.put("totalPages", tariffPage.getTotalPages());

            return ResponseEntity.ok(ApiResponse.success(200, "Tariffs retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching tariffs for park", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to fetch tariffs", "FETCH_FAILED")
            );
        }
    }

    /**
     * Get all parks for a specific tariff with filtering, pagination, and sorting
     *
     * @param tariffIdObfuscated Obfuscated tariff ID
     * @param assigned true = assigned parks, false = unassigned parks, null = assigned (default)
     * @param name Filter by park name
     * @param slug Filter by park slug
     * @param parkType Filter by park type
     * @param region Filter by region
     * @param district Filter by district
     * @param location Filter by location
     * @param parkSize Filter by park size
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param page Page number
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with paginated parks
     */
    public ResponseEntity<ApiResponse<?>> getParksForTariff(
        String tariffIdObfuscated,
        Boolean assigned,
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
        log.info("Fetching parks for tariff: {}, assigned: {}", tariffIdObfuscated, assigned);

        try {
            // Decode tariff ID
            Long tariffId = idObfuscator.decodeId(tariffIdObfuscated);

            // Verify tariff exists
            if (!tariffRepository.existsById(tariffId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Tariff not found", "TARIFF_NOT_FOUND")
                );
            }

            // Build specification - filter by tariff assignment
            Specification<Park> spec;
            if (assigned != null && !assigned) {
                // Get parks NOT assigned to this tariff
                spec = ParkSpecification.notByTariffId(tariffId);
            } else {
                // Get parks assigned to this tariff (default behavior)
                spec = ParkSpecification.byTariffId(tariffId);
            }

            // Add additional filters
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
            if (parkSize != null && !parkSize.isEmpty()) {
                spec = spec.and(ParkSpecification.sizeLike(parkSize));
            }
            if (isActive != null) {
                spec = spec.and(ParkSpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(ParkSpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));

            // Fetch parks
            Page<Park> parkPage = parkRepository.findAll(spec, pageable);

            // Convert to DTOs with notes (only include notes if assigned=true)
            boolean includeNotes = assigned == null || assigned;
            List<ParkWithNotesDTO> parkDTOs = parkPage.getContent().stream()
                .map(park -> convertParkToWithNotesDTO(park, includeNotes ? tariffId : null))
                .collect(Collectors.toList());

            // Response with pagination
            Map<String, Object> response = new HashMap<>();
            response.put("parks", parkDTOs);
            response.put("currentPage", parkPage.getNumber());
            response.put("totalItems", parkPage.getTotalElements());
            response.put("totalPages", parkPage.getTotalPages());

            return ResponseEntity.ok(ApiResponse.success(200, "Parks retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching parks for tariff", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to fetch parks", "FETCH_FAILED")
            );
        }
    }

    /**
     * Bulk upsert park-tariff relationships
     *
     * @param requests List of upsert operations
     * @return ResponseEntity with operation results
     */
    @Transactional
    @AuditLogAnnotation(action = "UPSERT_PARK_TARIFFS", description = "Bulk upsert park-tariff relationships", entityType = "ParkTariff")
    public ResponseEntity<ApiResponse<?>> upsertParkTariffs(List<ParkTariffUpsertDTO> requests) {
        log.info("Processing bulk upsert for {} park-tariff relationships", requests.size());

        ParkTariffUpsertResponseDTO response = new ParkTariffUpsertResponseDTO();
        response.setTotalProcessed(requests.size());

        for (ParkTariffUpsertDTO request : requests) {
            try {
                // Decode IDs
                Long parkId;
                Long tariffId;

                try {
                    parkId = idObfuscator.decodeId(request.getParkId());
                    tariffId = idObfuscator.decodeId(request.getTariffId());
                } catch (Exception e) {
                    response.addError("Invalid ID format for park: " + request.getParkId() + ", tariff: " + request.getTariffId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify park exists
                Park park = parkRepository.findById(parkId).orElse(null);
                if (park == null) {
                    response.addError("Park not found: " + request.getParkId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify tariff exists
                Tariff tariff = tariffRepository.findById(tariffId).orElse(null);
                if (tariff == null) {
                    response.addError("Tariff not found: " + request.getTariffId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                if (request.getStatus()) {
                    // Create or update relationship
                    ParkTariff parkTariff = parkTariffRepository.findByParkIdAndTariffId(parkId, tariffId).orElse(null);

                    if (parkTariff != null) {
                        // Update existing
                        parkTariff.setNotes(request.getNotes());
                        parkTariffRepository.save(parkTariff);
                        log.info("Updated park-tariff relationship: park={}, tariff={}", park.getName(), tariff.getName());
                    } else {
                        // Create new
                        parkTariff = ParkTariff.builder()
                            .park(park)
                            .tariff(tariff)
                            .notes(request.getNotes())
                            .build();
                        parkTariffRepository.save(parkTariff);
                        log.info("Created park-tariff relationship: park={}, tariff={}", park.getName(), tariff.getName());
                    }

                    response.setSuccessful(response.getSuccessful() + 1);

                } else {
                    // Delete relationship
                    ParkTariff parkTariff = parkTariffRepository.findByParkIdAndTariffId(parkId, tariffId).orElse(null);

                    if (parkTariff == null) {
                        response.addError("Relationship does not exist for park: " + park.getName() + ", tariff: " + tariff.getName());
                        response.setFailed(response.getFailed() + 1);
                    } else {
                        parkTariffRepository.delete(parkTariff);
                        log.info("Deleted park-tariff relationship: park={}, tariff={}", park.getName(), tariff.getName());
                        response.setSuccessful(response.getSuccessful() + 1);
                    }
                }

            } catch (Exception e) {
                log.error("Error processing park-tariff upsert", e);
                response.addError("Error processing relationship: " + e.getMessage());
                response.setFailed(response.getFailed() + 1);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Bulk upsert completed", response));
    }

    /**
     * Convert Tariff to DTO with park-specific notes
     */
    private TariffWithNotesDTO convertTariffToWithNotesDTO(Tariff tariff, Long parkId) {
        TariffWithNotesDTO dto = new TariffWithNotesDTO();
        dto.setId(idObfuscator.encodeId(tariff.getId()));
        dto.setName(tariff.getName());
        dto.setSlug(tariff.getSlug());
        dto.setChargingBasis(tariff.getChargingBasis());
        dto.setChargingBasisDisplayName(tariff.getChargingBasisDisplay());
        dto.setDescription(tariff.getDescription());
        dto.setRequiresAgeCategory(tariff.requiresAgeCategory());
        dto.setIsActive(tariff.getIsActive());
        dto.setIsSystem(tariff.getIsSystem());
        dto.setCreatedAt(tariff.getCreatedAt());
        dto.setUpdatedAt(tariff.getUpdatedAt());

        // Get notes from ParkTariff relationship (only if parkId is provided)
        if (parkId != null) {
            tariff.getParkTariffs().stream()
                .filter(pt -> pt.getPark().getId().equals(parkId))
                .findFirst()
                .ifPresent(pt -> dto.setNotes(pt.getNotes()));
        }

        return dto;
    }

    /**
     * Convert Park to DTO with tariff-specific notes
     */
    private ParkWithNotesDTO convertParkToWithNotesDTO(Park park, Long tariffId) {
        ParkWithNotesDTO dto = new ParkWithNotesDTO();
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

        // Get notes from ParkTariff relationship (only if tariffId is provided)
        if (tariffId != null) {
            park.getParkTariffs().stream()
                .filter(pt -> pt.getTariff().getId().equals(tariffId))
                .findFirst()
                .ifPresent(pt -> dto.setNotes(pt.getNotes()));
        }

        return dto;
    }
}
