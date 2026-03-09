# Accommodation Board Type API Documentation

This document provides detailed information about the Accommodation Board Type API endpoints, including request/response formats and examples.

## Base URL
```
/api/accommodation-board-types
```

## Authentication
All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_ACCOMMODATION_BOARD_TYPE` - Create board types
- `PERM_READ_ACCOMMODATION_BOARD_TYPE` - Read board types
- `PERM_UPDATE_ACCOMMODATION_BOARD_TYPE` - Update board types
- `PERM_DELETE_ACCOMMODATION_BOARD_TYPE` - Delete board types

---

## Endpoints

### 1. Create Accommodation Board Type

**POST** `/api/accommodation-board-types`

Creates a new board type (meal plan) for an accommodation.

**Required Permission:** `PERM_CREATE_ACCOMMODATION_BOARD_TYPE`

**Request Body:**
```json
{
  "accommodationId": "encoded_accommodation_id",
  "name": "Full Board",
  "description": "Includes all three meals plus snacks",
  "mealsIncluded": "Breakfast, Lunch, Dinner",
  "breakfastIncluded": true,
  "lunchIncluded": true,
  "dinnerIncluded": true,
  "snacksIncluded": true,
  "drinksIncluded": true,
  "alcoholicDrinksIncluded": false,
  "inclusions": "All meals, soft drinks, tea/coffee",
  "exclusions": "Alcoholic beverages, special dietary meals",
  "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm",
  "isActive": true
}
```

**Success Response (201 Created):**
```json
{
  "status": 201,
  "message": "Accommodation board type created successfully",
  "data": {
    "id": "encoded_board_type_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Full Board",
    "description": "Includes all three meals plus snacks",
    "mealsIncluded": "Breakfast, Lunch, Dinner",
    "breakfastIncluded": true,
    "lunchIncluded": true,
    "dinnerIncluded": true,
    "snacksIncluded": true,
    "drinksIncluded": true,
    "alcoholicDrinksIncluded": false,
    "inclusions": "All meals, soft drinks, tea/coffee",
    "exclusions": "Alcoholic beverages, special dietary meals",
    "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm",
    "isActive": true,
    "mealCount": 3,
    "isFullMealPlan": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:00:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID or duplicate board type name
- `404 Not Found` - Accommodation not found
- `500 Internal Server Error` - Server error

---

### 2. Update Accommodation Board Type

**PUT** `/api/accommodation-board-types/{id}`

Updates an existing board type.

**Required Permission:** `PERM_UPDATE_ACCOMMODATION_BOARD_TYPE`

**Path Parameters:**
- `id` - Encoded board type ID

**Request Body:**
```json
{
  "name": "Half Board Plus",
  "description": "Breakfast and dinner with afternoon snacks",
  "mealsIncluded": "Breakfast, Dinner, Snacks",
  "breakfastIncluded": true,
  "lunchIncluded": false,
  "dinnerIncluded": true,
  "snacksIncluded": true,
  "drinksIncluded": true,
  "alcoholicDrinksIncluded": false,
  "inclusions": "Breakfast, dinner, afternoon tea/snacks",
  "exclusions": "Lunch, alcoholic drinks",
  "mealTimes": "Breakfast: 7-10am, Afternoon Tea: 4-5pm, Dinner: 7-10pm",
  "isActive": true
}
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Accommodation board type updated successfully",
  "data": {
    "id": "encoded_board_type_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Half Board Plus",
    "description": "Breakfast and dinner with afternoon snacks",
    "mealsIncluded": "Breakfast, Dinner, Snacks",
    "breakfastIncluded": true,
    "lunchIncluded": false,
    "dinnerIncluded": true,
    "snacksIncluded": true,
    "drinksIncluded": true,
    "alcoholicDrinksIncluded": false,
    "inclusions": "Breakfast, dinner, afternoon tea/snacks",
    "exclusions": "Lunch, alcoholic drinks",
    "mealTimes": "Breakfast: 7-10am, Afternoon Tea: 4-5pm, Dinner: 7-10pm",
    "isActive": true,
    "mealCount": 2,
    "isFullMealPlan": false,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:30:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid board type ID or duplicate name
- `404 Not Found` - Board type not found
- `500 Internal Server Error` - Server error

---

### 3. Delete Accommodation Board Types

**DELETE** `/api/accommodation-board-types`

Deletes one or more board types by their IDs.

**Required Permission:** `PERM_DELETE_ACCOMMODATION_BOARD_TYPE`

**Request Body:**
```json
[
  "encoded_board_type_id_1",
  "encoded_board_type_id_2"
]
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "2 board type(s) deleted successfully",
  "data": null
}
```

**Error Responses:**
- `400 Bad Request` - No IDs provided
- `500 Internal Server Error` - Server error

---

### 4. Get Accommodation Board Type by ID

**GET** `/api/accommodation-board-types/{id}`

Retrieves a single board type by its ID.

**Required Permission:** `PERM_READ_ACCOMMODATION_BOARD_TYPE`

**Path Parameters:**
- `id` - Encoded board type ID

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Board type retrieved successfully",
  "data": {
    "id": "encoded_board_type_id",
    "accommodationId": "encoded_accommodation_id",
    "accommodationName": "Serengeti Safari Lodge",
    "name": "Full Board",
    "description": "Includes all three meals plus snacks",
    "mealsIncluded": "Breakfast, Lunch, Dinner",
    "breakfastIncluded": true,
    "lunchIncluded": true,
    "dinnerIncluded": true,
    "snacksIncluded": true,
    "drinksIncluded": true,
    "alcoholicDrinksIncluded": false,
    "inclusions": "All meals, soft drinks, tea/coffee",
    "exclusions": "Alcoholic beverages, special dietary meals",
    "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm",
    "isActive": true,
    "mealCount": 3,
    "isFullMealPlan": true,
    "createdAt": "2026-01-06T12:00:00",
    "updatedAt": "2026-01-06T12:00:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid board type ID
- `404 Not Found` - Board type not found
- `500 Internal Server Error` - Server error

---

### 5. Get All Accommodation Board Types

**GET** `/api/accommodation-board-types`

Retrieves all board types with optional filters and pagination. Accommodation ID is optional.

**Required Permission:** `PERM_READ_ACCOMMODATION_BOARD_TYPE`

**Query Parameters:**
- `accommodationId` (optional) - Filter by accommodation ID
- `name` (optional) - Filter by name (partial match)
- `breakfastIncluded` (optional) - Filter by breakfast included (true/false)
- `lunchIncluded` (optional) - Filter by lunch included (true/false)
- `dinnerIncluded` (optional) - Filter by dinner included (true/false)
- `drinksIncluded` (optional) - Filter by drinks included (true/false)
- `alcoholicDrinksIncluded` (optional) - Filter by alcoholic drinks included (true/false)
- `isActive` (optional) - Filter by active status (true/false)
- `hasFullMealPlan` (optional) - Filter board types with all meals (true/false)
- `keyword` (optional) - Search keyword across multiple fields
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 10) - Page size
- `sortDirection` (optional, default: desc) - Sort direction (asc/desc)

**Example Request:**
```
GET /api/accommodation-board-types?breakfastIncluded=true&isActive=true&page=0&size=10&sortDirection=desc
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Board types retrieved successfully",
  "data": {
    "boardTypes": [
      {
        "id": "encoded_board_type_id",
        "accommodationId": "encoded_accommodation_id",
        "accommodationName": "Serengeti Safari Lodge",
        "name": "Full Board",
        "description": "Includes all three meals plus snacks",
        "mealsIncluded": "Breakfast, Lunch, Dinner",
        "breakfastIncluded": true,
        "lunchIncluded": true,
        "dinnerIncluded": true,
        "snacksIncluded": true,
        "drinksIncluded": true,
        "alcoholicDrinksIncluded": false,
        "inclusions": "All meals, soft drinks, tea/coffee",
        "exclusions": "Alcoholic beverages",
        "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm",
        "isActive": true,
        "mealCount": 3,
        "isFullMealPlan": true,
        "createdAt": "2026-01-06T12:00:00",
        "updatedAt": "2026-01-06T12:00:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 1,
    "totalPages": 1
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID
- `500 Internal Server Error` - Server error

---

### 6. Get Board Types for Specific Accommodation

**GET** `/api/accommodation-board-types/accommodation/{accommodationId}`

Retrieves all board types for a specific accommodation. Accommodation ID is required.

**Required Permission:** `PERM_READ_ACCOMMODATION_BOARD_TYPE`

**Path Parameters:**
- `accommodationId` - Required accommodation ID

**Query Parameters:**
- `name` (optional) - Filter by name (partial match)
- `breakfastIncluded` (optional) - Filter by breakfast included (true/false)
- `lunchIncluded` (optional) - Filter by lunch included (true/false)
- `dinnerIncluded` (optional) - Filter by dinner included (true/false)
- `drinksIncluded` (optional) - Filter by drinks included (true/false)
- `alcoholicDrinksIncluded` (optional) - Filter by alcoholic drinks included (true/false)
- `isActive` (optional) - Filter by active status (true/false)
- `hasFullMealPlan` (optional) - Filter board types with all meals (true/false)
- `keyword` (optional) - Search keyword
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 10) - Page size
- `sortDirection` (optional, default: desc) - Sort direction (asc/desc)

**Example Request:**
```
GET /api/accommodation-board-types/accommodation/encoded_accommodation_id?isActive=true&page=0&size=10
```

**Success Response (200 OK):**
Same structure as "Get All Accommodation Board Types" response.

**Error Responses:**
- `400 Bad Request` - Invalid accommodation ID
- `500 Internal Server Error` - Server error

---

### 7. Get Unique Board Types by Meal Configuration

**GET** `/api/accommodation-board-types/unique`

Retrieves unique board types based on meal configuration. Returns one board type per unique meal configuration combination, sorted by name. This is useful for dropdowns where users can select existing board type configurations to apply to new accommodations.

**Required Permission:** `PERM_READ_ACCOMMODATION_BOARD_TYPE`

**Use Case:**
When creating a board type for a new accommodation, users can select from existing board type configurations (meal combinations) instead of manually configuring each meal setting. If multiple board types have the same meal configuration, only one is returned (the one with the lowest ID), sorted by name.

**Uniqueness Criteria:**
Board types are considered unique based on the following fields:
- `mealsIncluded`
- `breakfastIncluded`
- `lunchIncluded`
- `dinnerIncluded`
- `snacksIncluded`
- `drinksIncluded`
- `alcoholicDrinksIncluded`

**Example Request:**
```
GET /api/accommodation-board-types/unique
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Unique board types retrieved successfully",
  "data": [
    {
      "id": "encoded_board_type_id_1",
      "accommodationId": "encoded_accommodation_id",
      "accommodationName": "Serengeti Safari Lodge",
      "name": "All Inclusive",
      "description": "All meals, drinks, and snacks included",
      "mealsIncluded": "Breakfast, Lunch, Dinner, Snacks",
      "breakfastIncluded": true,
      "lunchIncluded": true,
      "dinnerIncluded": true,
      "snacksIncluded": true,
      "drinksIncluded": true,
      "alcoholicDrinksIncluded": true,
      "inclusions": "All meals, unlimited soft drinks, local alcoholic beverages",
      "exclusions": "Premium imported alcoholic beverages",
      "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm",
      "isActive": true,
      "mealCount": 3,
      "isFullMealPlan": true,
      "createdAt": "2026-01-06T12:00:00",
      "updatedAt": "2026-01-06T12:00:00"
    },
    {
      "id": "encoded_board_type_id_2",
      "accommodationId": "encoded_accommodation_id",
      "accommodationName": "Ngorongoro Crater Lodge",
      "name": "Bed & Breakfast",
      "description": "Includes breakfast only",
      "mealsIncluded": "Breakfast",
      "breakfastIncluded": true,
      "lunchIncluded": false,
      "dinnerIncluded": false,
      "snacksIncluded": false,
      "drinksIncluded": false,
      "alcoholicDrinksIncluded": false,
      "inclusions": "Continental breakfast",
      "exclusions": "All other meals",
      "mealTimes": "Breakfast: 7-10am",
      "isActive": true,
      "mealCount": 1,
      "isFullMealPlan": false,
      "createdAt": "2026-01-05T10:00:00",
      "updatedAt": "2026-01-05T10:00:00"
    },
    {
      "id": "encoded_board_type_id_3",
      "accommodationId": "encoded_accommodation_id",
      "accommodationName": "Tarangire Tented Camp",
      "name": "Full Board",
      "description": "Includes all three meals",
      "mealsIncluded": "Breakfast, Lunch, Dinner",
      "breakfastIncluded": true,
      "lunchIncluded": true,
      "dinnerIncluded": true,
      "snacksIncluded": false,
      "drinksIncluded": false,
      "alcoholicDrinksIncluded": false,
      "inclusions": "Breakfast, lunch, and dinner",
      "exclusions": "Drinks, snacks",
      "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm",
      "isActive": true,
      "mealCount": 3,
      "isFullMealPlan": true,
      "createdAt": "2026-01-04T15:00:00",
      "updatedAt": "2026-01-04T15:00:00"
    }
  ]
}
```

**Response Details:**
- Only active board types (`isActive = true`) are included
- Results are sorted alphabetically by name
- Returns a simple array (not paginated) for easy dropdown population
- Each unique meal configuration appears only once

**Error Responses:**
- `500 Internal Server Error` - Server error

---

## Data Models

### CreateAccommodationBoardTypeDTO
```json
{
  "accommodationId": "string (required)",
  "name": "string (required)",
  "description": "string (optional)",
  "mealsIncluded": "string (optional)",
  "breakfastIncluded": "boolean (optional, default: false)",
  "lunchIncluded": "boolean (optional, default: false)",
  "dinnerIncluded": "boolean (optional, default: false)",
  "snacksIncluded": "boolean (optional, default: false)",
  "drinksIncluded": "boolean (optional, default: false)",
  "alcoholicDrinksIncluded": "boolean (optional, default: false)",
  "inclusions": "string (optional)",
  "exclusions": "string (optional)",
  "mealTimes": "string (optional)",
  "isActive": "boolean (optional, default: true)"
}
```

### UpdateAccommodationBoardTypeDTO
```json
{
  "name": "string (optional)",
  "description": "string (optional)",
  "mealsIncluded": "string (optional)",
  "breakfastIncluded": "boolean (optional)",
  "lunchIncluded": "boolean (optional)",
  "dinnerIncluded": "boolean (optional)",
  "snacksIncluded": "boolean (optional)",
  "drinksIncluded": "boolean (optional)",
  "alcoholicDrinksIncluded": "boolean (optional)",
  "inclusions": "string (optional)",
  "exclusions": "string (optional)",
  "mealTimes": "string (optional)",
  "isActive": "boolean (optional)"
}
```

### AccommodationBoardTypeDTO
```json
{
  "id": "string",
  "accommodationId": "string",
  "accommodationName": "string",
  "name": "string",
  "description": "string",
  "mealsIncluded": "string",
  "breakfastIncluded": "boolean",
  "lunchIncluded": "boolean",
  "dinnerIncluded": "boolean",
  "snacksIncluded": "boolean",
  "drinksIncluded": "boolean",
  "alcoholicDrinksIncluded": "boolean",
  "inclusions": "string",
  "exclusions": "string",
  "mealTimes": "string",
  "isActive": "boolean",
  "mealCount": "integer (calculated)",
  "isFullMealPlan": "boolean (calculated)",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

## Common Board Type Examples

### Room Only (RO)
```json
{
  "name": "Room Only",
  "description": "No meals included",
  "breakfastIncluded": false,
  "lunchIncluded": false,
  "dinnerIncluded": false,
  "isActive": true
}
```

### Bed & Breakfast (BB)
```json
{
  "name": "Bed & Breakfast",
  "description": "Includes breakfast only",
  "mealsIncluded": "Breakfast",
  "breakfastIncluded": true,
  "lunchIncluded": false,
  "dinnerIncluded": false,
  "mealTimes": "Breakfast: 7-10am",
  "isActive": true
}
```

### Half Board (HB)
```json
{
  "name": "Half Board",
  "description": "Includes breakfast and dinner",
  "mealsIncluded": "Breakfast, Dinner",
  "breakfastIncluded": true,
  "lunchIncluded": false,
  "dinnerIncluded": true,
  "mealTimes": "Breakfast: 7-10am, Dinner: 7-10pm",
  "isActive": true
}
```

### Full Board (FB)
```json
{
  "name": "Full Board",
  "description": "Includes all three meals",
  "mealsIncluded": "Breakfast, Lunch, Dinner",
  "breakfastIncluded": true,
  "lunchIncluded": true,
  "dinnerIncluded": true,
  "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm",
  "isActive": true
}
```

### All Inclusive (AI)
```json
{
  "name": "All Inclusive",
  "description": "All meals, drinks, and snacks included",
  "mealsIncluded": "Breakfast, Lunch, Dinner, Snacks",
  "breakfastIncluded": true,
  "lunchIncluded": true,
  "dinnerIncluded": true,
  "snacksIncluded": true,
  "drinksIncluded": true,
  "alcoholicDrinksIncluded": true,
  "inclusions": "All meals, unlimited soft drinks, local alcoholic beverages, snacks",
  "mealTimes": "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm, Snacks: All day",
  "isActive": true
}
```

---

## Notes

- All IDs in requests and responses are obfuscated for security
- Board type names must be unique per accommodation
- The `mealCount` and `isFullMealPlan` fields are calculated automatically
- Timestamps are in ISO 8601 format
- Pagination uses 0-indexed page numbers
- All text fields support UTF-8 characters for international use
