# Accommodation Email API Documentation

## Base URL
```
/api/accommodation-emails
```

## Authentication
All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_ACCOMMODATION_EMAIL` - Create accommodation emails
- `PERM_READ_ACCOMMODATION_EMAIL` - Read accommodation emails
- `PERM_UPDATE_ACCOMMODATION_EMAIL` - Update accommodation emails
- `PERM_DELETE_ACCOMMODATION_EMAIL` - Delete accommodation emails

---

## Endpoints

### 1. Create Accommodation Email
**POST** `/api/accommodation-emails`

Creates a new email address for an accommodation.

**Permission Required:** `PERM_CREATE_ACCOMMODATION_EMAIL`

**Request Body:**
```json
{
  "accommodationId": "encoded-accommodation-id",
  "email": "info@serengetiserena.com",
  "emailType": "RESERVATIONS",
  "isPrimary": true,
  "isActive": true,
  "label": "Main Reservations Email"
}
```

**Email Types:**
- `GENERAL` - General inquiries and information
- `RESERVATIONS` - Booking and reservation inquiries
- `SALES` - Sales and business development
- `SUPPORT` - Customer support and assistance
- `BILLING` - Billing and payment inquiries
- `MARKETING` - Marketing and promotional communications
- `MANAGEMENT` - Management and administrative
- `OTHER` - Other purposes

**Response:**
```json
{
  "status": 201,
  "message": "Accommodation email created successfully",
  "data": {
    "id": "encoded-email-id",
    "accommodationId": "encoded-accommodation-id",
    "accommodationName": "Serengeti Serena Safari Lodge",
    "email": "info@serengetiserena.com",
    "emailType": "RESERVATIONS",
    "emailTypeDisplayName": "Reservations",
    "emailTypeDescription": "Booking and reservation inquiries",
    "isPrimary": true,
    "isActive": true,
    "label": "Main Reservations Email",
    "createdAt": "2025-01-06T10:30:00",
    "updatedAt": "2025-01-06T10:30:00"
  }
}
```

**Validation:**
- `accommodationId` - Required
- `email` - Required, must be valid email format, max 255 characters
- `emailType` - Required
- `isPrimary` - Optional, defaults to false
- `isActive` - Optional, defaults to true
- `label` - Optional, max 100 characters

**Features:**
- Validates accommodation exists
- Checks for duplicate email addresses
- Includes enum display fields for user-friendly presentation

---

### 2. Update Accommodation Email
**PUT** `/api/accommodation-emails/{id}`

Updates an existing accommodation email. Only provided fields will be updated.

**Permission Required:** `PERM_UPDATE_ACCOMMODATION_EMAIL`

**Path Parameters:**
- `id` (string) - Encoded email ID

**Request Body:**
All fields are optional. Only include fields you want to update.
```json
{
  "email": "newemail@serengetiserena.com",
  "emailType": "GENERAL",
  "isPrimary": false,
  "isActive": true,
  "label": "Updated Label"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation email updated successfully",
  "data": {
    "id": "encoded-email-id",
    "accommodationId": "encoded-accommodation-id",
    "accommodationName": "Serengeti Serena Safari Lodge",
    "email": "newemail@serengetiserena.com",
    "emailType": "GENERAL",
    "emailTypeDisplayName": "General",
    "emailTypeDescription": "General inquiries and information",
    "isPrimary": false,
    "isActive": true,
    "label": "Updated Label",
    "createdAt": "2025-01-06T10:30:00",
    "updatedAt": "2025-01-06T11:45:00"
  }
}
```

**Notes:**
- Email uniqueness is validated if changed
- Audit log is automatically created for the update

---

### 3. Delete Accommodation Emails
**DELETE** `/api/accommodation-emails`

Deletes one or more accommodation emails by their IDs.

**Permission Required:** `PERM_DELETE_ACCOMMODATION_EMAIL`

**Request Body:**
```json
["encoded-email-id-1", "encoded-email-id-2", "encoded-email-id-3"]
```

**Response:**
```json
{
  "status": 200,
  "message": "3 accommodation email(s) deleted successfully",
  "data": null
}
```

**Notes:**
- Non-existent IDs are skipped with a warning in logs
- Audit log is automatically created for each deletion
- Partial success is possible (some emails deleted, others skipped if not found)

---

### 4. Get Accommodation Email by ID
**GET** `/api/accommodation-emails/{id}`

Retrieves a single accommodation email by its ID.

**Permission Required:** `PERM_READ_ACCOMMODATION_EMAIL`

**Path Parameters:**
- `id` (string) - Encoded email ID

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation email retrieved successfully",
  "data": {
    "id": "encoded-email-id",
    "accommodationId": "encoded-accommodation-id",
    "accommodationName": "Serengeti Serena Safari Lodge",
    "email": "info@serengetiserena.com",
    "emailType": "RESERVATIONS",
    "emailTypeDisplayName": "Reservations",
    "emailTypeDescription": "Booking and reservation inquiries",
    "isPrimary": true,
    "isActive": true,
    "label": "Main Reservations Email",
    "createdAt": "2025-01-06T10:30:00",
    "updatedAt": "2025-01-06T10:30:00"
  }
}
```

**Error Response (404):**
```json
{
  "status": 404,
  "message": "Accommodation email not found",
  "errorCode": "ACCOMMODATION_EMAIL_NOT_FOUND"
}
```

---

### 5. Get All Accommodation Emails
**GET** `/api/accommodation-emails`

Retrieves all accommodation emails with optional filtering, pagination, and sorting.

**Permission Required:** `PERM_READ_ACCOMMODATION_EMAIL`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `accommodationId` | string | No | Filter by accommodation ID (optional) |
| `email` | string | No | Filter by email address (partial match) |
| `emailType` | EmailType | No | Filter by email type |
| `isPrimary` | boolean | No | Filter by primary status |
| `isActive` | boolean | No | Filter by active status |
| `label` | string | No | Filter by label (partial match) |
| `keyword` | string | No | Search across email and label fields |
| `page` | integer | No | Page number (0-indexed, default: 0) |
| `size` | integer | No | Page size (default: 10) |
| `sortDirection` | string | No | Sort direction: "asc" or "desc" (default: "desc") |

**Example Request:**
```
GET /api/accommodation-emails?emailType=RESERVATIONS&isActive=true&page=0&size=10&sortDirection=desc
```

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation emails retrieved successfully",
  "data": {
    "emails": [
      {
        "id": "encoded-email-id-1",
        "accommodationId": "encoded-accommodation-id-1",
        "accommodationName": "Serengeti Serena Safari Lodge",
        "email": "reservations@serengetiserena.com",
        "emailType": "RESERVATIONS",
        "emailTypeDisplayName": "Reservations",
        "emailTypeDescription": "Booking and reservation inquiries",
        "isPrimary": true,
        "isActive": true,
        "label": "Main Reservations",
        "createdAt": "2025-01-06T10:30:00",
        "updatedAt": "2025-01-06T10:30:00"
      },
      {
        "id": "encoded-email-id-2",
        "accommodationId": "encoded-accommodation-id-2",
        "accommodationName": "Ngorongoro Crater Lodge",
        "email": "bookings@ngorongorolodge.com",
        "emailType": "RESERVATIONS",
        "emailTypeDisplayName": "Reservations",
        "emailTypeDescription": "Booking and reservation inquiries",
        "isPrimary": true,
        "isActive": true,
        "label": null,
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
- The `keyword` parameter searches across email and label fields

---

### 6. Get Emails for Specific Accommodation
**GET** `/api/accommodation-emails/accommodation/{accommodationId}`

Retrieves all emails for a specific accommodation with optional filtering, pagination, and sorting.

**Permission Required:** `PERM_READ_ACCOMMODATION_EMAIL`

**Path Parameters:**
- `accommodationId` (string) - **Required** encoded accommodation ID

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | No | Filter by email address (partial match) |
| `emailType` | EmailType | No | Filter by email type |
| `isPrimary` | boolean | No | Filter by primary status |
| `isActive` | boolean | No | Filter by active status |
| `label` | string | No | Filter by label (partial match) |
| `keyword` | string | No | Search across email and label fields |
| `page` | integer | No | Page number (0-indexed, default: 0) |
| `size` | integer | No | Page size (default: 10) |
| `sortDirection` | string | No | Sort direction: "asc" or "desc" (default: "desc") |

**Example Request:**
```
GET /api/accommodation-emails/accommodation/encoded-accommodation-id?isActive=true&page=0&size=10
```

**Response:**
```json
{
  "status": 200,
  "message": "Accommodation emails retrieved successfully",
  "data": {
    "emails": [
      {
        "id": "encoded-email-id-1",
        "accommodationId": "encoded-accommodation-id",
        "accommodationName": "Serengeti Serena Safari Lodge",
        "email": "reservations@serengetiserena.com",
        "emailType": "RESERVATIONS",
        "emailTypeDisplayName": "Reservations",
        "emailTypeDescription": "Booking and reservation inquiries",
        "isPrimary": true,
        "isActive": true,
        "label": "Main Reservations",
        "createdAt": "2025-01-06T10:30:00",
        "updatedAt": "2025-01-06T10:30:00"
      },
      {
        "id": "encoded-email-id-2",
        "accommodationId": "encoded-accommodation-id",
        "accommodationName": "Serengeti Serena Safari Lodge",
        "email": "info@serengetiserena.com",
        "emailType": "GENERAL",
        "emailTypeDisplayName": "General",
        "emailTypeDescription": "General inquiries and information",
        "isPrimary": false,
        "isActive": true,
        "label": "General Inquiries",
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
- This endpoint is specifically designed for retrieving emails in the context of a single accommodation

---

## Error Codes

| Error Code | HTTP Status | Description |
|-----------|-------------|-------------|
| `INVALID_EMAIL_ID` | 400 | The provided email ID is invalid or cannot be decoded |
| `INVALID_ACCOMMODATION_ID` | 400 | The provided accommodation ID is invalid or cannot be decoded |
| `ACCOMMODATION_NOT_FOUND` | 404 | The specified accommodation does not exist |
| `ACCOMMODATION_EMAIL_NOT_FOUND` | 404 | The specified email does not exist |
| `DUPLICATE_EMAIL` | 400 | Email address already exists |
| `ACCOMMODATION_EMAIL_CREATE_FAILED` | 500 | Failed to create accommodation email |
| `ACCOMMODATION_EMAIL_UPDATE_FAILED` | 500 | Failed to update accommodation email |
| `ACCOMMODATION_EMAILS_DELETE_FAILED` | 500 | Failed to delete accommodation emails |
| `ACCOMMODATION_EMAIL_FETCH_FAILED` | 500 | Failed to fetch accommodation email |
| `ACCOMMODATION_EMAILS_FETCH_FAILED` | 500 | Failed to fetch accommodation emails |

---

## Important Notes

1. **ID Obfuscation**: All IDs in requests and responses are obfuscated for security. Never expose or use raw database IDs.

2. **Audit Logging**: All create, update, and delete operations are automatically logged for audit purposes.

3. **Enum Display Fields**: For user-friendly display, email type fields include additional information:
   - `emailType` - Enum value (e.g., "RESERVATIONS")
   - `emailTypeDisplayName` - Human-readable name (e.g., "Reservations")
   - `emailTypeDescription` - Detailed description (e.g., "Booking and reservation inquiries")

4. **Duplicate Email Prevention**: The system prevents duplicate email addresses across all accommodations.

5. **Two Get All Endpoints**:
   - `/api/accommodation-emails` - Returns emails from all accommodations (accommodationId is optional filter)
   - `/api/accommodation-emails/accommodation/{accommodationId}` - Returns emails for a specific accommodation (accommodationId is required)

6. **Pagination**: All list endpoints support pagination with default values (page=0, size=10). Adjust these parameters based on your needs.
