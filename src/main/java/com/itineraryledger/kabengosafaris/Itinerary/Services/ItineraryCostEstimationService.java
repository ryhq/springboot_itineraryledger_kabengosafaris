package com.itineraryledger.kabengosafaris.Itinerary.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO.*;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for estimating itinerary costs.
 *
 * Provides quick budget calculations based on:
 * - Park fees (tariffs)
 * - Accommodation rates
 * - Activity rates
 *
 * Uses current rates based on season determined from provided start date.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItineraryCostEstimationService {

    private final ItineraryFullGetService itineraryFullGetService;
    private final ParkTariffRateRepository parkTariffRateRepository;
    private final ParkTariffRepository parkTariffRepository;
    private final AccommodationRateRepository accommodationRateRepository;
    private final ActivityTariffRateRepository activityTariffRateRepository;
    private final ActivityRepository activityRepository;
    private final SeasonPeriodRepository seasonPeriodRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final IdObfuscator idObfuscator;

    // Default currency if not specified
    private static final String DEFAULT_CURRENCY = "USD";

    /**
     * Estimate costs for an itinerary
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param startDate Optional start date (defaults to today if not provided)
     * @param useStoRate Whether to use STO (Special Tour Operator) rates (default: true)
     * @param currency Preferred output currency (default: USD)
     * @return ResponseEntity with cost estimation or error
     */
    public ResponseEntity<ApiResponse<?>> estimateCosts(
            String itineraryIdObfuscated,
            LocalDate startDate,
            Boolean useStoRate,
            String currency
    ) {
        try {
            // Defaults
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (useStoRate == null) {
                useStoRate = true;
            }
            if (currency == null || currency.isBlank()) {
                currency = DEFAULT_CURRENCY;
            }

            // 1. Fetch full itinerary data
            ResponseEntity<ApiResponse<?>> itineraryResponse = itineraryFullGetService.getFullItinerary(itineraryIdObfuscated);
            if (!itineraryResponse.getStatusCode().is2xxSuccessful()) {
                return itineraryResponse;
            }

            ApiResponse<?> apiResponse = itineraryResponse.getBody();
            if (apiResponse == null || !(apiResponse.getData() instanceof FullItineraryDTO)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            FullItineraryDTO itinerary = (FullItineraryDTO) apiResponse.getData();

            // 2. Calculate end date
            LocalDate endDate = startDate.plusDays(itinerary.getTotalDays() - 1);

            // 3. Find applicable season for start date
            Season applicableSeason = findSeasonForDate(startDate);
            String seasonName = applicableSeason != null ? applicableSeason.getName() : "Unknown Season";

            // 4. Build pax summary
            List<PaxCategorySummary> paxBreakdown = buildPaxBreakdown(itinerary);
            int totalPax = paxBreakdown.stream().mapToInt(PaxCategorySummary::getCount).sum();

            // 5. Get car count from itinerary (default to 1)
            int carCount = itinerary.getCarCount() != null ? itinerary.getCarCount() : 1;

            // 6. Calculate costs
            List<String> warnings = new ArrayList<>();

            // 6a. Park fee costs (with carCount support)
            CostBreakdown parkFeeCosts = calculateParkFeeCosts(
                itinerary, startDate, applicableSeason, useStoRate, carCount, warnings
            );

            // 6b. Accommodation costs (per night calculation)
            CostBreakdown accommodationCosts = calculateAccommodationCosts(
                itinerary, startDate, useStoRate, warnings
            );

            // 6c. Activity costs (standalone and park activities)
            CostBreakdown activityCosts = calculateActivityCosts(
                itinerary, startDate, applicableSeason, useStoRate, carCount, warnings
            );

            // 7. Calculate totals by currency (multi-currency support)
            Map<String, BigDecimal> subtotalByCurrency = new HashMap<>();

            // Aggregate park fees by currency
            if (parkFeeCosts.getItems() != null) {
                for (CostLineItem item : parkFeeCosts.getItems()) {
                    String itemCurrency = item.getCurrency() != null ? item.getCurrency() : DEFAULT_CURRENCY;
                    subtotalByCurrency.merge(itemCurrency,
                        item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO,
                        BigDecimal::add);
                }
            }

            // Aggregate accommodation costs by currency
            if (accommodationCosts.getItems() != null) {
                for (CostLineItem item : accommodationCosts.getItems()) {
                    String itemCurrency = item.getCurrency() != null ? item.getCurrency() : DEFAULT_CURRENCY;
                    subtotalByCurrency.merge(itemCurrency,
                        item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO,
                        BigDecimal::add);
                }
            }

            // Aggregate activity costs by currency
            if (activityCosts.getItems() != null) {
                for (CostLineItem item : activityCosts.getItems()) {
                    String itemCurrency = item.getCurrency() != null ? item.getCurrency() : DEFAULT_CURRENCY;
                    subtotalByCurrency.merge(itemCurrency,
                        item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO,
                        BigDecimal::add);
                }
            }

            // Calculate subtotal in primary currency (or total if single currency)
            BigDecimal subtotal = subtotalByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            if (subtotalByCurrency.size() == 1) {
                subtotal = subtotalByCurrency.values().iterator().next();
                currency = subtotalByCurrency.keySet().iterator().next();
            }

            BigDecimal perPersonCost = totalPax > 0
                ? subtotal.divide(BigDecimal.valueOf(totalPax), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            // 8. Build day-by-day cost summary
            List<DayCostSummary> daySummaries = buildDayCostSummaries(
                itinerary, startDate, parkFeeCosts, accommodationCosts, activityCosts
            );

            // 9. Build response
            ItineraryCostEstimationDTO estimation = ItineraryCostEstimationDTO.builder()
                .itineraryId(itinerary.getId())
                .itineraryCode(itinerary.getCode())
                .itineraryName(itinerary.getName())
                .totalDays(itinerary.getTotalDays())
                .totalNights(itinerary.getTotalNights())
                .startDate(startDate)
                .endDate(endDate)
                .seasonName(seasonName)
                .totalPax(totalPax)
                .paxBreakdown(paxBreakdown)
                .parkFeeCosts(parkFeeCosts)
                .accommodationCosts(accommodationCosts)
                .activityCosts(activityCosts)
                .daySummaries(daySummaries)
                .currency(currency)
                .subtotal(subtotal)
                .subtotalByCurrency(subtotalByCurrency.size() > 1 ? subtotalByCurrency : null)
                .perPersonCost(perPersonCost)
                .rateType(useStoRate ? "STO" : "RACK")
                .hasIncompleteRates(!warnings.isEmpty())
                .warnings(warnings.isEmpty() ? null : warnings)
                .estimatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

            return ResponseEntity.ok(
                ApiResponse.success(200, "Cost estimation calculated successfully", estimation)
            );

        } catch (Exception e) {
            log.error("Failed to estimate costs for itinerary: {}", itineraryIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to estimate costs: " + e.getMessage(), "COST_ESTIMATION_FAILED")
            );
        }
    }

    /**
     * Find the applicable season for a given date
     */
    private Season findSeasonForDate(LocalDate date) {
        List<SeasonPeriod> allPeriods = seasonPeriodRepository.findAll();

        for (SeasonPeriod period : allPeriods) {
            if (period.containsDate(date) && period.getSeason().getIsGlobal()) {
                return period.getSeason();
            }
        }

        // Fallback: return first active global season
        return allPeriods.stream()
            .filter(p -> p.getSeason().getIsGlobal() && p.getSeason().getIsActive())
            .map(SeasonPeriod::getSeason)
            .findFirst()
            .orElse(null);
    }

    /**
     * Build pax category summary from itinerary
     */
    private List<PaxCategorySummary> buildPaxBreakdown(FullItineraryDTO itinerary) {
        if (itinerary.getPaxList() == null || itinerary.getPaxList().isEmpty()) {
            return new ArrayList<>();
        }

        return itinerary.getPaxList().stream()
            .map(pax -> PaxCategorySummary.builder()
                .nationCategoryId(pax.getNationCategoryId())
                .nationCategoryName(pax.getNationCategoryName())
                .ageCategoryId(pax.getAgeCategoryId())
                .ageCategoryName(pax.getAgeCategoryName())
                .count(pax.getCount())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Calculate park fee costs for all days
     *
     * @param carCount Number of vehicles in the safari (for PER_VEHICLE tariffs)
     */
    private CostBreakdown calculateParkFeeCosts(
            FullItineraryDTO itinerary,
            LocalDate startDate,
            Season season,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        List<CostLineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;

        if (itinerary.getDays() == null || season == null) {
            return CostBreakdown.builder()
                .total(BigDecimal.ZERO)
                .currency(currency)
                .itemCount(0)
                .items(items)
                .build();
        }

        Long seasonId = season.getId();

        for (FullItineraryDTO.DayDTO day : itinerary.getDays()) {
            if (day.getParks() == null) continue;

            for (FullItineraryDTO.DayParkDTO park : day.getParks()) {
                if (park.getTariffs() == null) continue;

                Long parkId = idObfuscator.decodeId(park.getParkId());
                if (parkId == null) continue;

                for (FullItineraryDTO.ParkTariffDTO tariff : park.getTariffs()) {
                    // Skip if not included in price
                    if (tariff.getIsIncludedInPrice() != null && !tariff.getIsIncludedInPrice()) {
                        continue;
                    }

                    Long tariffId = idObfuscator.decodeId(tariff.getTariffId());
                    if (tariffId == null) continue;

                    // Get the ParkTariff to determine charging basis
                    Optional<ParkTariff> parkTariffOpt = parkTariffRepository
                        .findByParkIdAndTariffId(parkId, tariffId);

                    if (parkTariffOpt.isEmpty()) {
                        warnings.add(String.format("Park tariff not found: Park %s, Tariff %s",
                            park.getParkName(), tariff.getTariffName()));
                        continue;
                    }

                    ParkTariff parkTariff = parkTariffOpt.get();
                    ChargingBasis chargingBasis = parkTariff.getTariff().getChargingBasis();

                    // Calculate based on charging basis (with carCount support)
                    CostLineItem lineItem = calculateParkTariffCost(
                        day.getDayNumber(),
                        park.getParkName(),
                        tariff.getTariffName(),
                        parkId,
                        tariffId,
                        seasonId,
                        chargingBasis,
                        itinerary.getPaxList(),
                        useStoRate,
                        carCount,
                        warnings
                    );

                    if (lineItem != null && lineItem.getTotalPrice() != null) {
                        items.add(lineItem);
                        total = total.add(lineItem.getTotalPrice());
                        if (lineItem.getCurrency() != null) {
                            currency = lineItem.getCurrency();
                        }
                    }
                }
            }
        }

        return CostBreakdown.builder()
            .total(total)
            .currency(currency)
            .itemCount(items.size())
            .items(items)
            .build();
    }

    /**
     * Calculate cost for a single park tariff
     *
     * @param carCount Number of vehicles (for PER_VEHICLE charging basis)
     */
    private CostLineItem calculateParkTariffCost(
            Integer dayNumber,
            String parkName,
            String tariffName,
            Long parkId,
            Long tariffId,
            Long seasonId,
            ChargingBasis chargingBasis,
            List<FullItineraryDTO.PaxDTO> paxList,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        if (paxList == null || paxList.isEmpty()) {
            warnings.add(String.format("No pax configured for tariff: %s at %s", tariffName, parkName));
            return null;
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        boolean rateFound = false;
        int quantity = 1;
        String chargingNote = chargingBasis.getDisplayName();

        if (chargingBasis == ChargingBasis.PER_PERSON) {
            // Calculate per person - need rate for each pax category
            int totalPaxCount = 0;
            for (FullItineraryDTO.PaxDTO pax : paxList) {
                Long nationCategoryId = idObfuscator.decodeId(pax.getNationCategoryId());
                Long ageCategoryId = idObfuscator.decodeId(pax.getAgeCategoryId());

                if (nationCategoryId == null || ageCategoryId == null) continue;

                Optional<ParkTariffRate> rateOpt = parkTariffRateRepository.findActiveRateForPerson(
                    parkId, tariffId, seasonId, nationCategoryId, ageCategoryId
                );

                if (rateOpt.isPresent()) {
                    ParkTariffRate rate = rateOpt.get();
                    BigDecimal paxUnitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    if (paxUnitPrice != null) {
                        BigDecimal paxCost = paxUnitPrice.multiply(BigDecimal.valueOf(pax.getCount()));
                        totalPrice = totalPrice.add(paxCost);
                        totalPaxCount += pax.getCount();
                        currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                        rateFound = true;
                    }
                } else {
                    warnings.add(String.format("Rate not found: %s for %s %s at %s",
                        tariffName, pax.getNationCategoryName(), pax.getAgeCategoryName(), parkName));
                }
            }
            quantity = totalPaxCount;
            // Calculate average unit price for display
            if (totalPaxCount > 0) {
                unitPrice = totalPrice.divide(BigDecimal.valueOf(totalPaxCount), 2, RoundingMode.HALF_UP);
            }
        } else if (chargingBasis == ChargingBasis.PER_VEHICLE) {
            // PER_VEHICLE - multiply by car count
            Long highestPriorityNationId = findHighestPriorityNationCategory(paxList);

            if (highestPriorityNationId != null) {
                Optional<ParkTariffRate> rateOpt = parkTariffRateRepository.findActiveRateForGroup(
                    parkId, tariffId, seasonId, highestPriorityNationId
                );

                if (rateOpt.isPresent()) {
                    ParkTariffRate rate = rateOpt.get();
                    unitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    if (unitPrice != null) {
                        // Multiply by number of vehicles
                        totalPrice = unitPrice.multiply(BigDecimal.valueOf(carCount));
                        quantity = carCount;
                        currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                        rateFound = true;
                        chargingNote = String.format("Per Vehicle x %d", carCount);
                    }
                } else {
                    warnings.add(String.format("Rate not found: %s (vehicle) at %s", tariffName, parkName));
                }
            }
        } else {
            // PER_GROUP, FLAT_RATE, PER_DAY, etc. - single charge regardless of vehicles/pax
            Long highestPriorityNationId = findHighestPriorityNationCategory(paxList);

            if (highestPriorityNationId != null) {
                Optional<ParkTariffRate> rateOpt = parkTariffRateRepository.findActiveRateForGroup(
                    parkId, tariffId, seasonId, highestPriorityNationId
                );

                if (rateOpt.isPresent()) {
                    ParkTariffRate rate = rateOpt.get();
                    unitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    if (unitPrice != null) {
                        totalPrice = unitPrice;
                        quantity = 1;
                        currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                        rateFound = true;
                    }
                } else {
                    warnings.add(String.format("Rate not found: %s (%s) at %s",
                        tariffName, chargingBasis.getDisplayName(), parkName));
                }
            }
        }

        return CostLineItem.builder()
            .dayNumber(dayNumber)
            .itemType("PARK_FEE")
            .itemName(tariffName + " - " + parkName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .totalPrice(totalPrice)
            .currency(currency)
            .paxCategory(chargingNote)
            .rateFound(rateFound)
            .build();
    }

    /**
     * Find the highest priority nation category from pax list
     * (For vehicle/group rates, the highest priority pax determines the rate)
     *
     * Priority is determined by the PaxNationCategory.priorityFactor field.
     * Higher priority factor = higher priority (e.g., Non-Resident > East African > Resident)
     */
    private Long findHighestPriorityNationCategory(List<FullItineraryDTO.PaxDTO> paxList) {
        if (paxList == null || paxList.isEmpty()) {
            return null;
        }

        Long highestPriorityId = null;
        int highestPriority = Integer.MIN_VALUE;

        for (FullItineraryDTO.PaxDTO pax : paxList) {
            Long nationCategoryId = idObfuscator.decodeId(pax.getNationCategoryId());
            if (nationCategoryId == null) continue;

            // Query the actual priority from the database
            Optional<PaxNationCategory> categoryOpt = nationCategoryRepository.findById(nationCategoryId);
            if (categoryOpt.isPresent()) {
                PaxNationCategory category = categoryOpt.get();
                int priority = category.getPriorityFactor() != null ? category.getPriorityFactor() : 0;
                if (priority > highestPriority) {
                    highestPriority = priority;
                    highestPriorityId = nationCategoryId;
                }
            }
        }

        // Fallback to first if no priority found
        if (highestPriorityId == null) {
            highestPriorityId = idObfuscator.decodeId(paxList.get(0).getNationCategoryId());
        }

        return highestPriorityId;
    }

    /**
     * Calculate accommodation costs for all days
     *
     * Accommodation rates are per night. Each day entry represents one night's stay.
     * The calculation considers:
     * - isOvernight: if false (e.g., departure day), skip accommodation cost
     * - isPerPerson: true = Per Person Sharing (PPS), false = Per Room
     * - roomCount: number of rooms booked
     */
    private CostBreakdown calculateAccommodationCosts(
            FullItineraryDTO itinerary,
            LocalDate startDate,
            boolean useStoRate,
            List<String> warnings
    ) {
        List<CostLineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;

        if (itinerary.getDays() == null) {
            return CostBreakdown.builder()
                .total(BigDecimal.ZERO)
                .currency(currency)
                .itemCount(0)
                .items(items)
                .build();
        }

        for (FullItineraryDTO.DayDTO day : itinerary.getDays()) {
            if (day.getAccommodations() == null) continue;

            // Skip non-overnight days (e.g., departure day) - no accommodation cost
            if (day.getIsOvernight() != null && !day.getIsOvernight()) {
                continue;
            }

            LocalDate dayDate = startDate.plusDays(day.getDayNumber() - 1);

            for (FullItineraryDTO.DayAccommodationDTO accommodation : day.getAccommodations()) {
                // Skip alternatives
                if (accommodation.getIsAlternative() != null && accommodation.getIsAlternative()) {
                    continue;
                }

                Long accommodationId = idObfuscator.decodeId(accommodation.getAccommodationId());
                Long roomTypeId = idObfuscator.decodeId(accommodation.getRoomTypeId());
                Long roomStandardId = idObfuscator.decodeId(accommodation.getRoomStandardId());
                Long boardTypeId = idObfuscator.decodeId(accommodation.getBoardTypeId());

                if (accommodationId == null) continue;

                // Find accommodation-specific season for this date
                Season accSeason = findAccommodationSeasonForDate(accommodationId, dayDate);
                Long seasonId = accSeason != null ? accSeason.getId() : null;

                if (seasonId == null || roomTypeId == null || roomStandardId == null || boardTypeId == null) {
                    warnings.add(String.format("Missing rate parameters for: %s on Day %d",
                        accommodation.getAccommodationName(), day.getDayNumber()));
                    continue;
                }

                Optional<AccommodationRate> rateOpt = accommodationRateRepository
                    .findByAccommodationIdAndSeasonIdAndRoomTypeIdAndRoomStandardIdAndBoardTypeId(
                        accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId
                    );

                if (rateOpt.isPresent()) {
                    AccommodationRate rate = rateOpt.get();
                    BigDecimal unitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    int roomCount = accommodation.getRoomCount() != null ? accommodation.getRoomCount() : 1;
                    int totalPax = itinerary.getPaxList() != null
                        ? itinerary.getPaxList().stream().mapToInt(p -> p.getCount() != null ? p.getCount() : 0).sum()
                        : 0;

                    // Calculate based on whether rate is per person or per room
                    // Rate is always per night (1 night per day entry)
                    BigDecimal itemTotal;
                    int quantity;
                    String rateTypeNote;

                    if (rate.getIsPerPerson() != null && rate.getIsPerPerson()) {
                        // Per Person Sharing (PPS) - multiply by number of people
                        // Rate is per person per night
                        quantity = totalPax;
                        itemTotal = unitPrice != null
                            ? unitPrice.multiply(BigDecimal.valueOf(totalPax))
                            : BigDecimal.ZERO;
                        rateTypeNote = "Per Person Sharing/Night";
                    } else {
                        // Per Room - multiply by number of rooms
                        // Rate is per room per night
                        quantity = roomCount;
                        itemTotal = unitPrice != null
                            ? unitPrice.multiply(BigDecimal.valueOf(roomCount))
                            : BigDecimal.ZERO;
                        rateTypeNote = "Per Room/Night";
                    }

                    CostLineItem item = CostLineItem.builder()
                        .dayNumber(day.getDayNumber())
                        .itemType("ACCOMMODATION")
                        .itemName(accommodation.getAccommodationName() + " - " + accommodation.getRoomTypeName())
                        .referenceId(accommodation.getAccommodationId())
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .totalPrice(itemTotal)
                        .currency(rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY)
                        .notes(accommodation.getBoardTypeName() + " (" + rateTypeNote + ")")
                        .rateFound(true)
                        .build();

                    items.add(item);
                    total = total.add(itemTotal);
                    currency = item.getCurrency();
                } else {
                    warnings.add(String.format("Accommodation rate not found: %s (%s, %s, %s) on Day %d",
                        accommodation.getAccommodationName(),
                        accommodation.getRoomTypeName(),
                        accommodation.getRoomStandardName(),
                        accommodation.getBoardTypeName(),
                        day.getDayNumber()));
                }
            }
        }

        return CostBreakdown.builder()
            .total(total)
            .currency(currency)
            .itemCount(items.size())
            .items(items)
            .build();
    }

    /**
     * Find accommodation-specific season for a date
     */
    private Season findAccommodationSeasonForDate(Long accommodationId, LocalDate date) {
        List<SeasonPeriod> periods = seasonPeriodRepository.findAll();

        // First try accommodation-specific seasons
        for (SeasonPeriod period : periods) {
            Season season = period.getSeason();
            if (!season.getIsGlobal() &&
                season.getAccommodation() != null &&
                season.getAccommodation().getId().equals(accommodationId) &&
                period.containsDate(date)) {
                return season;
            }
        }

        // Fallback to global season
        return findSeasonForDate(date);
    }

    /**
     * Calculate activity costs for all days
     *
     * Handles both:
     * - Standalone activities (ItineraryDayActivity - activities not tied to a park)
     * - Park activities (ItineraryDayParkActivity - activities within a park visit)
     *
     * Activity pricing follows the Activity's ChargingBasis:
     * - PER_PERSON: Rate per person multiplied by pax count
     * - PER_VEHICLE: Rate per vehicle multiplied by car count
     * - PER_GROUP/FLAT_RATE: Single flat rate for the entire group
     *
     * @param carCount Number of vehicles for PER_VEHICLE activities
     */
    private CostBreakdown calculateActivityCosts(
            FullItineraryDTO itinerary,
            LocalDate startDate,
            Season season,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        List<CostLineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;

        if (itinerary.getDays() == null || season == null) {
            return CostBreakdown.builder()
                .total(BigDecimal.ZERO)
                .currency(currency)
                .itemCount(0)
                .items(items)
                .build();
        }

        Long seasonId = season.getId();

        for (FullItineraryDTO.DayDTO day : itinerary.getDays()) {
            // 1. Process standalone day activities
            if (day.getActivities() != null) {
                for (FullItineraryDTO.DayActivityDTO activity : day.getActivities()) {
                    // Skip if not included in price
                    if (activity.getIsIncludedInPrice() != null && !activity.getIsIncludedInPrice()) {
                        continue;
                    }

                    CostLineItem lineItem = calculateActivityCost(
                        day.getDayNumber(),
                        activity.getActivityId(),
                        activity.getActivityName(),
                        null, // No park for standalone activities
                        null,
                        seasonId,
                        itinerary.getPaxList(),
                        useStoRate,
                        carCount,
                        warnings
                    );

                    if (lineItem != null && lineItem.getTotalPrice() != null
                        && lineItem.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                        items.add(lineItem);
                        total = total.add(lineItem.getTotalPrice());
                        if (lineItem.getCurrency() != null) {
                            currency = lineItem.getCurrency();
                        }
                    }
                }
            }

            // 2. Process park activities
            if (day.getParks() != null) {
                for (FullItineraryDTO.DayParkDTO park : day.getParks()) {
                    if (park.getActivities() == null) continue;

                    Long parkId = idObfuscator.decodeId(park.getParkId());

                    for (FullItineraryDTO.ParkActivityDTO parkActivity : park.getActivities()) {
                        // Skip if not included in price
                        if (parkActivity.getIsIncludedInPrice() != null && !parkActivity.getIsIncludedInPrice()) {
                            continue;
                        }

                        CostLineItem lineItem = calculateActivityCost(
                            day.getDayNumber(),
                            parkActivity.getActivityId(),
                            parkActivity.getActivityName(),
                            parkId,
                            park.getParkName(),
                            seasonId,
                            itinerary.getPaxList(),
                            useStoRate,
                            carCount,
                            warnings
                        );

                        if (lineItem != null && lineItem.getTotalPrice() != null
                            && lineItem.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                            items.add(lineItem);
                            total = total.add(lineItem.getTotalPrice());
                            if (lineItem.getCurrency() != null) {
                                currency = lineItem.getCurrency();
                            }
                        }
                    }
                }
            }
        }

        return CostBreakdown.builder()
            .total(total)
            .currency(currency)
            .itemCount(items.size())
            .items(items)
            .build();
    }

    /**
     * Calculate cost for a single activity
     */
    private CostLineItem calculateActivityCost(
            Integer dayNumber,
            String activityIdObfuscated,
            String activityName,
            Long parkId,
            String parkName,
            Long seasonId,
            List<FullItineraryDTO.PaxDTO> paxList,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        Long activityId = idObfuscator.decodeId(activityIdObfuscated);
        if (activityId == null) {
            return null;
        }

        // Get the activity to check if it has a tariff and its charging basis
        Optional<Activity> activityOpt = activityRepository.findById(activityId);
        if (activityOpt.isEmpty()) {
            warnings.add(String.format("Activity not found: %s on Day %d", activityName, dayNumber));
            return null;
        }

        Activity activity = activityOpt.get();

        // Skip if activity has no tariff
        if (activity.getHasTariff() == null || !activity.getHasTariff()) {
            return CostLineItem.builder()
                .dayNumber(dayNumber)
                .itemType("ACTIVITY")
                .itemName(activityName + (parkName != null ? " @ " + parkName : ""))
                .quantity(1)
                .unitPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .currency(DEFAULT_CURRENCY)
                .paxCategory("Included")
                .rateFound(true)
                .build();
        }

        ChargingBasis chargingBasis = activity.getChargingBasis() != null
            ? activity.getChargingBasis()
            : ChargingBasis.PER_PERSON;

        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        boolean rateFound = false;
        int quantity = 1;
        String chargingNote = chargingBasis.getDisplayName();

        if (chargingBasis == ChargingBasis.PER_PERSON) {
            // Calculate per person - need rate for each pax category
            int totalPaxCount = 0;
            for (FullItineraryDTO.PaxDTO pax : paxList) {
                Long nationCategoryId = idObfuscator.decodeId(pax.getNationCategoryId());
                Long ageCategoryId = idObfuscator.decodeId(pax.getAgeCategoryId());

                if (nationCategoryId == null || ageCategoryId == null) continue;

                // Try park-specific rate first, then global rate
                Optional<ActivityTariffRate> rateOpt;
                if (parkId != null) {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndParkIdAndSeasonIdAndNationCategoryIdAndAgeCategoryId(
                        activityId, parkId, seasonId, nationCategoryId, ageCategoryId
                    );
                } else {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndSeasonIdAndNationCategoryIdAndAgeCategoryIdAndParkIsNull(
                        activityId, seasonId, nationCategoryId, ageCategoryId
                    );
                }

                // Fallback to global rate if park-specific not found
                if (rateOpt.isEmpty() && parkId != null) {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndSeasonIdAndNationCategoryIdAndAgeCategoryIdAndParkIsNull(
                        activityId, seasonId, nationCategoryId, ageCategoryId
                    );
                }

                if (rateOpt.isPresent()) {
                    ActivityTariffRate rate = rateOpt.get();
                    BigDecimal paxUnitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    if (paxUnitPrice != null) {
                        BigDecimal paxCost = paxUnitPrice.multiply(BigDecimal.valueOf(pax.getCount()));
                        totalPrice = totalPrice.add(paxCost);
                        totalPaxCount += pax.getCount();
                        currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                        rateFound = true;
                    }
                } else {
                    warnings.add(String.format("Activity rate not found: %s for %s %s on Day %d",
                        activityName, pax.getNationCategoryName(), pax.getAgeCategoryName(), dayNumber));
                }
            }
            quantity = totalPaxCount;
            if (totalPaxCount > 0) {
                unitPrice = totalPrice.divide(BigDecimal.valueOf(totalPaxCount), 2, RoundingMode.HALF_UP);
            }
        } else if (chargingBasis == ChargingBasis.PER_VEHICLE) {
            // PER_VEHICLE - multiply by car count
            Long highestPriorityNationId = findHighestPriorityNationCategory(paxList);

            if (highestPriorityNationId != null) {
                // Try park-specific rate first
                Optional<ActivityTariffRate> rateOpt;
                if (parkId != null) {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndParkIdAndSeasonIdAndNationCategoryIdAndAgeCategoryIsNull(
                        activityId, parkId, seasonId, highestPriorityNationId
                    );
                } else {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndSeasonIdAndNationCategoryIdAndParkIsNullAndAgeCategoryIsNull(
                        activityId, seasonId, highestPriorityNationId
                    );
                }

                // Fallback to global rate
                if (rateOpt.isEmpty() && parkId != null) {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndSeasonIdAndNationCategoryIdAndParkIsNullAndAgeCategoryIsNull(
                        activityId, seasonId, highestPriorityNationId
                    );
                }

                if (rateOpt.isPresent()) {
                    ActivityTariffRate rate = rateOpt.get();
                    unitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    if (unitPrice != null) {
                        totalPrice = unitPrice.multiply(BigDecimal.valueOf(carCount));
                        quantity = carCount;
                        currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                        rateFound = true;
                        chargingNote = String.format("Per Vehicle x %d", carCount);
                    }
                } else {
                    warnings.add(String.format("Activity rate not found: %s (vehicle) on Day %d", activityName, dayNumber));
                }
            }
        } else {
            // PER_GROUP, FLAT_RATE, etc. - single charge
            Long highestPriorityNationId = findHighestPriorityNationCategory(paxList);

            if (highestPriorityNationId != null) {
                Optional<ActivityTariffRate> rateOpt;
                if (parkId != null) {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndParkIdAndSeasonIdAndNationCategoryIdAndAgeCategoryIsNull(
                        activityId, parkId, seasonId, highestPriorityNationId
                    );
                } else {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndSeasonIdAndNationCategoryIdAndParkIsNullAndAgeCategoryIsNull(
                        activityId, seasonId, highestPriorityNationId
                    );
                }

                // Fallback to global rate
                if (rateOpt.isEmpty() && parkId != null) {
                    rateOpt = activityTariffRateRepository.findByActivityIdAndSeasonIdAndNationCategoryIdAndParkIsNullAndAgeCategoryIsNull(
                        activityId, seasonId, highestPriorityNationId
                    );
                }

                if (rateOpt.isPresent()) {
                    ActivityTariffRate rate = rateOpt.get();
                    unitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    if (unitPrice != null) {
                        totalPrice = unitPrice;
                        quantity = 1;
                        currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                        rateFound = true;
                    }
                } else {
                    warnings.add(String.format("Activity rate not found: %s (%s) on Day %d",
                        activityName, chargingBasis.getDisplayName(), dayNumber));
                }
            }
        }

        return CostLineItem.builder()
            .dayNumber(dayNumber)
            .itemType("ACTIVITY")
            .itemName(activityName + (parkName != null ? " @ " + parkName : ""))
            .quantity(quantity)
            .unitPrice(unitPrice)
            .totalPrice(totalPrice)
            .currency(currency)
            .paxCategory(chargingNote)
            .rateFound(rateFound)
            .build();
    }

    /**
     * Build day-by-day cost summaries for breakdown view
     */
    private List<DayCostSummary> buildDayCostSummaries(
            FullItineraryDTO itinerary,
            LocalDate startDate,
            CostBreakdown parkFeeCosts,
            CostBreakdown accommodationCosts,
            CostBreakdown activityCosts
    ) {
        List<DayCostSummary> summaries = new ArrayList<>();

        if (itinerary.getDays() == null) {
            return summaries;
        }

        for (FullItineraryDTO.DayDTO day : itinerary.getDays()) {
            int dayNum = day.getDayNumber();
            LocalDate dayDate = startDate.plusDays(dayNum - 1);

            // Sum costs for this day
            BigDecimal parkTotal = sumCostsForDay(parkFeeCosts, dayNum);
            BigDecimal accTotal = sumCostsForDay(accommodationCosts, dayNum);
            BigDecimal actTotal = sumCostsForDay(activityCosts, dayNum);
            BigDecimal dayTotal = parkTotal.add(accTotal).add(actTotal);

            // Determine primary currency for this day
            String dayCurrency = determineCurrencyForDay(parkFeeCosts, accommodationCosts, activityCosts, dayNum);

            // Get season name for this day
            Season daySeason = findSeasonForDate(dayDate);
            String seasonName = daySeason != null ? daySeason.getName() : "Unknown";

            DayCostSummary summary = DayCostSummary.builder()
                .dayNumber(dayNum)
                .dayTitle(day.getTitle())
                .date(dayDate)
                .seasonName(seasonName)
                .accommodationCost(accTotal)
                .parkFeeCost(parkTotal)
                .activityCost(actTotal)
                .dayTotal(dayTotal)
                .currency(dayCurrency)
                .build();

            summaries.add(summary);
        }

        return summaries;
    }

    /**
     * Sum costs for a specific day from a cost breakdown
     */
    private BigDecimal sumCostsForDay(CostBreakdown breakdown, int dayNumber) {
        if (breakdown == null || breakdown.getItems() == null) {
            return BigDecimal.ZERO;
        }

        return breakdown.getItems().stream()
            .filter(item -> item.getDayNumber() != null && item.getDayNumber() == dayNumber)
            .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Determine the primary currency for a day's costs
     */
    private String determineCurrencyForDay(
            CostBreakdown parkFeeCosts,
            CostBreakdown accommodationCosts,
            CostBreakdown activityCosts,
            int dayNumber
    ) {
        // Check in order of priority: accommodation, park fees, activities
        String currency = findCurrencyForDay(accommodationCosts, dayNumber);
        if (currency == null) {
            currency = findCurrencyForDay(parkFeeCosts, dayNumber);
        }
        if (currency == null) {
            currency = findCurrencyForDay(activityCosts, dayNumber);
        }
        return currency != null ? currency : DEFAULT_CURRENCY;
    }

    /**
     * Find currency from a cost breakdown for a specific day
     */
    private String findCurrencyForDay(CostBreakdown breakdown, int dayNumber) {
        if (breakdown == null || breakdown.getItems() == null) {
            return null;
        }

        return breakdown.getItems().stream()
            .filter(item -> item.getDayNumber() != null && item.getDayNumber() == dayNumber)
            .map(CostLineItem::getCurrency)
            .filter(c -> c != null && !c.isEmpty())
            .findFirst()
            .orElse(null);
    }
}
