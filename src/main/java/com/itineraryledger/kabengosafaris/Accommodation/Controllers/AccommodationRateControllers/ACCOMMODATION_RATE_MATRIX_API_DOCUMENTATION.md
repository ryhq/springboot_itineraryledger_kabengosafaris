# Accommodation Rate Matrix API Documentation

This document describes how to use the Rate Matrix API to render a rate input UI grid for accommodations.

---

## Table of Contents

1. [Overview](#overview)
2. [API Endpoint](#api-endpoint)
3. [Response Structure](#response-structure)
4. [UI Rendering Logic](#ui-rendering-logic)
5. [Grid Structure](#grid-structure)
6. [Exclusion Filters](#exclusion-filters)
7. [Frontend Implementation Examples](#frontend-implementation-examples)
8. [Bulk Upsert Integration](#bulk-upsert-integration)

---

## Overview

The Rate Matrix API provides all the data needed to render a rate input grid for an accommodation. The grid is based on a **4-dimensional structure**:

| Dimension | Source | Description |
| --------- | ------ | ----------- |
| **Season** | Accommodation-specific | Pricing periods (High Season, Low Season, Shoulder Season) |
| **Room Type** | Accommodation-specific | Bed configurations (Single, Double, Twin, Triple) |
| **Room Standard** | Accommodation-specific | Room quality levels (Standard, Deluxe, Suite) |
| **Board Type** | Accommodation-specific | Meal plans (Room Only, B&B, Half Board, Full Board) |

### Grid Structure

```
Season × Room Standard × Room Type × Board Type | STO Rate | Rack Rate | Currency | Per Person
```

Or when filtered by a specific season:

```
Room Standard × Room Type × Board Type | STO Rate | Rack Rate | Currency | Per Person
```

### Rate Charging Models

| Model | `isPerPerson` | Description | Common Usage |
|-------|---------------|-------------|--------------|
| **Per Person Sharing (PPS)** | `true` (default) | Rate is per guest | Safari lodges, camps |
| **Per Room** | `false` | Rate is per room regardless of occupancy | Hotels, guesthouses |

### Key Differences from Activity/Park Tariff Rates

| Feature | Activity/Park Tariff Rate | Accommodation Rate |
|---------|---------------------------|-------------------|
| Dimensions | Season × Nation Category × Age Category | Season × Room Standard × Room Type × Board Type |
| Dimension Source | Global (PaxNationCategory, PaxAgeCategory) | All accommodation-specific |
| Age Category | Conditional (only for PER_PERSON) | Not applicable |
| Number of Dimensions | 2-3 (depends on charging basis) | 4 |

---

## API Endpoint

### Get Rate Matrix

```
GET /api/accommodation-rates/matrix
```

**Authentication:** Required

**Permission Required:** `PERM_READ_ACCOMMODATION_RATE_MATRIX`

> **Note:** This single permission grants access to the rate matrix view, which includes the necessary lookup data (seasons, room types, room standards, board types) required to render the rate input grid.

**Query Parameters:**

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| `accommodationId` | String | **Yes** | Obfuscated accommodation ID |
| `seasonId` | String | No | Filter by specific season (reduces grid to 3 dimensions) |
| `excludeSeasonIds` | List<String> | No | Comma-separated obfuscated season IDs to exclude |
| `excludeRoomTypeIds` | List<String> | No | Comma-separated obfuscated room type IDs to exclude |
| `excludeRoomStandardIds` | List<String> | No | Comma-separated obfuscated room standard IDs to exclude |
| `excludeBoardTypeIds` | List<String> | No | Comma-separated obfuscated board type IDs to exclude |

**Example Requests:**

```bash
# Get full matrix for an accommodation
GET /api/accommodation-rates/matrix?accommodationId=acc123

# Filter by specific season (3D grid)
GET /api/accommodation-rates/matrix?accommodationId=acc123&seasonId=s1

# With exclusions
GET /api/accommodation-rates/matrix?accommodationId=acc123&excludeSeasonIds=s1,s2&excludeRoomTypeIds=rt1

# Exclude certain room standards and board types
GET /api/accommodation-rates/matrix?accommodationId=acc123&excludeRoomStandardIds=rs3&excludeBoardTypeIds=bt4,bt5
```

---

## Response Structure

```json
{
  "status": 200,
  "message": "Rate matrix retrieved successfully",
  "data": {
    "accommodation": {
      "id": "acc123",
      "name": "Serena Hotel"
    },
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
    "roomTypes": [
      {
        "id": "rt1",
        "name": "Single Room",
        "bedConfiguration": "1 Single Bed"
      },
      {
        "id": "rt2",
        "name": "Double Room",
        "bedConfiguration": "1 King Bed"
      },
      {
        "id": "rt3",
        "name": "Twin Room",
        "bedConfiguration": "2 Single Beds"
      }
    ],
    "roomStandards": [
      {
        "id": "rs1",
        "name": "Standard"
      },
      {
        "id": "rs2",
        "name": "Deluxe"
      },
      {
        "id": "rs3",
        "name": "Suite"
      }
    ],
    "boardTypes": [
      {
        "id": "bt1",
        "name": "Room Only",
        "description": "No meals included"
      },
      {
        "id": "bt2",
        "name": "Bed & Breakfast",
        "description": "Breakfast included"
      },
      {
        "id": "bt3",
        "name": "Half Board",
        "description": "Breakfast and dinner included"
      },
      {
        "id": "bt4",
        "name": "Full Board",
        "description": "All meals included"
      }
    ],
    "existingRates": [
      {
        "id": "rate1",
        "accommodationId": "acc123",
        "accommodationName": "Serena Hotel",
        "seasonId": "s1",
        "seasonName": "High Season",
        "roomTypeId": "rt2",
        "roomTypeName": "Double Room",
        "roomStandardId": "rs2",
        "roomStandardName": "Deluxe",
        "boardTypeId": "bt4",
        "boardTypeName": "Full Board",
        "rackRate": 350.00,
        "stoRate": 300.00,
        "currency": "USD",
        "profitAmount": 50.00,
        "profitPercentage": 14.29,
        "isPerPerson": true,
        "notes": null,
        "isActive": true
      }
    ],
    "summary": {
      "totalPossibleRates": 108,
      "existingRatesCount": 1,
      "missingRatesCount": 107,
      "seasonsCount": 3,
      "roomTypesCount": 3,
      "roomStandardsCount": 3,
      "boardTypesCount": 4
    }
  }
}
```

### Response When Filtered by Season

When `seasonId` is provided, the response focuses on that specific season:

```json
{
  "status": 200,
  "message": "Rate matrix retrieved successfully",
  "data": {
    "accommodation": {
      "id": "acc123",
      "name": "Serena Hotel"
    },
    "selectedSeason": {
      "id": "s1",
      "name": "High Season",
      "seasonType": "HIGH_SEASON",
      "seasonTypeDisplayName": "High Season"
    },
    "seasons": [ ... ],
    "roomTypes": [ ... ],
    "roomStandards": [ ... ],
    "boardTypes": [ ... ],
    "existingRates": [ ... ],
    "summary": {
      "totalPossibleRates": 36,
      "existingRatesCount": 1,
      "missingRatesCount": 35,
      "seasonsCount": 1,
      "roomTypesCount": 3,
      "roomStandardsCount": 3,
      "boardTypesCount": 4
    }
  }
}
```

---

## UI Rendering Logic

### Step 1: Get Matrix Data

```javascript
const response = await fetch(`/api/accommodation-rates/matrix?accommodationId=${accommodationId}`);
const { data } = await response.json();

const {
  accommodation,
  seasons,
  roomTypes,
  roomStandards,
  boardTypes,
  existingRates
} = data;
```

### Step 2: Build Grid Rows

The grid is built by iterating through all 4 dimensions:

```javascript
function buildRateRows(seasons, roomStandards, roomTypes, boardTypes, existingRates) {
  const rows = [];
  let rowNumber = 1;

  seasons.forEach(season => {
    roomStandards.forEach(roomStandard => {
      roomTypes.forEach(roomType => {
        boardTypes.forEach(boardType => {
          rows.push({
            rowNumber: rowNumber++,
            seasonId: season.id,
            seasonName: season.name,
            roomStandardId: roomStandard.id,
            roomStandardName: roomStandard.name,
            roomTypeId: roomType.id,
            roomTypeName: roomType.name,
            bedConfiguration: roomType.bedConfiguration,
            boardTypeId: boardType.id,
            boardTypeName: boardType.name,
            // Find existing rate for this combination
            existingRate: findExistingRate(
              existingRates,
              season.id,
              roomType.id,
              roomStandard.id,
              boardType.id
            )
          });
        });
      });
    });
  });

  return rows;
}

function findExistingRate(existingRates, seasonId, roomTypeId, roomStandardId, boardTypeId) {
  return existingRates.find(rate =>
    rate.seasonId === seasonId &&
    rate.roomTypeId === roomTypeId &&
    rate.roomStandardId === roomStandardId &&
    rate.boardTypeId === boardTypeId
  );
}
```

### Step 3: Display Grid

**Example Output (Full 4-dimensional grid):**

| # | Season | Room Standard | Room Type | Board Type | STO Rate | Rack Rate | Currency |
|---|--------|---------------|-----------|------------|----------|-----------|----------|
| 1 | High Season | Standard | Single Room | Room Only | Not Set | Not Set | |
| 2 | High Season | Standard | Single Room | B&B | Not Set | Not Set | |
| 3 | High Season | Standard | Single Room | Half Board | Not Set | Not Set | |
| 4 | High Season | Standard | Single Room | Full Board | Not Set | Not Set | |
| 5 | High Season | Standard | Double Room | Room Only | Not Set | Not Set | |
| 6 | High Season | Standard | Double Room | B&B | Not Set | Not Set | |
| ... | ... | ... | ... | ... | ... | ... | |
| 36 | High Season | Suite | Twin Room | Full Board | 350.00 | 400.00 | USD |
| 37 | Low Season | Standard | Single Room | Room Only | Not Set | Not Set | |
| ... | ... | ... | ... | ... | ... | ... | |

---

## Grid Structure

### Full Grid (4 Dimensions)

When no `seasonId` filter is provided:

```
Total rows = seasons × roomStandards × roomTypes × boardTypes
Example: 3 seasons × 3 standards × 3 types × 4 boards = 108 rows
```

| Column | Description |
|--------|-------------|
| # | Row number |
| Season | Season name (High, Low, Shoulder) |
| Room Standard | Room standard name (Standard, Deluxe, Suite) |
| Room Type | Room type name (Single, Double, Twin) |
| Board Type | Board type name (Room Only, B&B, Half Board, Full Board) |
| STO Rate | Editable - Tour operator rate |
| Rack Rate | Editable - Customer rate |
| Currency | Editable - ISO 4217 code |
| Per Person | Editable - `true` for Per Person Sharing (PPS), `false` for Per Room |

### Filtered Grid (3 Dimensions)

When `seasonId` filter is provided:

```
Total rows = roomStandards × roomTypes × boardTypes
Example: 3 standards × 3 types × 4 boards = 36 rows
```

| Column | Description |
|--------|-------------|
| # | Row number |
| Room Standard | Room standard name |
| Room Type | Room type name |
| Board Type | Board type name |
| STO Rate | Editable |
| Rack Rate | Editable |
| Currency | Editable |
| Per Person | Editable - `true` for Per Person Sharing (PPS), `false` for Per Room |

---

## Exclusion Filters

Use exclusion filters to customize which categories appear in the grid.

### Use Case: Focus on Specific Seasons

```bash
# Exclude "Low Season" and "Shoulder Season"
GET /api/accommodation-rates/matrix?accommodationId=acc123&excludeSeasonIds=s2,s3
```

**Result:** Only "High Season" rows are returned.

### Use Case: Focus on Premium Room Standards

```bash
# Exclude "Standard" room standard
GET /api/accommodation-rates/matrix?accommodationId=acc123&excludeRoomStandardIds=rs1
```

**Result:** Only "Deluxe" and "Suite" rows are returned.

### Use Case: Skip Certain Room Types

```bash
# Exclude "Twin Room" type
GET /api/accommodation-rates/matrix?accommodationId=acc123&excludeRoomTypeIds=rt3
```

**Result:** Only "Single Room" and "Double Room" rows are returned.

### Use Case: Focus on Full Board Options

```bash
# Exclude room-only and B&B options
GET /api/accommodation-rates/matrix?accommodationId=acc123&excludeBoardTypeIds=bt1,bt2
```

**Result:** Only "Half Board" and "Full Board" rows are returned.

### Combined Exclusions

```bash
# High season only, deluxe rooms only, full board only
GET /api/accommodation-rates/matrix?accommodationId=acc123&excludeSeasonIds=s2,s3&excludeRoomStandardIds=rs1,rs3&excludeBoardTypeIds=bt1,bt2,bt3
```

---

## Frontend Implementation Examples

### Building the Table

```javascript
function getTableColumns(includesSeason) {
  const columns = [
    { key: 'rowNumber', header: '#' }
  ];

  if (includesSeason) {
    columns.push({ key: 'seasonName', header: 'Season' });
  }

  columns.push(
    { key: 'roomStandardName', header: 'Room Standard' },
    { key: 'roomTypeName', header: 'Room Type' },
    { key: 'boardTypeName', header: 'Board Type' },
    { key: 'stoRate', header: 'STO Rate', editable: true },
    { key: 'rackRate', header: 'Rack Rate', editable: true },
    { key: 'currency', header: 'Currency', editable: true },
    { key: 'isPerPerson', header: 'Per Person', editable: true, type: 'boolean' }
  );

  return columns;
}
```

### Season Dropdown for Filtering

```javascript
function buildSeasonDropdown(seasons, selectedSeasonId) {
  const options = [
    { value: null, label: 'All Seasons' }
  ];

  seasons.forEach(season => {
    options.push({
      value: season.id,
      label: season.name,
      isSelected: season.id === selectedSeasonId
    });
  });

  return options;
}

// When user selects a season
async function onSeasonChange(seasonId) {
  const url = seasonId
    ? `/api/accommodation-rates/matrix?accommodationId=${accommodationId}&seasonId=${seasonId}`
    : `/api/accommodation-rates/matrix?accommodationId=${accommodationId}`;

  const response = await fetch(url);
  // Re-render grid with new data
}
```

### UI Flow Recommendation

```
┌──────────────────────────────────────────────────────────────────────┐
│ Configure Rates for: Serena Hotel                                     │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Season Filter: [All Seasons ▼]                                       │
│                 ├── All Seasons    ✓                                  │
│                 ├── High Season                                       │
│                 ├── Low Season                                        │
│                 └── Shoulder Season                                   │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ # │ Season │ Std  │ Type   │ Board │ STO │ Rack │ Curr │ PPS   │  │
│  │───│────────│──────│────────│───────│─────│──────│──────│───────│  │
│  │ 1 │ High   │ Std  │ Single │ RO    │     │      │ USD  │ [✓]   │  │
│  │ 2 │ High   │ Std  │ Single │ B&B   │     │      │ USD  │ [✓]   │  │
│  │ 3 │ High   │ Std  │ Single │ HB    │     │      │ USD  │ [✓]   │  │
│  │ 4 │ High   │ Std  │ Single │ FB    │     │      │ USD  │ [✓]   │  │
│  │...│ ...    │ ...  │ ...    │ ...   │ ... │ ...  │ ...  │ ...   │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  Legend: PPS = Per Person Sharing (✓) / Per Room (☐)                  │
│  Summary: 108 possible rates | 12 configured | 96 missing             │
│                                                                       │
│  [Save Changes]  [Reset]                                              │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Bulk Upsert Integration

After user edits rates in the grid, save them using the bulk upsert endpoint.

### Collecting Modified Rates

```javascript
function collectModifiedRates(rows, accommodationId) {
  return rows
    .filter(row => row.isDirty) // Only include modified rows
    .map(row => ({
      accommodationId: accommodationId,
      seasonId: row.seasonId,
      roomTypeId: row.roomTypeId,
      roomStandardId: row.roomStandardId,
      boardTypeId: row.boardTypeId,
      rackRate: row.rackRate,
      stoRate: row.stoRate,
      currency: row.currency || 'USD',
      notes: row.notes || null,
      isActive: row.isActive !== false,
      isPerPerson: row.isPerPerson !== false // Defaults to true (PPS)
    }));
}
```

### Submitting to Bulk Upsert API

```bash
POST /api/accommodation-rates/bulk-upsert
Content-Type: application/json

[
  {
    "accommodationId": "acc123",
    "seasonId": "s1",
    "roomTypeId": "rt2",
    "roomStandardId": "rs2",
    "boardTypeId": "bt4",
    "rackRate": 350.00,
    "stoRate": 300.00,
    "currency": "USD",
    "isPerPerson": true
  },
  {
    "accommodationId": "acc123",
    "seasonId": "s1",
    "roomTypeId": "rt2",
    "roomStandardId": "rs3",
    "boardTypeId": "bt4",
    "rackRate": 450.00,
    "stoRate": 380.00,
    "currency": "USD",
    "isPerPerson": false
  }
]
```

### Complete Save Flow

```javascript
async function saveRates() {
  const modifiedRates = collectModifiedRates(rows, accommodationId);

  if (modifiedRates.length === 0) {
    alert('No changes to save');
    return;
  }

  const response = await fetch('/api/accommodation-rates/bulk-upsert', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(modifiedRates)
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

| Scenario | API Call | Grid Columns |
| -------- | -------- | ------------ |
| Full matrix (4D) | `?accommodationId=x` | Season, Room Standard, Room Type, Board Type, STO, Rack, Currency, Per Person |
| Season filtered (3D) | `?accommodationId=x&seasonId=y` | Room Standard, Room Type, Board Type, STO, Rack, Currency, Per Person |
| With exclusions | `?accommodationId=x&excludeSeasonIds=a,b` | Filtered rows based on exclusions |

The key difference from Activity/Park Tariff Rates is that **all 4 dimensions are accommodation-specific** (not global), so the grid always includes Season, Room Standard, Room Type, and Board Type - with no conditional age category logic.

### Per Person vs Per Room

The `isPerPerson` field indicates the rate charging model:
- **`true` (default)**: Per Person Sharing (PPS) - common in safari lodges/camps
- **`false`**: Per Room - common in hotels/guesthouses

This affects how costs are calculated in itinerary cost estimation:
- **PPS**: Rate × Number of guests
- **Per Room**: Rate × Number of rooms

### Total Possible Rates Calculation

```
Total = Seasons × RoomStandards × RoomTypes × BoardTypes

Example:
- 3 Seasons
- 3 Room Standards
- 3 Room Types
- 4 Board Types
= 3 × 3 × 3 × 4 = 108 possible rate combinations
```
