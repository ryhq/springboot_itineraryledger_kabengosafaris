package com.itineraryledger.kabengosafaris.Season.Services.SeasonServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs.SeasonDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs.UpdateSeasonDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.SeasonPeriodDTO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UpdateSeasonService - Service for updating seasons
 */
@Service
@Slf4j
@Transactional
public class UpdateSeasonService {

    private final SeasonRepository seasonRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateSeasonService(
        SeasonRepository seasonRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonRepository = seasonRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a season by obfuscated ID
     *
     * @param idObfuscated The obfuscated season ID
     * @param updateDTO The updated season data
     * @return ResponseEntity with ApiResponse containing the updated season
     */
    @AuditLogAnnotation(
        action = "UPDATE_SEASON",
        description = "Updating season",
        entityType = "Season",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateSeason(String idObfuscated, UpdateSeasonDTO updateDTO) {
        log.info("Updating season with ID: {}", idObfuscated);

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
                log.warn("Failed to decode season ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid season ID",
                        "INVALID_SEASON_ID"
                    )
                );
            }

            return updateSeasonById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating season", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update season",
                    "SEASON_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update a season by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateSeasonById(Long id, UpdateSeasonDTO updateDTO) {
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

        // Check for duplicate name if name is being updated
        if (updateDTO.getName() != null && !updateDTO.getName().equals(season.getName())) {
            boolean isDuplicate;
            if (season.getIsGlobal()) {
                // Check global seasons
                isDuplicate = seasonRepository.findAll(
                    SeasonSpecification.isGlobal(true)
                        .and(SeasonSpecification.hasName(updateDTO.getName()))
                ).stream().anyMatch(s -> !s.getId().equals(id));
            } else {
                // Check accommodation-specific seasons
                isDuplicate = seasonRepository.existsByAccommodationIdAndName(
                    season.getAccommodation().getId(),
                    updateDTO.getName()
                ) && !season.getName().equals(updateDTO.getName());
            }

            if (isDuplicate) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        season.getIsGlobal() ? "Global season name already exists" : "Season name already exists for this accommodation",
                        "DUPLICATE_SEASON_NAME"
                    )
                );
            }
            season.setName(updateDTO.getName());
        }

        // Update other fields if provided
        if (updateDTO.getSeasonType() != null) {
            season.setSeasonType(updateDTO.getSeasonType());
        }
        if (updateDTO.getDescription() != null) {
            season.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getIsActive() != null) {
            season.setIsActive(updateDTO.getIsActive());
        }

        // Save updated season
        season = seasonRepository.save(season);

        // Convert to DTO
        SeasonDTO seasonDTO = convertToDTO(season);

        log.info("Season updated successfully: {}", season.getName());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Season updated successfully",
                seasonDTO
            )
        );
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateSeasonDTO dto) {
        boolean hasUpdate =
            dto.getName() != null ||
            dto.getSeasonType() != null ||
            dto.getDescription() != null ||
            dto.getIsActive() != null;

        if (!hasUpdate) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
            );
        }

        return null;
    }

    /**
     * Validate input fields for season update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateSeasonDTO dto) {
        // Validate and sanitize name if provided
        if (dto.getName() != null) {
            String trimmedName = dto.getName().trim();
            if (trimmedName.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Season name cannot be empty", "INVALID_NAME")
                );
            }
            if (trimmedName.length() > 100) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Season name cannot exceed 100 characters", "NAME_TOO_LONG")
                );
            }
            dto.setName(trimmedName);
        }

        return null; // No validation errors
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
