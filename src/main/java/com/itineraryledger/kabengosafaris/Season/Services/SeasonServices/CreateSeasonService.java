package com.itineraryledger.kabengosafaris.Season.Services.SeasonServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs.CreateSeasonDTO;
import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs.SeasonDTO;
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
 * CreateSeasonService - Service for creating new seasons
 */
@Service
@Slf4j
public class CreateSeasonService {

    private final SeasonRepository seasonRepository;
    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateSeasonService(
        SeasonRepository seasonRepository,
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.seasonRepository = seasonRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new season (global or accommodation-specific)
     *
     * @param createDTO The season data
     * @return ResponseEntity with ApiResponse containing the created season
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_SEASON",
        description = "Creating a new season",
        entityType = "Season"
    )
    public ResponseEntity<ApiResponse<?>> createSeason(CreateSeasonDTO createDTO) {
        log.info("Creating new season: {}", createDTO.getName());

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Determine if this is a global or accommodation-specific season
            boolean isGlobal = createDTO.getAccommodationId() == null || createDTO.getAccommodationId().isBlank();
            Accommodation accommodation = null;
            Long accommodationId = null;

            if (!isGlobal) {
                // Decode accommodation ID
                try {
                    accommodationId = idObfuscator.decodeId(createDTO.getAccommodationId());
                } catch (Exception e) {
                    log.warn("Invalid accommodation ID: {}", createDTO.getAccommodationId());
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid accommodation ID",
                            "INVALID_ACCOMMODATION_ID"
                        )
                    );
                }

                // Check if accommodation exists
                accommodation = accommodationRepository.findById(accommodationId).orElse(null);
                if (accommodation == null) {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(
                            404,
                            "Accommodation not found",
                            "ACCOMMODATION_NOT_FOUND"
                        )
                    );
                }

                // Check for duplicate season name for this accommodation
                if (seasonRepository.existsByAccommodationIdAndName(accommodationId, createDTO.getName())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Season name already exists for this accommodation",
                            "DUPLICATE_SEASON_NAME"
                        )
                    );
                }
            } else {
                // Check for duplicate global season name
                if (seasonRepository.existsByIsGlobalTrueAndName(createDTO.getName())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Global season name already exists",
                            "DUPLICATE_GLOBAL_SEASON_NAME"
                        )
                    );
                }
            }

            // Create season entity
            Season season = Season.builder()
                .name(createDTO.getName())
                .seasonType(createDTO.getSeasonType())
                .description(createDTO.getDescription())
                .accommodation(accommodation)
                .isGlobal(isGlobal)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Add season periods if provided
            if (createDTO.getSeasonPeriods() != null && !createDTO.getSeasonPeriods().isEmpty()) {
                for (var periodDTO : createDTO.getSeasonPeriods()) {
                    SeasonPeriod period = SeasonPeriod.builder()
                        .season(season)
                        .startDate(periodDTO.getStartDate())
                        .endDate(periodDTO.getEndDate())
                        .year(periodDTO.getYear())
                        .notes(periodDTO.getNotes())
                        .isActive(periodDTO.getIsActive() != null ? periodDTO.getIsActive() : true)
                        .build();
                    season.addSeasonPeriod(period);
                }
            }

            // Save season
            season = seasonRepository.save(season);

            log.info("Season created successfully: {} ({})", season.getName(), isGlobal ? "Global" : "Accommodation-specific");

            // Convert to DTO
            SeasonDTO seasonDTO = convertToDTO(season);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Season created successfully",
                    seasonDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating season", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create season: " + e.getMessage(),
                    "SEASON_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for season creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreateSeasonDTO dto) {
        // Validate and sanitize name
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Season name cannot be empty", "INVALID_NAME")
            );
        }

        String trimmedName = dto.getName().trim();
        if (trimmedName.length() > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Season name cannot exceed 100 characters", "NAME_TOO_LONG")
            );
        }
        dto.setName(trimmedName);

        // Validate season periods if provided
        if (dto.getSeasonPeriods() != null && !dto.getSeasonPeriods().isEmpty()) {
            for (var period : dto.getSeasonPeriods()) {
                if (period.getStartDate() == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Season period start date is required", "MISSING_START_DATE")
                    );
                }
                if (period.getEndDate() == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Season period end date is required", "MISSING_END_DATE")
                    );
                }
                if (period.getYear() != null && (period.getYear() < 2000 || period.getYear() > 2100)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Season period year must be between 2000 and 2100", "INVALID_YEAR")
                    );
                }
            }
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
