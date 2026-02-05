# Safari Cost Estimation Algorithm

## Overview

This document describes the algorithm used to estimate safari costs. The system calculates costs for accommodations, park fees, and activities across all days of a safari, supporting two output modes (Per-Day and Per-PAX) while always returning both STO and Rack rates.

**Note**: The Safari cost estimation algorithm is identical to the Itinerary cost estimation algorithm, with the key difference being that Safari uses actual dates from the Safari entity rather than a start date parameter.

---

## High-Level Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SAFARI COST ESTIMATION FLOW                          │
└─────────────────────────────────────────────────────────────────────────┘

1. INPUT
   ├── Safari ID (obfuscated)
   └── Mode (PER_DAY or PER_PAX)

2. FETCH DATA
   └── Load Full Safari (days with dates, pax, parks, activities, accommodations)

3. FOR EACH DAY
   ├── Use Day Date (from SafariDay entity)
   ├── Resolve Season (global season for the day's date)
   ├── Calculate Accommodation Costs
   ├── Calculate Park Tariff Costs
   └── Calculate Activity Costs

4. AGGREGATE
   ├── PER_DAY Mode: Group by day → currency
   └── PER_PAX Mode: Group by pax category → currency

5. OUTPUT
   ├── Line items with STO + Rack prices
   ├── Totals grouped by currency (no mixing)
   └── Rate issues log
```

---

## Key Differences from Itinerary Cost Estimation

### 1. Date Handling
**Itinerary**: Takes `startDate` as a parameter (optional, defaults to today). Calculates day dates as `startDate + (dayNumber - 1)`.

**Safari**: Uses actual dates from the Safari entity:
- Safari has `startDate` and `endDate` in the entity
- Each SafariDay has its own `date` field
- No need for date calculation or startDate parameter

### 2. Entity Structure
**Itinerary**: Works with ItineraryDay, ItineraryDayAccommodation, ItineraryDayPark, ItineraryDayActivity entities via DTOs.

**Safari**: Works with SafariDay, SafariDayAccommodation, SafariDayPark, SafariDayActivity entities via DTOs.

### 3. API Endpoint
**Itinerary**: `GET /api/itineraries/{itineraryId}/cost-estimation?startDate=2024-01-01&mode=PER_DAY`

**Safari**: `GET /api/safaris/{safariId}/cost-estimation?mode=PER_DAY`

---

## Detailed Algorithm

### Phase 1: Initialization

```
FUNCTION estimate(safariId, mode):

    // 1. Apply defaults
    IF mode IS NULL:
        mode = PER_DAY

    // 2. Fetch full safari
    safari = fetchFullSafari(safariId)

    // 3. Validate dates
    IF safari.startDate IS NULL OR safari.endDate IS NULL:
        RETURN error("Safari must have start and end dates")

    // 4. Extract key values
    totalDays = safari.totalDays
    startDate = safari.startDate
    endDate = safari.endDate
    carCount = safari.carCount OR 1
    paxList = safari.paxList
    totalPax = SUM(pax.count FOR pax IN paxList)

    // 5. Clear rate issue logger (request-scoped)
    rateIssueLogger.clear()
```

### Phase 2: Day-by-Day Calculation

```
FOR EACH day IN safari.days:

    // Safari days already have dates from the entity
    dayDate = day.date
    globalSeason = resolveGlobalSeason(dayDate)

    // Calculate all cost types for this day
    accommodationItems = calculateAccommodationCosts(day, dayDate, paxList)
    parkFeeItems = calculateParkTariffCosts(day, dayDate, globalSeason, paxList, carCount)
    activityItems = calculateActivityCosts(day, dayDate, globalSeason, paxList, carCount)

    // Combine all line items for this day
    dayItems = accommodationItems + parkFeeItems + activityItems
```

---

## Calculation Components

The Safari cost estimation system reuses the following shared components from the Itinerary cost estimation:

### Shared Components (Identical Logic)

1. **DTOs** (except SafariCostEstimationResponseDTO):
   - CostLineItemDTO
   - CurrencyGroupedCostDTO
   - DayCostDetailDTO
   - PaxCategoryCostDTO
   - RateIssueLogDTO

2. **Enums**:
   - CalculationMode
   - CostItemType
   - RateIssueType

3. **Core Services**:
   - SeasonResolverService
   - AccommodationRateLookupService
   - ParkTariffRateLookupService
   - ActivityRateLookupService
   - RateIssueLoggerService

### Safari-Specific Components

1. **Controller**:
   - SafariCostEstimationController

2. **Response DTO**:
   - SafariCostEstimationResponseDTO (uses safariId, safariCode, safariName instead of itinerary equivalents)

3. **Orchestrator**:
   - SafariCostEstimationOrchestrator (works with FullSafariDTO)

4. **Calculators**:
   - SafariAccommodationCostCalculator (works with SafariDayAccommodation)
   - SafariParkTariffCostCalculator (works with SafariDayParkTariff)
   - SafariActivityCostCalculator (works with SafariDayActivity and SafariDayParkActivity)

5. **Aggregators**:
   - SafariPerDayCostAggregator
   - SafariPerPaxCostAggregator

---

## Accommodation Cost Calculation

**Same as Itinerary** - See Itinerary ALGORITHM.md for full details.

Key points:
- Skips non-overnight days
- Skips alternative accommodations
- Uses accommodation-specific season resolution (NO fallback to global)
- Supports per-person (PPS) and per-room pricing
- Returns both STO and Rack rates

---

## Park Tariff Cost Calculation

**Same as Itinerary** - See Itinerary ALGORITHM.md for full details.

Key points:
- Supports PER_PERSON, PER_VEHICLE, PER_GROUP, FLAT_RATE charging bases
- Uses highest priority nation category for group/vehicle rates
- Per-person rates calculated for each pax category
- Returns both STO and Rack rates

---

## Activity Cost Calculation

**Same as Itinerary** - See Itinerary ALGORITHM.md for full details.

Key points:
- Handles standalone activities and park activities
- Skips optional activities
- Skips activities with no tariff (free)
- Park-specific rates take precedence over global rates
- Supports all charging bases
- Returns both STO and Rack rates

---

## Aggregation Phase

### PER_DAY Mode Aggregation

```
FUNCTION aggregateByDay(safari):

    dayCostDetails = []

    FOR EACH day IN safari.days:

        dayDate = day.date  // Safari days have actual dates
        season = resolveGlobalSeason(dayDate)

        // Calculate all costs
        allItems = []
        allItems += calculateAccommodationCosts(day, dayDate, paxList)
        allItems += calculateParkTariffCosts(day, dayDate, season, paxList, carCount)
        allItems += calculateActivityCosts(day, dayDate, season, paxList, carCount)

        // Group totals by currency
        totalsByCurrency = groupByCurrency(allItems)

        dayCostDetails.ADD(DayCostDetail{
            dayNumber: day.dayNumber,
            dayTitle: day.title,
            date: day.date,
            seasonName: season.name,
            lineItems: allItems,
            totalsByCurrency: totalsByCurrency
        })

    RETURN dayCostDetails
```

### PER_PAX Mode Aggregation

**Same as Itinerary** - See Itinerary ALGORITHM.md for full details.

---

## Rate Issue Logging

**Same as Itinerary** - See Itinerary ALGORITHM.md for full details.

The system logs the following issue types:
- `MISSING`: No rate record exists
- `INACTIVE`: Rate exists but isActive = false
- `NO_STO_RATE`: Rate exists but stoRate is null
- `NO_SEASON`: Accommodation has no seasons configured
- `SEASON_NOT_FOUND`: No season matches date, using fallback

---

## Key Design Decisions

All design decisions are identical to the Itinerary cost estimation:

1. **Active Rates Only**: Only rates with `isActive = true` are used
2. **Dual Rate Response**: Both STO and Rack rates always calculated
3. **No Currency Mixing**: Each currency maintains separate totals
4. **Highest Priority Nation**: Used for group/vehicle rates
5. **Season Resolution Hierarchy**: Accommodation-specific with type priority fallback
6. **Skip Alternatives**: Alternative accommodations and optional activities excluded
7. **Request-Scoped Issue Logging**: Thread-safe issue tracking

---

## Response Structure

### SafariCostEstimationResponseDTO

```json
{
  "safariId": "abc123",
  "safariCode": "SAF-2024-001",
  "safariName": "Serengeti Adventure",
  "totalDays": 5,
  "totalNights": 4,
  "startDate": "2024-06-01",
  "endDate": "2024-06-05",
  "calculationMode": "PER_DAY",
  "totalPax": 4,
  "carCount": 1,
  "dayCostDetails": [...],
  "paxCostDetails": null,
  "grandTotalsByCurrency": [...],
  "rateIssues": [...],
  "hasIncompleteRates": false,
  "estimatedAt": "2024-01-15T10:30:00"
}
```

---

## Summary

The Safari cost estimation system is architecturally identical to the Itinerary cost estimation system, with the primary difference being:

**Date Source**:
- **Itinerary**: Uses `startDate` parameter and calculates day dates
- **Safari**: Uses actual dates from Safari and SafariDay entities

This allows both systems to share the same:
- Business logic
- Rate lookup services
- Season resolution
- Cost calculation algorithms
- Aggregation strategies
- Rate issue logging

The result is a consistent, maintainable codebase with minimal code duplication.
