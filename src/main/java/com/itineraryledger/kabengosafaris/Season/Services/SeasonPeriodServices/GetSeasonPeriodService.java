package com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.SeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
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

import java.time.MonthDay;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GetSeasonPeriodService - Service for retrieving season periods with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class GetSeasonPeriodService {

    private final SeasonPeriodRepository seasonPeriodRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public GetSeasonPeriodService(
        SeasonPeriodRepository seasonPeriodRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonPeriodRepository = seasonPeriodRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single season period by obfuscated ID
     *
     * @param idObfuscated The obfuscated season period ID
     * @return ResponseEntity with ApiResponse containing the season period
     */
    public ResponseEntity<ApiResponse<?>> getSeasonPeriodById(String idObfuscated) {
        log.info("Fetching season period with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode season period ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid season period ID",
                        "INVALID_SEASON_PERIOD_ID"
                    )
                );
            }

            // Find season period
            SeasonPeriod seasonPeriod = seasonPeriodRepository.findById(id).orElse(null);
            if (seasonPeriod == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Season period not found",
                        "SEASON_PERIOD_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            SeasonPeriodDTO seasonPeriodDTO = convertToDTO(seasonPeriod);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Season period retrieved successfully",
                    seasonPeriodDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching season period", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch season period",
                    "SEASON_PERIOD_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all season periods with pagination, sorting, and filtering
     *
     * @param seasonId Filter by season ID (obfuscated)
     * @param isActive Filter by active status
     * @param isSystem Filter by system status (true = system/protected, false = user-created)
     * @param year Filter by specific year
     * @param isRecurring Filter recurring periods (null year)
     * @param startDate Filter by start date
     * @param endDate Filter by end date
     * @param notes Filter by notes (partial match)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated season periods
     */
    public ResponseEntity<ApiResponse<?>> getAllSeasonPeriods(
        String seasonId,
        Boolean isActive,
        Boolean isSystem,
        Integer year,
        Boolean isRecurring,
        MonthDay startDate,
        MonthDay endDate,
        String notes,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching all season periods with filters");

        try {
            // Build specification based on filters
            Specification<SeasonPeriod> spec = Specification.unrestricted();

            // Decode and apply season ID filter
            if (seasonId != null && !seasonId.isBlank()) {
                try {
                    Long decodedSeasonId = idObfuscator.decodeId(seasonId);
                    spec = spec.and(SeasonPeriodSpecification.hasSeasonId(decodedSeasonId));
                } catch (Exception e) {
                    log.warn("Failed to decode season ID: {}", seasonId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid season ID",
                            "INVALID_SEASON_ID"
                        )
                    );
                }
            }

            // Apply other filters
            if (isActive != null) {
                spec = spec.and(SeasonPeriodSpecification.isActive(isActive));
            }

            if (isSystem != null) {
                spec = spec.and(SeasonPeriodSpecification.isSystem(isSystem));
            }

            if (isRecurring != null && isRecurring) {
                spec = spec.and(SeasonPeriodSpecification.isRecurring());
            } else if (isRecurring != null && !isRecurring) {
                spec = spec.and(SeasonPeriodSpecification.isNonRecurring());
            }

            if (year != null) {
                spec = spec.and(SeasonPeriodSpecification.hasYear(year));
            }

            if (startDate != null) {
                spec = spec.and(SeasonPeriodSpecification.hasStartDate(startDate));
            }

            if (endDate != null) {
                spec = spec.and(SeasonPeriodSpecification.hasEndDate(endDate));
            }

            if (notes != null && !notes.isBlank()) {
                spec = spec.and(SeasonPeriodSpecification.notesLike(notes));
            }

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set default sorting (by startDate)
            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "startDate"));

            // Fetch season periods
            Page<SeasonPeriod> periodPage = seasonPeriodRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<SeasonPeriodDTO> periodDTOs = periodPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("seasonPeriods", periodDTOs);
            response.put("currentPage", periodPage.getNumber());
            response.put("totalItems", periodPage.getTotalElements());
            response.put("totalPages", periodPage.getTotalPages());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Season periods retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching all season periods", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch season periods",
                    "SEASON_PERIODS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert SeasonPeriod entity to DTO
     */
    private SeasonPeriodDTO convertToDTO(SeasonPeriod period) {
        return SeasonPeriodDTO.builder()
            .id(idObfuscator.encodeId(period.getId()))
            .seasonId(idObfuscator.encodeId(period.getSeason().getId()))
            .seasonName(period.getSeason().getName())
            .startDate(period.getStartDate())
            .endDate(period.getEndDate())
            .year(period.getYear())
            .notes(period.getNotes())
            .isActive(period.getIsActive())
            .isSystem(period.getIsSystem())
            .isRecurring(period.isRecurring())
            .isYearWrapping(period.isYearWrapping())
            .durationDays(period.getDurationDays())
            .createdAt(period.getCreatedAt())
            .updatedAt(period.getUpdatedAt())
            .build();
    }
}
