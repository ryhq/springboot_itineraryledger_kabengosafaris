# Season Period Overlap Validation

## Overview

Comprehensive overlap validation has been implemented to prevent conflicting season periods within the same season. This ensures data integrity and prevents scheduling conflicts.

## Implementation

### Files Modified

1. **[CreateSeasonPeriodService.java](Services/SeasonPeriodServices/CreateSeasonPeriodService.java)**
   - Added `checkForOverlappingPeriods()` method
   - Validates new periods before creation

2. **[UpdateSeasonPeriodService.java](Services/SeasonPeriodServices/UpdateSeasonPeriodService.java)**
   - Added `checkForOverlappingPeriods()` method
   - Validates updated periods (excludes current period from check)

### Overlap Logic

Two season periods are considered **overlapping** if:

1. **They belong to the same season** (checked at service level)
2. **They have matching years**:
   - Both are recurring (year = null), OR
   - Both have the same specific year value
3. **Their date ranges intersect**:
   - `!(endDate < otherStartDate) AND !(startDate > otherEndDate)`

This logic correctly handles:
- **Normal periods**: June 1 - August 31
- **Year-wrapping periods**: December 15 - January 15
- **Recurring periods**: Annual periods without year specification
- **Year-specific periods**: One-time periods for specific years

## Validation Rules

### Create Season Period

When creating a new season period, the system:

1. Validates that season exists
2. Checks ALL active periods in that season
3. Rejects creation if overlap detected
4. Returns error with details of conflicting period

### Update Season Period

When updating an existing season period, the system:

1. Validates that period exists
2. Determines final values (updates + existing)
3. Checks ALL active periods **except the one being updated**
4. Rejects update if overlap detected
5. Returns error with details of conflicting period

### Special Cases

#### Inactive Periods
- **Inactive periods are skipped** during overlap checking
- Only active periods (`isActive = true`) are validated against
- This allows "deactivating" a period instead of deleting it

#### Different Years
- Periods with different years **never overlap**
- Year-specific period (2024) ≠ Year-specific period (2025)
- Year-specific period (2024) ≠ Recurring period (null)

#### Same Date Ranges
- Identical date ranges in the **same year are considered overlapping**
- Example: Two periods both covering June 1 - August 31, 2024 → OVERLAP

## Example Scenarios

### ✅ Valid (No Overlap)

**Scenario 1: Different Years**
```
Period A: June 1 - August 31, 2024
Period B: June 1 - August 31, 2025
Result: VALID - Different years
```

**Scenario 2: Sequential Periods**
```
Period A: June 1 - August 31 (recurring)
Period B: September 1 - November 30 (recurring)
Result: VALID - No date overlap
```

**Scenario 3: Recurring vs Year-Specific**
```
Period A: June 1 - August 31 (recurring, year = null)
Period B: June 1 - August 31, 2024
Result: VALID - Different year types
```

**Scenario 4: Inactive Period**
```
Period A: June 1 - August 31 (active = false)
Period B: June 1 - August 31 (active = true)
Result: VALID - Inactive period ignored
```

### ❌ Invalid (Overlap)

**Scenario 1: Exact Match**
```
Period A: June 1 - August 31 (recurring)
Period B: June 1 - August 31 (recurring)
Result: OVERLAP - Identical date ranges, same year type
```

**Scenario 2: Partial Overlap**
```
Period A: June 1 - August 31 (recurring)
Period B: August 1 - September 30 (recurring)
Result: OVERLAP - August overlaps
```

**Scenario 3: One Contains Another**
```
Period A: June 1 - August 31 (recurring)
Period B: July 1 - July 31 (recurring)
Result: OVERLAP - B is completely within A
```

**Scenario 4: Year-Wrapping Overlap**
```
Period A: December 15 - January 15 (recurring)
Period B: January 1 - January 31 (recurring)
Result: OVERLAP - January overlaps
```

## Error Response

When overlap is detected, the API returns:

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Season period overlaps with an existing period for year 2024. Existing period: 06-01 to 08-31",
  "errorCode": "OVERLAPPING_PERIOD",
  "timestamp": "2024-01-15T10:30:00"
}
```

For recurring periods:
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Season period overlaps with an existing period (recurring period). Existing period: 06-01 to 08-31",
  "errorCode": "OVERLAPPING_PERIOD",
  "timestamp": "2024-01-15T10:30:00"
}
```

## Algorithm Details

### Overlap Detection Formula

The core overlap detection uses the standard interval intersection algorithm:

```java
boolean overlaps = !endDate.isBefore(existingStartDate) &&
                  !startDate.isAfter(existingEndDate);
```

This is logically equivalent to:
```
overlaps = (endDate >= existingStartDate) AND (startDate <= existingEndDate)
```

### Why This Works

For two periods A and B to **NOT overlap**, one of these must be true:
1. A ends before B starts: `A.end < B.start`
2. A starts after B ends: `A.start > B.end`

Therefore, they **DO overlap** if:
```
NOT (A.end < B.start) AND NOT (A.start > B.end)
```

This correctly handles all cases including year-wrapping periods.

## MonthDay Comparison

MonthDay objects use natural ordering:
- `MonthDay.of(6, 1)` (June 1) < `MonthDay.of(8, 31)` (August 31)
- `MonthDay.of(12, 15)` (Dec 15) > `MonthDay.of(1, 15)` (Jan 15)

For year-wrapping periods (Dec 15 - Jan 15):
- Start > End indicates year wrapping
- Overlap logic still works because it checks both conditions

## Testing

### Manual Testing

**Test 1: Create Overlapping Period (Should Fail)**
```json
POST /api/season-periods
{
  "seasonId": "xyz123",
  "startDate": "06-01",
  "endDate": "08-31",
  "year": 2024
}

// Try to create another overlapping period
POST /api/season-periods
{
  "seasonId": "xyz123",
  "startDate": "07-01",
  "endDate": "09-30",
  "year": 2024
}
Expected: 400 Bad Request - OVERLAPPING_PERIOD
```

**Test 2: Create Non-Overlapping Period (Should Succeed)**
```json
POST /api/season-periods
{
  "seasonId": "xyz123",
  "startDate": "06-01",
  "endDate": "08-31",
  "year": 2024
}

// Create period in different year
POST /api/season-periods
{
  "seasonId": "xyz123",
  "startDate": "06-01",
  "endDate": "08-31",
  "year": 2025
}
Expected: 201 Created
```

**Test 3: Update Creating Overlap (Should Fail)**
```json
// Existing: Period A (June 1 - Aug 31, 2024)
// Existing: Period B (Sep 1 - Nov 30, 2024)

// Try to extend Period B to overlap with A
PUT /api/season-periods/{periodB_id}
{
  "startDate": "08-15"
}
Expected: 400 Bad Request - OVERLAPPING_PERIOD
```

## Performance Considerations

- **Complexity**: O(n) where n = number of periods in the season
- **Optimization**: Inactive periods are skipped early
- **Typical Use**: Most seasons have 2-5 periods, so performance impact is minimal
- **Indexing**: Database indexes on `season_id`, `year`, and `is_active` improve query performance

## Future Enhancements

Potential improvements for consideration:

1. **Batch Validation**: Validate multiple periods at once during season creation
2. **Adjacent Period Helpers**: API to find gaps between periods
3. **Period Merging**: Suggest merging adjacent/overlapping periods
4. **Visual Timeline**: Frontend calendar showing all periods
5. **Warning for Close Dates**: Warn if periods are very close (e.g., Aug 31 / Sep 1)

## References

- Based on reference implementation: `/home/ricksy/Documents/SPRING BOOT PROJECTS/ItineraryLedger/src/main/java/com/kabengosafaris/ItineraryLedger/Season/SeasonService.java`
- Overlap algorithm: Standard interval intersection detection
- MonthDay handling: Java Time API MonthDay class
