package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.BulkUpsertAccommodationRateDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.BulkUpsertAccommodationRateResponseDTO;
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
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AccommodationRateUpsertService - Service for bulk upsert operations on accommodation rates
 *
 * Handles create, update, and delete operations in bulk.
 *
 * Validates that:
 * - Accommodation exists
 * - Season exists and belongs to the accommodation
 * - RoomType exists and belongs to the accommodation
 * - RoomStandard exists and belongs to the accommodation
 * - BoardType exists and belongs to the accommodation
 * - Rack rate >= STO rate
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRateUpsertService {

    private final AccommodationRateRepository rateRepository;
    private final AccommodationRepository accommodationRepository;
    private final SeasonRepository seasonRepository;
    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Bulk upsert accommodation rates
     */
    @Transactional
    @AuditLogAnnotation(action = "BULK_UPSERT_ACCOMMODATION_RATES", description = "Bulk upsert accommodation rates", entityType = "AccommodationRate")
    public ResponseEntity<ApiResponse<?>> bulkUpsertRates(List<BulkUpsertAccommodationRateDTO> requests) {
        log.info("Processing bulk upsert for {} accommodation rates", requests.size());

        BulkUpsertAccommodationRateResponseDTO response = new BulkUpsertAccommodationRateResponseDTO();
        response.setTotalProcessed(requests.size());

        for (BulkUpsertAccommodationRateDTO request : requests) {
            try {
                // Decode accommodation ID
                Long accommodationId = idObfuscator.decodeId(request.getAccommodationId());
                if (accommodationId == null) {
                    response.addError("Invalid accommodation ID: " + request.getAccommodationId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify accommodation exists
                Optional<Accommodation> accommodationOpt = accommodationRepository.findById(accommodationId);
                if (accommodationOpt.isEmpty()) {
                    response.addError("Accommodation not found: " + request.getAccommodationId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                Accommodation accommodation = accommodationOpt.get();

                // Decode and verify season
                Long seasonId = idObfuscator.decodeId(request.getSeasonId());
                if (seasonId == null) {
                    response.addError("Invalid season ID: " + request.getSeasonId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                Optional<Season> seasonOpt = seasonRepository.findById(seasonId);
                if (seasonOpt.isEmpty()) {
                    response.addError("Season not found: " + request.getSeasonId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                Season season = seasonOpt.get();

                // Verify season belongs to accommodation
                if (season.getAccommodation() == null || !season.getAccommodation().getId().equals(accommodationId)) {
                    response.addError("Season '" + season.getName() + "' does not belong to accommodation '" + accommodation.getName() + "'");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Decode and verify room type
                Long roomTypeId = idObfuscator.decodeId(request.getRoomTypeId());
                if (roomTypeId == null) {
                    response.addError("Invalid room type ID: " + request.getRoomTypeId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                Optional<AccommodationRoomType> roomTypeOpt = roomTypeRepository.findById(roomTypeId);
                if (roomTypeOpt.isEmpty()) {
                    response.addError("Room type not found: " + request.getRoomTypeId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                AccommodationRoomType roomType = roomTypeOpt.get();

                // Verify room type belongs to accommodation
                if (!roomType.getAccommodation().getId().equals(accommodationId)) {
                    response.addError("Room type '" + roomType.getName() + "' does not belong to accommodation '" + accommodation.getName() + "'");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Decode and verify room standard
                Long roomStandardId = idObfuscator.decodeId(request.getRoomStandardId());
                if (roomStandardId == null) {
                    response.addError("Invalid room standard ID: " + request.getRoomStandardId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                Optional<AccommodationRoomStandard> roomStandardOpt = roomStandardRepository.findById(roomStandardId);
                if (roomStandardOpt.isEmpty()) {
                    response.addError("Room standard not found: " + request.getRoomStandardId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                AccommodationRoomStandard roomStandard = roomStandardOpt.get();

                // Verify room standard belongs to accommodation
                if (!roomStandard.getAccommodation().getId().equals(accommodationId)) {
                    response.addError("Room standard '" + roomStandard.getName() + "' does not belong to accommodation '" + accommodation.getName() + "'");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Decode and verify board type
                Long boardTypeId = idObfuscator.decodeId(request.getBoardTypeId());
                if (boardTypeId == null) {
                    response.addError("Invalid board type ID: " + request.getBoardTypeId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                Optional<AccommodationBoardType> boardTypeOpt = boardTypeRepository.findById(boardTypeId);
                if (boardTypeOpt.isEmpty()) {
                    response.addError("Board type not found: " + request.getBoardTypeId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                AccommodationBoardType boardType = boardTypeOpt.get();

                // Verify board type belongs to accommodation
                if (!boardType.getAccommodation().getId().equals(accommodationId)) {
                    response.addError("Board type '" + boardType.getName() + "' does not belong to accommodation '" + accommodation.getName() + "'");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Validate rack rate is provided
                if (request.getRackRate() == null) {
                    response.addError("Rack rate is required");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Validate STO rate is provided
                if (request.getStoRate() == null) {
                    response.addError("STO rate is required");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Validate currency is provided and valid
                if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
                    response.addError("Currency is required");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                if (!isValidCurrency(request.getCurrency())) {
                    response.addError("Invalid currency code: " + request.getCurrency() + ". Must be a valid ISO 4217 currency code (e.g., USD, EUR, TZS, KES)");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Validate rack rate >= sto rate
                if (request.getRackRate().compareTo(request.getStoRate()) < 0) {
                    response.addError("Rack rate (" + request.getRackRate() + ") cannot be less than STO rate (" + request.getStoRate() + ")");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Find existing rate by exact combination
                boolean exists = rateRepository.existsByExactCombination(
                    accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId
                );

                if (exists) {
                    // Update existing rate
                    Optional<AccommodationRate> existingOpt = rateRepository.findByAccommodationIdAndSeasonIdAndRoomTypeIdAndRoomStandardIdAndBoardTypeId(
                        accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId
                    );

                    if (existingOpt.isPresent()) {
                        AccommodationRate rate = existingOpt.get();
                        rate.setRackRate(request.getRackRate());
                        rate.setStoRate(request.getStoRate());
                        rate.setCurrency(request.getCurrency().toUpperCase().trim());
                        if (request.getNotes() != null) {
                            rate.setNotes(request.getNotes());
                        }
                        if (request.getIsActive() != null) {
                            rate.setIsActive(request.getIsActive());
                        }
                        rateRepository.save(rate);
                        response.setUpdated(response.getUpdated() + 1);
                    }
                } else {
                    // Create new rate
                    AccommodationRate rate = AccommodationRate.builder()
                        .accommodation(accommodation)
                        .season(season)
                        .roomType(roomType)
                        .roomStandard(roomStandard)
                        .boardType(boardType)
                        .rackRate(request.getRackRate())
                        .stoRate(request.getStoRate())
                        .currency(request.getCurrency().toUpperCase().trim())
                        .notes(request.getNotes())
                        .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                        .build();
                    rateRepository.save(rate);
                    response.setCreated(response.getCreated() + 1);
                }

            } catch (Exception e) {
                log.error("Error processing bulk upsert item", e);
                response.addError("Error: " + e.getMessage());
                response.setFailed(response.getFailed() + 1);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Bulk upsert completed", response));
    }

    /**
     * Validates that a currency code is a valid ISO 4217 currency code
     */
    private boolean isValidCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return false;
        }
        try {
            java.util.Currency.getInstance(currencyCode.toUpperCase().trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
