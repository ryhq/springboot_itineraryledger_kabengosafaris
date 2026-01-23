# Full Itinerary API Documentation

## Overview

This API endpoint retrieves a complete itinerary with all its nested data in a single request. It provides a comprehensive view of the entire itinerary structure including days, activities, parks, tariffs, accommodations, and passenger configurations.

## Endpoint

```
GET /api/itineraries/{id}/full
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | The obfuscated itinerary ID |

### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | Bearer {token} | Yes |
| `Content-Type` | application/json | Yes |

### Required Permission

```
PERM_READ_ITINERARY
```

## Response Structure

### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Full itinerary retrieved successfully",
  "data": {
    // Itinerary base fields
    "id": "abc123xyz",
    "name": "7-Day Serengeti & Ngorongoro Safari",
    "code": "ITI-7D6N-001",
    "status": "PUBLISHED",
    "statusDisplayName": "Published",
    "tripType": "PRIVATE",
    "tripTypeDisplayName": "Private Safari",
    "tripTypeDescription": "Exclusive private tour with dedicated guide",
    "budgetCategory": "LUXURY",
    "budgetCategoryDisplayName": "Luxury",
    "budgetCategoryDescription": "Premium lodges and exclusive experiences",
    "budgetCategoryTier": 5,
    "totalDays": 7,
    "totalNights": 6,
    "isDayTrip": false,
    "carCount": 1,
    "description": "An unforgettable safari experience...",
    "highlights": "[\"Big Five\", \"Great Migration\", \"Ngorongoro Crater\"]",
    "startLocation": "Arusha",
    "endLocation": "Arusha",
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-20T14:45:00",

    // Summary statistics
    "totalPaxCount": 4,
    "totalDaysCount": 7,
    "totalParksCount": 3,
    "totalActivitiesCount": 12,
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
        "notes": "Couple celebrating anniversary"
      },
      {
        "id": "pax124",
        "nationCategoryId": "nat001",
        "nationCategoryName": "Non-Resident",
        "ageCategoryId": "age002",
        "ageCategoryName": "Child",
        "count": 2,
        "notes": "Ages 8 and 12"
      }
    ],

    // Days with nested data
    "days": [
      {
        "id": "day001",
        "dayNumber": 1,
        "dayTag": "Day 1",
        "title": "Arrival in Arusha",
        "description": "Welcome to Tanzania! Your safari adventure begins...",
        "morningActivities": "Arrival at Kilimanjaro International Airport",
        "afternoonActivities": "Transfer to hotel, rest and acclimatize",
        "eveningActivities": "Welcome dinner with trip briefing",
        "wildlifeHighlights": null,
        "scenicHighlights": "Views of Mount Meru",
        "specialNotes": "Bring comfortable clothes for the transfer",
        "startLocation": "Kilimanjaro Airport",
        "endLocation": "Arusha",
        "distanceKm": 45,
        "isOvernight": true,
        "mealsIncluded": "D",
        "createdAt": "2024-01-15T10:30:00",

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
            "notes": "Meet & greet at arrivals",
            "isIncludedInPrice": true,
            "isOptional": false
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
            "notes": "Confirmed booking"
          }
        ],

        // Parks with nested activities and tariffs
        "parks": null
      },
      {
        "id": "day002",
        "dayNumber": 2,
        "dayTag": "Day 2",
        "title": "Arusha to Serengeti",
        "description": "Journey into the legendary Serengeti plains...",
        "morningActivities": "Early breakfast and departure",
        "afternoonActivities": "Game drive en route to camp",
        "eveningActivities": "Sunset game drive",
        "wildlifeHighlights": "Lions, elephants, giraffes",
        "scenicHighlights": "Endless Serengeti plains",
        "specialNotes": "Long drive day - bring snacks",
        "startLocation": "Arusha",
        "endLocation": "Central Serengeti",
        "distanceKm": 325,
        "isOvernight": true,
        "mealsIncluded": "B,L,D",
        "createdAt": "2024-01-15T10:30:00",

        "activities": null,

        "accommodations": [
          {
            "id": "acc002",
            "accommodationId": "lodge456",
            "accommodationName": "Four Seasons Safari Lodge",
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
            "notes": null
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
            "notes": "Main gate entry",

            // Park-specific activities
            "activities": [
              {
                "id": "pa001",
                "activityId": "gd123",
                "activityName": "Game Drive",
                "sortOrder": 1,
                "durationHours": 3.0,
                "startTime": "16:00",
                "endTime": "19:00",
                "notes": "Sunset game drive",
                "isIncludedInPrice": true
              }
            ],

            // Park tariffs
            "tariffs": [
              {
                "id": "pt001",
                "tariffId": "tariff123",
                "tariffName": "Park Entry Fee",
                "notes": null,
                "isIncludedInPrice": true
              },
              {
                "id": "pt002",
                "tariffId": "tariff456",
                "tariffName": "Conservation Fee",
                "notes": null,
                "isIncludedInPrice": true
              }
            ]
          }
        ]
      }
    ]
  },
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

### Error Responses

#### Invalid ID (400 Bad Request)

```json
{
  "status": 400,
  "message": "Invalid itinerary ID",
  "errorCode": "INVALID_ITINERARY_ID",
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

#### Not Found (404 Not Found)

```json
{
  "status": 404,
  "message": "Itinerary not found",
  "errorCode": "ITINERARY_NOT_FOUND",
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

#### Server Error (500 Internal Server Error)

```json
{
  "status": 500,
  "message": "Failed to fetch full itinerary",
  "errorCode": "FULL_ITINERARY_FETCH_FAILED",
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

## Data Structure Hierarchy

```
FullItineraryDTO
├── Itinerary fields (id, name, code, status, etc.)
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
        ├── Day fields (dayNumber, title, description, etc.)
        ├── activities: List<DayActivityDTO>  [Standalone activities]
        │   └── DayActivityDTO (activity details, sortOrder, times, etc.)
        ├── accommodations: List<DayAccommodationDTO>
        │   └── DayAccommodationDTO (accommodation, room config, etc.)
        └── parks: List<DayParkDTO>
            └── DayParkDTO
                ├── Park fields (park details, entryType, times)
                ├── activities: List<ParkActivityDTO>  [Park-specific activities]
                │   └── ParkActivityDTO (activity details within park)
                └── tariffs: List<ParkTariffDTO>
                    └── ParkTariffDTO (tariff details for park)
```

## Field Descriptions

### Itinerary Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated unique identifier |
| `name` | String | Itinerary name |
| `code` | String | Auto-generated itinerary code (e.g., ITI-7D6N-001) |
| `status` | Enum | DRAFT, COMPLETE, PUBLISHED, ARCHIVED |
| `statusDisplayName` | String | Human-readable status |
| `tripType` | Enum | PRIVATE, GROUP, CUSTOM, etc. |
| `budgetCategory` | Enum | LUXURY, MID_RANGE, BUDGET, etc. |
| `totalDays` | Integer | Total number of days |
| `totalNights` | Integer | Total number of nights |
| `isDayTrip` | Boolean | True if single-day trip |
| `carCount` | Integer | Number of vehicles needed |
| `description` | String | Full description |
| `highlights` | String | JSON array of highlights |
| `startLocation` | String | Starting point |
| `endLocation` | String | Ending point |
| `isActive` | Boolean | Active status |

### PaxDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated pax entry ID |
| `nationCategoryId` | String | Nationality category ID |
| `nationCategoryName` | String | e.g., "Resident", "Non-Resident" |
| `ageCategoryId` | String | Age category ID |
| `ageCategoryName` | String | e.g., "Adult", "Child", "Infant" |
| `count` | Integer | Number of passengers in this category |
| `notes` | String | Additional notes |

### DayDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated day ID |
| `dayNumber` | Integer | Sequential day number |
| `dayTag` | String | Display tag (e.g., "Day 1") |
| `title` | String | Day title |
| `description` | String | Main day description |
| `morningActivities` | String | Morning activities text |
| `afternoonActivities` | String | Afternoon activities text |
| `eveningActivities` | String | Evening activities text |
| `wildlifeHighlights` | String | Expected wildlife |
| `scenicHighlights` | String | Scenic views |
| `specialNotes` | String | Tips and notes |
| `startLocation` | String | Day start location |
| `endLocation` | String | Day end location |
| `distanceKm` | Integer | Driving distance |
| `isOvernight` | Boolean | Has overnight stay |
| `mealsIncluded` | String | Included meals (B,L,D) |

### DayParkDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated park visit ID |
| `parkId` | String | Park ID |
| `parkName` | String | Park name |
| `parkSlug` | String | URL-friendly park slug |
| `entryType` | Enum | TRANSIT, DAY_TRIP, SLEEP_OVER |
| `entryTypeDisplayName` | String | Human-readable entry type |
| `sortOrder` | Integer | Display order |
| `arrivalTime` | String | Expected arrival time |
| `departureTime` | String | Expected departure time |
| `notes` | String | Additional notes |

## Usage Examples

### cURL

```bash
curl -X GET \
  'https://api.kabengosafaris.com/api/itineraries/abc123xyz/full' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json'
```

### JavaScript (Fetch)

```javascript
const response = await fetch('/api/itineraries/abc123xyz/full', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log(data.data.name); // "7-Day Serengeti & Ngorongoro Safari"
console.log(data.data.days.length); // 7
console.log(data.data.totalPaxCount); // 4
```

## Notes

- All null fields are excluded from the response (using `@JsonInclude(JsonInclude.Include.NON_NULL)`)
- Days are ordered by `dayNumber` (ascending)
- Activities within days and parks are ordered by `sortOrder` (ascending)
- Parks within days are ordered by `sortOrder` (ascending)
- This endpoint returns all data in a single request - ideal for displaying full itinerary details
- For listing itineraries without nested data, use `GET /api/itineraries` instead
