package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs.AccommodationBoardTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs.CreateAccommodationBoardTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CreateAccommodationBoardTypeService - Service for creating accommodation board types
 */
@Service
@Slf4j
@Transactional
public class CreateAccommodationBoardTypeService {

    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateAccommodationBoardTypeService(
        AccommodationBoardTypeRepository boardTypeRepository,
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.boardTypeRepository = boardTypeRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation board type
     *
     * @param createDTO The board type data
     * @return ResponseEntity with ApiResponse containing the created board type
     */
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_BOARD_TYPE",
        description = "Creating accommodation board type",
        entityType = "AccommodationBoardType"
    )
    public ResponseEntity<ApiResponse<?>> createAccommodationBoardType(CreateAccommodationBoardTypeDTO createDTO) {
        log.info("Creating new accommodation board type: {}", createDTO.getName());

        try {
            // Decode accommodation ID
            Long accommodationId;
            try {
                accommodationId = idObfuscator.decodeId(createDTO.getAccommodationId());
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", createDTO.getAccommodationId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Check if accommodation exists
            Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Accommodation not found",
                        "ACCOMMODATION_NOT_FOUND"
                    )
                );
            }

            // Check for duplicate board type name for this accommodation
            if (boardTypeRepository.existsByAccommodationIdAndName(accommodationId, createDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Board type name already exists for this accommodation",
                        "DUPLICATE_BOARD_TYPE_NAME"
                    )
                );
            }

            // Create board type entity
            AccommodationBoardType boardType = AccommodationBoardType.builder()
                .accommodation(accommodation)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .mealsIncluded(createDTO.getMealsIncluded())
                .breakfastIncluded(createDTO.getBreakfastIncluded() != null ? createDTO.getBreakfastIncluded() : false)
                .lunchIncluded(createDTO.getLunchIncluded() != null ? createDTO.getLunchIncluded() : false)
                .dinnerIncluded(createDTO.getDinnerIncluded() != null ? createDTO.getDinnerIncluded() : false)
                .snacksIncluded(createDTO.getSnacksIncluded() != null ? createDTO.getSnacksIncluded() : false)
                .drinksIncluded(createDTO.getDrinksIncluded() != null ? createDTO.getDrinksIncluded() : false)
                .alcoholicDrinksIncluded(createDTO.getAlcoholicDrinksIncluded() != null ? createDTO.getAlcoholicDrinksIncluded() : false)
                .inclusions(createDTO.getInclusions())
                .exclusions(createDTO.getExclusions())
                .mealTimes(createDTO.getMealTimes())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save board type
            boardType = boardTypeRepository.save(boardType);

            // Convert to DTO
            AccommodationBoardTypeDTO boardTypeDTO = convertToDTO(boardType);

            log.info("Accommodation board type created successfully: {}", boardType.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Accommodation board type created successfully",
                    boardTypeDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating accommodation board type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create accommodation board type",
                    "ACCOMMODATION_BOARD_TYPE_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Convert AccommodationBoardType entity to DTO
     */
    private AccommodationBoardTypeDTO convertToDTO(AccommodationBoardType boardType) {
        return AccommodationBoardTypeDTO.builder()
            .id(idObfuscator.encodeId(boardType.getId()))
            .accommodationId(idObfuscator.encodeId(boardType.getAccommodation().getId()))
            .accommodationName(boardType.getAccommodation().getName())
            .name(boardType.getName())
            .description(boardType.getDescription())
            .mealsIncluded(boardType.getMealsIncluded())
            .breakfastIncluded(boardType.getBreakfastIncluded())
            .lunchIncluded(boardType.getLunchIncluded())
            .dinnerIncluded(boardType.getDinnerIncluded())
            .snacksIncluded(boardType.getSnacksIncluded())
            .drinksIncluded(boardType.getDrinksIncluded())
            .alcoholicDrinksIncluded(boardType.getAlcoholicDrinksIncluded())
            .inclusions(boardType.getInclusions())
            .exclusions(boardType.getExclusions())
            .mealTimes(boardType.getMealTimes())
            .isActive(boardType.getIsActive())
            .mealCount(boardType.getMealCount())
            .isFullMealPlan(boardType.isFullMealPlan())
            .createdAt(boardType.getCreatedAt())
            .updatedAt(boardType.getUpdatedAt())
            .build();
    }
}
