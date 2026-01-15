package com.itineraryledger.kabengosafaris.Itinerary.Services;

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

import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Specifications.ItinerarySpecification;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryGetService - Service for retrieving itineraries with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryGetService {

    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryGetService(
        ItineraryRepository itineraryRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single itinerary by obfuscated ID
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing the itinerary
     */
    public ResponseEntity<ApiResponse<?>> getItineraryById(String idObfuscated) {
        log.info("Fetching itinerary with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid itinerary ID",
                        "INVALID_ITINERARY_ID"
                    )
                );
            }

            // Find itinerary
            Itinerary itinerary = itineraryRepository.findById(id).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Itinerary not found",
                        "ITINERARY_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            ItineraryDTO itineraryDTO = convertToDTO(itinerary);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Itinerary retrieved successfully",
                    itineraryDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch itinerary",
                    "ITINERARY_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get a single itinerary by code
     *
     * @param code The itinerary code
     * @return ResponseEntity with ApiResponse containing the itinerary
     */
    public ResponseEntity<ApiResponse<?>> getItineraryByCode(String code) {
        log.info("Fetching itinerary with code: {}", code);

        try {
            // Find itinerary
            Itinerary itinerary = itineraryRepository.findByCodeIgnoreCase(code).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Itinerary not found",
                        "ITINERARY_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            ItineraryDTO itineraryDTO = convertToDTO(itinerary);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Itinerary retrieved successfully",
                    itineraryDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary by code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch itinerary",
                    "ITINERARY_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all itineraries with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param code Filter by code (partial match)
     * @param status Filter by status
     * @param tripType Filter by trip type
     * @param budgetCategory Filter by budget category
     * @param startLocation Filter by start location
     * @param endLocation Filter by end location
     * @param totalDays Filter by total days
     * @param isActive Filter by active status
     * @param isDayTrip Filter by day trip (totalDays == 1 && totalNights == 0)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated itineraries
     */
    public ResponseEntity<ApiResponse<?>> getAllItineraries(
        String name,
        String code,
        ItineraryStatus status,
        TripType tripType,
        BudgetCategory budgetCategory,
        String startLocation,
        String endLocation,
        Integer totalDays,
        Boolean isActive,
        Boolean isDayTrip,
        String keyword,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching all itineraries with filters");

        try {
            // Build specification for filtering
            Specification<Itinerary> spec = Specification.unrestricted();

            if (name != null && !name.isEmpty()) {
                spec = spec.and(ItinerarySpecification.nameLike(name));
            }
            if (code != null && !code.isEmpty()) {
                spec = spec.and(ItinerarySpecification.codeLike(code));
            }
            if (status != null) {
                spec = spec.and(ItinerarySpecification.hasStatus(status));
            }
            if (tripType != null) {
                spec = spec.and(ItinerarySpecification.hasTripType(tripType));
            }
            if (budgetCategory != null) {
                spec = spec.and(ItinerarySpecification.hasBudgetCategory(budgetCategory));
            }
            if (startLocation != null && !startLocation.isEmpty()) {
                spec = spec.and(ItinerarySpecification.startLocationLike(startLocation));
            }
            if (endLocation != null && !endLocation.isEmpty()) {
                spec = spec.and(ItinerarySpecification.endLocationLike(endLocation));
            }
            if (totalDays != null) {
                spec = spec.and(ItinerarySpecification.hasTotalDays(totalDays));
            }
            if (isActive != null) {
                spec = spec.and(ItinerarySpecification.isActive(isActive));
            }
            if (isDayTrip != null) {
                spec = spec.and(ItinerarySpecification.isDayTrip(isDayTrip));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(ItinerarySpecification.searchKeyword(keyword));
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

            // Fetch itineraries
            Page<Itinerary> itineraryPage = itineraryRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<ItineraryDTO> itineraryDTOs = itineraryPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("itineraries", itineraryDTOs);
            response.put("currentPage", itineraryPage.getNumber());
            response.put("totalItems", itineraryPage.getTotalElements());
            response.put("totalPages", itineraryPage.getTotalPages());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Itineraries retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching itineraries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch itineraries",
                    "ITINERARIES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert Itinerary entity to ItineraryDTO
     */
    private ItineraryDTO convertToDTO(Itinerary itinerary) {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(idObfuscator.encodeId(itinerary.getId()));
        dto.setName(itinerary.getName());
        dto.setCode(itinerary.getCode());
        dto.setStatus(itinerary.getStatus());
        dto.setStatusDisplayName(itinerary.getStatus().getDisplayName());
        dto.setTripType(itinerary.getTripType());
        dto.setTripTypeDisplayName(itinerary.getTripType() != null ? itinerary.getTripType().getDisplayName() : null);
        dto.setTripTypeDescription(itinerary.getTripType() != null ? itinerary.getTripType().getDescription() : null);
        dto.setBudgetCategory(itinerary.getBudgetCategory());
        dto.setBudgetCategoryDisplayName(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDisplayName() : null);
        dto.setBudgetCategoryDescription(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDescription() : null);
        dto.setBudgetCategoryTier(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getTier() : null);
        dto.setTotalDays(itinerary.getTotalDays());
        dto.setTotalNights(itinerary.getTotalNights());
        dto.setIsDayTrip(itinerary.getTotalDays() == 1 && itinerary.getTotalNights() == 0);
        dto.setCarCount(itinerary.getCarCount());
        dto.setDescription(itinerary.getDescription());
        dto.setHighlights(itinerary.getHighlights());
        dto.setStartLocation(itinerary.getStartLocation());
        dto.setEndLocation(itinerary.getEndLocation());
        dto.setIsActive(itinerary.getIsActive());
        dto.setTotalPaxCount(itinerary.getTotalPaxCount());
        dto.setTotalDaysCount(itinerary.getDays() != null ? itinerary.getDays().size() : 0);
        dto.setCreatedAt(itinerary.getCreatedAt());
        dto.setUpdatedAt(itinerary.getUpdatedAt());
        return dto;
    }
}
