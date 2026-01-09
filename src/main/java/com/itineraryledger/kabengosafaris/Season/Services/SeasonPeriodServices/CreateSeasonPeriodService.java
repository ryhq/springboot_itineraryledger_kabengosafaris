package com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.CreateSeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.SeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CreateSeasonPeriodService - Service for creating new season periods
 */
@Service
@Slf4j
public class CreateSeasonPeriodService {

    private final SeasonPeriodRepository seasonPeriodRepository;
    private final SeasonRepository seasonRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateSeasonPeriodService(
        SeasonPeriodRepository seasonPeriodRepository,
        SeasonRepository seasonRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonPeriodRepository = seasonPeriodRepository;
        this.seasonRepository = seasonRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new season period
     *
     * @param createDTO The season period data
     * @return ResponseEntity with ApiResponse containing the created season period
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_SEASON_PERIOD",
        description = "Creating a new season period",
        entityType = "SeasonPeriod"
    )
    public ResponseEntity<ApiResponse<?>> createSeasonPeriod(CreateSeasonPeriodDTO createDTO) {
        log.info("Creating new season period for season: {}", createDTO.getSeasonId());

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Decode and validate season ID
            Long seasonId;
            try {
                seasonId = idObfuscator.decodeId(createDTO.getSeasonId());
            } catch (Exception e) {
                log.warn("Invalid season ID: {}", createDTO.getSeasonId());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid season ID",
                        "INVALID_SEASON_ID"
                    )
                );
            }

            // Check if season exists
            Season season = seasonRepository.findById(seasonId).orElse(null);
            if (season == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Season not found",
                        "SEASON_NOT_FOUND"
                    )
                );
            }

            // Check for overlapping periods
            ResponseEntity<ApiResponse<?>> overlapError = checkForOverlappingPeriods(
                season,
                createDTO.getStartDate(),
                createDTO.getEndDate(),
                createDTO.getYear(),
                null // No existing period ID to exclude
            );
            if (overlapError != null) {
                return overlapError;
            }

            // Create season period entity
            SeasonPeriod seasonPeriod = SeasonPeriod.builder()
                .season(season)
                .startDate(createDTO.getStartDate())
                .endDate(createDTO.getEndDate())
                .year(createDTO.getYear())
                .notes(createDTO.getNotes())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save season period
            seasonPeriod = seasonPeriodRepository.save(seasonPeriod);

            log.info("Season period created successfully for season: {}", season.getName());

            // Convert to DTO
            SeasonPeriodDTO seasonPeriodDTO = convertToDTO(seasonPeriod);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Season period created successfully",
                    seasonPeriodDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating season period", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create season period: " + e.getMessage(),
                    "SEASON_PERIOD_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for season period creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreateSeasonPeriodDTO dto) {
        // Validate season ID
        if (dto.getSeasonId() == null || dto.getSeasonId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Season ID is required", "MISSING_SEASON_ID")
            );
        }

        // Validate start date
        if (dto.getStartDate() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Start date is required", "MISSING_START_DATE")
            );
        }

        // Validate end date
        if (dto.getEndDate() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "End date is required", "MISSING_END_DATE")
            );
        }

        // Validate year if provided
        if (dto.getYear() != null && (dto.getYear() < 2000 || dto.getYear() > 2100)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Year must be between 2000 and 2100", "INVALID_YEAR")
            );
        }

        return null; // No validation errors
    }

    /**
     * Check for overlapping periods in the same season
     *
     * Overlapping logic:
     * Two periods overlap if they share the same year (or both are recurring) AND
     * their date ranges intersect.
     *
     * Date range intersection: !(a.endDate < b.startDate) && !(a.startDate > b.endDate)
     * This handles both normal and year-wrapping periods.
     *
     * @param season The season to check within
     * @param startDate New period's start date
     * @param endDate New period's end date
     * @param year New period's year (null for recurring)
     * @param excludePeriodId Period ID to exclude from check (for updates)
     * @return ResponseEntity with error if overlap found, null otherwise
     */
    private ResponseEntity<ApiResponse<?>> checkForOverlappingPeriods(
        Season season,
        java.time.MonthDay startDate,
        java.time.MonthDay endDate,
        Integer year,
        Long excludePeriodId
    ) {
        // Get all periods for this season
        List<SeasonPeriod> existingPeriods = season.getSeasonPeriods();

        for (SeasonPeriod existing : existingPeriods) {
            // Skip the period being updated
            if (excludePeriodId != null && existing.getId().equals(excludePeriodId)) {
                continue;
            }

            // Skip inactive periods (they don't count for overlap)
            if (existing.getIsActive() == null || !existing.getIsActive()) {
                continue;
            }

            // Check if years match or both are recurring
            boolean yearMatches = (year == null && existing.getYear() == null) ||
                                 (year != null && year.equals(existing.getYear()));

            if (!yearMatches) {
                continue; // Different years, no overlap possible
            }

            // Check date range overlap
            // Periods overlap if: !(a.endDate < b.startDate) && !(a.startDate > b.endDate)
            boolean overlaps = !endDate.isBefore(existing.getStartDate()) &&
                              !startDate.isAfter(existing.getEndDate());

            if (overlaps) {
                String yearInfo = year != null ? " for year " + year : " (recurring period)";
                String existingPeriodInfo = existing.getStartDate() + " to " + existing.getEndDate();

                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Season period overlaps with an existing period" + yearInfo +
                        ". Existing period: " + existingPeriodInfo,
                        "OVERLAPPING_PERIOD"
                    )
                );
            }
        }

        return null; // No overlap found
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
