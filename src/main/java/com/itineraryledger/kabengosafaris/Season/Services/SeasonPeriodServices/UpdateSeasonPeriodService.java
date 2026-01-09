package com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.SeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.UpdateSeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.MonthDay;
import java.util.List;

/**
 * UpdateSeasonPeriodService - Service for updating season periods
 */
@Service
@Slf4j
@Transactional
public class UpdateSeasonPeriodService {

    private final SeasonPeriodRepository seasonPeriodRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateSeasonPeriodService(
        SeasonPeriodRepository seasonPeriodRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonPeriodRepository = seasonPeriodRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a season period by obfuscated ID
     *
     * @param idObfuscated The obfuscated season period ID
     * @param updateDTO The updated season period data
     * @return ResponseEntity with ApiResponse containing the updated season period
     */
    @AuditLogAnnotation(
        action = "UPDATE_SEASON_PERIOD",
        description = "Updating season period",
        entityType = "SeasonPeriod",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateSeasonPeriod(String idObfuscated, UpdateSeasonPeriodDTO updateDTO) {
        log.info("Updating season period with ID: {}", idObfuscated);

        try {
            // Validate that at least one field is provided for update
            ResponseEntity<ApiResponse<?>> validationError = validateAtLeastOneFieldProvided(updateDTO);
            if (validationError != null) {
                return validationError;
            }

            // Validate input fields
            validationError = validateInputFields(updateDTO);
            if (validationError != null) {
                return validationError;
            }

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

            return updateSeasonPeriodById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating season period", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update season period",
                    "SEASON_PERIOD_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update a season period by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateSeasonPeriodById(Long id, UpdateSeasonPeriodDTO updateDTO) {
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

        // Determine the final values after update (use existing if not updated)
        MonthDay finalStartDate = updateDTO.getStartDate() != null ? updateDTO.getStartDate() : seasonPeriod.getStartDate();
        MonthDay finalEndDate = updateDTO.getEndDate() != null ? updateDTO.getEndDate() : seasonPeriod.getEndDate();
        Integer finalYear = updateDTO.getYear() != null ? updateDTO.getYear() : seasonPeriod.getYear();

        // Check for overlapping periods (exclude current period from check)
        ResponseEntity<ApiResponse<?>> overlapError = checkForOverlappingPeriods(
            seasonPeriod.getSeason(),
            finalStartDate,
            finalEndDate,
            finalYear,
            id // Exclude current period from overlap check
        );
        if (overlapError != null) {
            return overlapError;
        }

        // Update fields if provided
        if (updateDTO.getStartDate() != null) {
            seasonPeriod.setStartDate(updateDTO.getStartDate());
        }
        if (updateDTO.getEndDate() != null) {
            seasonPeriod.setEndDate(updateDTO.getEndDate());
        }
        if (updateDTO.getYear() != null) {
            seasonPeriod.setYear(updateDTO.getYear());
        }
        if (updateDTO.getNotes() != null) {
            seasonPeriod.setNotes(updateDTO.getNotes());
        }
        if (updateDTO.getIsActive() != null) {
            seasonPeriod.setIsActive(updateDTO.getIsActive());
        }

        // Save updated season period
        seasonPeriod = seasonPeriodRepository.save(seasonPeriod);

        // Convert to DTO
        SeasonPeriodDTO seasonPeriodDTO = convertToDTO(seasonPeriod);

        log.info("Season period updated successfully for season: {}", seasonPeriod.getSeason().getName());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Season period updated successfully",
                seasonPeriodDTO
            )
        );
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateSeasonPeriodDTO dto) {
        boolean hasUpdate =
            dto.getStartDate() != null ||
            dto.getEndDate() != null ||
            dto.getYear() != null ||
            dto.getNotes() != null ||
            dto.getIsActive() != null;

        if (!hasUpdate) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
            );
        }

        return null;
    }

    /**
     * Validate input fields for season period update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateSeasonPeriodDTO dto) {
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
        MonthDay startDate,
        MonthDay endDate,
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
