package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.AccommodationRateDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.AccommodationRateMatrixResponseDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.AccommodationRateMatrixResponseDTO.*;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AccommodationRateMatrixService - Service for fetching rate matrix data for UI
 *
 * Provides an endpoint that returns:
 * - Accommodation information
 * - List of accommodation-specific seasons (with exclusion support)
 * - List of room types (with exclusion support)
 * - List of room standards (with exclusion support)
 * - List of board types (with exclusion support)
 * - Existing rates filtered by the same exclusions
 *
 * The matrix for accommodation rates:
 * Season x RoomStandard x RoomType x BoardType | STO Rate | Rack Rate | Currency
 *
 * All four dimensions (Season, RoomType, RoomStandard, BoardType) are accommodation-specific,
 * not global like PaxAgeCategory and PaxNationCategory.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRateMatrixService {

    private final AccommodationRepository accommodationRepository;
    private final SeasonRepository seasonRepository;
    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final AccommodationRateRepository rateRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Get rate matrix data for an accommodation
     *
     * @param accommodationIdObfuscated The obfuscated accommodation ID (required)
     * @param seasonIdObfuscated Optional - filter by specific season
     * @param excludeSeasonIds List of obfuscated season IDs to exclude
     * @param excludeRoomTypeIds List of obfuscated room type IDs to exclude
     * @param excludeRoomStandardIds List of obfuscated room standard IDs to exclude
     * @param excludeBoardTypeIds List of obfuscated board type IDs to exclude
     * @return ResponseEntity with AccommodationRateMatrixResponseDTO
     */
    public ResponseEntity<ApiResponse<?>> getRateMatrix(
        String accommodationIdObfuscated,
        String seasonIdObfuscated,
        List<String> excludeSeasonIds,
        List<String> excludeRoomTypeIds,
        List<String> excludeRoomStandardIds,
        List<String> excludeBoardTypeIds
    ) {
        log.info("Fetching rate matrix for accommodation: {}", accommodationIdObfuscated);

        // Validate accommodation ID
        if (accommodationIdObfuscated == null || accommodationIdObfuscated.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Accommodation ID is required", "ACCOMMODATION_ID_REQUIRED")
            );
        }

        // Decode accommodation ID
        Long accommodationId;
        try {
            accommodationId = idObfuscator.decodeId(accommodationIdObfuscated);
            if (accommodationId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid accommodation ID format", "INVALID_ACCOMMODATION_ID")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid accommodation ID format", "INVALID_ACCOMMODATION_ID")
            );
        }

        // Fetch accommodation
        Optional<Accommodation> accommodationOpt = accommodationRepository.findById(accommodationId);
        if (accommodationOpt.isEmpty()) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
            );
        }
        Accommodation accommodation = accommodationOpt.get();

        // Decode exclusion IDs
        Set<Long> excludedSeasonIdsDecoded = decodeIds(excludeSeasonIds);
        Set<Long> excludedRoomTypeIdsDecoded = decodeIds(excludeRoomTypeIds);
        Set<Long> excludedRoomStandardIdsDecoded = decodeIds(excludeRoomStandardIds);
        Set<Long> excludedBoardTypeIdsDecoded = decodeIds(excludeBoardTypeIds);

        // Decode optional season filter
        Long seasonId = null;
        Season currentSeason = null;
        if (seasonIdObfuscated != null && !seasonIdObfuscated.trim().isEmpty()) {
            try {
                seasonId = idObfuscator.decodeId(seasonIdObfuscated);
                if (seasonId != null) {
                    Optional<Season> seasonOpt = seasonRepository.findById(seasonId);
                    if (seasonOpt.isPresent()) {
                        Season season = seasonOpt.get();
                        // Verify season belongs to this accommodation
                        if (season.getAccommodation() != null && season.getAccommodation().getId().equals(accommodationId)) {
                            currentSeason = season;
                        } else {
                            return ResponseEntity.badRequest().body(
                                ApiResponse.error(400,
                                    "Season '" + season.getName() + "' does not belong to accommodation '" + accommodation.getName() + "'",
                                    "SEASON_NOT_ASSOCIATED")
                            );
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to decode season ID: {}", seasonIdObfuscated);
            }
        }

        // Fetch accommodation-specific data (excluding specified ones)
        List<Season> seasons = fetchAccommodationSeasons(accommodationId, excludedSeasonIdsDecoded);
        List<AccommodationRoomType> roomTypes = fetchAccommodationRoomTypes(accommodationId, excludedRoomTypeIdsDecoded);
        List<AccommodationRoomStandard> roomStandards = fetchAccommodationRoomStandards(accommodationId, excludedRoomStandardIdsDecoded);
        List<AccommodationBoardType> boardTypes = fetchAccommodationBoardTypes(accommodationId, excludedBoardTypeIdsDecoded);

        // Validate all four dimensions are present
        if (seasons.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Accommodation '" + accommodation.getName() + "' has no seasons defined. Please create seasons first.",
                    "NO_SEASONS")
            );
        }
        if (roomTypes.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Accommodation '" + accommodation.getName() + "' has no room types defined. Please create room types first.",
                    "NO_ROOM_TYPES")
            );
        }
        if (roomStandards.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Accommodation '" + accommodation.getName() + "' has no room standards defined. Please create room standards first.",
                    "NO_ROOM_STANDARDS")
            );
        }
        if (boardTypes.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Accommodation '" + accommodation.getName() + "' has no board types defined. Please create board types first.",
                    "NO_BOARD_TYPES")
            );
        }

        // Fetch existing rates with exclusions applied
        List<AccommodationRate> existingRates = fetchExistingRates(
            accommodationId, seasonId,
            excludedSeasonIdsDecoded, excludedRoomTypeIdsDecoded,
            excludedRoomStandardIdsDecoded, excludedBoardTypeIdsDecoded
        );

        // Build response
        AccommodationRateMatrixResponseDTO response = buildResponse(
            accommodation, currentSeason, seasons, roomTypes, roomStandards, boardTypes, existingRates
        );

        return ResponseEntity.ok(ApiResponse.success(200, "Rate matrix retrieved successfully", response));
    }

    /**
     * Decode a list of obfuscated IDs, ignoring invalid ones
     */
    private Set<Long> decodeIds(List<String> obfuscatedIds) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
            return Set.of();
        }

        return obfuscatedIds.stream()
            .filter(id -> id != null && !id.trim().isEmpty())
            .map(id -> {
                try {
                    return idObfuscator.decodeId(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", id);
                    return null;
                }
            })
            .filter(id -> id != null)
            .collect(Collectors.toSet());
    }

    /**
     * Fetch accommodation-specific seasons, excluding specified IDs
     */
    private List<Season> fetchAccommodationSeasons(Long accommodationId, Set<Long> excludeIds) {
        return seasonRepository.findAll().stream()
            .filter(s -> s.getAccommodation() != null && s.getAccommodation().getId().equals(accommodationId))
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> !excludeIds.contains(s.getId()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch accommodation room types, excluding specified IDs
     */
    private List<AccommodationRoomType> fetchAccommodationRoomTypes(Long accommodationId, Set<Long> excludeIds) {
        return roomTypeRepository.findAll().stream()
            .filter(rt -> rt.getAccommodation().getId().equals(accommodationId))
            .filter(rt -> Boolean.TRUE.equals(rt.getIsActive()))
            .filter(rt -> !excludeIds.contains(rt.getId()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch accommodation room standards, excluding specified IDs
     */
    private List<AccommodationRoomStandard> fetchAccommodationRoomStandards(Long accommodationId, Set<Long> excludeIds) {
        return roomStandardRepository.findAll().stream()
            .filter(rs -> rs.getAccommodation().getId().equals(accommodationId))
            .filter(rs -> Boolean.TRUE.equals(rs.getIsActive()))
            .filter(rs -> !excludeIds.contains(rs.getId()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch accommodation board types, excluding specified IDs
     */
    private List<AccommodationBoardType> fetchAccommodationBoardTypes(Long accommodationId, Set<Long> excludeIds) {
        return boardTypeRepository.findAll().stream()
            .filter(bt -> bt.getAccommodation().getId().equals(accommodationId))
            .filter(bt -> Boolean.TRUE.equals(bt.getIsActive()))
            .filter(bt -> !excludeIds.contains(bt.getId()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch existing rates for the accommodation with exclusions applied
     */
    private List<AccommodationRate> fetchExistingRates(
        Long accommodationId, Long seasonId,
        Set<Long> excludedSeasonIds, Set<Long> excludedRoomTypeIds,
        Set<Long> excludedRoomStandardIds, Set<Long> excludedBoardTypeIds
    ) {
        // Build base specification
        Specification<AccommodationRate> spec = AccommodationRateSpecification.byAccommodationId(accommodationId);

        // Apply season filter if specified
        if (seasonId != null) {
            spec = spec.and(AccommodationRateSpecification.bySeasonId(seasonId));
        }

        // Fetch all matching rates
        List<AccommodationRate> rates = rateRepository.findAll(spec);

        // Apply exclusions in memory
        return rates.stream()
            .filter(r -> !excludedSeasonIds.contains(r.getSeason().getId()))
            .filter(r -> !excludedRoomTypeIds.contains(r.getRoomType().getId()))
            .filter(r -> !excludedRoomStandardIds.contains(r.getRoomStandard().getId()))
            .filter(r -> !excludedBoardTypeIds.contains(r.getBoardType().getId()))
            .collect(Collectors.toList());
    }

    /**
     * Build the response DTO
     */
    private AccommodationRateMatrixResponseDTO buildResponse(
        Accommodation accommodation,
        Season currentSeason,
        List<Season> seasons,
        List<AccommodationRoomType> roomTypes,
        List<AccommodationRoomStandard> roomStandards,
        List<AccommodationBoardType> boardTypes,
        List<AccommodationRate> existingRates
    ) {
        // Convert seasons to DTOs
        List<SeasonInfo> seasonInfos = seasons.stream()
            .map(s -> SeasonInfo.builder()
                .id(idObfuscator.encodeId(s.getId()))
                .name(s.getName())
                .seasonType(s.getSeasonType() != null ? s.getSeasonType().name() : null)
                .seasonTypeDisplayName(s.getSeasonType() != null ? s.getSeasonType().getDisplayName() : null)
                .isActive(s.getIsActive())
                .build())
            .collect(Collectors.toList());

        // Convert room types to DTOs
        List<RoomTypeInfo> roomTypeInfos = roomTypes.stream()
            .map(rt -> RoomTypeInfo.builder()
                .id(idObfuscator.encodeId(rt.getId()))
                .name(rt.getName())
                .bedConfiguration(rt.getBedConfiguration())
                .maxOccupancy(rt.getMaxOccupancy())
                .isActive(rt.getIsActive())
                .build())
            .collect(Collectors.toList());

        // Convert room standards to DTOs
        List<RoomStandardInfo> roomStandardInfos = roomStandards.stream()
            .map(rs -> RoomStandardInfo.builder()
                .id(idObfuscator.encodeId(rs.getId()))
                .name(rs.getName())
                .viewType(rs.getViewType())
                .isActive(rs.getIsActive())
                .build())
            .collect(Collectors.toList());

        // Convert board types to DTOs
        List<BoardTypeInfo> boardTypeInfos = boardTypes.stream()
            .map(bt -> BoardTypeInfo.builder()
                .id(idObfuscator.encodeId(bt.getId()))
                .name(bt.getName())
                .mealsIncluded(bt.getMealsIncluded())
                .breakfastIncluded(bt.getBreakfastIncluded())
                .lunchIncluded(bt.getLunchIncluded())
                .dinnerIncluded(bt.getDinnerIncluded())
                .isActive(bt.getIsActive())
                .build())
            .collect(Collectors.toList());

        // Convert existing rates to DTOs
        List<AccommodationRateDTO> rateDTOs = existingRates.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        // Calculate summary
        int totalPossibleRates = seasons.size() * roomTypes.size() * roomStandards.size() * boardTypes.size();

        MatrixSummary summary = MatrixSummary.builder()
            .totalPossibleRates(totalPossibleRates)
            .existingRatesCount(existingRates.size())
            .missingRatesCount(Math.max(0, totalPossibleRates - existingRates.size()))
            .seasonsCount(seasons.size())
            .roomTypesCount(roomTypes.size())
            .roomStandardsCount(roomStandards.size())
            .boardTypesCount(boardTypes.size())
            .build();

        // Build accommodation info
        AccommodationInfo accommodationInfo = AccommodationInfo.builder()
            .id(idObfuscator.encodeId(accommodation.getId()))
            .name(accommodation.getName())
            .accommodationType(accommodation.getAccommodationType() != null
                ? accommodation.getAccommodationType().name() : null)
            .category(accommodation.getCategory() != null
                ? accommodation.getCategory().name() : null)
            .isActive(accommodation.getIsActive())
            .build();

        // Build current season info (if filtering by season)
        SeasonInfo currentSeasonInfo = null;
        if (currentSeason != null) {
            currentSeasonInfo = SeasonInfo.builder()
                .id(idObfuscator.encodeId(currentSeason.getId()))
                .name(currentSeason.getName())
                .seasonType(currentSeason.getSeasonType() != null ? currentSeason.getSeasonType().name() : null)
                .seasonTypeDisplayName(currentSeason.getSeasonType() != null ? currentSeason.getSeasonType().getDisplayName() : null)
                .isActive(currentSeason.getIsActive())
                .build();
        }

        return AccommodationRateMatrixResponseDTO.builder()
            .accommodation(accommodationInfo)
            .currentSeason(currentSeasonInfo)
            .seasons(seasonInfos)
            .roomTypes(roomTypeInfos)
            .roomStandards(roomStandardInfos)
            .boardTypes(boardTypeInfos)
            .existingRates(rateDTOs)
            .summary(summary)
            .build();
    }

    /**
     * Convert AccommodationRate entity to DTO
     */
    private AccommodationRateDTO convertToDTO(AccommodationRate rate) {
        return AccommodationRateDTO.builder()
            .id(idObfuscator.encodeId(rate.getId()))
            .accommodationId(idObfuscator.encodeId(rate.getAccommodation().getId()))
            .accommodationName(rate.getAccommodation().getName())
            .seasonId(idObfuscator.encodeId(rate.getSeason().getId()))
            .seasonName(rate.getSeason().getName())
            .seasonType(rate.getSeason().getSeasonType() != null
                ? rate.getSeason().getSeasonType().getDisplayName() : null)
            .roomTypeId(idObfuscator.encodeId(rate.getRoomType().getId()))
            .roomTypeName(rate.getRoomType().getName())
            .bedConfiguration(rate.getRoomType().getBedConfiguration())
            .roomStandardId(idObfuscator.encodeId(rate.getRoomStandard().getId()))
            .roomStandardName(rate.getRoomStandard().getName())
            .boardTypeId(idObfuscator.encodeId(rate.getBoardType().getId()))
            .boardTypeName(rate.getBoardType().getName())
            .rackRate(rate.getRackRate())
            .stoRate(rate.getStoRate())
            .currency(rate.getCurrency())
            .profitAmount(rate.getProfitAmount())
            .profitPercentage(rate.getProfitPercentage())
            .notes(rate.getNotes())
            .isActive(rate.getIsActive())
            .createdAt(rate.getCreatedAt())
            .updatedAt(rate.getUpdatedAt())
            .build();
    }
}
