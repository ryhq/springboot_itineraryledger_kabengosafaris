# Safari Cost Estimation API Documentation

## Overview

The Safari Cost Estimation API provides detailed cost breakdowns for safaris, including accommodations, park fees, and activities. It supports two calculation modes (PER_DAY and PER_PAX) and returns both STO (Tour Operator) and Rack (Public) rates.

---

## Endpoint

### Estimate Safari Costs

Calculates comprehensive cost estimates for a safari based on actual rates and configurations.

**Endpoint**: `GET /api/safaris/{safariId}/cost-estimation`

**Permission**: `PERM_READ_SAFARI`

---

## Request Parameters

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `safariId` | String | Yes | Obfuscated safari ID |

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `mode` | String | No | `PER_DAY` | Calculation mode: `PER_DAY` or `PER_PAX` |

---

## Calculation Modes

### PER_DAY Mode

Groups costs by day with daily subtotals.

**Use Case**: Budget planning, day-by-day cost analysis

**Response Includes**:
- `dayCostDetails`: Array of day cost breakdowns
- Each day includes all line items and totals by currency

**Example**:
```
Day 1 (2024-06-01):
  - Accommodation: $200 STO, $250 Rack
  - Park Fees: $100 STO, $150 Rack
  - Activities: $50 STO, $75 Rack
  Total: $350 STO, $475 Rack

Day 2 (2024-06-02):
  ...
```

### PER_PAX Mode

Groups costs by passenger category with per-pax subtotals.

**Use Case**: Per-person pricing, passenger-specific cost analysis

**Response Includes**:
- `paxCostDetails`: Array of pax category cost breakdowns
- Each pax category includes proportional share of all costs

**Example**:
```
Non-Resident Adult (2 pax):
  - Accommodation: $400 STO, $500 Rack
  - Park Fees: $200 STO, $300 Rack
  - Activities: $100 STO, $150 Rack
  Total: $700 STO, $950 Rack

Non-Resident Child (2 pax):
  ...
```

---

## Response Structure

### Success Response (200 OK)

```json
{
  "statusCode": 200,
  "message": "Cost estimation calculated successfully",
  "data": {
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
    "dayCostDetails": [
      {
        "dayNumber": 1,
        "dayTitle": "Arrival at Serengeti",
        "date": "2024-06-01",
        "seasonName": "High Season",
        "isOvernight": true,
        "lineItems": [
          {
            "dayNumber": 1,
            "itemType": "ACCOMMODATION",
            "itemName": "Serengeti Serena Lodge - Deluxe Room",
            "itemId": "xyz789",
            "chargingBasis": "Per Person Sharing",
            "quantity": 4,
            "stoUnitPrice": 200.00,
            "rackUnitPrice": 250.00,
            "stoTotalPrice": 800.00,
            "rackTotalPrice": 1000.00,
            "currency": "USD",
            "notes": "Full Board (Per Person Sharing/Night)"
          },
          {
            "dayNumber": 1,
            "itemType": "PARK_FEE",
            "itemName": "Conservation Fee - Serengeti National Park",
            "itemId": "def456",
            "chargingBasis": "Per Person",
            "quantity": 4,
            "stoUnitPrice": 70.00,
            "rackUnitPrice": 70.00,
            "stoTotalPrice": 280.00,
            "rackTotalPrice": 280.00,
            "currency": "USD",
            "paxCategory": "Per Person"
          }
        ],
        "totalsByCurrency": [
          {
            "currency": "USD",
            "accommodationSto": 800.00,
            "accommodationRack": 1000.00,
            "parkFeesSto": 280.00,
            "parkFeesRack": 280.00,
            "activitiesSto": 0.00,
            "activitiesRack": 0.00,
            "grandTotalSto": 1080.00,
            "grandTotalRack": 1280.00
          }
        ]
      }
    ],
    "paxCostDetails": null,
    "grandTotalsByCurrency": [
      {
        "currency": "USD",
        "accommodationSto": 4000.00,
        "accommodationRack": 5000.00,
        "parkFeesSto": 1400.00,
        "parkFeesRack": 1400.00,
        "activitiesSto": 200.00,
        "activitiesRack": 300.00,
        "grandTotalSto": 5600.00,
        "grandTotalRack": 6700.00
      }
    ],
    "rateIssues": null,
    "hasIncompleteRates": false,
    "estimatedAt": "2024-01-15T10:30:00"
  }
}
```

### Field Descriptions

#### Top-Level Fields

| Field | Type | Description |
|-------|------|-------------|
| `safariId` | String | Obfuscated safari ID |
| `safariCode` | String | Human-readable safari code |
| `safariName` | String | Safari name |
| `totalDays` | Integer | Total number of days |
| `totalNights` | Integer | Total number of nights |
| `startDate` | Date | Safari start date (from Safari entity) |
| `endDate` | Date | Safari end date (from Safari entity) |
| `calculationMode` | String | Calculation mode used: `PER_DAY` or `PER_PAX` |
| `totalPax` | Integer | Total number of passengers |
| `carCount` | Integer | Number of vehicles |
| `dayCostDetails` | Array | Day-by-day cost breakdown (PER_DAY mode only) |
| `paxCostDetails` | Array | Pax category cost breakdown (PER_PAX mode only) |
| `grandTotalsByCurrency` | Array | Overall totals grouped by currency |
| `rateIssues` | Array | List of rate issues (null if none) |
| `hasIncompleteRates` | Boolean | True if any rate issues were found |
| `estimatedAt` | String | Timestamp when estimation was calculated |

#### DayCostDetail Fields

| Field | Type | Description |
|-------|------|-------------|
| `dayNumber` | Integer | Day number (1-indexed) |
| `dayTitle` | String | Day title |
| `date` | Date | Actual date for this day |
| `seasonName` | String | Applicable season name |
| `isOvernight` | Boolean | Whether this is an overnight day |
| `lineItems` | Array | All cost line items for this day |
| `totalsByCurrency` | Array | Day totals grouped by currency |

#### CostLineItem Fields

| Field | Type | Description |
|-------|------|-------------|
| `dayNumber` | Integer | Day number this item belongs to |
| `itemType` | String | Type: `ACCOMMODATION`, `PARK_FEE`, or `ACTIVITY` |
| `itemName` | String | Item name/description |
| `itemId` | String | Obfuscated item ID |
| `chargingBasis` | String | How it's charged: `Per Person`, `Per Room`, `Per Vehicle`, etc. |
| `quantity` | Integer | Quantity (pax count, room count, vehicle count, etc.) |
| `stoUnitPrice` | Decimal | STO rate per unit |
| `rackUnitPrice` | Decimal | Rack rate per unit |
| `stoTotalPrice` | Decimal | STO total (unit price × quantity) |
| `rackTotalPrice` | Decimal | Rack total (unit price × quantity) |
| `currency` | String | Currency code (USD, TZS, etc.) |
| `paxCategory` | String | Pax category (for categorization) |
| `notes` | String | Additional notes |

#### CurrencyGroupedCost Fields

| Field | Type | Description |
|-------|------|-------------|
| `currency` | String | Currency code |
| `accommodationSto` | Decimal | Total accommodation STO cost |
| `accommodationRack` | Decimal | Total accommodation Rack cost |
| `parkFeesSto` | Decimal | Total park fees STO cost |
| `parkFeesRack` | Decimal | Total park fees Rack cost |
| `activitiesSto` | Decimal | Total activities STO cost |
| `activitiesRack` | Decimal | Total activities Rack cost |
| `grandTotalSto` | Decimal | Grand total STO cost |
| `grandTotalRack` | Decimal | Grand total Rack cost |

#### RateIssueLog Fields

| Field | Type | Description |
|-------|------|-------------|
| `issueType` | String | Type: `MISSING`, `INACTIVE`, `NO_STO_RATE`, `NO_SEASON`, `SEASON_NOT_FOUND` |
| `itemType` | String | Item type: `ACCOMMODATION`, `PARK_FEE`, or `ACTIVITY` |
| `itemName` | String | Item name |
| `itemId` | String | Obfuscated item ID |
| `dayNumber` | Integer | Day number (if applicable) |
| `paxCategory` | String | Pax category (if applicable) |
| `seasonName` | String | Season name (if applicable) |
| `message` | String | Human-readable issue description |

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

### 400 Bad Request - Missing Dates

```json
{
  "statusCode": 400,
  "message": "Safari must have start and end dates",
  "errorCode": "MISSING_DATES"
}
```

### 404 Not Found - Safari Not Found

```json
{
  "statusCode": 404,
  "message": "Safari not found",
  "errorCode": "SAFARI_NOT_FOUND"
}
```

### 500 Internal Server Error

```json
{
  "statusCode": 500,
  "message": "Failed to estimate costs: [error details]",
  "errorCode": "COST_ESTIMATION_FAILED"
}
```

---

## Example Requests

### PER_DAY Mode (Default)

```bash
GET /api/safaris/abc123/cost-estimation
```

### PER_PAX Mode

```bash
GET /api/safaris/abc123/cost-estimation?mode=PER_PAX
```

---

## Rate Calculation Details

### STO vs Rack Rates

- **STO Rate**: Tour Operator rate (wholesale/contracted rate)
- **Rack Rate**: Public rate (retail/published rate)

Both rates are always calculated and returned, allowing frontend to choose which to display.

### Currency Handling

- Costs are never mixed across currencies
- Each currency maintains separate totals
- Grand totals are grouped by currency

### Active Rates Only

- Only rates with `isActive = true` are used
- Inactive or missing rates are logged in `rateIssues`

### Season Resolution

**Accommodations**:
1. Check if accommodation has seasons → if not, skip cost calculation
2. Find season period containing the date → use that season
3. If no period matches → fallback to highest priority season type
4. NEVER fallback to global seasons

**Parks/Activities**: Use global seasons

### Charging Bases

- **Per Person**: Cost per passenger (sums up all pax categories)
- **Per Room**: Cost per room
- **Per Vehicle**: Cost per vehicle (uses highest priority nation rate)
- **Per Group**: Fixed cost per group (uses highest priority nation rate)
- **Flat Rate**: Fixed total cost

---

## Rate Issue Types

| Issue Type | Description | Severity |
|------------|-------------|----------|
| `MISSING` | No rate record exists | Warning |
| `INACTIVE` | Rate exists but isActive = false | Warning |
| `NO_STO_RATE` | Rate exists but stoRate is null | Info |
| `NO_SEASON` | Accommodation has no seasons configured | Warning |
| `SEASON_NOT_FOUND` | No season matches date, using fallback | Info |

---

## Business Rules

### Inclusions

- **Accommodations**: Non-alternative accommodations on overnight days
- **Park Fees**: Tariffs marked as included in price
- **Activities**: Non-optional activities marked as included in price

### Exclusions

- Alternative accommodations (backup options)
- Optional activities (add-ons)
- Activities with no tariff (free/included)
- Tariffs not included in price

### Highest Priority Nation

For group/vehicle rates, the system uses the highest priority nation category:
- Non-Resident: Priority 100 (highest)
- East African: Priority 50
- Resident: Priority 10 (lowest)

This ensures accurate pricing for mixed-nationality groups.

---

## Performance Considerations

- Request-scoped rate issue logger ensures thread safety
- Read-only transaction for cost estimation
- No data modification during estimation
- Caching can be applied at client level (estimates are deterministic for same input)

---

## Integration Notes

### Frontend Usage

1. **Default View**: Use PER_DAY mode for initial display
2. **Alternate View**: Provide PER_PAX mode toggle for per-person breakdown
3. **Rate Selection**: Allow user to toggle between STO and Rack rates
4. **Currency Display**: Group costs by currency, show totals for each
5. **Issue Handling**: Display rate issues as warnings/alerts
6. **Date Context**: Show actual safari dates (no date parameter needed)

### Comparison with Itinerary Cost Estimation

The Safari cost estimation API is identical to the Itinerary cost estimation API, with one key difference:

**Safari**: Uses actual dates from Safari and SafariDay entities (no `startDate` parameter)
**Itinerary**: Requires `startDate` parameter to calculate day dates

All other aspects (calculation logic, response structure, business rules) are identical.
