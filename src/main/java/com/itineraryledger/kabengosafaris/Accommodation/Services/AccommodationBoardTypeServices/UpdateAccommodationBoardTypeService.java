package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs.AccommodationBoardTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs.UpdateAccommodationBoardTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
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
 * UpdateAccommodationBoardTypeService - Service for updating accommodation board types
 */
@Service
@Slf4j
@Transactional
public class UpdateAccommodationBoardTypeService {

    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateAccommodationBoardTypeService(
        AccommodationBoardTypeRepository boardTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.boardTypeRepository = boardTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing accommodation board type
     *
     * @param idObfuscated The obfuscated board type ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated board type
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_BOARD_TYPE",
        description = "Updating accommodation board type",
        entityType = "AccommodationBoardType"
    )
    public ResponseEntity<ApiResponse<?>> updateAccommodationBoardType(
        String idObfuscated,
        UpdateAccommodationBoardTypeDTO updateDTO
    ) {
        log.info("Updating accommodation board type: {}", idObfuscated);

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

            // Decode board type ID
            Long boardTypeId;
            try {
                boardTypeId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode board type ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid board type ID",
                        "INVALID_BOARD_TYPE_ID"
                    )
                );
            }

            // Find existing board type
            AccommodationBoardType boardType = boardTypeRepository.findById(boardTypeId).orElse(null);
            if (boardType == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Board type not found",
                        "BOARD_TYPE_NOT_FOUND"
                    )
                );
            }

            // Validate drink logic against existing entity
            if (updateDTO.getAlcoholicDrinksIncluded() != null && updateDTO.getAlcoholicDrinksIncluded()) {
                // Check if drinks will be included after this update
                boolean drinksWillBeIncluded = updateDTO.getDrinksIncluded() != null
                    ? updateDTO.getDrinksIncluded()
                    : boardType.getDrinksIncluded();

                if (!drinksWillBeIncluded) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Alcoholic drinks cannot be included without regular drinks being included",
                            "INVALID_DRINK_LOGIC"
                        )
                    );
                }
            }

            // Check for duplicate name if name is being updated
            if (updateDTO.getName() != null && !updateDTO.getName().equals(boardType.getName())) {
                Long accommodationId = boardType.getAccommodation().getId();
                if (boardTypeRepository.existsByAccommodationIdAndNameExcludingId(accommodationId, updateDTO.getName(), boardTypeId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Board type name already exists for this accommodation",
                            "DUPLICATE_BOARD_TYPE_NAME"
                        )
                    );
                }
                boardType.setName(updateDTO.getName());
            }

            // Update other fields if provided
            if (updateDTO.getDescription() != null) {
                boardType.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getMealsIncluded() != null) {
                boardType.setMealsIncluded(updateDTO.getMealsIncluded());
            }
            if (updateDTO.getBreakfastIncluded() != null) {
                boardType.setBreakfastIncluded(updateDTO.getBreakfastIncluded());
            }
            if (updateDTO.getLunchIncluded() != null) {
                boardType.setLunchIncluded(updateDTO.getLunchIncluded());
            }
            if (updateDTO.getDinnerIncluded() != null) {
                boardType.setDinnerIncluded(updateDTO.getDinnerIncluded());
            }
            if (updateDTO.getSnacksIncluded() != null) {
                boardType.setSnacksIncluded(updateDTO.getSnacksIncluded());
            }
            if (updateDTO.getDrinksIncluded() != null) {
                boardType.setDrinksIncluded(updateDTO.getDrinksIncluded());
            }
            if (updateDTO.getAlcoholicDrinksIncluded() != null) {
                boardType.setAlcoholicDrinksIncluded(updateDTO.getAlcoholicDrinksIncluded());
            }
            if (updateDTO.getInclusions() != null) {
                boardType.setInclusions(updateDTO.getInclusions());
            }
            if (updateDTO.getExclusions() != null) {
                boardType.setExclusions(updateDTO.getExclusions());
            }
            if (updateDTO.getMealTimes() != null) {
                boardType.setMealTimes(updateDTO.getMealTimes());
            }
            if (updateDTO.getIsActive() != null) {
                boardType.setIsActive(updateDTO.getIsActive());
            }

            // Save updated board type
            boardType = boardTypeRepository.save(boardType);

            // Convert to DTO
            AccommodationBoardTypeDTO boardTypeDTO = convertToDTO(boardType);

            log.info("Accommodation board type updated successfully: {}", boardType.getName());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation board type updated successfully",
                    boardTypeDTO
                )
            );

        } catch (Exception e) {
            log.error("Error updating accommodation board type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update accommodation board type",
                    "ACCOMMODATION_BOARD_TYPE_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateAccommodationBoardTypeDTO dto) {
        boolean hasUpdate =
            dto.getName() != null ||
            dto.getDescription() != null ||
            dto.getMealsIncluded() != null ||
            dto.getBreakfastIncluded() != null ||
            dto.getLunchIncluded() != null ||
            dto.getDinnerIncluded() != null ||
            dto.getSnacksIncluded() != null ||
            dto.getDrinksIncluded() != null ||
            dto.getAlcoholicDrinksIncluded() != null ||
            dto.getInclusions() != null ||
            dto.getExclusions() != null ||
            dto.getMealTimes() != null ||
            dto.getIsActive() != null;

        if (!hasUpdate) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
            );
        }

        return null;
    }

    /**
     * Validate input fields for board type update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateAccommodationBoardTypeDTO dto) {
        // Validate and sanitize name if provided
        if (dto.getName() != null) {
            String trimmedName = dto.getName().trim();
            if (trimmedName.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Board type name cannot be empty", "INVALID_NAME")
                );
            }
            if (trimmedName.length() > 100) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Board type name cannot exceed 100 characters", "NAME_TOO_LONG")
                );
            }
            dto.setName(trimmedName);
        }

        // Validate mealsIncluded length if provided
        if (dto.getMealsIncluded() != null && dto.getMealsIncluded().length() > 255) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Meals included text cannot exceed 255 characters", "MEALS_INCLUDED_TOO_LONG")
            );
        }

        // Validate mealTimes length if provided
        if (dto.getMealTimes() != null && dto.getMealTimes().length() > 500) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Meal times text cannot exceed 500 characters", "MEAL_TIMES_TOO_LONG")
            );
        }

        // Validate drink logic if alcoholicDrinksIncluded is being set to true
        if (dto.getAlcoholicDrinksIncluded() != null && dto.getAlcoholicDrinksIncluded()) {
            // If drinksIncluded is also being updated, check it's true
            if (dto.getDrinksIncluded() != null && !dto.getDrinksIncluded()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Alcoholic drinks cannot be included without regular drinks being included", "INVALID_DRINK_LOGIC")
                );
            }
            // Note: If drinksIncluded is not in this update, we can't validate against existing value here
            // The validation would need to check the existing entity's drinksIncluded value
        }

        return null; // No validation errors
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
