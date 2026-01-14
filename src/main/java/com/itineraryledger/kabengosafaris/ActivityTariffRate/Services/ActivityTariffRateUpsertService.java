package com.itineraryledger.kabengosafaris.ActivityTariffRate.Services;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.BulkUpsertActivityRateDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.BulkUpsertActivityRateResponseDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Specifications.ActivityTariffRateSpecification;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ActivityTariffRateUpsertService - Service for bulk upsert operations on activity tariff rates
 *
 * Handles create, update, and delete operations in bulk.
 *
 * Validates that:
 * - Activity exists and has hasTariff=true and chargingBasis is not null
 * - Age category is required for PER_PERSON charging basis
 * - Age category is not allowed for non-PER_PERSON charging basis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityTariffRateUpsertService {

    private final ActivityTariffRateRepository rateRepository;
    private final ActivityRepository activityRepository;
    private final ParkRepository parkRepository;
    private final SeasonRepository seasonRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final PaxAgeCategoryRepository ageCategoryRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Bulk upsert activity rates
     *
     * Validates that each activity can have rates:
     * - hasTariff must be true
     * - chargingBasis must not be null
     */
    @Transactional
    @AuditLogAnnotation(action = "BULK_UPSERT_ACTIVITY_TARIFF_RATES", description = "Bulk upsert activity tariff rates", entityType = "ActivityTariffRate")
    public ResponseEntity<ApiResponse<?>> bulkUpsertRates(List<BulkUpsertActivityRateDTO> requests) {
        log.info("Processing bulk upsert for {} activity rates", requests.size());

        BulkUpsertActivityRateResponseDTO response = new BulkUpsertActivityRateResponseDTO();
        response.setTotalProcessed(requests.size());

        for (BulkUpsertActivityRateDTO request : requests) {
            try {
                // Decode activity ID
                Long activityId = idObfuscator.decodeId(request.getActivityId());
                if (activityId == null) {
                    response.addError("Invalid activity ID: " + request.getActivityId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify activity exists
                Optional<Activity> activityOpt = activityRepository.findById(activityId);
                if (activityOpt.isEmpty()) {
                    response.addError("Activity not found: " + request.getActivityId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                Activity activity = activityOpt.get();

                // Validate activity can have rates (hasTariff=true and chargingBasis not null)
                String validationError = validateActivityCanHaveRates(activity);
                if (validationError != null) {
                    response.addError(validationError);
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Decode optional park ID
                Park park = null;
                Long parkId = null;
                if (request.getParkId() != null && !request.getParkId().isEmpty()) {
                    parkId = idObfuscator.decodeId(request.getParkId());
                    if (parkId != null) {
                        Optional<Park> parkOpt = parkRepository.findById(parkId);
                        if (parkOpt.isEmpty()) {
                            response.addError("Park not found: " + request.getParkId());
                            response.setFailed(response.getFailed() + 1);
                            continue;
                        }
                        park = parkOpt.get();
                    }
                }

                // Decode other IDs
                Long seasonId = idObfuscator.decodeId(request.getSeasonId());
                Long nationCategoryId = idObfuscator.decodeId(request.getNationCategoryId());
                Long ageCategoryId = request.getAgeCategoryId() != null && !request.getAgeCategoryId().isEmpty()
                    ? idObfuscator.decodeId(request.getAgeCategoryId()) : null;

                if (seasonId == null || nationCategoryId == null) {
                    response.addError("Invalid season or nation category ID");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Validate age category requirement
                ChargingBasis chargingBasis = activity.getChargingBasis();
                if (chargingBasis == ChargingBasis.PER_PERSON && ageCategoryId == null) {
                    response.addError("Age category required for PER_PERSON activity: " + activity.getName());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                if (chargingBasis != ChargingBasis.PER_PERSON && ageCategoryId != null) {
                    response.addError("Age category not allowed for non-PER_PERSON activity: " + activity.getName());
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

                // Validate rack rate >= sto rate (rack rate can never be less than sto rate)
                if (request.getStoRate() != null && request.getRackRate().compareTo(request.getStoRate()) < 0) {
                    response.addError("Rack rate (" + request.getRackRate() + ") cannot be less than STO rate (" + request.getStoRate() + ")");
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify season and nation category exist
                Optional<Season> seasonOpt = seasonRepository.findById(seasonId);
                if (seasonOpt.isEmpty()) {
                    response.addError("Season not found: " + request.getSeasonId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                Optional<PaxNationCategory> nationCategoryOpt = nationCategoryRepository.findById(nationCategoryId);
                if (nationCategoryOpt.isEmpty()) {
                    response.addError("Nation category not found: " + request.getNationCategoryId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                PaxAgeCategory ageCategory = null;
                if (ageCategoryId != null) {
                    Optional<PaxAgeCategory> ageCategoryOpt = ageCategoryRepository.findById(ageCategoryId);
                    if (ageCategoryOpt.isEmpty()) {
                        response.addError("Age category not found: " + request.getAgeCategoryId());
                        response.setFailed(response.getFailed() + 1);
                        continue;
                    }
                    ageCategory = ageCategoryOpt.get();
                }

                // Find existing rate by exact combination
                boolean exists = rateRepository.existsByExactCombination(activityId, parkId, seasonId, nationCategoryId, ageCategoryId);

                if (exists) {
                    // Update existing rate
                    Specification<ActivityTariffRate> spec = ActivityTariffRateSpecification
                        .forPersonRate(activityId, parkId, seasonId, nationCategoryId, ageCategoryId);
                    Optional<ActivityTariffRate> existingOpt = rateRepository.findOne(spec);

                    if (existingOpt.isPresent()) {
                        ActivityTariffRate rate = existingOpt.get();
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
                    ActivityTariffRate rate = ActivityTariffRate.builder()
                        .activity(activity)
                        .park(park)
                        .season(seasonOpt.get())
                        .nationCategory(nationCategoryOpt.get())
                        .ageCategory(ageCategory)
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
     * Validates that an activity can have rates.
     * Activity must have hasTariff=true and chargingBasis not null.
     *
     * @param activity The activity to validate
     * @return Error message if validation fails, null if validation passes
     */
    private String validateActivityCanHaveRates(Activity activity) {
        if (!Boolean.TRUE.equals(activity.getHasTariff())) {
            return "Activity '" + activity.getName() + "' cannot have rates because hasTariff is false. " +
                   "Only activities with hasTariff=true can have rates.";
        }

        if (activity.getChargingBasis() == null) {
            return "Activity '" + activity.getName() + "' cannot have rates because chargingBasis is not set. " +
                   "Please set a charging basis for the activity first.";
        }

        return null; // Validation passed
    }

    /**
     * Validates that a currency code is a valid ISO 4217 currency code
     *
     * @param currencyCode The currency code to validate
     * @return true if valid, false otherwise
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
