# Itinerary Module Implementation Plan

## Overview

Implement a comprehensive Itinerary module for the kabengosafaris system - a skeleton/template system for safari itineraries that tracks days, activities, parks, tariffs, accommodations, and passenger categories.

## Entity Hierarchy

```
Itinerary (Root)
├── ItineraryPax (Passenger categories with counts)
└── ItineraryDay (Day-by-day breakdown)
    ├── ItineraryDayActivity (Standalone activities per day)
    ├── ItineraryDayAccommodation (Lodging per day)
    └── ItineraryDayPark (Parks visited per day)
        ├── ItineraryDayParkActivity (Activities within a park)
        └── ItineraryDayParkTariff (Tariffs for park visit)
```

## Directory Structure

```
/src/main/java/com/itineraryledger/kabengosafaris/Itinerary/
├── Entities/
│   ├── Itinerary.java
│   ├── ItineraryPax.java
│   ├── ItineraryDay.java
│   ├── ItineraryDayActivity.java
│   ├── ItineraryDayPark.java
│   ├── ItineraryDayParkActivity.java
│   ├── ItineraryDayParkTariff.java
│   └── ItineraryDayAccommodation.java
├── Repositories/
│   └── (8 repository interfaces)
├── Services/
│   ├── ItineraryServices/
│   ├── ItineraryDayServices/
│   ├── ItineraryPaxServices/
│   ├── ItineraryDayParkServices/
│   └── ItineraryDayAccommodationServices/
├── Controllers/
│   ├── ItineraryController.java
│   ├── ItineraryDayController.java
│   ├── ItineraryPaxController.java
│   ├── ItineraryDayParkController.java
│   └── ItineraryDayAccommodationController.java
├── DTOs/
│   └── (Organized by entity)
└── Specifications/
    └── ItinerarySpecification.java
```

---

## Phase 1: Core Entities

### 1.1 Itinerary Entity
**File:** `Itinerary/Entities/Itinerary.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| name | String(200) | Required, e.g., "7-Day Serengeti Safari" |
| slug | String(250) | Auto-generated, unique |
| status | ItineraryStatus | DRAFT, COMPLETE, PUBLISHED, ARCHIVED |
| totalDays | Integer | Required, min 1 |
| totalNights | Integer | Required, default = totalDays - 1 |
| carCount | Integer | Default 1 |
| description | TEXT | Rich description |
| highlights | TEXT | Key highlights (JSON array) |
| startLocation | String(200) | e.g., "Arusha" |
| endLocation | String(200) | e.g., "Arusha" |
| difficultyLevel | String(50) | Easy/Moderate/Challenging |
| isTemplate | Boolean | Default true |
| isActive | Boolean | Default true |
| createdBy | Long | User ID |
| internalNotes | TEXT | Staff notes |

**Relationships:**
- `@OneToMany` → ItineraryDay (cascade all, orphan removal, ordered by dayNumber)
- `@OneToMany` → ItineraryPax (cascade all, orphan removal)

### 1.2 ItineraryDay Entity
**File:** `Itinerary/Entities/ItineraryDay.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| itinerary | Itinerary | ManyToOne, required |
| dayNumber | Integer | Required, unique per itinerary |
| title | String(200) | Required, e.g., "Arrival in Arusha" |
| description | TEXT | Day description |
| startLocation | String(200) | |
| endLocation | String(200) | |
| distanceKm | Integer | Driving distance |
| driveTimeHours | BigDecimal | |
| isOvernight | Boolean | Default true, false for departure day |
| mealsIncluded | String(100) | e.g., "B,L,D" |
| internalNotes | TEXT | |

**Unique Constraint:** (itinerary_id, day_number)

**Relationships:**
- `@OneToMany` → ItineraryDayActivity (ordered by sortOrder)
- `@OneToMany` → ItineraryDayPark (ordered by sortOrder)
- `@OneToMany` → ItineraryDayAccommodation

### 1.3 ItineraryPax Entity
**File:** `Itinerary/Entities/ItineraryPax.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| itinerary | Itinerary | ManyToOne, required |
| nationCategory | PaxNationCategory | ManyToOne, required |
| ageCategory | PaxAgeCategory | ManyToOne, required |
| count | Integer | Required, min 0 |
| notes | TEXT | |

**Unique Constraint:** (itinerary_id, nation_category_id, age_category_id)

---

## Phase 2: Nested Entities

### 2.1 ItineraryDayActivity Entity
**File:** `Itinerary/Entities/ItineraryDayActivity.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| itineraryDay | ItineraryDay | ManyToOne, required |
| activity | Activity | ManyToOne, required |
| sortOrder | Integer | Default 0 |
| durationHours | BigDecimal | |
| startTime | String(10) | e.g., "06:00" |
| endTime | String(10) | |
| notes | TEXT | |
| isIncludedInPrice | Boolean | Default true |

### 2.2 ItineraryDayPark Entity
**File:** `Itinerary/Entities/ItineraryDayPark.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| itineraryDay | ItineraryDay | ManyToOne, required |
| park | Park | ManyToOne, required |
| entryType | ParkEntryType | TRANSIT, DAY_TRIP, SLEEP_OVER |
| sortOrder | Integer | Default 0 |
| arrivalTime | String(10) | |
| departureTime | String(10) | |
| notes | TEXT | |

**Relationships:**
- `@OneToMany` → ItineraryDayParkActivity
- `@OneToMany` → ItineraryDayParkTariff

### 2.3 ItineraryDayParkActivity Entity
**File:** `Itinerary/Entities/ItineraryDayParkActivity.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| itineraryDayPark | ItineraryDayPark | ManyToOne, required |
| parkActivity | ParkActivity | ManyToOne via @JoinColumns (park_id, activity_id) |
| sortOrder | Integer | Default 0 |
| durationHours | BigDecimal | |
| notes | TEXT | |
| isIncludedInPrice | Boolean | Default true |

### 2.4 ItineraryDayParkTariff Entity
**File:** `Itinerary/Entities/ItineraryDayParkTariff.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| itineraryDayPark | ItineraryDayPark | ManyToOne, required |
| parkTariff | ParkTariff | ManyToOne via @JoinColumns (park_id, tariff_id) |
| notes | TEXT | |
| isIncludedInPrice | Boolean | Default true |

### 2.5 ItineraryDayAccommodation Entity
**File:** `Itinerary/Entities/ItineraryDayAccommodation.java`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Auto-generated PK |
| itineraryDay | ItineraryDay | ManyToOne, required |
| accommodation | Accommodation | ManyToOne, required |
| roomType | AccommodationRoomType | ManyToOne, optional |
| roomStandard | AccommodationRoomStandard | ManyToOne, optional |
| boardType | AccommodationBoardType | ManyToOne, optional |
| roomCount | Integer | Default 1 |
| nights | Integer | Default 1 |
| isAlternative | Boolean | Default false (backup option) |
| notes | TEXT | |

---

## Phase 3: Enums

### ItineraryStatus
```java
public enum ItineraryStatus {
    DRAFT("Draft", "Itinerary is being created/edited"),
    COMPLETE("Complete", "All required data is filled"),
    PUBLISHED("Published", "Itinerary is available for booking"),
    ARCHIVED("Archived", "Itinerary is no longer in use");
}
```

### ParkEntryType
```java
public enum ParkEntryType {
    TRANSIT("Transit", "Passing through without overnight"),
    DAY_TRIP("Day Trip", "Full day visit without overnight"),
    SLEEP_OVER("Sleep Over", "Overnight stay in the park");
}
```

---

## Phase 4: Repositories

Create 8 repositories extending `JpaRepository` and `JpaSpecificationExecutor`:

1. **ItineraryRepository** - findBySlug, existsByNameIgnoreCase
2. **ItineraryDayRepository** - findByItineraryIdOrderByDayNumber
3. **ItineraryPaxRepository** - findByItineraryId
4. **ItineraryDayActivityRepository** - findByItineraryDayId
5. **ItineraryDayParkRepository** - findByItineraryDayId
6. **ItineraryDayParkActivityRepository** - findByItineraryDayParkId
7. **ItineraryDayParkTariffRepository** - findByItineraryDayParkId
8. **ItineraryDayAccommodationRepository** - findByItineraryDayId

---

## Phase 5: Services

### Itinerary Services
| Service | Methods |
|---------|---------|
| CreateItineraryService | createItinerary(CreateItineraryDTO) |
| GetItineraryService | getById, getBySlug, getAll (with spec), getFullItinerary |
| UpdateItineraryService | updateItinerary(id, UpdateItineraryDTO) |
| DeleteItineraryService | deleteItineraries(List<String> ids) |
| ItineraryStatusService | evaluateStatus, publishItinerary, archiveItinerary |
| CloneItineraryService | cloneItinerary(id, newName) |

### ItineraryDay Services
| Service | Methods |
|---------|---------|
| CreateItineraryDayService | createDay(itineraryId, CreateItineraryDayDTO) |
| GetItineraryDayService | getDays(itineraryId), getDay(itineraryId, dayId) |
| UpdateItineraryDayService | updateDay(itineraryId, dayId, UpdateItineraryDayDTO) |
| DeleteItineraryDayService | deleteDay(itineraryId, dayId) |
| ReorderItineraryDaysService | reorderDays(itineraryId, List<ReorderDayDTO>) |

### ItineraryPax Services
| Service | Methods |
|---------|---------|
| UpsertItineraryPaxService | upsertPax(itineraryId, List<UpsertItineraryPaxDTO>) |
| GetItineraryPaxService | getPax(itineraryId) |
| DeleteItineraryPaxService | deletePax(itineraryId, List<String> paxIds) |

---

## Phase 6: Controllers

### ItineraryController (`/api/itineraries`)
| Method | Endpoint | Permission |
|--------|----------|------------|
| POST | `/` | PERM_CREATE_ITINERARY |
| GET | `/{id}` | PERM_READ_ITINERARY |
| GET | `/slug/{slug}` | PERM_READ_ITINERARY |
| GET | `/` | PERM_READ_ITINERARY |
| PUT | `/{id}` | PERM_UPDATE_ITINERARY |
| DELETE | `/` | PERM_DELETE_ITINERARY |
| POST | `/{id}/publish` | PERM_PUBLISH_ITINERARY |
| POST | `/{id}/archive` | PERM_UPDATE_ITINERARY |
| GET | `/{id}/full` | PERM_READ_FULL_ITINERARY |
| POST | `/{id}/clone` | PERM_CLONE_ITINERARY |

### ItineraryDayController (`/api/itineraries/{itineraryId}/days`)
| Method | Endpoint | Permission |
|--------|----------|------------|
| POST | `/` | PERM_CREATE_ITINERARY_DAY |
| GET | `/` | PERM_READ_ITINERARY_DAY |
| GET | `/{dayId}` | PERM_READ_ITINERARY_DAY |
| PUT | `/{dayId}` | PERM_UPDATE_ITINERARY_DAY |
| DELETE | `/{dayId}` | PERM_DELETE_ITINERARY_DAY |
| POST | `/reorder` | PERM_UPDATE_ITINERARY_DAY |

### ItineraryPaxController (`/api/itineraries/{itineraryId}/pax`)
| Method | Endpoint | Permission |
|--------|----------|------------|
| POST | `/bulk` | PERM_UPDATE_ITINERARY_PAX |
| GET | `/` | PERM_READ_ITINERARY_PAX |
| DELETE | `/` | PERM_DELETE_ITINERARY_PAX |

### ItineraryDayParkController (`/api/itineraries/{itineraryId}/days/{dayId}/parks`)
| Method | Endpoint | Permission |
|--------|----------|------------|
| POST | `/` | PERM_CREATE_ITINERARY_DAY_PARK |
| GET | `/` | PERM_READ_ITINERARY_DAY_PARK |
| PUT | `/{parkVisitId}` | PERM_UPDATE_ITINERARY_DAY_PARK |
| DELETE | `/{parkVisitId}` | PERM_DELETE_ITINERARY_DAY_PARK |
| POST | `/{parkVisitId}/activities` | PERM_UPDATE_ITINERARY_DAY_PARK |
| POST | `/{parkVisitId}/tariffs` | PERM_UPDATE_ITINERARY_DAY_PARK |

---

## Phase 7: Permission Initialization

Update `PermissionInitializer.java` to add:

**Entity Permissions:**
```java
"ITINERARY",
"ITINERARY_DAY",
"ITINERARY_PAX",
"ITINERARY_DAY_PARK",
"ITINERARY_DAY_ACCOMMODATION"
```

**Custom Permissions:**
```java
{"READ_FULL_ITINERARY", "READ", "ITINERARY", "View complete itinerary with all nested data"},
{"CLONE_ITINERARY", "CREATE", "ITINERARY", "Duplicate an existing itinerary as a new template"},
{"PUBLISH_ITINERARY", "UPDATE", "ITINERARY", "Publish an itinerary for booking"}
```

---

## Status Evaluation Logic

An itinerary is **COMPLETE** when:
1. `totalDays >= 1`
2. `days.size() == totalDays`
3. `paxList` is not empty
4. Each day has a `title`
5. Each overnight day has at least one accommodation

---

## Critical Reference Files

| File | Purpose |
|------|---------|
| [Park.java](src/main/java/com/itineraryledger/kabengosafaris/Park/Park.java) | Entity pattern, relationships |
| [ParkTariffRate.java](src/main/java/com/itineraryledger/kabengosafaris/ParkTariffRate/ParkTariffRate.java) | @JoinColumns composite key |
| [ParkCreateService.java](src/main/java/com/itineraryledger/kabengosafaris/Park/Services/ParkCreateService.java) | Service pattern |
| [ParkController.java](src/main/java/com/itineraryledger/kabengosafaris/Park/ParkController.java) | Controller pattern |
| [PermissionInitializer.java](src/main/java/com/itineraryledger/kabengosafaris/Initializers/PermissionInitializer.java) | Permission setup |

---

## Implementation Order

1. **Entities** (8 files) → Itinerary, ItineraryDay, ItineraryPax, then nested entities
2. **Repositories** (8 files)
3. **DTOs** (~20 files) - Create/Update/Response for each entity
4. **Services** (~15 files) - CRUD services for each entity
5. **Controllers** (5 files)
6. **Permissions** - Update PermissionInitializer

---

## Verification

1. **Build:** `mvn clean compile` - no compilation errors
2. **Database:** Verify tables created with correct constraints
3. **API Testing:**
   - Create itinerary → verify DRAFT status
   - Add days, pax, parks, accommodations
   - Verify status transitions to COMPLETE
   - Test publish endpoint
   - Test clone functionality
   - Verify cascade deletes
4. **Permission Testing:** Verify @PreAuthorize works correctly
