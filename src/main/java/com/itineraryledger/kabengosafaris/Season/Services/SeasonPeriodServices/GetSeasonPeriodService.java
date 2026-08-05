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
import java.util.Arrays;
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

    // filter-aware prev/next + the N of M readout, and declarative counters
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private final SeasonPeriodRepository seasonPeriodRepository;
    private final IdObfuscator idObfuscator;

    /*
     * A list of date ranges is only useful in date order, so startDate and endDate
     * are sortable and startDate is the default — sorting periods by creation time
     * tells you nothing about when they apply.
     */
    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "startDate", "endDate", "year", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "startDate";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

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
        return getSeasonPeriodById(idObfuscated, null, null, null, null, null, null, null, null);
    }

    /**
     * One period, plus where it sits in the set the caller was looking at.
     *
     * The filters are accepted here because paging out of a filtered list must stay
     * inside that filter, and N of M must count that same set.
     */
    public ResponseEntity<ApiResponse<?>> getSeasonPeriodById(
        String idObfuscated,
        String seasonId,
        Boolean isActive,
        Boolean isSystem,
        Integer year,
        Boolean isRecurring,
        String notes,
        String sortBy,
        String sortDirection
    ) {
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

            /*
             * Prev/next walks the SAME filtered, sorted set the list showed. The old
             * findNextId/findPreviousId pair walked raw id order over every period in
             * the system, so paging out of one season landed in another's dates.
             */
            Long decodedSeasonId = null;
            if (seasonId != null && !seasonId.isBlank()) {
                try {
                    decodedSeasonId = idObfuscator.decodeId(seasonId);
                } catch (Exception ex) {
                    log.warn("Invalid seasonId scope: {}, paging across every period", seasonId);
                }
            }
            Specification<SeasonPeriod> navSpec = buildSpec(
                decodedSeasonId, isActive, isSystem, year, isRecurring, null, null, notes
            );
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            boolean navAscending = sortDirection == null || sortDirection.equalsIgnoreCase("asc");
            Map<String, Object> nav = recordNavigation.navigate(
                SeasonPeriod.class, navSpec, navSortBy, navAscending, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("seasonPeriod", seasonPeriodDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Season period retrieved successfully",
                    response
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
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all season periods with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }
            Long decodedSeasonId = null;
            if (seasonId != null && !seasonId.isBlank()) {
                try {
                    decodedSeasonId = idObfuscator.decodeId(seasonId);
                } catch (Exception e) {
                    log.warn("Failed to decode season ID: {}", seasonId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid season ID", "INVALID_SEASON_ID")
                    );
                }
            }
            Specification<SeasonPeriod> spec = buildSpec(
                decodedSeasonId, isActive, isSystem, year, isRecurring, startDate, endDate, notes
            );

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set default sorting (by startDate)
            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

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
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

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

    /**
     * The ONE place a period filter is expressed — rows, counters and prev/next all
     * build from this, so a card can never disagree with the table and the arrows
     * can never walk a different set from the one on screen.
     */
    private Specification<SeasonPeriod> buildSpec(
        Long seasonId,
        Boolean isActive,
        Boolean isSystem,
        Integer year,
        Boolean isRecurring,
        MonthDay startDate,
        MonthDay endDate,
        String notes
    ) {
        Specification<SeasonPeriod> spec = Specification.unrestricted();
        if (seasonId != null) spec = spec.and(SeasonPeriodSpecification.hasSeasonId(seasonId));
        if (isActive != null) spec = spec.and(SeasonPeriodSpecification.isActive(isActive));
        if (isSystem != null) spec = spec.and(SeasonPeriodSpecification.isSystem(isSystem));
        if (isRecurring != null) {
            spec = spec.and(isRecurring
                ? SeasonPeriodSpecification.isRecurring()
                : SeasonPeriodSpecification.isNonRecurring());
        }
        if (year != null) spec = spec.and(SeasonPeriodSpecification.hasYear(year));
        if (startDate != null) spec = spec.and(SeasonPeriodSpecification.hasStartDate(startDate));
        if (endDate != null) spec = spec.and(SeasonPeriodSpecification.hasEndDate(endDate));
        if (notes != null && !notes.isBlank()) spec = spec.and(SeasonPeriodSpecification.notesLike(notes));
        return spec;
    }

    /** Counters built from the SAME Specification as the rows. */
    private Map<String, Object> computeStats(Specification<SeasonPeriod> base) {
        return listStats.of(SeasonPeriod.class, base)
            .total()
            .count("active", SeasonPeriodSpecification.isActive(true))
            .complement("inactive", "active")
            .count("recurring", SeasonPeriodSpecification.isRecurring())
            .complement("fixedYear", "recurring")
            .count("system", SeasonPeriodSpecification.isSystem(true))
            .count("wrapsYear", SeasonPeriodSpecification.wrapsTheYear())
            .recency(SeasonPeriodSpecification::createdAfter)
            .build();
    }
}
