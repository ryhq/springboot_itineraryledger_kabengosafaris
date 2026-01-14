# Accommodation API Documentation

## Base URL
```
/api/accommodations
```

## Authentication
All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_ACCOMMODATION` - Create accommodations
- `PERM_READ_ACCOMMODATION` - Read accommodations
- `PERM_UPDATE_ACCOMMODATION` - Update accommodations
- `PERM_DELETE_ACCOMMODATION` - Delete accommodations

---

## Endpoints

### 1. Create Accommodation
**POST** `/api/accommodations`

Creates a new accommodation (hotel, lodge, camp, etc.).

**Permission Required:** `PERM_CREATE_ACCOMMODATION`

**Request Body:**
```json
{
  "name": "Serengeti Serena Safari Lodge",
  "slug": "serengeti-serena-safari-lodge",
  "accommodationType": "LODGE",
  "category": "LUXURY",
  "tin": "123456789",
  "vrn": "VRN123456",
  "website": "https://example.com",
  "parentAccommodationId": "encoded-parent-id",
  "region": "Serengeti",
  "district": "Serengeti",
  "location": "Central Serengeti, Tanzania",
  "address": "P.O. Box 123, Arusha",
  "latitude": -2.333333,
  "longitude": 34.833333,
  "elevation": "1,400m above sea level",
  "totalRooms": 66,
  "totalBeds": 132,
  "maxGuests": 200,
  "starRating": 5,
  "shortDescription": "Luxury lodge in the heart of Serengeti",
  "details": "Full detailed description...",
  "amenities": "Pool, Spa, Restaurant, Bar, Wifi",
  "services": "Game drives, Cultural tours, Airport transfers",
  "nearbyAttractions": "Serengeti National Park, Ngorongoro Crater",
  "termsAndConditions": "Full terms...",
  "cancellationPolicy": "Free cancellation up to 48 hours...",
  "checkInPolicy": "Check-in from 2 PM",
  "checkOutPolicy": "Check-out by 11 AM",
  "childPolicy": "Children of all ages welcome",
  "petPolicy": "Pets not allowed",
  "priceRange": "$300 - $800 per night",
  "currency": "USD",
  "bestSeason": "June to October (Dry season)",
  "operatingSeason": "Year-round",
  "tags": "Safari Lodge, Pool, Wifi, Family Friendly",
  "isActive": true,
  "internalNotes": "Staff notes..."
}
```

**Accommodation Types:**
- `HOTEL`, `LODGE`, `TENTED_CAMP`, `MOBILE_CAMP`, `GUESTHOUSE`, `HOSTEL`, `RESORT`, `VILLA`, `COTTAGE`, `APARTMENT`, `CAMPSITE`, `BANDA`, `TREE_HOUSE`, `ECO_LODGE`, `BOUTIQUE_HOTEL`, `OTHER`

**Categories:**
- `LUXURY`, `PREMIUM`, `MID_RANGE`, `BUDGET`, `BACKPACKER`, `ULTRA_LUXURY`

**Response:**
```json
{
  "status": 201,
  "message": "Accommodation created successfully",
  "data": {
    "id": "encoded-accommodation-id",
    "name": "Serengeti Serena Safari Lodge",
    "slug": "serengeti-serena-safari-lodge",
    "accommodationType": "LODGE",
    "accommodationTypeDisplayName": "Lodge",
    "accommodationTypeDescription": "Safari lodge, typically in or near national parks",
    "category": "LUXURY",
    "categoryDisplayName": "Luxury",
    "categoryDescription": "High-end luxury accommodation with premium services",
    "categoryApproximateStars": 5,
    "region": "Serengeti",
    "starRating": 5,
    "isActive": true,
    "emailCount": 0,
    "phoneCount": 0,
    "imageCount": 0,
    "branchCount": 0,
    "roomTypeCount": 0,
    "roomStandardCount": 0,
    "boardTypeCount": 0,
    "rateCount": 0,
    "documentCount": 0,
    "createdAt": "2025-01-02T10:30:00",
    "updatedAt": "2025-01-02T10:30:00"
  }
}
```

**Validation:**
- `name` - Required, max 200 characters
- `accommodationType` - Required
- `details` - Required
- `latitude` - Must be between -90 and 90
- `longitude` - Must be between -180 and 180
- `starRating` - Must be between 1 and 5
- `slug` - Auto-generated if not provided

**Features:**
- Auto-generates URL-friendly slug from name if not provided
- Validates parent accommodation exists (if provided)
- Updates parent's `hasBranch` flag when adding branches
- Checks for duplicate names and slugs

---

### 2. Update Accommodation
**PUT** `/api/accommodations/{id}`

Updates an existing accommodation. Only provided fields will be updated.

**Permission Required:** `PERM_UPDATE_ACCOMMODATION`

**Path Parameters:**
- `id` (string) - Encoded accommodation ID

**Request Body:**
All fields are optional. Only include fields you want to update.
```json
{
  "name": "Updated Name",
  "starRating": 5,
  "isActive": false,
  "parentAccommodationId": ""
}
```

**Notes:**
- Set `parentAccommodationId` to empty string to remove parent
- Name and slug uniqueness is validated if changed
- Parent accommodation's `hasBranch` flag is updated automatically

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation updated successfully",
  "data": { /* AccommodationDTO */ }
}
```

---

### 3. Delete Accommodations
**DELETE** `/api/accommodations`

Deletes one or more accommodations. **If an accommodation has branches, all branches are deleted recursively.**

**Permission Required:** `PERM_DELETE_ACCOMMODATION`

**Request Body:**
```json
[
  "encoded-accommodation-id-1",
  "encoded-accommodation-id-2"
]
```

**Response:**
```json
{
  "status": 200,
  "message": "5 accommodation(s) deleted successfully",
  "data": null
}
```

**Important:**
- Cascade deletion - deleting a parent automatically deletes all its branches and sub-branches
- All related data (emails, phones, images, rates, documents) are deleted via cascade
- Returns total count including all cascade-deleted items

**Example:**
If you delete HQ with 2 branches, and each branch has 1 sub-branch:
```
HQ
├── Branch A
│   └── Sub-branch A1
└── Branch B
    └── Sub-branch B1
```
Result: **5 accommodations deleted** (1 HQ + 2 branches + 2 sub-branches)

---

### 4. Get Accommodation by ID
**GET** `/api/accommodations/{id}`

Retrieves a single accommodation by its encoded ID.

**Permission Required:** `PERM_READ_ACCOMMODATION`

**Path Parameters:**
- `id` (string) - Encoded accommodation ID

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation retrieved successfully",
  "data": {
    "id": "encoded-accommodation-id",
    "name": "Serengeti Serena Safari Lodge",
    "slug": "serengeti-serena-safari-lodge",
    "accommodationType": "LODGE",
    "accommodationTypeDisplayName": "Lodge",
    "accommodationTypeDescription": "Safari lodge, typically in or near national parks",
    "category": "LUXURY",
    "categoryDisplayName": "Luxury",
    "categoryDescription": "High-end luxury accommodation with premium services",
    "categoryApproximateStars": 5,
    "tin": "123456789",
    "vrn": "VRN123456",
    "website": "https://example.com",
    "hasBranch": true,
    "isHeadquarters": true,
    "parentAccommodationId": null,
    "parentAccommodationName": null,
    "region": "Serengeti",
    "district": "Serengeti",
    "location": "Central Serengeti, Tanzania",
    "address": "P.O. Box 123, Arusha",
    "latitude": -2.333333,
    "longitude": 34.833333,
    "elevation": "1,400m above sea level",
    "totalRooms": 66,
    "totalBeds": 132,
    "maxGuests": 200,
    "starRating": 5,
    "shortDescription": "Luxury lodge in the heart of Serengeti",
    "details": "Full detailed description...",
    "amenities": "Pool, Spa, Restaurant, Bar, Wifi",
    "services": "Game drives, Cultural tours",
    "nearbyAttractions": "Serengeti National Park",
    "termsAndConditions": "Full terms...",
    "cancellationPolicy": "Free cancellation...",
    "checkInPolicy": "Check-in from 2 PM",
    "checkOutPolicy": "Check-out by 11 AM",
    "childPolicy": "Children welcome",
    "petPolicy": "Pets not allowed",
    "priceRange": "$300 - $800 per night",
    "currency": "USD",
    "bestSeason": "June to October",
    "operatingSeason": "Year-round",
    "tags": "Safari Lodge, Pool, Wifi",
    "isActive": true,
    "createdAt": "2025-01-02T10:30:00",
    "updatedAt": "2025-01-02T10:30:00",
    "emailCount": 2,
    "phoneCount": 3,
    "imageCount": 15,
    "branchCount": 2,
    "roomTypeCount": 5,
    "roomStandardCount": 4,
    "boardTypeCount": 3,
    "rateCount": 60,
    "documentCount": 8
  }
}
```

---

### 5. Get Accommodation by Slug
**GET** `/api/accommodations/slug/{slug}`

Retrieves a single accommodation by its slug (URL-friendly identifier).

**Permission Required:** `PERM_READ_ACCOMMODATION`

**Path Parameters:**
- `slug` (string) - Accommodation slug (e.g., "serengeti-serena-safari-lodge")

**Response:**
Same as "Get Accommodation by ID"

**Use Case:**
Ideal for public-facing URLs and SEO-friendly routes.

---

### 6. Get Accommodations List (Lightweight for Dropdowns)
**GET** `/api/accommodations/list`

Retrieves a lightweight list of accommodations for dropdown/select purposes. Returns only essential fields without pagination.

**Permission Required:** `PERM_READ_ACCOMMODATION`

**Query Parameters:**
- `hasBranch` (boolean, optional) - Filter accommodations with branches
  - `true` - Returns only accommodations that have branches (headquarters with branches)
  - `false` - Returns only accommodations without branches
  - Not provided - Returns all accommodations

**Example Requests:**
```
# Get all accommodations for dropdown
GET /api/accommodations/list

# Get only accommodations with branches (headquarters)
GET /api/accommodations/list?hasBranch=true

# Get only single-location accommodations (no branches)
GET /api/accommodations/list?hasBranch=false
```

**Success Response (200 OK):**
```json
{
  "status": 200,
  "message": "Accommodations list retrieved successfully",
  "data": [
    {
      "id": "encoded_accommodation_id_1",
      "name": "Serengeti Serena Safari Lodge",
      "location": "Central Serengeti, Tanzania",
      "region": "Serengeti",
      "isActive": true
    },
    {
      "id": "encoded_accommodation_id_2",
      "name": "Ngorongoro Crater Lodge",
      "location": "Ngorongoro Conservation Area",
      "region": "Ngorongoro",
      "isActive": true
    }
  ]
}
```

**Use Cases:**
- Populate dropdown lists in forms
- Select parent accommodation when creating branches
- Quick accommodation selection in UI components
- Selecting accommodation for emails, phones, and other related entities

**Note:** This endpoint returns all matching accommodations without pagination, sorted alphabetically by name. It's optimized for UI components requiring simple selection lists.

---

### 7. Get All Accommodations (List with Filters)
**GET** `/api/accommodations`

Retrieves a paginated list of accommodations with extensive filtering options.

**Permission Required:** `PERM_READ_ACCOMMODATION`

**Query Parameters:**

#### Basic Filters
- `name` (string) - Filter by name (partial match, case-insensitive)
- `slug` (string) - Filter by slug (partial match)
- `accommodationType` (enum) - Filter by type (HOTEL, LODGE, etc.)
- `category` (enum) - Filter by category (LUXURY, BUDGET, etc.)
- `isActive` (boolean) - Filter by active status

#### Location Filters
- `region` (string) - Filter by region (partial match)
- `exactRegion` (string) - Filter by exact region
- `district` (string) - Filter by district (partial match)
- `exactDistrict` (string) - Filter by exact district

#### Rating Filters
- `starRating` (integer) - Filter by exact star rating
- `minStarRating` (integer) - Minimum star rating (1-5)
- `maxStarRating` (integer) - Maximum star rating (1-5)

#### Hierarchy Filters
- `isHeadquarters` (boolean) - Filter headquarters
- `hasBranch` (boolean) - Filter accommodations with/without branches
- `parentId` (string) - Filter branches of specific parent (obfuscated accommodation ID)
- `hasNoParent` (boolean) - Filter only headquarters (no parent)

#### Capacity Filters
- `minRooms` (integer) - Minimum number of rooms
- `maxRooms` (integer) - Maximum number of rooms
- `minGuests` (integer) - Minimum guest capacity
- `maxGuests` (integer) - Maximum guest capacity

#### Content Filters
- `tags` (string) - Filter by tags (partial match)
- `amenities` (string) - Filter by amenities (partial match)
- `keyword` (string) - Search across name, location, description, tags, amenities, attractions

#### Special Filters
- `hasImages` (boolean) - Only accommodations with images
- `hasRates` (boolean) - Only accommodations with rates
- `hasCoordinates` (boolean) - Only accommodations with GPS coordinates

#### Pagination & Sorting
- `page` (integer) - Page number, 0-indexed (default: 0)
- `size` (integer) - Page size (default: 10)
- `sortDirection` (string) - "asc" or "desc" (default: "desc", sorts by createdAt)

**Example Request:**
```
GET /api/accommodations?region=Serengeti&category=LUXURY&minStarRating=4&isActive=true&page=0&size=20
```

**Response:**
```json
{
  "status": 200,
  "message": "Accommodations retrieved successfully",
  "data": {
    "accommodations": [
      {
        "id": "encoded-accommodation-id-1",
        "name": "Serengeti Serena Safari Lodge",
        "slug": "serengeti-serena-safari-lodge",
        "accommodationType": "LODGE",
        "accommodationTypeDisplayName": "Lodge",
        "category": "LUXURY",
        "categoryDisplayName": "Luxury",
        "region": "Serengeti",
        "starRating": 5,
        "isActive": true,
        "emailCount": 2,
        "phoneCount": 3,
        "imageCount": 15,
        "createdAt": "2025-01-02T10:30:00",
        "updatedAt": "2025-01-02T10:30:00"
      },
      {
        "id": "encoded-accommodation-id-2",
        "name": "Ngorongoro Crater Lodge",
        "slug": "ngorongoro-crater-lodge",
        "accommodationType": "LODGE",
        "accommodationTypeDisplayName": "Lodge",
        "category": "LUXURY",
        "categoryDisplayName": "Luxury",
        "region": "Ngorongoro",
        "starRating": 5,
        "isActive": true,
        "emailCount": 1,
        "phoneCount": 2,
        "imageCount": 12,
        "createdAt": "2025-01-01T14:20:00",
        "updatedAt": "2025-01-01T14:20:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 45,
    "totalPages": 3
  }
}
```

**Common Use Cases:**

1. **Find luxury lodges in Serengeti:**
   ```
   ?region=Serengeti&accommodationType=LODGE&category=LUXURY
   ```

2. **Search for accommodations with pool:**
   ```
   ?amenities=pool
   ```

3. **Find all branches of an accommodation:**
   ```
   ?parentId=encoded_accommodation_id
   ```
   **Note**: Use the obfuscated ID of the headquarters/parent accommodation

4. **Find all headquarters (no branches):**
   ```
   ?isHeadquarters=true&hasBranch=false
   ```

5. **Find 4-5 star accommodations with 50+ rooms:**
   ```
   ?minStarRating=4&minRooms=50
   ```

6. **Full-text search:**
   ```
   ?keyword=safari+lodge+serengeti
   ```

---

## Error Responses

### 400 Bad Request
```json
{
  "status": 400,
  "message": "Accommodation name already exists",
  "errorCode": "DUPLICATE_ACCOMMODATION_NAME",
  "data": null
}
```

**Error Codes:**
- `DUPLICATE_ACCOMMODATION_NAME` - Name already exists
- `DUPLICATE_ACCOMMODATION_SLUG` - Slug already exists
- `INVALID_ACCOMMODATION_ID` - Invalid or malformed ID
- `INVALID_PARENT_ACCOMMODATION_ID` - Invalid parent ID
- `PARENT_ACCOMMODATION_NOT_FOUND` - Parent not found

### 404 Not Found
```json
{
  "status": 404,
  "message": "Accommodation not found",
  "errorCode": "ACCOMMODATION_NOT_FOUND",
  "data": null
}
```

### 500 Internal Server Error
```json
{
  "status": 500,
  "message": "Failed to create accommodation: [error details]",
  "errorCode": "ACCOMMODATION_CREATE_FAILED",
  "data": null
}
```

---

## Multi-Branch Support

### Creating a Branch
To create a branch of an existing accommodation:
```json
{
  "name": "Serengeti Serena - Ngorongoro Branch",
  "parentAccommodationId": "encoded-parent-id",
  "isHeadquarters": false,
  "region": "Ngorongoro",
  ...
}
```

### Querying Branch Hierarchy
```
# Get all branches of a parent headquarters
GET /api/accommodations?parentId=encoded_accommodation_id

# Get only headquarters
GET /api/accommodations?isHeadquarters=true

# Get accommodations without branches
GET /api/accommodations?hasBranch=false

# Get accommodations that have no parent (headquarters and single locations)
GET /api/accommodations?hasNoParent=true
```

**Note**: The `parentId` parameter accepts the obfuscated/encoded ID of the parent accommodation (headquarters). To get branches of an accommodation, pass its obfuscated ID as the `parentId` value.

### Deleting with Branches
When you delete an accommodation with branches:
1. All direct branches are identified
2. Each branch is recursively deleted (including their sub-branches)
3. All related data is cascade-deleted
4. Returns total count of all deleted items

---

## Notes

1. **ID Obfuscation**: All IDs are encoded for security. Never expose raw database IDs.

2. **Slug Auto-Generation**: If not provided, slug is auto-generated from name:
   - Converts to lowercase
   - Replaces spaces with hyphens
   - Removes special characters
   - Example: "Serengeti Serena Safari Lodge" → "serengeti-serena-safari-lodge"

3. **Parent-Branch Relationships**:
   - When a branch is created, parent's `hasBranch` flag is automatically set to `true`
   - Unlimited depth supported (branches can have sub-branches)
   - Cascade delete ensures complete cleanup

4. **Enum Display Fields**: For user-friendly display, enum fields include additional information:
   - `accommodationType` - Enum value (e.g., "LODGE")
   - `accommodationTypeDisplayName` - Human-readable name (e.g., "Lodge")
   - `accommodationTypeDescription` - Detailed description (e.g., "Safari lodge, typically in or near national parks")
   - `category` - Enum value (e.g., "LUXURY")
   - `categoryDisplayName` - Human-readable name (e.g., "Luxury")
   - `categoryDescription` - Detailed description (e.g., "High-end luxury accommodation with premium services")
   - `categoryApproximateStars` - Star rating equivalent (1-5)

5. **Relationship Counts**: Response DTOs include counts for:
   - Emails, phones, images
   - Branches, room types, room standards, board types
   - Rates, documents

6. **Filtering**: All filters can be combined for complex queries. Use specification pattern for type-safe filtering.

7. **Pagination**: Default page size is 10. Maximum recommended: 100 per page.

8. **Sorting**: Currently sorts by `createdAt` only. Direction can be `asc` or `desc`.
