package com.itineraryledger.kabengosafaris.ParkTariffRate.Services;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.BulkUpsertParkRateDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.BulkUpsertParkRateResponseDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Specifications.ParkTariffRateSpecification;
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
 * ParkTariffRateUpsertService - Service for bulk upsert operations on park tariff rates
 *
 * Handles create and update operations in bulk.
 *
 * Validates that:
 * - Park-Tariff relationship exists
 * - Age category is required for PER_PERSON charging basis
 * - Age category is not allowed for non-PER_PERSON charging basis
 * - Rack rate >= STO rate (cannot charge less than what we pay)
 * - Currency is a valid ISO 4217 code
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkTariffRateUpsertService {

    private final ParkTariffRateRepository rateRepository;
    private final ParkTariffRepository parkTariffRepository;
    private final SeasonRepository seasonRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final PaxAgeCategoryRepository ageCategoryRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Bulk upsert park tariff rates
     *
     * Validates that each park-tariff can have rates and applies business rules.
     */
    @Transactional
    @AuditLogAnnotation(action = "BULK_UPSERT_PARK_TARIFF_RATES", description = "Bulk upsert park tariff rates", entityType = "ParkTariffRate")
    public ResponseEntity<ApiResponse<?>> bulkUpsertRates(List<BulkUpsertParkRateDTO> requests) {
        log.info("Processing bulk upsert for {} park tariff rates", requests.size());

        BulkUpsertParkRateResponseDTO response = new BulkUpsertParkRateResponseDTO();
        response.setTotalProcessed(requests.size());

        for (BulkUpsertParkRateDTO request : requests) {
            try {
                // Decode park ID
                Long parkId = idObfuscator.decodeId(request.getParkId());
                if (parkId == null) {
                    response.addError("Invalid park ID: " + request.getParkId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Decode tariff ID
                Long tariffId = idObfuscator.decodeId(request.getTariffId());
                if (tariffId == null) {
                    response.addError("Invalid tariff ID: " + request.getTariffId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify park-tariff relationship exists
                Optional<ParkTariff> parkTariffOpt = parkTariffRepository.findByParkIdAndTariffId(parkId, tariffId);
                if (parkTariffOpt.isEmpty()) {
                    response.addError("Park-tariff relationship not found for park: " + request.getParkId() + ", tariff: " + request.getTariffId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                ParkTariff parkTariff = parkTariffOpt.get();

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
                ChargingBasis chargingBasis = parkTariff.getTariff().getChargingBasis();
                if (chargingBasis == ChargingBasis.PER_PERSON && ageCategoryId == null) {
                    response.addError("Age category required for PER_PERSON tariff: " + parkTariff.getTariff().getName());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }
                if (chargingBasis != ChargingBasis.PER_PERSON && ageCategoryId != null) {
                    response.addError("Age category not allowed for non-PER_PERSON tariff: " + parkTariff.getTariff().getName());
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
                boolean exists = rateRepository.existsByExactCombination(parkId, tariffId, seasonId, nationCategoryId, ageCategoryId);

                if (exists) {
                    // Update existing rate
                    Specification<ParkTariffRate> spec = ParkTariffRateSpecification
                        .forPersonRate(parkId, tariffId, seasonId, nationCategoryId, ageCategoryId);
                    Optional<ParkTariffRate> existingOpt = rateRepository.findOne(spec);

                    if (existingOpt.isPresent()) {
                        ParkTariffRate rate = existingOpt.get();
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
                    ParkTariffRate rate = ParkTariffRate.builder()
                        .parkTariff(parkTariff)
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
