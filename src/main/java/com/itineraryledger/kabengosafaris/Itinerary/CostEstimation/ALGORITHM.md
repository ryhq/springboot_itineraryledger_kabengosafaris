# Cost Estimation Algorithm

## Overview

This document describes the algorithm used to estimate itinerary costs. The system calculates costs for accommodations, park fees, and activities across all days of an itinerary, supporting two output modes (Per-Day and Per-PAX) while always returning both STO and Rack rates.

---

## High-Level Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         COST ESTIMATION FLOW                            │
└─────────────────────────────────────────────────────────────────────────┘

1. INPUT
   ├── Itinerary ID (obfuscated)
   ├── Start Date (optional, defaults to today)
   └── Mode (PER_DAY or PER_PAX)

2. FETCH DATA
   └── Load Full Itinerary (days, pax, parks, activities, accommodations)

3. FOR EACH DAY
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

## Detailed Algorithm

### Phase 1: Initialization

```
FUNCTION estimate(itineraryId, startDate, mode):

    // 1. Apply defaults
    IF startDate IS NULL:
        startDate = TODAY

    IF mode IS NULL:
        mode = PER_DAY

    // 2. Fetch full itinerary
    itinerary = fetchFullItinerary(itineraryId)

    // 3. Extract key values
    totalDays = itinerary.totalDays
    endDate = startDate + (totalDays - 1) days
    carCount = itinerary.carCount OR 1
    paxList = itinerary.paxList
    totalPax = SUM(pax.count FOR pax IN paxList)

    // 4. Clear rate issue logger (request-scoped)
    rateIssueLogger.clear()
```

### Phase 2: Day-by-Day Calculation

```
FOR EACH day IN itinerary.days:

    dayDate = startDate + (day.dayNumber - 1) days
    globalSeason = resolveGlobalSeason(dayDate)

    // Calculate all cost types for this day
    accommodationItems = calculateAccommodationCosts(day, dayDate, paxList)
    parkFeeItems = calculateParkTariffCosts(day, dayDate, globalSeason, paxList, carCount)
    activityItems = calculateActivityCosts(day, dayDate, globalSeason, paxList, carCount)

    // Combine all line items for this day
    dayItems = accommodationItems + parkFeeItems + activityItems
```

---

## Accommodation Cost Calculation

### Algorithm

```
FUNCTION calculateAccommodationCosts(day, dayDate, paxList):

    items = []

    // Skip non-overnight days (e.g., departure day)
    IF day.isOvernight == FALSE:
        RETURN items

    totalPax = SUM(pax.count FOR pax IN paxList)

    FOR EACH accommodation IN day.accommodations:

        // Skip alternative accommodations
        IF accommodation.isAlternative == TRUE:
            CONTINUE

        // Resolve accommodation-specific season (fallback to global)
        season = resolveAccommodationSeason(accommodation.id, dayDate)

        // Lookup ACTIVE rate only
        rate = findActiveRate(
            accommodationId = accommodation.id,
            seasonId = season.id,
            roomTypeId = accommodation.roomTypeId,
            roomStandardId = accommodation.roomStandardId,
            boardTypeId = accommodation.boardTypeId
        )

        IF rate NOT FOUND:
            logMissingRate(...)
            CONTINUE

        // Determine quantity based on pricing mode
        IF rate.isPerPerson == TRUE:
            // Per Person Sharing (PPS) - multiply by total pax
            quantity = totalPax
        ELSE:
            // Per Room - multiply by room count
            quantity = accommodation.roomCount OR 1

        // Calculate totals (always both STO and Rack)
        stoUnit = rate.stoRate OR 0
        rackUnit = rate.rackRate OR 0
        stoTotal = stoUnit × quantity
        rackTotal = rackUnit × quantity

        // Log if STO rate is missing
        IF rate.stoRate IS NULL:
            logNoStoRate(...)

        items.ADD(CostLineItem{
            dayNumber: day.dayNumber,
            itemType: ACCOMMODATION,
            itemName: accommodation.name + " - " + roomType.name,
            chargingBasis: rate.isPerPerson ? "Per Person Sharing" : "Per Room",
            quantity: quantity,
            stoUnitPrice: stoUnit,
            rackUnitPrice: rackUnit,
            stoTotalPrice: stoTotal,
            rackTotalPrice: rackTotal,
            currency: rate.currency
        })

    RETURN items
```

### Season Resolution for Accommodations

**IMPORTANT**: Accommodation rates are stored per accommodation's specific seasons, NOT global seasons. Therefore, the fallback logic never uses global seasons for accommodations.

```
FUNCTION resolveAccommodationSeason(accommodationId, date):

    // 1. Check if accommodation has ANY seasons configured
    hasSeasons = existsByAccommodationId(accommodationId)

    IF hasSeasons == FALSE:
        // No seasons at all - skip cost calculation, log NO_SEASON issue
        LOG "Accommodation has no seasons configured - cost calculation skipped"
        RETURN {
            season: NULL,
            hasSeasons: FALSE,
            usedFallback: FALSE
        }

    // 2. Try to find season that contains the specific date
    FOR EACH period IN allSeasonPeriods:
        IF period.season.accommodation.id == accommodationId
           AND period.season.isActive == TRUE
           AND period.containsDate(date):
            RETURN {
                season: period.season,
                hasSeasons: TRUE,
                usedFallback: FALSE
            }

    // 3. Date not in any season period - fallback to highest priority season TYPE
    //    Priority: HIGH_SEASON > PEAK_SEASON > SHOULDER_SEASON > FESTIVE_SEASON >
    //              SPECIAL_EVENT > LOW_SEASON > STANDARD > CUSTOM
    prioritizedSeasons = findActiveByAccommodationIdOrderedByTypePriority(accommodationId)

    IF prioritizedSeasons NOT EMPTY:
        fallbackSeason = prioritizedSeasons[0]  // Highest priority type
        LOG "No season for date, using fallback: " + fallbackSeason.name + " (" + fallbackSeason.type + ")"
        RETURN {
            season: fallbackSeason,
            hasSeasons: TRUE,
            usedFallback: TRUE  // Logged as SEASON_NOT_FOUND (informational)
        }

    // 4. Accommodation has seasons but none are active
    RETURN {
        season: NULL,
        hasSeasons: FALSE,  // Effectively no usable seasons
        usedFallback: FALSE
    }

    // NOTE: NEVER fallback to global seasons for accommodations
    // Rates are stored per accommodation's season, not global seasons
```

**Season Type Priority Order:**
| Priority | Season Type | Description |
|----------|-------------|-------------|
| 1 | HIGH_SEASON | High demand period with premium pricing |
| 2 | PEAK_SEASON | Highest demand period with maximum pricing |
| 3 | SHOULDER_SEASON | Moderate demand period between peak and low |
| 4 | FESTIVE_SEASON | Special holiday and festive periods |
| 5 | SPECIAL_EVENT | Specific events or occasions |
| 6 | LOW_SEASON | Off-peak period with reduced pricing |
| 7 | STANDARD | Regular year-round pricing |
| 8 | CUSTOM | Custom defined season |

---

## Park Tariff Cost Calculation

### Algorithm

```
FUNCTION calculateParkTariffCosts(day, dayDate, season, paxList, carCount):

    items = []

    FOR EACH park IN day.parks:
        FOR EACH tariff IN park.tariffs:

            // Skip if not included in price
            IF tariff.isIncludedInPrice == FALSE:
                CONTINUE

            // Get charging basis from ParkTariff entity
            parkTariff = findParkTariff(park.id, tariff.id)
            chargingBasis = parkTariff.tariff.chargingBasis

            // Calculate based on charging basis
            SWITCH chargingBasis:

                CASE PER_PERSON:
                    item = calculatePerPersonTariff(...)

                CASE PER_VEHICLE:
                    item = calculateGroupTariff(..., quantity=carCount)

                CASE PER_GROUP, FLAT_RATE, PER_DAY:
                    item = calculateGroupTariff(..., quantity=1)

            items.ADD(item)

    RETURN items
```

### Per-Person Tariff Calculation

```
FUNCTION calculatePerPersonTariff(park, tariff, season, paxList):

    stoTotal = 0
    rackTotal = 0
    totalPaxCount = 0
    currency = "USD"

    // Calculate for each pax category
    FOR EACH pax IN paxList:

        // Lookup ACTIVE rate for this specific pax category
        rate = findActiveRateForPerson(
            parkId = park.id,
            tariffId = tariff.id,
            seasonId = season.id,
            nationCategoryId = pax.nationCategoryId,
            ageCategoryId = pax.ageCategoryId
        )

        IF rate NOT FOUND:
            logMissingRate(..., paxCategory = pax.nationName + " " + pax.ageName)
            CONTINUE

        // Accumulate costs
        stoUnit = rate.stoRate OR 0
        rackUnit = rate.rackRate OR 0

        stoTotal += stoUnit × pax.count
        rackTotal += rackUnit × pax.count
        totalPaxCount += pax.count
        currency = rate.currency

    // Calculate average unit price for display
    stoUnitAvg = totalPaxCount > 0 ? stoTotal / totalPaxCount : 0
    rackUnitAvg = totalPaxCount > 0 ? rackTotal / totalPaxCount : 0

    RETURN CostLineItem{
        itemType: PARK_FEE,
        chargingBasis: "PER_PERSON",
        quantity: totalPaxCount,
        stoUnitPrice: stoUnitAvg,
        rackUnitPrice: rackUnitAvg,
        stoTotalPrice: stoTotal,
        rackTotalPrice: rackTotal,
        currency: currency
    }
```

### Group/Vehicle Tariff Calculation

```
FUNCTION calculateGroupTariff(park, tariff, season, paxList, quantity, chargingBasis):

    // Find highest priority nation category
    // Higher priorityFactor = higher priority (e.g., Non-Resident > Resident)
    highestPriorityNationId = findHighestPriorityNation(paxList)

    // Lookup ACTIVE rate using highest priority nation
    rate = findActiveRateForGroup(
        parkId = park.id,
        tariffId = tariff.id,
        seasonId = season.id,
        nationCategoryId = highestPriorityNationId
    )

    IF rate NOT FOUND:
        logMissingRate(...)
        RETURN notFoundLineItem

    stoUnit = rate.stoRate OR 0
    rackUnit = rate.rackRate OR 0
    stoTotal = stoUnit × quantity
    rackTotal = rackUnit × quantity

    RETURN CostLineItem{
        itemType: PARK_FEE,
        chargingBasis: chargingBasis.name,
        quantity: quantity,
        stoUnitPrice: stoUnit,
        rackUnitPrice: rackUnit,
        stoTotalPrice: stoTotal,
        rackTotalPrice: rackTotal,
        currency: rate.currency
    }
```

### Highest Priority Nation Selection

```
FUNCTION findHighestPriorityNation(paxList):

    highestPriorityId = NULL
    highestPriority = -∞

    FOR EACH pax IN paxList:
        nationCategory = findNationCategory(pax.nationCategoryId)
        priority = nationCategory.priorityFactor OR 0

        IF priority > highestPriority:
            highestPriority = priority
            highestPriorityId = pax.nationCategoryId

    // Fallback to first pax if no priority found
    IF highestPriorityId IS NULL AND paxList NOT EMPTY:
        highestPriorityId = paxList[0].nationCategoryId

    RETURN highestPriorityId
```

**Priority Factor Example:**
| Nation Category | Priority Factor | Description |
|-----------------|-----------------|-------------|
| Non-Resident | 100 | Highest priority (highest rates) |
| East African | 50 | Medium priority |
| Resident | 10 | Lowest priority (lowest rates) |

When a group has mixed nationalities, the **highest** priority (Non-Resident) rate applies to group/vehicle charges.

---

## Activity Cost Calculation

### Algorithm

```
FUNCTION calculateActivityCosts(day, dayDate, season, paxList, carCount):

    items = []

    // 1. Process standalone day activities
    FOR EACH activity IN day.activities:

        // Skip if not included or optional
        IF activity.isIncludedInPrice == FALSE OR activity.isOptional == TRUE:
            CONTINUE

        item = calculateActivityCost(
            activityId = activity.activityId,
            activityName = activity.activityName,
            parkId = NULL,  // Standalone - no park
            parkName = NULL,
            season, paxList, carCount
        )

        IF item.totalPrice > 0:
            items.ADD(item)

    // 2. Process park activities
    FOR EACH park IN day.parks:
        FOR EACH parkActivity IN park.activities:

            IF parkActivity.isIncludedInPrice == FALSE:
                CONTINUE

            item = calculateActivityCost(
                activityId = parkActivity.activityId,
                activityName = parkActivity.activityName,
                parkId = park.id,      // Park-specific
                parkName = park.name,
                season, paxList, carCount
            )

            IF item.totalPrice > 0:
                items.ADD(item)

    RETURN items
```

### Single Activity Cost Calculation

```
FUNCTION calculateActivityCost(activityId, activityName, parkId, parkName, season, paxList, carCount):

    activity = findActivity(activityId)

    // Skip if activity has no tariff (included/free)
    IF activity.hasTariff == FALSE:
        RETURN includedLineItem(activityName, parkName)

    chargingBasis = activity.chargingBasis OR PER_PERSON

    SWITCH chargingBasis:

        CASE PER_PERSON:
            RETURN calculatePerPersonActivity(...)

        CASE PER_VEHICLE:
            RETURN calculateGroupActivity(..., quantity=carCount)

        CASE PER_GROUP, FLAT_RATE:
            RETURN calculateGroupActivity(..., quantity=1)
```

### Activity Rate Lookup (Park-Specific → Global Fallback)

```
FUNCTION findActiveActivityRate(activityId, parkId, seasonId, nationCategoryId, ageCategoryId):

    // 1. Try park-specific rate first
    IF parkId IS NOT NULL:
        rate = findActiveParkRateForPerson(activityId, parkId, seasonId, nationCategoryId, ageCategoryId)
        IF rate FOUND:
            RETURN rate (isParkSpecific = TRUE)

    // 2. Fallback to global rate
    rate = findActiveGlobalRateForPerson(activityId, seasonId, nationCategoryId, ageCategoryId)
    IF rate FOUND:
        RETURN rate (isParkSpecific = FALSE)

    RETURN NOT_FOUND
```

---

## Aggregation Phase

### PER_DAY Mode Aggregation

```
FUNCTION aggregateByDay(itinerary, startDate):

    dayCostDetails = []

    FOR EACH day IN itinerary.days:

        dayDate = startDate + (day.dayNumber - 1) days
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
            date: dayDate,
            seasonName: season.name,
            lineItems: allItems,
            totalsByCurrency: totalsByCurrency
        })

    RETURN dayCostDetails
```

### PER_PAX Mode Aggregation

```
FUNCTION aggregateByPax(itinerary, startDate):

    // Initialize containers for each pax category
    paxCostMap = {}
    FOR EACH pax IN itinerary.paxList:
        key = pax.nationCategoryId + "-" + pax.ageCategoryId
        paxCostMap[key] = PaxCategoryCost{
            nationCategoryId: pax.nationCategoryId,
            nationCategoryName: pax.nationCategoryName,
            ageCategoryId: pax.ageCategoryId,
            ageCategoryName: pax.ageCategoryName,
            paxCount: pax.count,
            lineItems: []
        }

    totalPax = SUM(pax.count FOR pax IN paxList)

    // Process all days
    FOR EACH day IN itinerary.days:

        // Calculate all costs for this day
        allItems = calculateAllCostsForDay(day, ...)

        // Distribute costs to pax categories
        distributeItemsToPax(allItems, paxCostMap, paxList, totalPax, carCount)

    // Calculate totals by currency for each pax category
    FOR EACH paxCost IN paxCostMap.values():
        paxCost.totalsByCurrency = groupByCurrency(paxCost.lineItems)

    RETURN paxCostMap.values()
```

### Cost Distribution to Pax Categories

```
FUNCTION distributeItemsToPax(items, paxCostMap, paxList, totalPax, carCount):

    FOR EACH item IN items:

        IF isPerPersonCharging(item.chargingBasis):
            // Per-person items: distribute proportionally
            FOR EACH pax IN paxList:
                proportion = pax.count / totalPax
                paxItem = createProportionalItem(item, proportion, pax.count)
                paxCostMap[pax.key].lineItems.ADD(paxItem)

        ELSE:
            // Group/vehicle items: distribute proportionally by pax count
            FOR EACH pax IN paxList:
                proportion = pax.count / totalPax
                paxItem = createProportionalItem(item, proportion, pax.count)
                paxItem.notes += " (Share: " + pax.count + "/" + totalPax + ")"
                paxCostMap[pax.key].lineItems.ADD(paxItem)


FUNCTION createProportionalItem(original, proportion, paxCount):

    stoTotal = original.stoTotalPrice × proportion
    rackTotal = original.rackTotalPrice × proportion

    // Calculate per-person unit price for this category
    stoUnit = paxCount > 0 ? stoTotal / paxCount : 0
    rackUnit = paxCount > 0 ? rackTotal / paxCount : 0

    RETURN CostLineItem{
        ...original,
        quantity: paxCount,
        stoUnitPrice: stoUnit,
        rackUnitPrice: rackUnit,
        stoTotalPrice: stoTotal,
        rackTotalPrice: rackTotal
    }
```

---

## Currency Grouping

```
FUNCTION groupByCurrency(items):

    currencyTotals = {}  // Map<Currency, CurrencyGroupedCost>

    FOR EACH item IN items:

        currency = item.currency OR "USD"

        IF currency NOT IN currencyTotals:
            currencyTotals[currency] = CurrencyGroupedCost{
                currency: currency,
                accommodationSto: 0, accommodationRack: 0,
                parkFeesSto: 0, parkFeesRack: 0,
                activitiesSto: 0, activitiesRack: 0,
                grandTotalSto: 0, grandTotalRack: 0
            }

        totals = currencyTotals[currency]

        SWITCH item.itemType:
            CASE ACCOMMODATION:
                totals.accommodationSto += item.stoTotalPrice
                totals.accommodationRack += item.rackTotalPrice

            CASE PARK_FEE:
                totals.parkFeesSto += item.stoTotalPrice
                totals.parkFeesRack += item.rackTotalPrice

            CASE ACTIVITY:
                totals.activitiesSto += item.stoTotalPrice
                totals.activitiesRack += item.rackTotalPrice

    // Calculate grand totals
    FOR EACH totals IN currencyTotals.values():
        totals.grandTotalSto = totals.accommodationSto + totals.parkFeesSto + totals.activitiesSto
        totals.grandTotalRack = totals.accommodationRack + totals.parkFeesRack + totals.activitiesRack

    RETURN currencyTotals.values()
```

---

## Rate Issue Logging

The system logs issues when rates cannot be found or are unusable:

| Issue Type | Trigger | Impact | Severity |
|------------|---------|--------|----------|
| `MISSING` | No rate record exists for the parameters | Cost = 0 | Warning |
| `INACTIVE` | Rate exists but `isActive = false` | Cost = 0 | Warning |
| `NO_STO_RATE` | Rate exists but `stoRate` is null | STO cost = 0, Rack cost calculated | Info |
| `NO_SEASON` | Accommodation has no seasons configured at all | Cost calculation skipped | Warning |
| `SEASON_NOT_FOUND` | No season matches date, using fallback by priority | Cost calculated with fallback season | Info |

### Issue Type Details

#### NO_SEASON
Logged when an accommodation has **zero seasons** configured. This is a data issue that needs to be fixed by adding seasons to the accommodation.

```
FUNCTION logNoSeason(itemName, itemId, dayNumber):

    message = "Accommodation '" + itemName + "' has no seasons configured - cost calculation skipped"

    rateIssues.ADD(RateIssueLog{
        issueType: NO_SEASON,
        itemType: ACCOMMODATION,
        itemName: itemName,
        itemId: itemId,
        dayNumber: dayNumber,
        message: message
    })
```

#### SEASON_NOT_FOUND
Logged when an accommodation has seasons but the requested date doesn't fall within any season period. The system uses the highest priority season type as fallback. This is **informational** - cost calculation proceeds normally.

```
FUNCTION logSeasonNotFound(itemName, itemId, dayNumber, requestedDate, fallbackSeasonName, fallbackSeasonType):

    message = "No season found for accommodation '" + itemName + "' on " + requestedDate
              + " - using fallback '" + fallbackSeasonName + "' (" + fallbackSeasonType + ")"

    rateIssues.ADD(RateIssueLog{
        issueType: SEASON_NOT_FOUND,
        itemType: ACCOMMODATION,
        itemName: itemName,
        itemId: itemId,
        dayNumber: dayNumber,
        seasonName: fallbackSeasonName,
        message: message
    })
```

#### MISSING
```
FUNCTION logMissingRate(itemType, itemName, itemId, dayNumber, paxCategory, seasonName):

    message = "No active rate found for " + itemType + " '" + itemName + "'"
    IF paxCategory IS NOT NULL:
        message += " for " + paxCategory
    IF seasonName IS NOT NULL:
        message += " in " + seasonName

    rateIssues.ADD(RateIssueLog{
        issueType: MISSING,
        itemType: itemType,
        itemName: itemName,
        itemId: itemId,
        dayNumber: dayNumber,
        paxCategory: paxCategory,
        seasonName: seasonName,
        message: message
    })
```

---

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           COST ESTIMATION ALGORITHM                          │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │  API Request    │
                              │  - itineraryId  │
                              │  - startDate    │
                              │  - mode         │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │ Fetch Full      │
                              │ Itinerary       │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │ Clear Rate      │
                              │ Issue Logger    │
                              └────────┬────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                    ▼                  ▼                  ▼
           ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
           │ FOR EACH DAY  │  │ FOR EACH DAY  │  │ FOR EACH DAY  │
           │               │  │               │  │               │
           │ Calculate     │  │ Calculate     │  │ Calculate     │
           │ Accommodation │  │ Park Tariff   │  │ Activity      │
           │ Costs         │  │ Costs         │  │ Costs         │
           └───────┬───────┘  └───────┬───────┘  └───────┬───────┘
                   │                  │                  │
                   │    ┌─────────────┼─────────────┐    │
                   │    │             │             │    │
                   ▼    ▼             ▼             ▼    ▼
           ┌─────────────────────────────────────────────────┐
           │              RATE LOOKUP FLOW                   │
           │                                                 │
           │  1. Check isActive = true                       │
           │  2. For activities: park-specific → global rate │
           │  3. For accommodations:                         │
           │     - No seasons? → Skip (log NO_SEASON)        │
           │     - Date in period? → Use that season         │
           │     - Else → Fallback by season TYPE priority   │
           │     - NEVER use global seasons                  │
           │  4. Log issues if not found                     │
           │  5. Always calculate both STO and Rack          │
           └───────────────────────┬─────────────────────────┘
                                   │
                                   ▼
                    ┌──────────────┴──────────────┐
                    │                             │
            MODE=PER_DAY                   MODE=PER_PAX
                    │                             │
                    ▼                             ▼
           ┌───────────────┐             ┌───────────────┐
           │ Group by Day  │             │ Distribute to │
           │               │             │ Pax Categories│
           │ Day 1:        │             │               │
           │  - lineItems  │             │ Non-Res Adult:│
           │  - totals     │             │  - lineItems  │
           │    by currency│             │  - totals     │
           │               │             │               │
           │ Day 2:        │             │ Non-Res Child:│
           │  ...          │             │  ...          │
           └───────┬───────┘             └───────┬───────┘
                   │                             │
                   └──────────────┬──────────────┘
                                  │
                                  ▼
                         ┌───────────────┐
                         │ Group Grand   │
                         │ Totals by     │
                         │ Currency      │
                         │               │
                         │ USD:          │
                         │  - STO total  │
                         │  - Rack total │
                         │               │
                         │ TZS:          │
                         │  - STO total  │
                         │  - Rack total │
                         └───────┬───────┘
                                 │
                                 ▼
                         ┌───────────────┐
                         │ Collect Rate  │
                         │ Issues        │
                         └───────┬───────┘
                                 │
                                 ▼
                         ┌───────────────┐
                         │ Build Final   │
                         │ Response      │
                         └───────────────┘
```

---

## Key Design Decisions

### 1. Active Rates Only
Only rates with `isActive = true` are used. This prevents outdated or draft rates from affecting calculations.

### 2. Dual Rate Response
Both STO and Rack rates are **always** calculated and returned. The frontend can choose which to display based on context.

### 3. No Currency Mixing
Costs are never summed across different currencies. Each currency maintains separate totals to ensure accuracy.

### 4. Highest Priority Nation for Group Rates
For PER_VEHICLE and PER_GROUP charges, the system uses the highest priority nation category (typically Non-Resident, which has the highest rates) to determine the applicable rate.

### 5. Season Resolution Hierarchy

**Accommodations** (NEVER use global seasons):
1. Check if accommodation has ANY seasons → if not, skip cost calculation
2. Find season period containing the date → use that season
3. If no period matches date → fallback to highest priority season TYPE:
   - HIGH_SEASON > PEAK_SEASON > SHOULDER_SEASON > FESTIVE_SEASON > SPECIAL_EVENT > LOW_SEASON > STANDARD > CUSTOM
4. **Never** fallback to global seasons (rates are stored per accommodation's season)

**Parks/Activities**: Use global seasons

### 6. Skip Alternatives
Alternative accommodations and optional activities are excluded from cost calculations to represent the primary itinerary cost.

### 7. Request-Scoped Issue Logging
Rate issues are accumulated in a request-scoped service, ensuring thread safety and isolation between concurrent requests.
