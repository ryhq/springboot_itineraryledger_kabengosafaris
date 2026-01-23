# Cost Estimation API Documentation

## Overview

The Cost Estimation API provides endpoints for calculating itinerary costs with support for two calculation modes (Per-Day and Per-PAX), dual rate responses (STO and Rack), and comprehensive rate issue logging.

## Base URL

```
/api/itineraries
```

---

## Endpoints

### Estimate Itinerary Costs

Calculate cost estimation for an itinerary.

```
GET /api/itineraries/{itineraryId}/cost-estimation
```

#### Authentication

Requires `PERM_READ_ITINERARY` permission.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `itineraryId` | String | Yes | Obfuscated itinerary ID |

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `startDate` | Date (ISO) | No | Today | Trip start date in `YYYY-MM-DD` format |
| `mode` | String | No | `PER_DAY` | Calculation mode: `PER_DAY` or `PER_PAX` |

#### Calculation Modes

| Mode | Description |
|------|-------------|
| `PER_DAY` | Costs grouped by day with daily subtotals. Response includes `dayCostDetails` array. |
| `PER_PAX` | Costs grouped by passenger category with per-pax subtotals. Response includes `paxCostDetails` array. |

---

## Request Examples

### Basic Request (Per-Day Mode)

```http
GET /api/itineraries/abc123xyz/cost-estimation
Authorization: Bearer <token>
```

### With Start Date

```http
GET /api/itineraries/abc123xyz/cost-estimation?startDate=2026-06-15
Authorization: Bearer <token>
```

### Per-PAX Mode

```http
GET /api/itineraries/abc123xyz/cost-estimation?mode=PER_PAX
Authorization: Bearer <token>
```

### Full Parameters

```http
GET /api/itineraries/abc123xyz/cost-estimation?startDate=2026-06-15&mode=PER_DAY
Authorization: Bearer <token>
```

---

## Response Structure

### Success Response (200 OK)

```json
{
  "statusCode": 200,
  "message": "Cost estimation calculated successfully",
  "data": {
    "itineraryId": "abc123xyz",
    "itineraryCode": "ITN-2026-001",
    "itineraryName": "7-Day Serengeti Safari",
    "totalDays": 7,
    "totalNights": 6,
    "startDate": "2026-06-15",
    "endDate": "2026-06-21",
    "calculationMode": "PER_DAY",
    "totalPax": 4,
    "carCount": 1,
    "dayCostDetails": [...],
    "paxCostDetails": null,
    "grandTotalsByCurrency": [...],
    "rateIssues": [...],
    "hasIncompleteRates": true,
    "estimatedAt": "2026-01-20T23:15:30"
  }
}
```

---

## Response Fields

### Root Level

| Field | Type | Description |
|-------|------|-------------|
| `itineraryId` | String | Obfuscated itinerary ID |
| `itineraryCode` | String | Itinerary code (e.g., "ITN-2026-001") |
| `itineraryName` | String | Itinerary name |
| `totalDays` | Integer | Total number of days |
| `totalNights` | Integer | Total number of nights |
| `startDate` | Date | Trip start date |
| `endDate` | Date | Trip end date |
| `calculationMode` | String | Mode used: `PER_DAY` or `PER_PAX` |
| `totalPax` | Integer | Total number of passengers |
| `carCount` | Integer | Number of vehicles |
| `dayCostDetails` | Array | Per-day breakdown (only when mode=PER_DAY) |
| `paxCostDetails` | Array | Per-pax breakdown (only when mode=PER_PAX) |
| `grandTotalsByCurrency` | Array | Grand totals grouped by currency |
| `rateIssues` | Array | List of rate issues found (null if none) |
| `hasIncompleteRates` | Boolean | True if any rate issues exist |
| `estimatedAt` | DateTime | Timestamp of calculation |

### DayCostDetail Object

| Field | Type | Description |
|-------|------|-------------|
| `dayNumber` | Integer | Day number (1-indexed) |
| `dayTitle` | String | Day title from itinerary |
| `date` | Date | Actual date |
| `seasonName` | String | Applicable season name |
| `isOvernight` | Boolean | Whether accommodation applies |
| `lineItems` | Array | All cost line items for this day |
| `totalsByCurrency` | Array | Day totals grouped by currency |

### PaxCategoryCost Object

| Field | Type | Description |
|-------|------|-------------|
| `nationCategoryId` | String | Obfuscated nation category ID |
| `nationCategoryName` | String | e.g., "Non-Resident" |
| `ageCategoryId` | String | Obfuscated age category ID |
| `ageCategoryName` | String | e.g., "Adult" |
| `paxCount` | Integer | Number of passengers in this category |
| `lineItems` | Array | Cost line items for this pax category |
| `totalsByCurrency` | Array | Totals grouped by currency |

### CostLineItem Object

| Field | Type | Description |
|-------|------|-------------|
| `dayNumber` | Integer | Day number |
| `itemType` | String | `ACCOMMODATION`, `PARK_FEE`, or `ACTIVITY` |
| `itemName` | String | Display name |
| `itemId` | String | Obfuscated item ID |
| `chargingBasis` | String | e.g., `PER_PERSON`, `PER_VEHICLE`, `FLAT_RATE` |
| `quantity` | Integer | Quantity (pax count, vehicle count, or 1) |
| `stoUnitPrice` | Decimal | STO unit price |
| `rackUnitPrice` | Decimal | Rack unit price |
| `stoTotalPrice` | Decimal | STO total (unit × quantity) |
| `rackTotalPrice` | Decimal | Rack total (unit × quantity) |
| `currency` | String | Currency code (e.g., "USD", "TZS") |
| `paxCategory` | String | Pax category description (for per-person items) |
| `notes` | String | Additional context |

### CurrencyGroupedCost Object

| Field | Type | Description |
|-------|------|-------------|
| `currency` | String | Currency code |
| `accommodationSto` | Decimal | STO total for accommodations |
| `accommodationRack` | Decimal | Rack total for accommodations |
| `parkFeesSto` | Decimal | STO total for park fees |
| `parkFeesRack` | Decimal | Rack total for park fees |
| `activitiesSto` | Decimal | STO total for activities |
| `activitiesRack` | Decimal | Rack total for activities |
| `grandTotalSto` | Decimal | Combined STO total |
| `grandTotalRack` | Decimal | Combined Rack total |

### RateIssueLog Object

| Field | Type | Description |
|-------|------|-------------|
| `issueType` | String | `MISSING`, `INACTIVE`, or `NO_STO_RATE` |
| `itemType` | String | `ACCOMMODATION`, `PARK_FEE`, or `ACTIVITY` |
| `itemName` | String | Name of the item |
| `itemId` | String | Obfuscated ID |
| `dayNumber` | Integer | Day where issue occurred |
| `paxCategory` | String | Pax category (if applicable) |
| `seasonName` | String | Season that was looked up |
| `message` | String | Human-readable description |

---

## Response Examples

### PER_DAY Mode Response

```json
{
  "statusCode": 200,
  "message": "Cost estimation calculated successfully",
  "data": {
    "itineraryId": "abc123xyz",
    "itineraryCode": "ITN-2026-001",
    "itineraryName": "7-Day Serengeti Safari",
    "totalDays": 7,
    "totalNights": 6,
    "startDate": "2026-06-15",
    "endDate": "2026-06-21",
    "calculationMode": "PER_DAY",
    "totalPax": 4,
    "carCount": 1,
    "dayCostDetails": [
      {
        "dayNumber": 1,
        "dayTitle": "Arrival in Arusha",
        "date": "2026-06-15",
        "seasonName": "High Season",
        "isOvernight": true,
        "lineItems": [
          {
            "dayNumber": 1,
            "itemType": "ACCOMMODATION",
            "itemName": "Arusha Coffee Lodge - Deluxe Room",
            "itemId": "acc123",
            "chargingBasis": "Per Person Sharing",
            "quantity": 4,
            "stoUnitPrice": 180.00,
            "rackUnitPrice": 220.00,
            "stoTotalPrice": 720.00,
            "rackTotalPrice": 880.00,
            "currency": "USD",
            "notes": "Full Board (Per Person Sharing/Night)"
          }
        ],
        "totalsByCurrency": [
          {
            "currency": "USD",
            "accommodationSto": 720.00,
            "accommodationRack": 880.00,
            "parkFeesSto": 0,
            "parkFeesRack": 0,
            "activitiesSto": 0,
            "activitiesRack": 0,
            "grandTotalSto": 720.00,
            "grandTotalRack": 880.00
          }
        ]
      },
      {
        "dayNumber": 2,
        "dayTitle": "Serengeti Game Drive",
        "date": "2026-06-16",
        "seasonName": "High Season",
        "isOvernight": true,
        "lineItems": [
          {
            "dayNumber": 2,
            "itemType": "PARK_FEE",
            "itemName": "Conservation Fee - Serengeti",
            "itemId": "tariff456",
            "chargingBasis": "Per Person",
            "quantity": 4,
            "stoUnitPrice": 60.00,
            "rackUnitPrice": 70.00,
            "stoTotalPrice": 240.00,
            "rackTotalPrice": 280.00,
            "currency": "USD",
            "paxCategory": "Per Person"
          },
          {
            "dayNumber": 2,
            "itemType": "ACTIVITY",
            "itemName": "Game Drive @ Serengeti",
            "itemId": "act789",
            "chargingBasis": "PER_VEHICLE",
            "quantity": 1,
            "stoUnitPrice": 150.00,
            "rackUnitPrice": 180.00,
            "stoTotalPrice": 150.00,
            "rackTotalPrice": 180.00,
            "currency": "USD",
            "paxCategory": "Per Vehicle",
            "notes": "Per Vehicle"
          },
          {
            "dayNumber": 2,
            "itemType": "ACCOMMODATION",
            "itemName": "Serengeti Serena Lodge - Standard Room",
            "itemId": "acc456",
            "chargingBasis": "Per Person Sharing",
            "quantity": 4,
            "stoUnitPrice": 250.00,
            "rackUnitPrice": 320.00,
            "stoTotalPrice": 1000.00,
            "rackTotalPrice": 1280.00,
            "currency": "USD",
            "notes": "Full Board (Per Person Sharing/Night)"
          }
        ],
        "totalsByCurrency": [
          {
            "currency": "USD",
            "accommodationSto": 1000.00,
            "accommodationRack": 1280.00,
            "parkFeesSto": 240.00,
            "parkFeesRack": 280.00,
            "activitiesSto": 150.00,
            "activitiesRack": 180.00,
            "grandTotalSto": 1390.00,
            "grandTotalRack": 1740.00
          }
        ]
      }
    ],
    "grandTotalsByCurrency": [
      {
        "currency": "USD",
        "accommodationSto": 4500.00,
        "accommodationRack": 5600.00,
        "parkFeesSto": 960.00,
        "parkFeesRack": 1120.00,
        "activitiesSto": 450.00,
        "activitiesRack": 540.00,
        "grandTotalSto": 5910.00,
        "grandTotalRack": 7260.00
      }
    ],
    "rateIssues": [
      {
        "issueType": "MISSING",
        "itemType": "PARK_FEE",
        "itemName": "Concession Fee - Serengeti",
        "itemId": "tariff789",
        "dayNumber": 3,
        "paxCategory": "Non-Resident Child",
        "seasonName": "High Season",
        "message": "No active rate found for Park Fee 'Concession Fee - Serengeti' for Non-Resident Child in High Season"
      }
    ],
    "hasIncompleteRates": true,
    "estimatedAt": "2026-01-20T23:15:30"
  }
}
```

### PER_PAX Mode Response

```json
{
  "statusCode": 200,
  "message": "Cost estimation calculated successfully",
  "data": {
    "itineraryId": "abc123xyz",
    "itineraryCode": "ITN-2026-001",
    "itineraryName": "7-Day Serengeti Safari",
    "totalDays": 7,
    "totalNights": 6,
    "startDate": "2026-06-15",
    "endDate": "2026-06-21",
    "calculationMode": "PER_PAX",
    "totalPax": 4,
    "carCount": 1,
    "dayCostDetails": null,
    "paxCostDetails": [
      {
        "nationCategoryId": "nat123",
        "nationCategoryName": "Non-Resident",
        "ageCategoryId": "age456",
        "ageCategoryName": "Adult",
        "paxCount": 2,
        "lineItems": [
          {
            "dayNumber": 1,
            "itemType": "ACCOMMODATION",
            "itemName": "Arusha Coffee Lodge - Deluxe Room",
            "chargingBasis": "Per Person Sharing",
            "quantity": 2,
            "stoUnitPrice": 180.00,
            "rackUnitPrice": 220.00,
            "stoTotalPrice": 360.00,
            "rackTotalPrice": 440.00,
            "currency": "USD"
          }
        ],
        "totalsByCurrency": [
          {
            "currency": "USD",
            "accommodationSto": 2250.00,
            "accommodationRack": 2800.00,
            "parkFeesSto": 480.00,
            "parkFeesRack": 560.00,
            "activitiesSto": 225.00,
            "activitiesRack": 270.00,
            "grandTotalSto": 2955.00,
            "grandTotalRack": 3630.00
          }
        ]
      },
      {
        "nationCategoryId": "nat123",
        "nationCategoryName": "Non-Resident",
        "ageCategoryId": "age789",
        "ageCategoryName": "Child",
        "paxCount": 2,
        "lineItems": [...],
        "totalsByCurrency": [
          {
            "currency": "USD",
            "accommodationSto": 2250.00,
            "accommodationRack": 2800.00,
            "parkFeesSto": 480.00,
            "parkFeesRack": 560.00,
            "activitiesSto": 225.00,
            "activitiesRack": 270.00,
            "grandTotalSto": 2955.00,
            "grandTotalRack": 3630.00
          }
        ]
      }
    ],
    "grandTotalsByCurrency": [
      {
        "currency": "USD",
        "accommodationSto": 4500.00,
        "accommodationRack": 5600.00,
        "parkFeesSto": 960.00,
        "parkFeesRack": 1120.00,
        "activitiesSto": 450.00,
        "activitiesRack": 540.00,
        "grandTotalSto": 5910.00,
        "grandTotalRack": 7260.00
      }
    ],
    "hasIncompleteRates": false,
    "estimatedAt": "2026-01-20T23:15:30"
  }
}
```

---

## Error Responses

### 400 Bad Request - Invalid Mode

```json
{
  "statusCode": 400,
  "message": "Invalid mode. Must be PER_DAY or PER_PAX",
  "errorCode": "INVALID_MODE"
}
```

### 404 Not Found - Itinerary Not Found

```json
{
  "statusCode": 404,
  "message": "Itinerary not found",
  "errorCode": "ITINERARY_NOT_FOUND"
}
```

### 500 Internal Server Error

```json
{
  "statusCode": 500,
  "message": "Failed to estimate costs: <error details>",
  "errorCode": "COST_ESTIMATION_FAILED"
}
```

---

## Key Features

### 1. Dual Rate Response

Both STO (Special Tour Operator) and Rack (Public) rates are always calculated and returned. This allows tour operators to see their negotiated rates alongside public rates.

### 2. Currency Grouping

Costs are never mixed across currencies. Each currency has its own totals:
- If all costs are in USD, you get one entry in `grandTotalsByCurrency`
- If some costs are in USD and others in TZS, you get separate entries for each

### 3. Active Rates Only

Only rates marked as `isActive=true` are used. Inactive rates are ignored and logged as rate issues.

### 4. Alternative Skipping

- Alternative accommodations (`isAlternative=true`) are skipped
- Optional activities (`isOptional=true`) are skipped
- Only items with `isIncludedInPrice=true` are calculated

### 5. Comprehensive Rate Issue Logging

When rates are missing or inactive, detailed information is logged:
- Issue type (MISSING, INACTIVE, NO_STO_RATE)
- Item details (name, type, ID)
- Day number
- Pax category (if applicable)
- Season name

---

## Charging Basis Types

| Basis | Description | Quantity Used |
|-------|-------------|---------------|
| `PER_PERSON` | Charged per passenger | Total pax count |
| `PER_VEHICLE` | Charged per vehicle | Car count from itinerary |
| `PER_GROUP` | Single charge for entire group | 1 |
| `FLAT_RATE` | Fixed rate regardless of size | 1 |
| `PER_DAY` | Daily rate | 1 |

---

## Notes

1. **Season Resolution**: The system automatically resolves the applicable season based on the date:
   - For accommodations: Checks accommodation-specific seasons first, then falls back to global
   - For parks/activities: Uses global seasons

2. **Per-Person Sharing (PPS)**: Accommodation rates with `isPerPerson=true` are multiplied by total pax count. Rates with `isPerPerson=false` are multiplied by room count.

3. **Highest Priority Nation**: For group/vehicle rates, the system uses the highest priority nation category (based on `priorityFactor`) to determine which rate applies to the entire group.
