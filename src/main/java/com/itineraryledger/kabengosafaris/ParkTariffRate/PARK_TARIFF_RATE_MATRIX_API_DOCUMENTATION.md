# Park Tariff Rate Matrix API Documentation

This document describes how to use the Rate Matrix API to render a rate input UI grid for park tariffs.

---

## Table of Contents

1. [Overview](#overview)
2. [API Endpoint](#api-endpoint)
3. [Response Structure](#response-structure)
4. [UI Rendering Logic](#ui-rendering-logic)
5. [Charging Basis Rendering](#charging-basis-rendering)
6. [Switching Between Parks](#switching-between-parks)
7. [Exclusion Filters](#exclusion-filters)
8. [Frontend Implementation Examples](#frontend-implementation-examples)
9. [Bulk Upsert Integration](#bulk-upsert-integration)

---

## Overview

The Rate Matrix API provides all the data needed to render a rate input grid for a park tariff. The grid structure varies based on the tariff's **charging basis**:

| Charging Basis | Grid Structure                          | Age Categories |
| -------------- | --------------------------------------- | -------------- |
| `PER_PERSON`   | Season × Nation Category × Age Category | **Included**   |
| `PER_VEHICLE`  | Season × Nation Category                | **Excluded**   |
| `PER_GROUP`    | Season × Nation Category                | **Excluded**   |
| `PER_DAY`      | Season × Nation Category                | **Excluded**   |
| `PER_HOUR`     | Season × Nation Category                | **Excluded**   |
| `PER_SESSION`  | Season × Nation Category                | **Excluded**   |
| `FLAT_RATE`    | Season × Nation Category                | **Excluded**   |

> **Note:** Unlike Activity Tariff Rates, Park Tariff Rates are **always park-specific**. There are no global rates - both `parkId` and `tariffId` are required.

---

## API Endpoint

### Get Rate Matrix

```
GET /api/park-tariff-rates/matrix
```

**Authentication:** Required

**Permission Required:** `PERM_READ_PARK_TARIFF_RATE_MATRIX`

> **Note:** This single permission grants access to the rate matrix view, which includes the necessary lookup data (seasons, age categories, nation categories, parks with tariff) required to render the rate input grid. This is a task-based permission that simplifies access control for rate management.

**Query Parameters:**

| Parameter                  | Type         | Required | Description                                                                                  |
| -------------------------- | ------------ | -------- | -------------------------------------------------------------------------------------------- |
| `parkId`                   | String       | **Yes**  | Obfuscated park ID                                                                           |
| `tariffId`                 | String       | **Yes**  | Obfuscated tariff ID                                                                         |
| `excludeSeasonIds`         | List<String> | No       | Comma-separated obfuscated season IDs to exclude                                             |
| `excludeNationCategoryIds` | List<String> | No       | Comma-separated obfuscated nation category IDs to exclude                                    |
| `excludeAgeCategoryIds`    | List<String> | No       | Comma-separated obfuscated age category IDs to exclude (only applies to `PER_PERSON` tariffs)|

**Example Requests:**

```bash
# Get rate matrix for a park-tariff combination
GET /api/park-tariff-rates/matrix?parkId=park123&tariffId=tariff123

# With exclusions
GET /api/park-tariff-rates/matrix?parkId=park123&tariffId=tariff123&excludeSeasonIds=s1,s2

# Exclude certain nation categories
GET /api/park-tariff-rates/matrix?parkId=park123&tariffId=tariff123&excludeNationCategoryIds=n1
```

---

## Response Structure

```json
{
  "status": 200,
  "message": "Rate matrix retrieved successfully",
  "data": {
    "park": {
      "id": "park1",
      "name": "Serengeti National Park"
    },
    "tariff": {
      "id": "tariff1",
      "name": "Park Entry Fee",
      "chargingBasis": "PER_PERSON",
      "chargingBasisDisplayName": "Per Person"
    },
    "parksWithTariff": [
      {
        "id": "park1",
        "name": "Serengeti National Park"
      },
      {
        "id": "park2",
        "name": "Ngorongoro Conservation Area"
      },
      {
        "id": "park3",
        "name": "Tarangire National Park"
      }
    ],
    "seasons": [
      {
        "id": "s1",
        "name": "High Season",
        "seasonType": "HIGH_SEASON",
        "seasonTypeDisplayName": "High Season"
      },
      {
        "id": "s2",
        "name": "Low Season",
        "seasonType": "LOW_SEASON",
        "seasonTypeDisplayName": "Low Season"
      },
      {
        "id": "s3",
        "name": "Shoulder Season",
        "seasonType": "SHOULDER_SEASON",
        "seasonTypeDisplayName": "Shoulder Season"
      }
    ],
    "nationCategories": [
      {
        "id": "nc1",
        "name": "EAC",
        "categoryType": "EAST_AFRICAN",
        "priorityFactor": 1
      },
      {
        "id": "nc2",
        "name": "Non-EAC",
        "categoryType": "NON_RESIDENT",
        "priorityFactor": 2
      },
      {
        "id": "nc3",
        "name": "Expatriates",
        "categoryType": "EXPATRIATE",
        "priorityFactor": 3
      }
    ],
    "ageCategories": [
      {
        "id": "ac1",
        "name": "Adult",
        "categoryType": "ADULT",
        "ageRange": "15+ years",
        "minAge": 15,
        "maxAge": 150
      },
      {
        "id": "ac2",
        "name": "Youth",
        "categoryType": "YOUTH",
        "ageRange": "6-14 years",
        "minAge": 6,
        "maxAge": 14
      },
      {
        "id": "ac3",
        "name": "Child",
        "categoryType": "CHILD",
        "ageRange": "0-5 years",
        "minAge": 0,
        "maxAge": 5
      }
    ],
    "includesAgeCategories": true,
    "existingRates": [
      {
        "id": "rate1",
        "parkId": "park1",
        "parkName": "Serengeti National Park",
        "tariffId": "tariff1",
        "tariffName": "Park Entry Fee",
        "tariffChargingBasis": "Per Person",
        "seasonId": "s1",
        "seasonName": "High Season",
        "nationCategoryId": "nc1",
        "nationCategoryName": "EAC",
        "ageCategoryId": "ac1",
        "ageCategoryName": "Adult",
        "ageCategoryAgeRange": "15+ years",
        "rackRate": 70.00,
        "stoRate": 60.00,
        "currency": "USD",
        "profitAmount": 10.00,
        "profitPercentage": 14.29,
        "isActive": true
      }
    ],
    "summary": {
      "totalPossibleRates": 27,
      "existingRatesCount": 1,
      "missingRatesCount": 26,
      "seasonsCount": 3,
      "nationCategoriesCount": 3,
      "ageCategoriesCount": 3
    }
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `park` | ParkInfo | Current park being viewed |
| `tariff` | TariffInfo | Tariff information including charging basis |
| `parksWithTariff` | List<ParkInfo> | All parks where this tariff is configured (for dropdown selection) |
| `seasons` | List<SeasonInfo> | Active global seasons (after exclusions) |
| `nationCategories` | List<NationCategoryInfo> | Active nation categories (after exclusions) |
| `ageCategories` | List<AgeCategoryInfo> | Active age categories (null if not `PER_PERSON`, after exclusions) |
| `includesAgeCategories` | Boolean | Whether age categories apply to this tariff |
| `existingRates` | List<ParkTariffRateDTO> | Existing rates for this park-tariff combination |
| `summary` | MatrixSummary | Summary counts for the matrix |

---

## UI Rendering Logic

### Step 1: Check `includesAgeCategories`

```javascript
const { includesAgeCategories, seasons, nationCategories, ageCategories } =
  response.data;

if (includesAgeCategories) {
  // Render PER_PERSON grid (with age categories)
  renderPerPersonGrid(seasons, nationCategories, ageCategories);
} else {
  // Render non-PER_PERSON grid (without age categories)
  renderSimpleGrid(seasons, nationCategories);
}
```

### Step 2: Build Grid Rows

#### For `PER_PERSON` Tariffs (3-dimensional grid)

Each row represents: **Season × Nation Category × Age Category**

```javascript
function buildPerPersonRows(seasons, nationCategories, ageCategories) {
  const rows = [];
  let rowNumber = 1;

  seasons.forEach((season) => {
    nationCategories.forEach((nationCategory) => {
      ageCategories.forEach((ageCategory) => {
        rows.push({
          rowNumber: rowNumber++,
          seasonId: season.id,
          seasonName: season.name,
          nationCategoryId: nationCategory.id,
          nationCategoryName: nationCategory.name,
          ageCategoryId: ageCategory.id,
          ageCategoryName: ageCategory.name,
          ageRange: ageCategory.ageRange,
          // Find existing rate for this combination
          existingRate: findExistingRate(
            season.id,
            nationCategory.id,
            ageCategory.id
          ),
        });
      });
    });
  });

  return rows;
}
```

**Example Output (PER_PERSON):**

| #   | Season          | Pax Nationality | Pax Age | STO Rate       | Rack Rate      | Currency |
| --- | --------------- | --------------- | ------- | -------------- | -------------- | -------- |
| 1   | High Season     | EAC             | Adult   | Not Set / Free | Not Set / Free |          |
| 2   | High Season     | EAC             | Youth   | Not Set / Free | Not Set / Free |          |
| 3   | High Season     | EAC             | Child   | Not Set / Free | Not Set / Free |          |
| 4   | High Season     | Non-EAC         | Adult   | Not Set / Free | Not Set / Free |          |
| 5   | High Season     | Non-EAC         | Youth   | Not Set / Free | Not Set / Free |          |
| 6   | High Season     | Non-EAC         | Child   | Not Set / Free | Not Set / Free |          |
| 7   | High Season     | Expatriates     | Adult   | Not Set / Free | Not Set / Free |          |
| 8   | High Season     | Expatriates     | Youth   | Not Set / Free | Not Set / Free |          |
| 9   | High Season     | Expatriates     | Child   | Not Set / Free | Not Set / Free |          |
| 10  | Shoulder Season | EAC             | Adult   | Not Set / Free | Not Set / Free |          |
| ... | ...             | ...             | ...     | ...            | ...            |          |

#### For Non-`PER_PERSON` Tariffs (2-dimensional grid)

Each row represents: **Season × Nation Category** (no age category)

```javascript
function buildSimpleRows(seasons, nationCategories) {
  const rows = [];
  let rowNumber = 1;

  seasons.forEach((season) => {
    nationCategories.forEach((nationCategory) => {
      rows.push({
        rowNumber: rowNumber++,
        seasonId: season.id,
        seasonName: season.name,
        nationCategoryId: nationCategory.id,
        nationCategoryName: nationCategory.name,
        ageCategoryId: null, // No age category
        ageCategoryName: null,
        existingRate: findExistingRate(season.id, nationCategory.id, null),
      });
    });
  });

  return rows;
}
```

**Example Output (PER_VEHICLE, PER_GROUP, etc.):**

| #   | Season          | Pax Nationality | STO Rate       | Rack Rate      | Currency |
| --- | --------------- | --------------- | -------------- | -------------- | -------- |
| 1   | High Season     | EAC             | Not Set / Free | Not Set / Free |          |
| 2   | High Season     | Non-EAC         | Not Set / Free | Not Set / Free |          |
| 3   | High Season     | Expatriates     | Not Set / Free | Not Set / Free |          |
| 4   | Shoulder Season | EAC             | Not Set / Free | Not Set / Free |          |
| 5   | Shoulder Season | Non-EAC         | Not Set / Free | Not Set / Free |          |
| 6   | Shoulder Season | Expatriates     | Not Set / Free | Not Set / Free |          |
| 7   | Low Season      | EAC             | Not Set / Free | Not Set / Free |          |
| 8   | Low Season      | Non-EAC         | Not Set / Free | Not Set / Free |          |
| 9   | Low Season      | Expatriates     | Not Set / Free | Not Set / Free |          |

---

## Charging Basis Rendering

### Detecting Charging Basis

```javascript
const { chargingBasis, chargingBasisDisplayName } = response.data.tariff;

// Display charging basis info to user
console.log(`Tariff charges: ${chargingBasisDisplayName}`);
// Output: "Tariff charges: Per Person" or "Tariff charges: Per Vehicle"
```

### Conditional Column Rendering

```javascript
function getTableColumns(includesAgeCategories) {
  const baseColumns = [
    { key: "rowNumber", header: "#" },
    { key: "seasonName", header: "Season" },
    { key: "nationCategoryName", header: "Pax Nationality" },
  ];

  if (includesAgeCategories) {
    baseColumns.push({ key: "ageCategoryName", header: "Pax Age" });
  }

  baseColumns.push(
    { key: "stoRate", header: "STO Rate", editable: true },
    { key: "rackRate", header: "Rack Rate", editable: true },
    { key: "currency", header: "Currency", editable: true }
  );

  return baseColumns;
}
```

---

## Switching Between Parks

The `parksWithTariff` field provides all parks that have the same tariff configured. This enables a dropdown for switching between parks to view/edit their rates.

### UI Flow Recommendation

```
┌─────────────────────────────────────────────────────────┐
│ Configure Rates for: Park Entry Fee                     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Park: [Serengeti National Park ▼]                     │
│        ├── Serengeti National Park    ✓                │
│        ├── Ngorongoro Conservation Area                │
│        └── Tarangire National Park                     │
│                                                         │
│  Charging Basis: Per Person                            │
│  (Rates will include age categories)                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Building the Park Dropdown

```javascript
function buildParkDropdown(response) {
  const { park, parksWithTariff } = response.data;

  return parksWithTariff.map(p => ({
    value: p.id,
    label: p.name,
    isSelected: park.id === p.id
  }));
}

// When user selects a different park
function onParkChange(newParkId) {
  // Re-fetch matrix with new park
  fetchMatrix(newParkId, tariffId);
}
```

### Example: Switching Parks

```bash
# User is viewing Serengeti rates
GET /api/park-tariff-rates/matrix?parkId=park1&tariffId=tariff1

# User switches to Ngorongoro
GET /api/park-tariff-rates/matrix?parkId=park2&tariffId=tariff1
```

---

## Exclusion Filters

Use exclusion filters to customize which categories appear in the grid.

### Use Case: User Wants to Focus on Specific Seasons

```bash
# Exclude "Low Season" and "Shoulder Season" from the matrix
GET /api/park-tariff-rates/matrix?parkId=park1&tariffId=t1&excludeSeasonIds=s2,s3
```

**Result:** Only "High Season" rows are returned.

### Use Case: User Wants to Skip Certain Nationalities

```bash
# Exclude "Expatriates" category
GET /api/park-tariff-rates/matrix?parkId=park1&tariffId=t1&excludeNationCategoryIds=nc3
```

**Result:** Only "EAC" and "Non-EAC" rows are returned.

### Use Case: User Wants to Skip Certain Age Groups

```bash
# Exclude "Child" age category (only for PER_PERSON tariffs)
GET /api/park-tariff-rates/matrix?parkId=park1&tariffId=t1&excludeAgeCategoryIds=ac3
```

**Result:** Only "Adult" and "Youth" rows are returned.

---

## Frontend Implementation Examples

### Complete Matrix Fetching

```javascript
async function fetchRateMatrix(parkId, tariffId, exclusions = {}) {
  const params = new URLSearchParams({
    parkId,
    tariffId,
  });

  if (exclusions.seasonIds?.length) {
    params.append('excludeSeasonIds', exclusions.seasonIds.join(','));
  }
  if (exclusions.nationCategoryIds?.length) {
    params.append('excludeNationCategoryIds', exclusions.nationCategoryIds.join(','));
  }
  if (exclusions.ageCategoryIds?.length) {
    params.append('excludeAgeCategoryIds', exclusions.ageCategoryIds.join(','));
  }

  const response = await fetch(`/api/park-tariff-rates/matrix?${params}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  return response.json();
}
```

### Finding Existing Rate for a Cell

```javascript
function findExistingRate(existingRates, seasonId, nationCategoryId, ageCategoryId) {
  return existingRates.find(rate =>
    rate.seasonId === seasonId &&
    rate.nationCategoryId === nationCategoryId &&
    rate.ageCategoryId === ageCategoryId
  );
}
```

### Building Editable Grid Rows

```javascript
function buildGridRows(matrixData) {
  const { seasons, nationCategories, ageCategories, includesAgeCategories, existingRates } = matrixData;
  const rows = [];
  let rowNumber = 1;

  seasons.forEach(season => {
    nationCategories.forEach(nationCategory => {
      if (includesAgeCategories) {
        // PER_PERSON: include age categories
        ageCategories.forEach(ageCategory => {
          const existingRate = findExistingRate(existingRates, season.id, nationCategory.id, ageCategory.id);
          rows.push({
            rowNumber: rowNumber++,
            season,
            nationCategory,
            ageCategory,
            stoRate: existingRate?.stoRate ?? null,
            rackRate: existingRate?.rackRate ?? null,
            currency: existingRate?.currency ?? 'USD',
            existingRateId: existingRate?.id ?? null,
            isDirty: false,
          });
        });
      } else {
        // Non-PER_PERSON: no age category
        const existingRate = findExistingRate(existingRates, season.id, nationCategory.id, null);
        rows.push({
          rowNumber: rowNumber++,
          season,
          nationCategory,
          ageCategory: null,
          stoRate: existingRate?.stoRate ?? null,
          rackRate: existingRate?.rackRate ?? null,
          currency: existingRate?.currency ?? 'USD',
          existingRateId: existingRate?.id ?? null,
          isDirty: false,
        });
      }
    });
  });

  return rows;
}
```

---

## Bulk Upsert Integration

After user edits rates in the grid, save them using the bulk upsert endpoint.

### Collecting Modified Rates

```javascript
function collectModifiedRates(rows, parkId, tariffId) {
  return rows
    .filter(row => row.isDirty) // Only include modified rows
    .filter(row => row.stoRate != null && row.rackRate != null) // Must have both rates
    .map(row => ({
      parkId: parkId,
      tariffId: tariffId,
      seasonId: row.season.id,
      nationCategoryId: row.nationCategory.id,
      ageCategoryId: row.ageCategory?.id ?? null, // null for non-PER_PERSON
      rackRate: row.rackRate,
      stoRate: row.stoRate,
      currency: row.currency || 'USD',
    }));
}
```

### Submitting to Bulk Upsert API

```bash
POST /api/park-tariff-rates/bulk-upsert
Content-Type: application/json

[
  {
    "parkId": "park123",
    "tariffId": "tariff123",
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac1",
    "rackRate": 70.00,
    "stoRate": 60.00,
    "currency": "USD"
  },
  {
    "parkId": "park123",
    "tariffId": "tariff123",
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac2",
    "rackRate": 35.00,
    "stoRate": 30.00,
    "currency": "USD"
  }
]
```

### Complete Save Flow

```javascript
async function saveRates(rows, parkId, tariffId) {
  const modifiedRates = collectModifiedRates(rows, parkId, tariffId);

  if (modifiedRates.length === 0) {
    alert('No changes to save');
    return;
  }

  // Validate: rackRate >= stoRate
  const invalidRates = modifiedRates.filter(r => r.rackRate < r.stoRate);
  if (invalidRates.length > 0) {
    alert('Rack rate must be greater than or equal to STO rate');
    return;
  }

  const response = await fetch('/api/park-tariff-rates/bulk-upsert', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(modifiedRates),
  });

  const result = await response.json();

  if (result.status === 200) {
    const { created, updated, failed } = result.data;
    alert(`Saved ${created + updated} rates. ${failed > 0 ? `${failed} failed.` : ''}`);

    if (result.data.errors?.length > 0) {
      console.error('Errors:', result.data.errors);
    }

    // Refresh the matrix to get updated data
    await fetchRateMatrix(parkId, tariffId);
  } else {
    alert(`Error: ${result.message}`);
  }
}
```

---

## Error Responses

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `PARK_ID_REQUIRED` | Park ID is required |
| 400 | `TARIFF_ID_REQUIRED` | Tariff ID is required |
| 400 | `INVALID_PARK_ID` | Invalid park ID format |
| 400 | `INVALID_TARIFF_ID` | Invalid tariff ID format |
| 404 | `PARK_NOT_FOUND` | Park not found |
| 404 | `TARIFF_NOT_FOUND` | Tariff not found |
| 400 | `PARK_TARIFF_NOT_FOUND` | Park-tariff relationship not found |
| 400 | `TARIFF_NO_CHARGING_BASIS` | Tariff has no charging basis set |

---

## Summary

| Scenario                    | API Call                                          | Grid Columns                                      |
| --------------------------- | ------------------------------------------------- | ------------------------------------------------- |
| PER_PERSON tariff           | `?parkId=x&tariffId=y`                            | Season, Nationality, **Age**, STO, Rack, Currency |
| PER_VEHICLE tariff          | `?parkId=x&tariffId=y`                            | Season, Nationality, STO, Rack, Currency          |
| PER_GROUP tariff            | `?parkId=x&tariffId=y`                            | Season, Nationality, STO, Rack, Currency          |
| With season exclusions      | `?parkId=x&tariffId=y&excludeSeasonIds=a,b`       | Filtered rows based on exclusions                 |
| With nation exclusions      | `?parkId=x&tariffId=y&excludeNationCategoryIds=c` | Filtered rows based on exclusions                 |

The key differentiator is `includesAgeCategories` in the response - when `true`, render the age category column; when `false`, omit it.

---

## Related Documentation

- [Park Tariff Rate API](PARK_TARIFF_RATE_API_DOCUMENTATION.md) - For CRUD operations on rates
