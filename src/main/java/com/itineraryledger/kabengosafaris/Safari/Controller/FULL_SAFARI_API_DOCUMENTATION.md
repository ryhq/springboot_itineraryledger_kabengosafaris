# Full Safari API Documentation

## Overview

This API endpoint retrieves a complete safari with all its nested data in a single request. It provides a comprehensive view of the entire safari structure including days, activities, parks, tariffs, accommodations, passenger configurations, and real-time tracking data for actual times, completion status, and operational notes.

## Endpoint

```
GET /api/safaris/{id}/full
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | The obfuscated safari ID |

### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | Bearer {token} | Yes |
| `Content-Type` | application/json | Yes |

### Required Permission

```
PERM_READ_SAFARI
```

## Response Structure

### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Full safari retrieved successfully",
  "data": {
    // Safari base fields
    "id": "abc123xyz",
    "itineraryId": "itinerary789",
    "customerId": "customer456",
    "customerName": "John & Jane Smith",
    "name": "7-Day Serengeti & Ngorongoro Safari - Smith Family",
    "code": "SAF-7D6N-001",
    "slug": "7-day-serengeti-ngorongoro-safari-smith-family",
    "state": "CONFIRMED",
    "stateDisplayName": "Confirmed",
    "stateReason": "Payment received and lodges confirmed",
    "stateChangedAt": "2024-02-10T14:30:00",
    "currentPhase": "UPCOMING",
    "currentPhaseDisplayName": "Upcoming (1-7 days away)",
    "isUrgentPhase": false,
    "startDate": "2024-03-15",
    "endDate": "2024-03-21",
    "totalDays": 7,
    "totalNights": 6,
    "isDayTrip": false,
    "carCount": 1,
    "description": "An unforgettable family safari experience through Tanzania's most iconic wildlife destinations...",
    "highlights": "[\"Big Five\", \"Great Migration\", \"Ngorongoro Crater\", \"Family-friendly lodges\"]",
    "startLocation": "Arusha",
    "endLocation": "Arusha",
    "specialRequests": "Vegetarian meals for 2 guests, early morning game drives preferred",
    "dietaryRequirements": "2 vegetarians, 1 gluten-free",
    "internalNotes": "Regular clients, prefer same guide as last year",
    "emergencyContact": "+1-555-0123 (John Smith)",
    "isActive": true,
    "currentDayNumber": null,
    "daysUntilStart": 30,
    "daysSinceEnd": null,
    "hasStarted": false,
    "hasEnded": false,
    "isInProgress": false,
    "isEditable": true,
    "isCancellable": true,
    "createdByName": "Sarah Johnson",
    "updatedByName": "Michael Brown",
    "createdAt": "2024-02-01T10:30:00",
    "updatedAt": "2024-02-10T14:30:00",

    // Summary statistics
    "totalPaxCount": 4,
    "totalDaysCount": 7,
    "totalParksCount": 3,
    "totalActivitiesCount": 15,
    "totalAccommodationsCount": 6,

    // Pax configurations
    "paxList": [
      {
        "id": "pax123",
        "nationCategoryId": "nat001",
        "nationCategoryName": "Non-Resident",
        "ageCategoryId": "age001",
        "ageCategoryName": "Adult",
        "count": 2,
        "notes": "Parents - John & Jane Smith"
      },
      {
        "id": "pax124",
        "nationCategoryId": "nat001",
        "nationCategoryName": "Non-Resident",
        "ageCategoryId": "age002",
        "ageCategoryName": "Child",
        "count": 2,
        "notes": "Children - Ages 8 and 12"
      }
    ],

    // Days with nested data
    "days": [
      {
        "id": "day001",
        "dayNumber": 1,
        "date": "2024-03-15",
        "dayTag": "Day 1",
        "title": "Arrival in Arusha",
        "description": "Welcome to Tanzania! Your safari adventure begins with a warm reception at the airport...",
        "morningActivities": "Arrival at Kilimanjaro International Airport",
        "afternoonActivities": "Transfer to hotel in Arusha, rest and acclimatize",
        "eveningActivities": "Welcome dinner with safari briefing",
        "wildlifeHighlights": null,
        "scenicHighlights": "Views of Mount Meru from the lodge",
        "specialNotes": "Bring comfortable clothes for the transfer, passport required",
        "startLocation": "Kilimanjaro Airport",
        "endLocation": "Arusha",
        "distanceKm": 45,
        "isOvernight": true,
        "mealsIncluded": "D",
        "actualStartTime": null,
        "actualEndTime": null,
        "weatherConditions": null,
        "dayNotes": null,
        "highlightsOfDay": null,
        "createdAt": "2024-02-01T10:30:00",

        // Standalone activities (not in park)
        "activities": [
          {
            "id": "act001",
            "activityId": "activity123",
            "activityName": "Airport Transfer",
            "activitySlug": "airport-transfer",
            "sortOrder": 1,
            "durationHours": 1.5,
            "startTime": "14:00",
            "endTime": "15:30",
            "notes": "Meet & greet at arrivals, transfer to Arusha Coffee Lodge",
            "isIncludedInPrice": true,
            "isOptional": false,
            "wasCompleted": null,
            "actualStartTime": null,
            "actualEndTime": null,
            "completionNotes": null
          },
          {
            "id": "act002",
            "activityId": "activity456",
            "activityName": "Safari Briefing",
            "activitySlug": "safari-briefing",
            "sortOrder": 2,
            "durationHours": 1.0,
            "startTime": "19:00",
            "endTime": "20:00",
            "notes": "Overview of itinerary, wildlife expectations, safety guidelines",
            "isIncludedInPrice": true,
            "isOptional": false,
            "wasCompleted": null,
            "actualStartTime": null,
            "actualEndTime": null,
            "completionNotes": null
          }
        ],

        // Accommodations
        "accommodations": [
          {
            "id": "acc001",
            "accommodationId": "lodge123",
            "accommodationName": "Arusha Coffee Lodge",
            "accommodationSlug": "arusha-coffee-lodge",
            "roomTypeId": "rt001",
            "roomTypeName": "Double",
            "roomTypeMaxOccupancy": 2,
            "roomTypeMinOccupancy": 1,
            "roomStandardId": "rs001",
            "roomStandardName": "Plantation Suite",
            "boardTypeId": "bt001",
            "boardTypeName": "Bed & Breakfast",
            "roomCount": 2,
            "isAlternative": false,
            "notes": "Confirmed booking, vegetarian breakfast requested for room 102",
            "confirmationNumber": "ACL-2024-0315-001",
            "checkInTime": null,
            "checkOutTime": null,
            "actualRoomNumbers": null,
            "guestFeedback": null
          }
        ],

        // Parks with nested activities and tariffs
        "parks": null
      },
      {
        "id": "day002",
        "dayNumber": 2,
        "date": "2024-03-16",
        "dayTag": "Day 2",
        "title": "Arusha to Serengeti",
        "description": "Journey into the legendary Serengeti plains, one of Africa's most spectacular wildlife destinations...",
        "morningActivities": "Early breakfast and departure at 6:00 AM",
        "afternoonActivities": "Packed lunch en route, game drive upon arrival",
        "eveningActivities": "Sunset game drive, dinner at lodge",
        "wildlifeHighlights": "Lions, elephants, giraffes, zebras, wildebeest",
        "scenicHighlights": "Endless Serengeti plains, kopjes (rock formations)",
        "specialNotes": "Long drive day (6-7 hours) - bring snacks, sunscreen, and cameras",
        "startLocation": "Arusha",
        "endLocation": "Central Serengeti",
        "distanceKm": 325,
        "isOvernight": true,
        "mealsIncluded": "B,L,D",
        "actualStartTime": null,
        "actualEndTime": null,
        "weatherConditions": null,
        "dayNotes": null,
        "highlightsOfDay": null,
        "createdAt": "2024-02-01T10:30:00",

        "activities": null,

        "accommodations": [
          {
            "id": "acc002",
            "accommodationId": "lodge456",
            "accommodationName": "Four Seasons Safari Lodge Serengeti",
            "accommodationSlug": "four-seasons-serengeti",
            "roomTypeId": "rt002",
            "roomTypeName": "Twin",
            "roomTypeMaxOccupancy": 2,
            "roomTypeMinOccupancy": 1,
            "roomStandardId": "rs002",
            "roomStandardName": "Savannah Room",
            "boardTypeId": "bt002",
            "boardTypeName": "Full Board",
            "roomCount": 2,
            "isAlternative": false,
            "notes": "Rooms 205 and 207 - adjoining rooms for family",
            "confirmationNumber": "FSSL-2024-0316-042",
            "checkInTime": null,
            "checkOutTime": null,
            "actualRoomNumbers": null,
            "guestFeedback": null
          }
        ],

        "parks": [
          {
            "id": "park001",
            "parkId": "serengeti123",
            "parkName": "Serengeti National Park",
            "parkSlug": "serengeti-national-park",
            "entryType": "SLEEP_OVER",
            "entryTypeDisplayName": "Sleep Over",
            "sortOrder": 1,
            "arrivalTime": "14:00",
            "departureTime": null,
            "notes": "Entry via Naabi Hill Gate",
            "actualArrivalTime": null,
            "actualDepartureTime": null,
            "entryReceiptNumber": null,
            "wildlifeSightings": null,
            "visitNotes": null,
            "feesPaid": false,
            "feesPaidAt": null,
            "weatherConditions": null,

            // Park-specific activities
            "activities": [
              {
                "id": "pa001",
                "activityId": "gd123",
                "activityName": "Afternoon Game Drive",
                "sortOrder": 1,
                "durationHours": 3.0,
                "startTime": "14:30",
                "endTime": "17:30",
                "notes": "First game drive in Serengeti - focus on Big Five",
                "isIncludedInPrice": true,
                "wasCompleted": null,
                "actualStartTime": null,
                "actualEndTime": null,
                "completionNotes": null
              },
              {
                "id": "pa002",
                "activityId": "gd124",
                "activityName": "Sunset Game Drive",
                "sortOrder": 2,
                "durationHours": 2.0,
                "startTime": "17:30",
                "endTime": "19:30",
                "notes": "Evening game drive - photography opportunities",
                "isIncludedInPrice": true,
                "wasCompleted": null,
                "actualStartTime": null,
                "actualEndTime": null,
                "completionNotes": null
              }
            ],

            // Park tariffs
            "tariffs": [
              {
                "id": "pt001",
                "parkId": "serengeti123",
                "parkName": "Serengeti National Park",
                "tariffId": "tariff123",
                "tariffName": "Park Entry Fee - Adult Non-Resident",
                "notes": "Valid for 24 hours from entry",
                "isIncludedInPrice": true,
                "isPaid": false,
                "paidAt": null,
                "receiptNumber": null,
                "paymentNotes": null,
                "paxCount": 2,
                "isWaived": false,
                "waiverReason": null
              },
              {
                "id": "pt002",
                "parkId": "serengeti123",
                "parkName": "Serengeti National Park",
                "tariffId": "tariff456",
                "tariffName": "Park Entry Fee - Child Non-Resident",
                "notes": "Valid for 24 hours from entry",
                "isIncludedInPrice": true,
                "isPaid": false,
                "paidAt": null,
                "receiptNumber": null,
                "paymentNotes": null,
                "paxCount": 2,
                "isWaived": false,
                "waiverReason": null
              },
              {
                "id": "pt003",
                "parkId": "serengeti123",
                "parkName": "Serengeti National Park",
                "tariffId": "tariff789",
                "tariffName": "Conservation Fee",
                "notes": "18% VAT included",
                "isIncludedInPrice": true,
                "isPaid": false,
                "paidAt": null,
                "receiptNumber": null,
                "paymentNotes": null,
                "paxCount": 4,
                "isWaived": false,
                "waiverReason": null
              }
            ]
          }
        ]
      }
    ]
  },
  "timestamp": "2024-02-15T15:00:00.000Z"
}
```

### Error Responses

#### Invalid ID (400 Bad Request)

```json
{
  "status": 400,
  "message": "Invalid safari ID",
  "errorCode": "INVALID_SAFARI_ID",
  "timestamp": "2024-02-15T15:00:00.000Z"
}
```

#### Not Found (404 Not Found)

```json
{
  "status": 404,
  "message": "Safari not found",
  "errorCode": "SAFARI_NOT_FOUND",
  "timestamp": "2024-02-15T15:00:00.000Z"
}
```

#### Server Error (500 Internal Server Error)

```json
{
  "status": 500,
  "message": "Failed to fetch full safari",
  "errorCode": "FULL_SAFARI_FETCH_FAILED",
  "timestamp": "2024-02-15T15:00:00.000Z"
}
```

## Data Structure Hierarchy

```
FullSafariDTO
├── Safari fields (id, name, code, state, phase, dates, etc.)
├── Summary statistics
│   ├── totalPaxCount
│   ├── totalDaysCount
│   ├── totalParksCount
│   ├── totalActivitiesCount
│   └── totalAccommodationsCount
├── paxList: List<PaxDTO>
│   └── PaxDTO (id, nationCategory, ageCategory, count, notes)
└── days: List<DayDTO>
    └── DayDTO
        ├── Day fields (dayNumber, date, title, description, actual times, etc.)
        ├── activities: List<DayActivityDTO>  [Standalone activities]
        │   └── DayActivityDTO (activity details, completion status, actual times)
        ├── accommodations: List<DayAccommodationDTO>
        │   └── DayAccommodationDTO (accommodation, rooms, confirmation, check-in/out)
        └── parks: List<DayParkDTO>
            └── DayParkDTO
                ├── Park fields (park details, entryType, actual times, fees paid)
                ├── activities: List<ParkActivityDTO>  [Park-specific activities]
                │   └── ParkActivityDTO (activity details, completion status)
                └── tariffs: List<ParkTariffDTO>
                    └── ParkTariffDTO (tariff details, payment status, receipts)
```

## Field Descriptions

### Safari Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated unique identifier |
| `itineraryId` | String | Source itinerary template ID |
| `customerId` | String | Customer/client ID |
| `customerName` | String | Customer display name |
| `name` | String | Safari name (usually includes customer name) |
| `code` | String | Auto-generated safari code (e.g., SAF-7D6N-001) |
| `slug` | String | URL-friendly slug |
| `state` | Enum | DRAFT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, etc. |
| `stateDisplayName` | String | Human-readable state |
| `stateReason` | String | Reason for current state |
| `stateChangedAt` | DateTime | When state was last changed |
| `currentPhase` | Enum | FAR_FUTURE, UPCOMING, STARTING_SOON, IN_PROGRESS, etc. |
| `currentPhaseDisplayName` | String | Human-readable phase |
| `isUrgentPhase` | Boolean | True for STARTING_SOON or IN_PROGRESS phases |
| `startDate` | Date | Safari start date |
| `endDate` | Date | Safari end date |
| `totalDays` | Integer | Total number of days |
| `totalNights` | Integer | Total number of nights |
| `isDayTrip` | Boolean | True if single-day trip |
| `carCount` | Integer | Number of vehicles |
| `description` | String | Full safari description |
| `highlights` | String | JSON array of highlights |
| `startLocation` | String | Starting point |
| `endLocation` | String | Ending point |
| `specialRequests` | String | Customer special requests |
| `dietaryRequirements` | String | Dietary needs |
| `internalNotes` | String | Staff-only notes |
| `emergencyContact` | String | Emergency contact info |
| `isActive` | Boolean | Active status |
| `currentDayNumber` | Integer | Current day number (if in progress) |
| `daysUntilStart` | Long | Days remaining until start |
| `daysSinceEnd` | Long | Days since completion |
| `hasStarted` | Boolean | True if safari has begun |
| `hasEnded` | Boolean | True if safari is complete |
| `isInProgress` | Boolean | True if currently ongoing |
| `isEditable` | Boolean | True if can be edited |
| `isCancellable` | Boolean | True if can be cancelled |
| `createdByName` | String | User who created the safari |
| `updatedByName` | String | User who last updated |

### SafariState Values

| State | Description | Typical Usage |
|-------|-------------|---------------|
| `DRAFT` | Initial creation | Safari being planned, not yet finalized |
| `PENDING_CONFIRMATION` | Awaiting confirmation | Customer reviewing, lodge bookings pending |
| `CONFIRMED` | Fully confirmed | All bookings confirmed, ready to go |
| `PAYMENT_PENDING` | Awaiting payment | Confirmed but payment not received |
| `PAID` | Payment received | Full payment completed |
| `IN_PROGRESS` | Safari ongoing | Currently happening |
| `COMPLETED` | Safari finished | Successfully completed |
| `CANCELLED` | Cancelled | Booking cancelled |
| `POSTPONED` | Postponed | Rescheduled to future date |
| `NO_SHOW` | Customer no-show | Customer didn't arrive |

### SafariPhase Values

| Phase | Description | Days Calculation |
|-------|-------------|------------------|
| `FAR_FUTURE` | More than 30 days away | daysUntilStart > 30 |
| `UPCOMING` | 8-30 days away | 8 ≤ daysUntilStart ≤ 30 |
| `STARTING_SOON` | 1-7 days away | 1 ≤ daysUntilStart ≤ 7 |
| `IN_PROGRESS` | Currently happening | startDate ≤ today ≤ endDate |
| `RECENTLY_COMPLETED` | Finished within 7 days | 0 ≤ daysSinceEnd ≤ 7 |
| `COMPLETED` | Finished over 7 days ago | daysSinceEnd > 7 |

### PaxDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated pax entry ID |
| `nationCategoryId` | String | Nationality category ID |
| `nationCategoryName` | String | e.g., "Resident", "Non-Resident", "East African" |
| `ageCategoryId` | String | Age category ID |
| `ageCategoryName` | String | e.g., "Adult", "Child", "Infant" |
| `count` | Integer | Number of passengers in this category |
| `notes` | String | Additional notes about passengers |

### DayDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated day ID |
| `dayNumber` | Integer | Sequential day number |
| `date` | Date | Actual date of this safari day |
| `dayTag` | String | Display tag (e.g., "Day 1") |
| `title` | String | Day title |
| `description` | String | Main day description |
| `morningActivities` | String | Morning activities text |
| `afternoonActivities` | String | Afternoon activities text |
| `eveningActivities` | String | Evening activities text |
| `wildlifeHighlights` | String | Expected wildlife |
| `scenicHighlights` | String | Scenic views |
| `specialNotes` | String | Tips and important notes |
| `startLocation` | String | Day start location |
| `endLocation` | String | Day end location |
| `distanceKm` | Integer | Driving distance |
| `isOvernight` | Boolean | Has overnight accommodation |
| `mealsIncluded` | String | Included meals (B,L,D) |
| `actualStartTime` | String | Actual start time (recorded during safari) |
| `actualEndTime` | String | Actual end time (recorded during safari) |
| `weatherConditions` | String | Weather notes |
| `dayNotes` | String | Guide's notes for the day |
| `highlightsOfDay` | String | Actual highlights experienced |

### DayActivityDTO Fields (Standalone Activities)

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated activity ID |
| `activityId` | String | Reference activity ID |
| `activityName` | String | Activity name |
| `activitySlug` | String | URL-friendly slug |
| `sortOrder` | Integer | Display order |
| `durationHours` | Decimal | Estimated duration in hours |
| `startTime` | String | Planned start time (HH:mm) |
| `endTime` | String | Planned end time (HH:mm) |
| `notes` | String | Activity notes |
| `isIncludedInPrice` | Boolean | Included in package price |
| `isOptional` | Boolean | Optional activity |
| `wasCompleted` | Boolean | True if completed during safari |
| `actualStartTime` | String | Actual start time |
| `actualEndTime` | String | Actual end time |
| `completionNotes` | String | Notes recorded after completion |

### DayAccommodationDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated accommodation entry ID |
| `accommodationId` | String | Accommodation facility ID |
| `accommodationName` | String | Lodge/hotel name |
| `accommodationSlug` | String | URL-friendly slug |
| `roomTypeId` | String | Room type ID |
| `roomTypeName` | String | e.g., "Double", "Twin", "Family Suite" |
| `roomTypeMaxOccupancy` | Integer | Maximum guests per room |
| `roomTypeMinOccupancy` | Integer | Minimum guests per room |
| `roomStandardId` | String | Room standard/category ID |
| `roomStandardName` | String | e.g., "Deluxe Room", "Suite" |
| `boardTypeId` | String | Meal plan ID |
| `boardTypeName` | String | e.g., "Full Board", "Half Board" |
| `roomCount` | Integer | Number of rooms booked |
| `isAlternative` | Boolean | Alternative option |
| `notes` | String | Booking notes |
| `confirmationNumber` | String | Booking confirmation number |
| `checkInTime` | DateTime | Actual check-in time |
| `checkOutTime` | DateTime | Actual check-out time |
| `actualRoomNumbers` | String | Assigned room numbers |
| `guestFeedback` | String | Guest feedback/comments |

### DayParkDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated park visit ID |
| `parkId` | String | Park ID |
| `parkName` | String | Park name |
| `parkSlug` | String | URL-friendly park slug |
| `entryType` | Enum | TRANSIT, DAY_TRIP, SLEEP_OVER |
| `entryTypeDisplayName` | String | Human-readable entry type |
| `sortOrder` | Integer | Display order (for multi-park days) |
| `arrivalTime` | String | Planned arrival time |
| `departureTime` | String | Planned departure time |
| `notes` | String | Visit notes |
| `actualArrivalTime` | String | Actual arrival time |
| `actualDepartureTime` | String | Actual departure time |
| `entryReceiptNumber` | String | Park entry receipt number |
| `wildlifeSightings` | String | Wildlife spotted during visit |
| `visitNotes` | String | Guide's notes about the visit |
| `feesPaid` | Boolean | True if park fees paid |
| `feesPaidAt` | DateTime | When fees were paid |
| `weatherConditions` | String | Weather during park visit |

### ParkActivityDTO Fields (Park-Specific Activities)

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated park activity ID |
| `activityId` | String | Reference activity ID |
| `activityName` | String | Activity name |
| `sortOrder` | Integer | Display order |
| `durationHours` | Decimal | Duration in hours |
| `startTime` | String | Planned start time |
| `endTime` | String | Planned end time |
| `notes` | String | Activity notes |
| `isIncludedInPrice` | Boolean | Included in package |
| `wasCompleted` | Boolean | Completion status |
| `actualStartTime` | String | Actual start time |
| `actualEndTime` | String | Actual end time |
| `completionNotes` | String | Post-activity notes |

### ParkTariffDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated tariff entry ID |
| `parkId` | String | Park ID |
| `parkName` | String | Park name |
| `tariffId` | String | Tariff/fee ID |
| `tariffName` | String | Fee name |
| `notes` | String | Tariff notes |
| `isIncludedInPrice` | Boolean | Included in package |
| `isPaid` | Boolean | Payment status |
| `paidAt` | DateTime | Payment timestamp |
| `receiptNumber` | String | Payment receipt number |
| `paymentNotes` | String | Payment-related notes |
| `paxCount` | Integer | Number of passengers for this fee |
| `isWaived` | Boolean | True if fee was waived |
| `waiverReason` | String | Reason for waiver |

## Usage Examples

### cURL

```bash
curl -X GET \
  'https://api.kabengosafaris.com/api/safaris/abc123xyz/full' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json'
```

### JavaScript (Fetch)

```javascript
const response = await fetch('/api/safaris/abc123xyz/full', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log(data.data.name); // "7-Day Serengeti & Ngorongoro Safari - Smith Family"
console.log(data.data.state); // "CONFIRMED"
console.log(data.data.currentPhase); // "UPCOMING"
console.log(data.data.days.length); // 7
console.log(data.data.totalPaxCount); // 4
console.log(data.data.daysUntilStart); // 30
```

### Python (Requests)

```python
import requests

url = "https://api.kabengosafaris.com/api/safaris/abc123xyz/full"
headers = {
    "Authorization": f"Bearer {access_token}",
    "Content-Type": "application/json"
}

response = requests.get(url, headers=headers)
safari = response.json()["data"]

print(f"Safari: {safari['name']}")
print(f"State: {safari['stateDisplayName']}")
print(f"Phase: {safari['currentPhaseDisplayName']}")
print(f"Days until start: {safari['daysUntilStart']}")
print(f"Total days: {safari['totalDaysCount']}")
print(f"Total parks: {safari['totalParksCount']}")
```

## Use Cases

### 1. Safari Dashboard Display

Display comprehensive safari information on the main dashboard:

```javascript
// Fetch full safari data
const safari = await getFullSafari(safariId);

// Display header
renderSafariHeader({
  name: safari.name,
  code: safari.code,
  state: safari.stateDisplayName,
  phase: safari.currentPhaseDisplayName,
  daysUntilStart: safari.daysUntilStart
});

// Display summary
renderSummary({
  totalDays: safari.totalDaysCount,
  totalParks: safari.totalParksCount,
  totalPax: safari.totalPaxCount
});

// Display itinerary
renderItinerary(safari.days);
```

### 2. Operational Tracking

Track safari progress and update actual times:

```javascript
const safari = await getFullSafari(safariId);

if (safari.isInProgress) {
  const currentDay = safari.days.find(d => d.dayNumber === safari.currentDayNumber);

  // Display current day activities
  renderCurrentDayActivities(currentDay.activities);

  // Check park fees payment status
  currentDay.parks?.forEach(park => {
    if (!park.feesPaid) {
      alertParkFeesUnpaid(park.parkName);
    }
  });
}
```

### 3. Customer Portal

Display safari details to customers:

```javascript
const safari = await getFullSafari(safariId);

// Show safari overview
renderOverview({
  name: safari.name,
  dates: `${safari.startDate} to ${safari.endDate}`,
  description: safari.description,
  highlights: JSON.parse(safari.highlights)
});

// Show daily itinerary
safari.days.forEach(day => {
  renderDayCard({
    dayNumber: day.dayNumber,
    date: day.date,
    title: day.title,
    description: day.description,
    accommodations: day.accommodations,
    parks: day.parks
  });
});
```

### 4. Guide Mobile App

Mobile app for safari guides to update progress:

```javascript
const safari = await getFullSafari(safariId);
const currentDay = safari.days.find(d => d.dayNumber === safari.currentDayNumber);

// Record activity completion
async function completeActivity(activityId) {
  await updateActivity(activityId, {
    wasCompleted: true,
    actualStartTime: recordedStartTime,
    actualEndTime: new Date().toISOString(),
    completionNotes: notes
  });
}

// Record wildlife sightings
async function recordSightings(parkId, sightings) {
  await updateParkVisit(parkId, {
    wildlifeSightings: sightings
  });
}
```

### 5. Financial Tracking

Track payment status for park fees:

```javascript
const safari = await getFullSafari(safariId);

// Calculate total unpaid fees
let unpaidTariffs = [];
safari.days.forEach(day => {
  day.parks?.forEach(park => {
    park.tariffs?.forEach(tariff => {
      if (!tariff.isPaid && !tariff.isWaived) {
        unpaidTariffs.push({
          day: day.dayNumber,
          park: park.parkName,
          tariff: tariff.tariffName,
          paxCount: tariff.paxCount
        });
      }
    });
  });
});

renderUnpaidFeesReport(unpaidTariffs);
```

## Notes

- All null fields are excluded from the response (using `@JsonInclude(JsonInclude.Include.NON_NULL)`)
- Days are ordered by `dayNumber` (ascending) and include actual `date` values
- Activities within days and parks are ordered by `sortOrder` (ascending)
- Parks within days are ordered by `sortOrder` (ascending)
- Safari-specific fields track actual vs. planned times for operational tracking
- Completion status fields (`wasCompleted`, `isPaid`, etc.) enable progress tracking
- The `currentPhase` field is automatically calculated based on dates
- The `isUrgentPhase` flag indicates safaris requiring immediate attention
- This endpoint returns all data in a single request - ideal for displaying complete safari details
- For listing safaris without nested data, use `GET /api/safaris` instead
- Safari state can be managed via `/api/safaris/{id}/state/*` endpoints
- Real-time updates (actual times, completion notes, wildlife sightings) can be recorded via update endpoints

## Related Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/safaris` | List safaris with filtering (lightweight) |
| `GET /api/safaris/{id}` | Get single safari (basic fields only) |
| `POST /api/safaris` | Create safari from itinerary |
| `PUT /api/safaris/{id}` | Update safari basic fields |
| `POST /api/safaris/{id}/state/confirm` | Confirm safari booking |
| `POST /api/safaris/{id}/state/cancel` | Cancel safari |
| `GET /api/safari-documents` | Get safari documents |
| `POST /api/safari-documents/upload` | Upload safari documents |

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-02-15 | Initial documentation |

---

**API Version:** 1.0
**Last Updated:** 2024-02-15
