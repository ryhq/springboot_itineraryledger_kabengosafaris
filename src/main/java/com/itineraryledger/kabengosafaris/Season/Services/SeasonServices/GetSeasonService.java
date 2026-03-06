package com.itineraryledger.kabengosafaris.Season.Services.SeasonServices;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs.SeasonDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.SeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GetSeasonService - Service for retrieving seasons with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class GetSeasonService {

    private final SeasonRepository seasonRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "seasonType", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    @Autowired
    public GetSeasonService(
        SeasonRepository seasonRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonRepository = seasonRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single season by obfuscated ID
     *
     * @param idObfuscated The obfuscated season ID
     * @return ResponseEntity with ApiResponse containing the season
     */
    public ResponseEntity<ApiResponse<?>> getSeasonById(String idObfuscated) {
        log.info("Fetching season with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode season ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid season ID",
                        "INVALID_SEASON_ID"
                    )
                );
            }

            // Find season
            Season season = seasonRepository.findById(id).orElse(null);
            if (season == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Season not found",
                        "SEASON_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            SeasonDTO seasonDTO = convertToDTO(season);

            // Navigation IDs
            Long nextId = seasonRepository.findNextId(id).orElse(null);
            Long previousId = seasonRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = seasonRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = seasonRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("season", seasonDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Season retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching season", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch season",
                    "SEASON_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all seasons with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param seasonType Filter by season type
     * @param isActive Filter by active status
     * @param isGlobal Filter by global status (true = global, false = accommodation-specific)
     * @param isSystem Filter by system status (true = system/protected, false = user-created)
     * @param accommodationId Filter by accommodation ID (for accommodation-specific seasons)
     * @param description Filter by description (partial match)
     * @param keyword Search keyword across name and description
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated seasons
     */
    public ResponseEntity<ApiResponse<?>> getAllSeasons(
        String name,
        Season.SeasonType seasonType,
        Boolean isActive,
        Boolean isGlobal,
        Boolean isSystem,
        String accommodationId,
        String description,
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all seasons with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }
            // Build specification for filtering
            Specification<Season> spec = Specification.unrestricted();

            if (name != null && !name.isEmpty()) {
                spec = spec.and(SeasonSpecification.nameLike(name));
            }
            if (seasonType != null) {
                spec = spec.and(SeasonSpecification.hasSeasonType(seasonType));
            }
            if (isActive != null) {
                spec = spec.and(SeasonSpecification.isActive(isActive));
            }
            if (isGlobal != null) {
                spec = spec.and(SeasonSpecification.isGlobal(isGlobal));
            }
            if (isSystem != null) {
                spec = spec.and(SeasonSpecification.isSystem(isSystem));
            }
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    Long decodedAccommodationId = idObfuscator.decodeId(accommodationId);
                    spec = spec.and(SeasonSpecification.hasAccommodationId(decodedAccommodationId));
                } catch (Exception e) {
                    log.warn("Failed to decode accommodation ID: {}", accommodationId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid accommodation ID",
                            "INVALID_ACCOMMODATION_ID"
                        )
                    );
                }
            }
            if (description != null && !description.isEmpty()) {
                spec = spec.and(SeasonSpecification.descriptionLike(description));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(SeasonSpecification.searchKeyword(keyword));
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
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch seasons
            Page<Season> seasonPage = seasonRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<SeasonDTO> seasonDTOs = seasonPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("seasons", seasonDTOs);
            response.put("currentPage", seasonPage.getNumber());
            response.put("totalItems", seasonPage.getTotalElements());
            response.put("totalPages", seasonPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Seasons retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching seasons", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch seasons",
                    "SEASONS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get unique seasons based on season type
     * Returns one season per unique season type, sorted by name
     * This is useful for dropdowns where users select existing season type configurations
     *
     * @return ResponseEntity with ApiResponse containing list of unique seasons
     */
    public ResponseEntity<ApiResponse<?>> getUniqueSeasons() {
        log.info("Fetching unique seasons by type");

        try {
            // Fetch unique seasons from repository
            List<Season> uniqueSeasons = seasonRepository.findUniqueSeasonsByType();

            // Convert to DTOs
            List<SeasonDTO> dtos = uniqueSeasons.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Unique seasons retrieved successfully",
                    dtos
                )
            );

        } catch (Exception e) {
            log.error("Error fetching unique seasons", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch unique seasons",
                    "UNIQUE_SEASONS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert Season entity to DTO
     */
    private SeasonDTO convertToDTO(Season season) {
        List<SeasonPeriodDTO> periodDTOs = new ArrayList<>();
        if (season.getSeasonPeriods() != null) {
            periodDTOs = season.getSeasonPeriods().stream()
                .map(this::convertPeriodToDTO)
                .collect(Collectors.toList());
        }

        return SeasonDTO.builder()
            .id(idObfuscator.encodeId(season.getId()))
            .name(season.getName())
            .seasonType(season.getSeasonType())
            .seasonTypeDisplayName(season.getSeasonType() != null ? season.getSeasonType().getDisplayName() : null)
            .seasonTypeDescription(season.getSeasonType() != null ? season.getSeasonType().getDescription() : null)
            .description(season.getDescription())
            .isGlobal(season.getIsGlobal())
            .isActive(season.getIsActive())
            .isSystem(season.getIsSystem())
            .accommodationId(season.getAccommodation() != null ? idObfuscator.encodeId(season.getAccommodation().getId()) : null)
            .accommodationName(season.getAccommodation() != null ? season.getAccommodation().getName() : null)
            .seasonPeriods(periodDTOs)
            .createdAt(season.getCreatedAt())
            .updatedAt(season.getUpdatedAt())
            .build();
    }

    /**
     * Convert SeasonPeriod entity to DTO
     */
    private SeasonPeriodDTO convertPeriodToDTO(SeasonPeriod period) {
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
