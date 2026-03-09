package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.Specifications.SafariSpecification;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SafariGetService - Service for retrieving safaris with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "code", "slug", "startDate", "endDate", "totalDays", "totalNights", "state", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariGetService(
            SafariRepository safariRepository,
            IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single safari by obfuscated ID
     */
    public ResponseEntity<ApiResponse<?>> getSafariById(String idObfuscated) {
        log.info("Fetching safari with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode safari ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            Safari safari = safariRepository.findById(id).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            SafariDTO safariDTO = convertToDTO(safari);

            // Build navigation
            Long nextId = safariRepository.findNextId(id).orElse(null);
            Long previousId = safariRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = safariRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = safariRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("safari", safariDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch safari", "SAFARI_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single safari by code
     */
    public ResponseEntity<ApiResponse<?>> getSafariByCode(String code) {
        log.info("Fetching safari with code: {}", code);

        try {
            Safari safari = safariRepository.findByCode(code).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            SafariDTO safariDTO = convertToDTO(safari);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari retrieved successfully", safariDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching safari by code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch safari", "SAFARI_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all safaris with pagination, sorting, and filtering
     */
    public ResponseEntity<ApiResponse<?>> getAllSafaris(
            String name,
            String code,
            SafariState state,
            SafariPhase phase,
            String startLocation,
            String endLocation,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            Boolean isActive,
            String keyword,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching all safaris with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Specification<Safari> spec = Specification.unrestricted();

            if (name != null && !name.isEmpty()) {
                spec = spec.and(SafariSpecification.nameLike(name));
            }
            if (code != null && !code.isEmpty()) {
                spec = spec.and(SafariSpecification.codeLike(code));
            }
            if (state != null) {
                spec = spec.and(SafariSpecification.hasState(state));
            }
            if (startLocation != null && !startLocation.isEmpty()) {
                spec = spec.and(SafariSpecification.startLocationLike(startLocation));
            }
            if (endLocation != null && !endLocation.isEmpty()) {
                spec = spec.and(SafariSpecification.endLocationLike(endLocation));
            }
            if (startDateFrom != null) {
                spec = spec.and(SafariSpecification.startDateAfter(startDateFrom));
            }
            if (startDateTo != null) {
                spec = spec.and(SafariSpecification.startDateBefore(startDateTo));
            }
            if (isActive != null) {
                spec = spec.and(SafariSpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(SafariSpecification.searchKeyword(keyword));
            }

            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            Page<Safari> safariPage = safariRepository.findAll(spec, pageable);

            // Filter by phase if specified (phase is calculated, not stored in DB)
            List<SafariDTO> safariDTOs;
            long totalFiltered;
            if (phase != null) {
                List<Safari> filteredSafaris = safariPage.getContent().stream()
                        .filter(safari -> safari.getCurrentPhase() == phase)
                        .collect(Collectors.toList());
                safariDTOs = filteredSafaris.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
                totalFiltered = filteredSafaris.size();
            } else {
                safariDTOs = safariPage.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
                totalFiltered = safariPage.getTotalElements();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("safaris", safariDTOs);
            response.put("currentPage", safariPage.getNumber());
            response.put("totalItems", phase != null ? totalFiltered : safariPage.getTotalElements());
            response.put("totalPages", safariPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (phase != null) {
                response.put("filteredByPhase", phase.name());
            }

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safaris retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching safaris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch safaris", "SAFARIS_FETCH_FAILED")
            );
        }
    }

    /**
     * Validate and return the sort field, or null if invalid
     */
    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert Safari entity to SafariDTO
     */
    private SafariDTO convertToDTO(Safari safari) {
        SafariDTO dto = new SafariDTO();
        dto.setId(idObfuscator.encodeId(safari.getId()));
        dto.setName(safari.getName());
        dto.setCode(safari.getCode());
        dto.setSlug(safari.getSlug());

        if (safari.getItinerary() != null) {
            dto.setItineraryId(idObfuscator.encodeId(safari.getItinerary().getId()));
            dto.setItineraryName(safari.getItinerary().getName());
            dto.setItineraryCode(safari.getItinerary().getCode());
        }

        // Customer information
        if (safari.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(safari.getCustomer().getId()));
            dto.setCustomerCode(safari.getCustomer().getCode());

            // Build customer name based on customer type
            String customerName = switch (safari.getCustomer().getCustomerType()) {
                case INDIVIDUAL -> {
                    String firstName = safari.getCustomer().getFirstName() != null ? safari.getCustomer().getFirstName() : "";
                    String lastName = safari.getCustomer().getLastName() != null ? safari.getCustomer().getLastName() : "";
                    yield (firstName + " " + lastName).trim();
                }
                case CORPORATE, TRAVEL_AGENT -> safari.getCustomer().getCompanyName() != null ? safari.getCustomer().getCompanyName() : "";
            };
            dto.setCustomerName(customerName);
        }

        // State information (booking/operational)
        dto.setState(safari.getState());
        dto.setStateDisplayName(safari.getState().getDisplayName());
        dto.setStateDescription(safari.getState().getDescription());
        dto.setStateReason(safari.getStateReason());
        dto.setStateChangedAt(safari.getStateChangedAt());

        // Phase information (time-based)
        var phase = safari.getCurrentPhase();
        dto.setPhase(phase);
        dto.setPhaseDisplayName(phase.getDisplayName());
        dto.setPhaseDescription(phase.getDescription());
        dto.setPhaseUrgencyLevel(phase.getUrgencyLevel());
        dto.setPhaseColorCode(phase.getColorCode());

        dto.setStartDate(safari.getStartDate());
        dto.setEndDate(safari.getEndDate());

        dto.setTotalDays(safari.getTotalDays());
        dto.setTotalNights(safari.getTotalNights());
        dto.setCarCount(safari.getCarCount());

        dto.setDescription(safari.getDescription());
        dto.setHighlights(safari.getHighlights());
        dto.setStartLocation(safari.getStartLocation());
        dto.setEndLocation(safari.getEndLocation());

        dto.setSpecialRequests(safari.getSpecialRequests());
        dto.setDietaryRequirements(safari.getDietaryRequirements());
        dto.setEmergencyContact(safari.getEmergencyContact());

        dto.setIsActive(safari.getIsActive());
        dto.setIsEditable(safari.isEditable());
        dto.setIsCancellable(safari.isCancellable());
        dto.setHasStarted(safari.hasStarted());
        dto.setHasEnded(safari.hasEnded());
        dto.setIsInProgress(safari.isInProgress());
        dto.setIsUrgentPhase(safari.isUrgentPhase());

        // Time calculations
        dto.setDaysUntilStart(safari.getDaysUntilStart());
        dto.setDaysSinceEnd(safari.getDaysSinceEnd());
        dto.setCurrentDayNumber(safari.getCurrentDayNumber());

        dto.setTotalPaxCount(safari.getTotalPaxCount());
        dto.setTotalDaysCount(safari.getDays() != null ? safari.getDays().size() : 0);

        // Audit information - Created By
        if (safari.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(safari.getCreatedBy().getId()));
            dto.setCreatedByUsername(safari.getCreatedBy().getUsername());
            String createdByFullName = (safari.getCreatedBy().getFirstName() + " " + safari.getCreatedBy().getLastName()).trim();
            dto.setCreatedByFullName(createdByFullName);
        }

        // Audit information - Updated By
        if (safari.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(safari.getUpdatedBy().getId()));
            dto.setUpdatedByUsername(safari.getUpdatedBy().getUsername());
            String updatedByFullName = (safari.getUpdatedBy().getFirstName() + " " + safari.getUpdatedBy().getLastName()).trim();
            dto.setUpdatedByFullName(updatedByFullName);
        }

        dto.setCreatedAt(safari.getCreatedAt());
        dto.setUpdatedAt(safari.getUpdatedAt());

        return dto;
    }
}
