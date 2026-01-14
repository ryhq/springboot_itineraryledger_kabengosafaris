# Accommodation Phone API Documentation

## Base URL
```
/api/accommodation-phones
```

## Authentication
All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_ACCOMMODATION_PHONE` - Create accommodation phones
- `PERM_READ_ACCOMMODATION_PHONE` - Read accommodation phones
- `PERM_UPDATE_ACCOMMODATION_PHONE` - Update accommodation phones
- `PERM_DELETE_ACCOMMODATION_PHONE` - Delete accommodation phones

---

## Endpoints

### 1. Create Accommodation Phone
**POST** `/api/accommodation-phones`

Creates a new phone number for an accommodation.

**Permission Required:** `PERM_CREATE_ACCOMMODATION_PHONE`

**Request Body:**
```json
{
  "accommodationId": "encoded-accommodation-id",
  "phoneNumber": "+255 123 456 789",
  "countryCode": "+255",
  "phoneType": "RESERVATIONS",
  "isPrimary": true,
  "isWhatsApp": true,
  "isActive": true,
  "label": "24/7 Reservations Hotline",
  "operatingHours": "24/7"
}
```

**Phone Types:**
- `LANDLINE` - Fixed landline number
- `MOBILE` - Mobile phone number
- `RESERVATIONS` - Reservations hotline
- `RECEPTION` - Reception desk
- `EMERGENCY` - Emergency contact
- `FAX` - Fax number
- `TOLL_FREE` - Toll-free number
- `WHATSAPP` - WhatsApp Business number
- `OTHER` - Other phone type

**Response:**
```json
{
  "status": 201,
  "message": "Accommodation phone created successfully",
  "data": {
    "id": "encoded-phone-id",
    "accommodationId": "encoded-accommodation-id",
    "accommodationName": "Serengeti Serena Safari Lodge",
    "phoneNumber": "+255 123 456 789",
    "countryCode": "+255",
    "phoneType": "RESERVATIONS",
    "phoneTypeDisplayName": "Reservations",
    "phoneTypeDescription": "Reservations hotline",
    "isPrimary": true,
    "isWhatsApp": true,
    "isActive": true,
    "label": "24/7 Reservations Hotline",
    "operatingHours": "24/7",
    "createdAt": "2025-01-06T10:30:00",
    "updatedAt": "2025-01-06T10:30:00"
  }
}
```

**Validation:**
- `accommodationId` - Required
- `phoneNumber` - Required, max 50 characters
- `countryCode` - Optional, max 10 characters
- `phoneType` - Required
- `isPrimary` - Optional, defaults to false
- `isWhatsApp` - Optional, defaults to false
- `isActive` - Optional, defaults to true
- `label` - Optional, max 100 characters
- `operatingHours` - Optional, max 200 characters

**Features:**
- Validates accommodation exists
- Checks for duplicate phone numbers
- Includes enum display fields for user-friendly presentation

---

### 2. Update Accommodation Phone
**PUT** `/api/accommodation-phones/{id}`

Updates an existing accommodation phone. Only provided fields will be updated.

**Permission Required:** `PERM_UPDATE_ACCOMMODATION_PHONE`

**Path Parameters:**
- `id` (string) - Encoded phone ID

**Request Body:**
All fields are optional. Only include fields you want to update.
```json
{
  "phoneNumber": "+255 987 654 321",
  "phoneType": "RECEPTION",
  "isPrimary": false,
  "isWhatsApp": false,
  "isActive": true,
  "label": "Updated Label",
  "operatingHours": "8:00 AM - 6:00 PM"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation phone updated successfully",
  "data": {
    "id": "encoded-phone-id",
    "accommodationId": "encoded-accommodation-id",
    "accommodationName": "Serengeti Serena Safari Lodge",
    "phoneNumber": "+255 987 654 321",
    "countryCode": "+255",
    "phoneType": "RECEPTION",
    "phoneTypeDisplayName": "Reception",
    "phoneTypeDescription": "Reception desk",
    "isPrimary": false,
    "isWhatsApp": false,
    "isActive": true,
    "label": "Updated Label",
    "operatingHours": "8:00 AM - 6:00 PM",
    "createdAt": "2025-01-06T10:30:00",
    "updatedAt": "2025-01-06T11:45:00"
  }
}
```

**Notes:**
- Phone number uniqueness is validated if changed
- Audit log is automatically created for the update

---

### 3. Delete Accommodation Phones
**DELETE** `/api/accommodation-phones`

Deletes one or more accommodation phones by their IDs.

**Permission Required:** `PERM_DELETE_ACCOMMODATION_PHONE`

**Request Body:**
```json
["encoded-phone-id-1", "encoded-phone-id-2", "encoded-phone-id-3"]
```

**Response:**
```json
{
  "status": 200,
  "message": "3 accommodation phone(s) deleted successfully",
  "data": null
}
```

**Notes:**
- Non-existent IDs are skipped with a warning in logs
- Audit log is automatically created for each deletion
- Partial success is possible (some phones deleted, others skipped if not found)

---

### 4. Get Accommodation Phone by ID
**GET** `/api/accommodation-phones/{id}`

Retrieves a single accommodation phone by its ID.

**Permission Required:** `PERM_READ_ACCOMMODATION_PHONE`

**Path Parameters:**
- `id` (string) - Encoded phone ID

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation phone retrieved successfully",
  "data": {
    "id": "encoded-phone-id",
    "accommodationId": "encoded-accommodation-id",
    "accommodationName": "Serengeti Serena Safari Lodge",
    "phoneNumber": "+255 123 456 789",
    "countryCode": "+255",
    "phoneType": "RESERVATIONS",
    "phoneTypeDisplayName": "Reservations",
    "phoneTypeDescription": "Reservations hotline",
    "isPrimary": true,
    "isWhatsApp": true,
    "isActive": true,
    "label": "24/7 Reservations Hotline",
    "operatingHours": "24/7",
    "createdAt": "2025-01-06T10:30:00",
    "updatedAt": "2025-01-06T10:30:00"
  }
}
```

**Error Response (404):**
```json
{
  "status": 404,
  "message": "Accommodation phone not found",
  "errorCode": "ACCOMMODATION_PHONE_NOT_FOUND"
}
```

---

### 5. Get All Accommodation Phones
**GET** `/api/accommodation-phones`

Retrieves all accommodation phones with optional filtering, pagination, and sorting.

**Permission Required:** `PERM_READ_ACCOMMODATION_PHONE`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `accommodationId` | string | No | Filter by accommodation ID (optional) |
| `phoneNumber` | string | No | Filter by phone number (partial match) |
| `countryCode` | string | No | Filter by country code |
| `phoneType` | PhoneType | No | Filter by phone type |
| `isPrimary` | boolean | No | Filter by primary status |
| `isWhatsApp` | boolean | No | Filter by WhatsApp status |
| `isActive` | boolean | No | Filter by active status |
| `label` | string | No | Filter by label (partial match) |
| `keyword` | string | No | Search across phoneNumber, label, and operatingHours fields |
| `page` | integer | No | Page number (0-indexed, default: 0) |
| `size` | integer | No | Page size (default: 10) |
| `sortDirection` | string | No | Sort direction: "asc" or "desc" (default: "desc") |

**Example Request:**
```
GET /api/accommodation-phones?phoneType=RESERVATIONS&isWhatsApp=true&page=0&size=10&sortDirection=desc
```

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation phones retrieved successfully",
  "data": {
    "phones": [
      {
        "id": "encoded-phone-id-1",
        "accommodationId": "encoded-accommodation-id-1",
        "accommodationName": "Serengeti Serena Safari Lodge",
        "phoneNumber": "+255 123 456 789",
        "countryCode": "+255",
        "phoneType": "RESERVATIONS",
        "phoneTypeDisplayName": "Reservations",
        "phoneTypeDescription": "Reservations hotline",
        "isPrimary": true,
        "isWhatsApp": true,
        "isActive": true,
        "label": "24/7 Reservations",
        "operatingHours": "24/7",
        "createdAt": "2025-01-06T10:30:00",
        "updatedAt": "2025-01-06T10:30:00"
      },
      {
        "id": "encoded-phone-id-2",
        "accommodationId": "encoded-accommodation-id-2",
        "accommodationName": "Ngorongoro Crater Lodge",
        "phoneNumber": "+255 987 654 321",
        "countryCode": "+255",
        "phoneType": "RESERVATIONS",
        "phoneTypeDisplayName": "Reservations",
        "phoneTypeDescription": "Reservations hotline",
        "isPrimary": true,
        "isWhatsApp": true,
        "isActive": true,
        "label": null,
        "operatingHours": "8:00 AM - 6:00 PM",
        "createdAt": "2025-01-05T14:20:00",
        "updatedAt": "2025-01-05T14:20:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 2,
    "totalPages": 1
  }
}
```

**Notes:**
- Results are sorted by `createdAt` in descending order by default (newest first)
- All filters are optional and can be combined
- The `keyword` parameter searches across phoneNumber, label, and operatingHours fields

---

### 6. Get Phones for Specific Accommodation
**GET** `/api/accommodation-phones/accommodation/{accommodationId}`

Retrieves all phones for a specific accommodation with optional filtering, pagination, and sorting.

**Permission Required:** `PERM_READ_ACCOMMODATION_PHONE`

**Path Parameters:**
- `accommodationId` (string) - **Required** encoded accommodation ID

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `phoneNumber` | string | No | Filter by phone number (partial match) |
| `countryCode` | string | No | Filter by country code |
| `phoneType` | PhoneType | No | Filter by phone type |
| `isPrimary` | boolean | No | Filter by primary status |
| `isWhatsApp` | boolean | No | Filter by WhatsApp status |
| `isActive` | boolean | No | Filter by active status |
| `label` | string | No | Filter by label (partial match) |
| `keyword` | string | No | Search across phoneNumber, label, and operatingHours fields |
| `page` | integer | No | Page number (0-indexed, default: 0) |
| `size` | integer | No | Page size (default: 10) |
| `sortDirection` | string | No | Sort direction: "asc" or "desc" (default: "desc") |

**Example Request:**
```
GET /api/accommodation-phones/accommodation/encoded-accommodation-id?isActive=true&page=0&size=10
```

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation phones retrieved successfully",
  "data": {
    "phones": [
      {
        "id": "encoded-phone-id-1",
        "accommodationId": "encoded-accommodation-id",
        "accommodationName": "Serengeti Serena Safari Lodge",
        "phoneNumber": "+255 123 456 789",
        "countryCode": "+255",
        "phoneType": "RESERVATIONS",
        "phoneTypeDisplayName": "Reservations",
        "phoneTypeDescription": "Reservations hotline",
        "isPrimary": true,
        "isWhatsApp": true,
        "isActive": true,
        "label": "24/7 Reservations",
        "operatingHours": "24/7",
        "createdAt": "2025-01-06T10:30:00",
        "updatedAt": "2025-01-06T10:30:00"
      },
      {
        "id": "encoded-phone-id-2",
        "accommodationId": "encoded-accommodation-id",
        "accommodationName": "Serengeti Serena Safari Lodge",
        "phoneNumber": "+255 987 654 321",
        "countryCode": "+255",
        "phoneType": "RECEPTION",
        "phoneTypeDisplayName": "Reception",
        "phoneTypeDescription": "Reception desk",
        "isPrimary": false,
        "isWhatsApp": false,
        "isActive": true,
        "label": "Front Desk",
        "operatingHours": "8:00 AM - 10:00 PM",
        "createdAt": "2025-01-06T09:15:00",
        "updatedAt": "2025-01-06T09:15:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 2,
    "totalPages": 1
  }
}
```

**Notes:**
- `accommodationId` is **required** as a path parameter
- All other filters are optional and can be combined
- This endpoint is specifically designed for retrieving phones in the context of a single accommodation

---

## Error Codes

| Error Code | HTTP Status | Description |
|-----------|-------------|-------------|
| `INVALID_PHONE_ID` | 400 | The provided phone ID is invalid or cannot be decoded |
| `INVALID_ACCOMMODATION_ID` | 400 | The provided accommodation ID is invalid or cannot be decoded |
| `ACCOMMODATION_NOT_FOUND` | 404 | The specified accommodation does not exist |
| `ACCOMMODATION_PHONE_NOT_FOUND` | 404 | The specified phone does not exist |
| `DUPLICATE_PHONE_NUMBER` | 400 | Phone number already exists |
| `ACCOMMODATION_PHONE_CREATE_FAILED` | 500 | Failed to create accommodation phone |
| `ACCOMMODATION_PHONE_UPDATE_FAILED` | 500 | Failed to update accommodation phone |
| `ACCOMMODATION_PHONES_DELETE_FAILED` | 500 | Failed to delete accommodation phones |
| `ACCOMMODATION_PHONE_FETCH_FAILED` | 500 | Failed to fetch accommodation phone |
| `ACCOMMODATION_PHONES_FETCH_FAILED` | 500 | Failed to fetch accommodation phones |

---

## Important Notes

1. **ID Obfuscation**: All IDs in requests and responses are obfuscated for security. Never expose or use raw database IDs.

2. **Audit Logging**: All create, update, and delete operations are automatically logged for audit purposes.

3. **Enum Display Fields**: For user-friendly display, phone type fields include additional information:
   - `phoneType` - Enum value (e.g., "RESERVATIONS")
   - `phoneTypeDisplayName` - Human-readable name (e.g., "Reservations")
   - `phoneTypeDescription` - Detailed description (e.g., "Reservations hotline")

4. **Duplicate Phone Prevention**: The system prevents duplicate phone numbers across all accommodations.

5. **WhatsApp Support**: The `isWhatsApp` field indicates whether WhatsApp is available on the phone number, useful for providing messaging options to customers.

6. **Operating Hours**: The `operatingHours` field helps communicate availability (e.g., "24/7", "8:00 AM - 6:00 PM", "Weekdays only").

7. **Two Get All Endpoints**:
   - `/api/accommodation-phones` - Returns phones from all accommodations (accommodationId is optional filter)
   - `/api/accommodation-phones/accommodation/{accommodationId}` - Returns phones for a specific accommodation (accommodationId is required)

8. **Pagination**: All list endpoints support pagination with default values (page=0, size=10). Adjust these parameters based on your needs.
