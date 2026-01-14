# Activity Tariff Rate Matrix API Documentation

This document describes how to use the Rate Matrix API to render a rate input UI grid for activities.

---

## Table of Contents

1. [Overview](#overview)
2. [API Endpoint](#api-endpoint)
3. [Response Structure](#response-structure)
4. [UI Rendering Logic](#ui-rendering-logic)
5. [Charging Basis Rendering](#charging-basis-rendering)
6. [Global vs Park-Specific Rates](#global-vs-park-specific-rates)
7. [Exclusion Filters](#exclusion-filters)
8. [Frontend Implementation Examples](#frontend-implementation-examples)
9. [Bulk Upsert Integration](#bulk-upsert-integration)

---

## Overview

The Rate Matrix API provides all the data needed to render a rate input grid for an activity. The grid structure varies based on the activity's **charging basis**:

| Charging Basis | Grid Structure                          | Age Categories |
| -------------- | --------------------------------------- | -------------- |
| `PER_PERSON`   | Season × Nation Category × Age Category | **Included**   |
| `PER_VEHICLE`  | Season × Nation Category                | **Excluded**   |
| `PER_GROUP`    | Season × Nation Category                | **Excluded**   |
| `PER_DAY`      | Season × Nation Category                | **Excluded**   |
| `PER_HOUR`     | Season × Nation Category                | **Excluded**   |
| `PER_SESSION`  | Season × Nation Category                | **Excluded**   |
| `FLAT_RATE`    | Season × Nation Category                | **Excluded**   |

---

## API Endpoint

### Get Rate Matrix

```
GET /api/activity-tariff-rates/matrix
```

**Authentication:** Required

**Permission Required:** `PERM_READ_ACTIVITY_TARIFF_RATE_MATRIX`

> **Note:** This single permission grants access to the rate matrix view, which includes the necessary lookup data (seasons, age categories, nation categories, parks) required to render the rate input grid. This is a task-based permission that simplifies access control for rate management.

**Query Parameters:**

| Parameter                  | Type         | Required | Description                                                                                                                     |
| -------------------------- | ------------ | -------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `activityId`               | String       | Yes      | Obfuscated activity ID                                                                                                          |
| `parkId`                   | String       | No       | Obfuscated park ID. **If omitted, auto-selects the first available park** that offers this activity.                            |
| `excludeSeasonIds`         | List<String> | No       | Comma-separated obfuscated season IDs to exclude                                                                                |
| `excludeNationCategoryIds` | List<String> | No       | Comma-separated obfuscated nation category IDs to exclude                                                                       |
| `excludeAgeCategoryIds`    | List<String> | No       | Comma-separated obfuscated age category IDs to exclude (only applies to `PER_PERSON` activities)                                |

**Park Selection Logic:**

| Scenario                        | Behavior                                                                                           |
| ------------------------------- | -------------------------------------------------------------------------------------------------- |
| Activity has no associated parks | Global rates are returned (`isGlobalRateMatrix: true`, `park: null`, `availableParks: []`)        |
| Activity has parks, no `parkId` | Auto-selects the first available park                                                              |
| Activity has parks, `parkId` provided | Returns rates for the specified park (must be associated with the activity)                    |

> **Note:** Global rates are determined by whether an activity has parks associated with it. Activities with no park associations are global activities.

**Example Requests:**

```bash
# Auto-select first park (default behavior for activities with parks)
GET /api/activity-tariff-rates/matrix?activityId=abc123

# Specific park rates
GET /api/activity-tariff-rates/matrix?activityId=abc123&parkId=xyz789

# With exclusions
GET /api/activity-tariff-rates/matrix?activityId=abc123&excludeSeasonIds=s1,s2&excludeNationCategoryIds=n1
```

---

## Response Structure

```json
{
  "status": 200,
  "message": "Rate matrix retrieved successfully",
  "data": {
    "activity": {
      "id": "abc123",
      "name": "Game Drive",
      "chargingBasis": "PER_PERSON",
      "chargingBasisDisplayName": "Per Person",
      "hasTariff": true
    },
    "park": {
      "id": "park1",
      "name": "Serengeti National Park"
    },
    "availableParks": [
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
    "isGlobalRateMatrix": false,
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
        "activityId": "abc123",
        "activityName": "Game Drive",
        "seasonId": "s1",
        "seasonName": "High Season",
        "nationCategoryId": "nc1",
        "nationCategoryName": "EAC",
        "ageCategoryId": "ac1",
        "ageCategoryName": "Adult",
        "rackRate": 50.0,
        "stoRate": 45.0,
        "currency": "USD",
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

#### For `PER_PERSON` Activities (3-dimensional grid)

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

#### For Non-`PER_PERSON` Activities (2-dimensional grid)

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
const { chargingBasis, chargingBasisDisplayName } = response.data.activity;

// Display charging basis info to user
console.log(`Activity charges: ${chargingBasisDisplayName}`);
// Output: "Activity charges: Per Person" or "Activity charges: Per Vehicle"
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

## Global vs Park-Specific Rates

### How Global vs Park-Specific is Determined

**Global activities** are activities that have **no parks associated** with them. These are standalone activities not tied to any specific park.

**Park-specific activities** are activities that have **one or more parks associated** with them via the ParkActivity relationship.

### Global Activity (No Parks)

When an activity has no associated parks, it is a global activity.

```bash
GET /api/activity-tariff-rates/matrix?activityId=abc123
```

**Response indicators:**

```json
{
  "park": null,
  "availableParks": [],
  "isGlobalRateMatrix": true
}
```

**UI Label:** "Global Activity" or "Activity Rates"

### Park-Specific Activity (Has Parks)

When an activity has associated parks, the API auto-selects the first park if no `parkId` is provided.

```bash
# Auto-selects first park
GET /api/activity-tariff-rates/matrix?activityId=abc123
```

**Response indicators (auto-selected):**

```json
{
  "park": {
    "id": "park1",
    "name": "Serengeti National Park"
  },
  "availableParks": [
    { "id": "park1", "name": "Serengeti National Park" },
    { "id": "park2", "name": "Ngorongoro Conservation Area" }
  ],
  "isGlobalRateMatrix": false
}
```

### Switching Between Parks

For activities with multiple parks, users can switch between them using the `parkId` parameter.

```bash
# Fetch rates for a specific park
GET /api/activity-tariff-rates/matrix?activityId=abc123&parkId=park2
```

**Response:**

```json
{
  "park": {
    "id": "park2",
    "name": "Ngorongoro Conservation Area"
  },
  "availableParks": [
    { "id": "park1", "name": "Serengeti National Park" },
    { "id": "park2", "name": "Ngorongoro Conservation Area" }
  ],
  "isGlobalRateMatrix": false
}
```

> **Note:** The `parkId` must be associated with the activity. If a park is not associated with the activity, a `PARK_NOT_ASSOCIATED` error is returned.

### UI Flow Recommendation

The `availableParks` field provides all parks associated with the activity, enabling a dropdown for switching between parks.

```
┌─────────────────────────────────────────────────────────┐
│ Configure Rates for: Game Drive                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Park: [Serengeti National Park ▼]  <-- Auto-selected on first load
│        ├── Serengeti National Park    ✓                │
│        ├── Ngorongoro Conservation Area                │
│        └── Tarangire National Park                     │
│                                                         │
│  Charging Basis: Per Person                            │
│  (Rates will include age categories)                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Building the dropdown from response:**

```javascript
function buildParkDropdown(response) {
  // For global activities, no dropdown needed
  if (response.data.isGlobalRateMatrix) {
    return null; // or show "Global Activity" label
  }

  // For park-specific activities, build dropdown from available parks
  return response.data.availableParks.map(park => ({
    value: park.id,
    label: park.name,
    isSelected: response.data.park?.id === park.id
  }));
}
```

When user selects a park from dropdown:

- Call API with that park's `parkId`

---

## Exclusion Filters

Use exclusion filters to customize which categories appear in the grid.

### Use Case: User Wants to Focus on Specific Seasons

```bash
# Exclude "Low Season" and "Shoulder Season" from the matrix
GET /api/activity-tariff-rates/matrix?activityId=abc123&excludeSeasonIds=s2,s3
```

**Result:** Only "High Season" rows are returned.

### Use Case: User Wants to Skip Certain Nationalities

```bash
# Exclude "Expatriates" category
GET /api/activity-tariff-rates/matrix?activityId=abc123&excludeNationCategoryIds=nc3
```

**Result:** Only "EAC" and "Non-EAC" rows are returned.

### Use Case: User Wants to Skip Certain Age Groups

```bash
# Exclude "Child" age category (only for PER_PERSON activities)
GET /api/activity-tariff-rates/matrix?activityId=abc123&excludeAgeCategoryIds=ac3
```

**Result:** Only "Adult" and "Youth" rows are returned.


---

## Bulk Upsert Integration

After user edits rates in the grid, save them using the bulk upsert endpoint.

### Collecting Modified Rates

```javascript
function collectModifiedRates(rows, activityId, parkId) {
  return rows
    .filter((row) => row.isDirty) // Only include modified rows
    .map((row) => ({
      activityId: activityId,
      parkId: parkId, // null for global rates
      seasonId: row.season.id,
      nationCategoryId: row.nation.id,
      ageCategoryId: row.age?.id ?? null, // null for non-PER_PERSON
      rackRate: row.rackRate,
      stoRate: row.stoRate,
      currency: row.currency || "USD",
    }));
}
```

### Submitting to Bulk Upsert API

```bash
POST /api/activity-tariff-rates/bulk-upsert
Content-Type: application/json

[
  {
    "activityId": "abc123",
    "parkId": null,
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac1",
    "rackRate": 50.00,
    "stoRate": 45.00,
    "currency": "USD"
  },
  {
    "activityId": "abc123",
    "parkId": null,
    "seasonId": "s1",
    "nationCategoryId": "nc1",
    "ageCategoryId": "ac2",
    "rackRate": 25.00,
    "stoRate": 20.00,
    "currency": "USD"
  }
]
```

### Complete Save Flow

```javascript
async function saveRates() {
  const modifiedRates = collectModifiedRates(rows, activityId, parkId);

  if (modifiedRates.length === 0) {
    alert("No changes to save");
    return;
  }

  const response = await fetch("/api/activity-tariff-rates/bulk-upsert", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(modifiedRates),
  });

  const result = await response.json();

  if (result.status === 200) {
    alert(`Saved ${result.data.created + result.data.updated} rates`);
    // Refresh the matrix to get updated data
    await fetchMatrix();
  } else {
    alert(`Error: ${result.message}`);
  }
}
```

---

## Summary

| Scenario                    | API Call                             | Grid Columns                                      |
| --------------------------- | ------------------------------------ | ------------------------------------------------- |
| Global PER_PERSON activity  | `?activityId=x`                      | Season, Nationality, **Age**, STO, Rack, Currency |
| Global PER_VEHICLE activity | `?activityId=x`                      | Season, Nationality, STO, Rack, Currency          |
| Park-specific PER_PERSON    | `?activityId=x&parkId=y`             | Season, Nationality, **Age**, STO, Rack, Currency |
| Park-specific PER_VEHICLE   | `?activityId=x&parkId=y`             | Season, Nationality, STO, Rack, Currency          |
| With exclusions             | `?activityId=x&excludeSeasonIds=a,b` | Filtered rows based on exclusions                 |

The key differentiator is `includesAgeCategories` in the response - when `true`, render the age category column; when `false`, omit it.
