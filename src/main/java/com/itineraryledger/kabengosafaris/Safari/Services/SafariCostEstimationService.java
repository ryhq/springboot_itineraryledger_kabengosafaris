package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.Flight.CostEstimation.FlightFarePricer;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO.*;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
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
 * Service for estimating safari costs.
 *
 * Provides quick budget calculations based on:
 * - Park fees (tariffs)
 * - Accommodation rates
 * - Activity rates
 *
 * Uses current rates based on season determined from safari start date.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SafariCostEstimationService {

    private final SafariFullGetService safariFullGetService;
    private final ParkTariffRateRepository parkTariffRateRepository;
    private final ParkTariffRepository parkTariffRepository;
    private final AccommodationRateRepository accommodationRateRepository;
    private final ActivityTariffRateRepository activityTariffRateRepository;
    private final ActivityRepository activityRepository;
    private final SeasonPeriodRepository seasonPeriodRepository;
    private final SeasonResolverService seasonResolverService;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final IdObfuscator idObfuscator;

    // Default currency if not specified
    private static final String DEFAULT_CURRENCY = "USD";

    /**
     * Estimate costs for a safari
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param useStoRate Whether to use STO (Special Tour Operator) rates (default: true)
     * @param currency Preferred output currency (default: USD)
     * @return ResponseEntity with cost estimation or error
     */
    public ResponseEntity<ApiResponse<?>> estimateCosts(
            String safariIdObfuscated,
            Boolean useStoRate,
            String currency
    ) {
        try {
            // Defaults
            if (useStoRate == null) {
                useStoRate = true;
            }
            if (currency == null || currency.isBlank()) {
                currency = DEFAULT_CURRENCY;
            }

            // 1. Fetch full safari data
            ResponseEntity<ApiResponse<?>> safariResponse = safariFullGetService.getFullSafari(safariIdObfuscated);
            if (!safariResponse.getStatusCode().is2xxSuccessful()) {
                return safariResponse;
            }

            ApiResponse<?> apiResponse = safariResponse.getBody();
            if (apiResponse == null || apiResponse.getData() == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Extract safari from wrapped response (getFullSafari returns {safari: ..., nextId: ..., previousId: ...})
            FullSafariDTO safari;
            Object responseData = apiResponse.getData();
            if (responseData instanceof FullSafariDTO) {
                safari = (FullSafariDTO) responseData;
            } else if (responseData instanceof Map) {
                Object safariObj = ((Map<?, ?>) responseData).get("safari");
                if (safariObj instanceof FullSafariDTO) {
                    safari = (FullSafariDTO) safariObj;
                } else {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                    );
                }
            } else {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // 2. Use safari's startDate and endDate directly
            LocalDate startDate = safari.getStartDate();
            LocalDate endDate = safari.getEndDate();

            if (startDate == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari has no start date configured", "NO_START_DATE")
                );
            }

            if (endDate == null) {
                endDate = startDate.plusDays(safari.getTotalDays() != null ? safari.getTotalDays() - 1 : 0);
            }

            // 3. Find applicable season for start date
            Season applicableSeason = findSeasonForDate(startDate);
            String seasonName = applicableSeason != null ? applicableSeason.getName() : "Unknown Season";

            // 4. Build pax summary
            List<PaxCategorySummary> paxBreakdown = buildPaxBreakdown(safari);
            int totalPax = paxBreakdown.stream().mapToInt(PaxCategorySummary::getCount).sum();

            // 5. Get car count from safari (default to 1)
            int carCount = safari.getCarCount() != null ? safari.getCarCount() : 1;

            // 6. Calculate costs
            List<String> warnings = new ArrayList<>();

            // 6a. Park fee costs (with carCount support)
            CostBreakdown parkFeeCosts = calculateParkFeeCosts(
                safari, startDate, applicableSeason, useStoRate, carCount, warnings
            );

            // 6b. Accommodation costs (per night calculation)
            CostBreakdown accommodationCosts = calculateAccommodationCosts(
                safari, startDate, useStoRate, warnings
            );

            // 6c. Activity costs (standalone and park activities)
            CostBreakdown activityCosts = calculateActivityCosts(
                safari, startDate, applicableSeason, useStoRate, carCount, warnings
            );

            // 7. Calculate totals by currency (multi-currency support)
            CostBreakdown flightCosts = calculateFlightCosts(safari, startDate, warnings);

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

            /*
             * Flights into the subtotal too. Easy to forget, and the failure is quiet: the sector
             * shows in its own breakdown and is missing from the figure the invoice is built from.
             */
            if (flightCosts.getItems() != null) {
                for (CostLineItem item : flightCosts.getItems()) {
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
                safari, startDate, parkFeeCosts, accommodationCosts, activityCosts
            );

            // 9. Build response
            ItineraryCostEstimationDTO estimation = ItineraryCostEstimationDTO.builder()
                .itineraryId(safari.getId())
                .itineraryCode(safari.getCode())
                .itineraryName(safari.getName())
                .totalDays(safari.getTotalDays())
                .totalNights(safari.getTotalNights())
                .startDate(startDate)
                .endDate(endDate)
                .seasonName(seasonName)
                .totalPax(totalPax)
                .paxBreakdown(paxBreakdown)
                .parkFeeCosts(parkFeeCosts)
                .accommodationCosts(accommodationCosts)
                .activityCosts(activityCosts)
                .flightCosts(flightCosts)
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
            log.error("Failed to estimate costs for safari: {}", safariIdObfuscated, e);
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
     * Build pax category summary from safari
     */
    private List<PaxCategorySummary> buildPaxBreakdown(FullSafariDTO safari) {
        if (safari.getPaxList() == null || safari.getPaxList().isEmpty()) {
            return new ArrayList<>();
        }

        return safari.getPaxList().stream()
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
            FullSafariDTO safari,
            LocalDate startDate,
            Season season,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        List<CostLineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;

        if (safari.getDays() == null || season == null) {
            return CostBreakdown.builder()
                .total(BigDecimal.ZERO)
                .currency(currency)
                .itemCount(0)
                .items(items)
                .build();
        }

        Long seasonId = season.getId();

        for (FullSafariDTO.DayDTO day : safari.getDays()) {
            if (day.getParks() == null) continue;

            for (FullSafariDTO.DayParkDTO park : day.getParks()) {
                if (park.getTariffs() == null) continue;

                Long parkId = idObfuscator.decodeId(park.getParkId());
                if (parkId == null) continue;

                for (FullSafariDTO.ParkTariffDTO tariff : park.getTariffs()) {
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
                    List<CostLineItem> lineItems = calculateParkTariffCost(
                        day.getDayNumber(),
                        park.getParkName(),
                        tariff.getTariffName(),
                        parkId,
                        tariffId,
                        seasonId,
                        chargingBasis,
                        safari.getPaxList(),
                        useStoRate,
                        carCount,
                        warnings
                    );

                    for (CostLineItem lineItem : lineItems) {
                        if (lineItem.getTotalPrice() == null) continue;
                        items.add(lineItem);
                        /*
                         * `total` and `currency` are a single-currency convenience and become a
                         * cross-currency sum the moment a party mixes nationalities. Nothing that
                         * reaches a customer reads them: the invoice is built from `items`, each
                         * carrying its own currency.
                         */
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
    /**
     * One line per currency the bands are charged in.
     *
     * <p>Ngorongoro holds its resident fee in TZS and its visitor fee in USD. Summing them and
     * labelling the result with whichever rate the loop read last put TZS 222,222 of shillings
     * into a dollar invoice and turned USD 24,655 into USD 246,877.
     */
    private List<CostLineItem> calculateParkTariffCost(
            Integer dayNumber,
            String parkName,
            String tariffName,
            Long parkId,
            Long tariffId,
            Long seasonId,
            ChargingBasis chargingBasis,
            List<FullSafariDTO.PaxDTO> paxList,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        if (paxList == null || paxList.isEmpty()) {
            warnings.add(String.format("No pax configured for tariff: %s at %s", tariffName, parkName));
            return List.of();
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        boolean rateFound = false;
        int quantity = 1;
        String chargingNote = chargingBasis.getDisplayName();

        if (chargingBasis == ChargingBasis.PER_PERSON) {
            Map<String, BigDecimal> totalByCurrency = new LinkedHashMap<>();
            Map<String, Integer> paxByCurrency = new LinkedHashMap<>();

            for (FullSafariDTO.PaxDTO pax : paxList) {
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
                        String rateCurrency = rate.getCurrency() != null
                            ? rate.getCurrency() : DEFAULT_CURRENCY;
                        totalByCurrency.merge(rateCurrency,
                            paxUnitPrice.multiply(BigDecimal.valueOf(pax.getCount())), BigDecimal::add);
                        paxByCurrency.merge(rateCurrency, pax.getCount(), Integer::sum);
                    }
                } else {
                    warnings.add(String.format("Rate not found: %s for %s %s at %s",
                        tariffName, pax.getNationCategoryName(), pax.getAgeCategoryName(), parkName));
                }
            }

            if (totalByCurrency.isEmpty()) {
                return List.of(CostLineItem.builder()
                    .dayNumber(dayNumber).itemType("PARK_FEE")
                    .itemName(tariffName + " - " + parkName)
                    .quantity(0).unitPrice(BigDecimal.ZERO).totalPrice(BigDecimal.ZERO)
                    .currency(DEFAULT_CURRENCY).paxCategory(chargingNote).rateFound(false).build());
            }

            List<CostLineItem> perCurrency = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> e : totalByCurrency.entrySet()) {
                int paxCount = paxByCurrency.getOrDefault(e.getKey(), 0);
                perCurrency.add(CostLineItem.builder()
                    .dayNumber(dayNumber).itemType("PARK_FEE")
                    .itemName(tariffName + " - " + parkName)
                    .quantity(paxCount)
                    .unitPrice(paxCount > 0
                        ? e.getValue().divide(BigDecimal.valueOf(paxCount), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                    .totalPrice(e.getValue())
                    .currency(e.getKey()).paxCategory(chargingNote).rateFound(true).build());
            }
            return perCurrency;
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

        /* Vehicle and group rates are a single rate, so a single currency. */
        return List.of(CostLineItem.builder()
            .dayNumber(dayNumber)
            .itemType("PARK_FEE")
            .itemName(tariffName + " - " + parkName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .totalPrice(totalPrice)
            .currency(currency)
            .paxCategory(chargingNote)
            .rateFound(rateFound)
            .build());
    }

    /**
     * Find the highest priority nation category from pax list
     * (For vehicle/group rates, the highest priority pax determines the rate)
     *
     * Priority is determined by the PaxNationCategory.priorityFactor field.
     * Higher priority factor = higher priority (e.g., Non-Resident > East African > Resident)
     */
    private Long findHighestPriorityNationCategory(List<FullSafariDTO.PaxDTO> paxList) {
        if (paxList == null || paxList.isEmpty()) {
            return null;
        }

        Long highestPriorityId = null;
        int highestPriority = Integer.MIN_VALUE;

        for (FullSafariDTO.PaxDTO pax : paxList) {
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
            FullSafariDTO safari,
            LocalDate startDate,
            boolean useStoRate,
            List<String> warnings
    ) {
        List<CostLineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;

        if (safari.getDays() == null) {
            return CostBreakdown.builder()
                .total(BigDecimal.ZERO)
                .currency(currency)
                .itemCount(0)
                .items(items)
                .build();
        }

        for (FullSafariDTO.DayDTO day : safari.getDays()) {
            if (day.getAccommodations() == null) continue;

            // Skip non-overnight days (e.g., departure day) - no accommodation cost
            if (day.getIsOvernight() != null && !day.getIsOvernight()) {
                continue;
            }

            LocalDate dayDate = startDate.plusDays(day.getDayNumber() - 1);

            /*
             * Who sleeps where, settled for the whole night before any row is priced. A
             * per-person rate charges per guest, and this used to hand every row the whole
             * party, so a night written as several room types billed the party several times.
             */
            java.util.List<FullSafariDTO.DayAccommodationDTO> chosenStays = day.getAccommodations().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsAlternative()))
                .toList();
            int partyOnTrip = safari.getPaxList() != null
                ? safari.getPaxList().stream().mapToInt(p -> p.getCount() != null ? p.getCount() : 0).sum()
                : 0;
            com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices
                .RoomOccupancyAllocator.Allocation nightAllocation =
                com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices
                    .RoomOccupancyAllocator.allocate(
                        chosenStays.stream()
                            .map(a -> new com.itineraryledger.kabengosafaris.Accommodation.Services
                                .AccommodationServices.RoomOccupancyAllocator.RoomBlock(
                                    a.getRoomCount() != null ? a.getRoomCount() : 1,
                                    a.getRoomTypeMaxOccupancy()))
                            .toList(),
                        partyOnTrip);
            if (nightAllocation.occupancyUnknown()) {
                warnings.add(String.format(
                    "Day %d: a room type has no occupancy recorded, so the night could not be "
                    + "split across its rooms", day.getDayNumber()));
            }
            if (nightAllocation.isShort()) {
                /* Priced for the beds it has, so the figure drops and somebody asks why. */
                warnings.add(String.format("Day %d: %s - priced for the beds it has",
                    day.getDayNumber(), nightAllocation.shortfallMessage()));
            }

            for (int stayIndex = 0; stayIndex < chosenStays.size(); stayIndex++) {
                FullSafariDTO.DayAccommodationDTO accommodation = chosenStays.get(stayIndex);
                int sleepingHere = nightAllocation.perRow().get(stayIndex);

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
                    items.add(unpricedNight(day, accommodation, "no season or room details"));
                    continue;
                }

                /* Active rates only: a deactivated rate row is not a price anybody may be billed. */
                Optional<AccommodationRate> rateOpt = accommodationRateRepository
                    .findActiveRate(accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId);

                if (rateOpt.isPresent()) {
                    AccommodationRate rate = rateOpt.get();
                    BigDecimal unitPrice = useStoRate && rate.getStoRate() != null
                        ? rate.getStoRate()
                        : rate.getRackRate();

                    int roomCount = accommodation.getRoomCount() != null ? accommodation.getRoomCount() : 1;
                    int totalPax = safari.getPaxList() != null
                        ? safari.getPaxList().stream().mapToInt(p -> p.getCount() != null ? p.getCount() : 0).sum()
                        : 0;

                    // Calculate based on whether rate is per person or per room
                    // Rate is always per night (1 night per day entry)
                    BigDecimal itemTotal;
                    int quantity;
                    String rateTypeNote;

                    if (rate.getIsPerPerson() != null && rate.getIsPerPerson()) {
                        /* Per person means per person IN THESE ROOMS, not everybody on the
                         * trip - otherwise a second room type bills the party a second time. */
                        quantity = sleepingHere;
                        itemTotal = unitPrice != null
                            ? unitPrice.multiply(BigDecimal.valueOf(sleepingHere))
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
                    items.add(unpricedNight(day, accommodation, "no rate for this season"));
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
     * A night we could not price, still on the list.
     *
     * <p>The same bug as on the quote side, on the path that bills. When a rate could not be found
     * this service appended a sentence to a warnings list that nothing downstream reads, and did
     * not add the line. The night vanished from the invoice, and the property was not even named
     * for anybody to notice it was absent.
     *
     * <p>A zero on a line somebody can see gets queried before the invoice goes out. A line that
     * is not there does not. {@code rateFound = false} is what the writers already look for to
     * print "Rate not found" beside it.
     */
    private CostLineItem unpricedNight(
            FullSafariDTO.DayDTO day,
            FullSafariDTO.DayAccommodationDTO accommodation,
            String why
    ) {
        return CostLineItem.builder()
            .dayNumber(day.getDayNumber())
            .itemType("ACCOMMODATION")
            .itemName(accommodation.getAccommodationName()
                + (accommodation.getRoomTypeName() != null ? " - " + accommodation.getRoomTypeName() : ""))
            .referenceId(accommodation.getAccommodationId())
            .quantity(accommodation.getRoomCount() != null ? accommodation.getRoomCount() : 1)
            .unitPrice(BigDecimal.ZERO)
            .totalPrice(BigDecimal.ZERO)
            .currency(DEFAULT_CURRENCY)
            .notes("Rate not found - " + why)
            .rateFound(false)
            .build();
    }

    /**
     * Find the accommodation's season for a date, through the shared resolver.
     *
     * <p>This used to walk every season period in the database, take the first whose dates matched
     * and never ask whether the season itself was still switched on -- then fall back to a GLOBAL
     * season, which can never hold an accommodation rate, so the night could not be priced and was
     * dropped. That is the bug that took 1,695 and 1,830 off two quotes, and it lived here too, on
     * the path that raises invoices.
     *
     * <p>Kept as a one-line delegation rather than deleted outright so the call site reads the same
     * as before. What it must never do again is carry its own rules.
     */
    private Season findAccommodationSeasonForDate(Long accommodationId, LocalDate date) {
        return seasonResolverService.resolveAccommodationSeasonWithDetails(accommodationId, date)
            .season();
    }

    /**
     * Calculate activity costs for all days
     *
     * Handles both:
     * - Standalone activities (SafariDayActivity - activities not tied to a park)
     * - Park activities (SafariDayParkActivity - activities within a park visit)
     *
     * Activity pricing follows the Activity's ChargingBasis:
     * - PER_PERSON: Rate per person multiplied by pax count
     * - PER_VEHICLE: Rate per vehicle multiplied by car count
     * - PER_GROUP/FLAT_RATE: Single flat rate for the entire group
     *
     * @param carCount Number of vehicles for PER_VEHICLE activities
     */
    private CostBreakdown calculateActivityCosts(
            FullSafariDTO safari,
            LocalDate startDate,
            Season season,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        List<CostLineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;

        if (safari.getDays() == null || season == null) {
            return CostBreakdown.builder()
                .total(BigDecimal.ZERO)
                .currency(currency)
                .itemCount(0)
                .items(items)
                .build();
        }

        Long seasonId = season.getId();

        for (FullSafariDTO.DayDTO day : safari.getDays()) {
            // 1. Process standalone day activities
            if (day.getActivities() != null) {
                for (FullSafariDTO.DayActivityDTO activity : day.getActivities()) {
                    // Skip if not included in price
                    if (activity.getIsIncludedInPrice() != null && !activity.getIsIncludedInPrice()) {
                        continue;
                    }

                    for (CostLineItem lineItem : calculateActivityCost(
                        day.getDayNumber(),
                        activity.getActivityId(),
                        activity.getActivityName(),
                        null, // No park for standalone activities
                        null,
                        seasonId,
                        safari.getPaxList(),
                        useStoRate,
                        carCount,
                        warnings
                    )) {
                        if (lineItem.getTotalPrice() != null
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

            // 2. Process park activities
            if (day.getParks() != null) {
                for (FullSafariDTO.DayParkDTO park : day.getParks()) {
                    if (park.getActivities() == null) continue;

                    Long parkId = idObfuscator.decodeId(park.getParkId());

                    for (FullSafariDTO.ParkActivityDTO parkActivity : park.getActivities()) {
                        // Skip if not included in price
                        if (parkActivity.getIsIncludedInPrice() != null && !parkActivity.getIsIncludedInPrice()) {
                            continue;
                        }

                        for (CostLineItem lineItem : calculateActivityCost(
                            day.getDayNumber(),
                            parkActivity.getActivityId(),
                            parkActivity.getActivityName(),
                            parkId,
                            park.getParkName(),
                            seasonId,
                            safari.getPaxList(),
                            useStoRate,
                            carCount,
                            warnings
                        )) {
                            if (lineItem.getTotalPrice() != null
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
    /**
     * One line per currency, for the same reason the park fee is: the activity rate card holds
     * resident bands in shillings and visitor bands in dollars.
     */
    private List<CostLineItem> calculateActivityCost(
            Integer dayNumber,
            String activityIdObfuscated,
            String activityName,
            Long parkId,
            String parkName,
            Long seasonId,
            List<FullSafariDTO.PaxDTO> paxList,
            boolean useStoRate,
            int carCount,
            List<String> warnings
    ) {
        Long activityId = idObfuscator.decodeId(activityIdObfuscated);
        if (activityId == null) {
            return List.of();
        }

        // Get the activity to check if it has a tariff and its charging basis
        Optional<Activity> activityOpt = activityRepository.findById(activityId);
        if (activityOpt.isEmpty()) {
            warnings.add(String.format("Activity not found: %s on Day %d", activityName, dayNumber));
            return List.of();
        }

        Activity activity = activityOpt.get();

        // Skip if activity has no tariff
        if (activity.getHasTariff() == null || !activity.getHasTariff()) {
            return List.of(CostLineItem.builder()
                .dayNumber(dayNumber)
                .itemType("ACTIVITY")
                .itemName(activityName + (parkName != null ? " @ " + parkName : ""))
                .quantity(1)
                .unitPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .currency(DEFAULT_CURRENCY)
                .paxCategory("Included")
                .rateFound(true)
                .build());
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
            Map<String, BigDecimal> totalByCurrency = new LinkedHashMap<>();
            Map<String, Integer> paxByCurrency = new LinkedHashMap<>();

            for (FullSafariDTO.PaxDTO pax : paxList) {
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
                        String rateCurrency = rate.getCurrency() != null
                            ? rate.getCurrency() : DEFAULT_CURRENCY;
                        totalByCurrency.merge(rateCurrency,
                            paxUnitPrice.multiply(BigDecimal.valueOf(pax.getCount())), BigDecimal::add);
                        paxByCurrency.merge(rateCurrency, pax.getCount(), Integer::sum);
                    }
                } else {
                    warnings.add(String.format("Activity rate not found: %s for %s %s on Day %d",
                        activityName, pax.getNationCategoryName(), pax.getAgeCategoryName(), dayNumber));
                }
            }

            String activityItemName = activityName + (parkName != null ? " @ " + parkName : "");
            if (totalByCurrency.isEmpty()) {
                return List.of(CostLineItem.builder()
                    .dayNumber(dayNumber).itemType("ACTIVITY").itemName(activityItemName)
                    .quantity(0).unitPrice(BigDecimal.ZERO).totalPrice(BigDecimal.ZERO)
                    .currency(DEFAULT_CURRENCY).paxCategory(chargingNote).rateFound(false).build());
            }

            List<CostLineItem> perCurrency = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> e : totalByCurrency.entrySet()) {
                int paxCount = paxByCurrency.getOrDefault(e.getKey(), 0);
                perCurrency.add(CostLineItem.builder()
                    .dayNumber(dayNumber).itemType("ACTIVITY").itemName(activityItemName)
                    .quantity(paxCount)
                    .unitPrice(paxCount > 0
                        ? e.getValue().divide(BigDecimal.valueOf(paxCount), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                    .totalPrice(e.getValue())
                    .currency(e.getKey()).paxCategory(chargingNote).rateFound(true).build());
            }
            return perCurrency;
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

        /* Vehicle and group rates are a single rate, so a single currency. */
        return List.of(CostLineItem.builder()
            .dayNumber(dayNumber)
            .itemType("ACTIVITY")
            .itemName(activityName + (parkName != null ? " @ " + parkName : ""))
            .quantity(quantity)
            .unitPrice(unitPrice)
            .totalPrice(totalPrice)
            .currency(currency)
            .paxCategory(chargingNote)
            .rateFound(rateFound)
            .build());
    }

    /**
     * Price the flight sectors on a safari.
     *
     * <p>This did not exist, and nothing said so. The invoice generator has always asked for
     * {@code getFlightCosts()}, the orchestrator behind the Cost tab has always priced flights,
     * and this service - the one that actually builds the invoice - returned nothing. So the
     * Arusha to Zanzibar sector showed on screen at USD 764.16 and was absent from the invoice
     * the customer was asked to pay. Ported from the itinerary service, which has had it all
     * along.
     */
    private CostBreakdown calculateFlightCosts(
            FullSafariDTO safari,
            LocalDate startDate,
            List<String> warnings
    ) {
        List<CostLineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;

        if (safari.getDays() == null) {
            return CostBreakdown.builder()
                .total(BigDecimal.ZERO).currency(currency).itemCount(0).items(items).build();
        }

        int adults = 0;
        int children = 0;
        if (safari.getPaxList() != null) {
            for (FullSafariDTO.PaxDTO pax : safari.getPaxList()) {
                int count = pax.getCount() == null ? 0 : pax.getCount();
                String age = pax.getAgeCategoryName() == null ? "" : pax.getAgeCategoryName().toLowerCase();
                if (age.contains("child") || age.contains("infant") || age.contains("youth")) children += count;
                else adults += count;
            }
        }

        for (FullSafariDTO.DayDTO day : safari.getDays()) {
            if (day.getFlights() == null) continue;
            LocalDate dayDate = startDate == null ? null
                : startDate.plusDays((day.getDayNumber() == null ? 1 : day.getDayNumber()) - 1);

            for (FullItineraryDTO.DayFlightDTO flight : day.getFlights()) {
                if (Boolean.TRUE.equals(flight.getIsAlternative())) continue;
                if (Boolean.FALSE.equals(flight.getIsIncludedInPrice())) continue;

                int seatAdults = adults;
                int seatChildren = children;
                if (flight.getPassengerCount() != null && flight.getPassengerCount() > 0) {
                    seatAdults = flight.getPassengerCount();
                    seatChildren = 0;
                }
                if (seatAdults + seatChildren == 0) continue;

                FlightFare fare = FlightFare.builder()
                    .netFare(flight.getNetFare())
                    .taxesAndFees(flight.getTaxesAndFees() == null ? BigDecimal.ZERO : flight.getTaxesAndFees())
                    .childPercent(flight.getChildPercent())
                    .currency(flight.getCurrency() == null ? DEFAULT_CURRENCY : flight.getCurrency())
                    .operatingMonths(flight.getOperatingMonths())
                    .minimumSeats(flight.getMinimumSeats())
                    .validFrom(LocalDate.MIN).validTo(LocalDate.MAX)
                    .build();

                MarkupType markupType = null;
                if (flight.getMarkupType() != null && !flight.getMarkupType().isBlank()) {
                    try {
                        markupType = MarkupType.valueOf(flight.getMarkupType());
                    } catch (IllegalArgumentException ignored) {
                        /* An unrecognised stored value means no markup, not a failed estimate. */
                    }
                }

                var priced = FlightFarePricer.price(fare, null, seatAdults, seatChildren,
                    markupType, flight.getMarkupValue(), dayDate);
                warnings.addAll(priced.warnings());

                String name = ((flight.getAirlineName() == null ? "Flight" : flight.getAirlineName())
                    + " " + (flight.getSectorLabel() == null ? "" : flight.getSectorLabel())
                    + (flight.getEtd() == null ? "" : ", " + flight.getEtd())).trim();

                items.add(CostLineItem.builder()
                    .dayNumber(day.getDayNumber())
                    .itemType("FLIGHT")
                    .itemName(name)
                    .referenceId(flight.getFlightFareId())
                    .quantity(seatAdults + seatChildren)
                    .unitPrice(priced.sellingPerAdult())
                    .totalPrice(priced.totalSelling())
                    .currency(priced.currency())
                    .paxCategory("Per Person")
                    .notes("Fare " + priced.currency() + " " + priced.netPerAdult()
                        + ", markup from " + (flight.getMarkupSource() == null
                            ? priced.markup().source() : flight.getMarkupSource())
                        + ", tax " + priced.taxPerPerson() + " per person (never marked up)")
                    /* A flight with no net fare is priced at nothing and says so, like a missing rate. */
                    .rateFound(priced.isPriceable())
                    .build());

                total = total.add(priced.totalSelling());
                currency = priced.currency();
            }
        }

        return CostBreakdown.builder()
            .total(total).currency(currency).itemCount(items.size()).items(items).build();
    }

    /**
     * Build day-by-day cost summaries for breakdown view
     */
    private List<DayCostSummary> buildDayCostSummaries(
            FullSafariDTO safari,
            LocalDate startDate,
            CostBreakdown parkFeeCosts,
            CostBreakdown accommodationCosts,
            CostBreakdown activityCosts
    ) {
        List<DayCostSummary> summaries = new ArrayList<>();

        if (safari.getDays() == null) {
            return summaries;
        }

        for (FullSafariDTO.DayDTO day : safari.getDays()) {
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
