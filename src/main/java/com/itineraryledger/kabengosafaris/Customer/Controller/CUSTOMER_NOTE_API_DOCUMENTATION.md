# Customer Note API Documentation

## Overview

The Customer Note API provides endpoints for managing customer notes and communication history. Notes can track phone calls, emails, meetings, follow-ups, complaints, and general observations about customers.

## Base URL

```
/api/customer-notes
```

## Authentication

All endpoints require authentication and appropriate permissions:
- `PERM_CREATE_CUSTOMER_NOTE` - Create customer notes
- `PERM_READ_CUSTOMER_NOTE` - View customer notes
- `PERM_UPDATE_CUSTOMER_NOTE` - Update customer notes and mark follow-ups complete
- `PERM_DELETE_CUSTOMER_NOTE` - Delete customer notes

---

## Note Types

| Type | Display Name | Description |
|------|--------------|-------------|
| PHONE_CALL | Phone Call | Notes from a phone conversation |
| EMAIL | Email | Summary of email communication |
| MEETING | Meeting | Notes from an in-person or virtual meeting |
| GENERAL | General | General observations or notes |
| FOLLOW_UP | Follow-up | Follow-up reminder or action item |
| COMPLAINT | Complaint | Customer complaint or issue |
| FEEDBACK | Feedback | Customer feedback or suggestion |

---

## Note Priorities

| Priority | Display Name | Description |
|----------|--------------|-------------|
| LOW | Low | Low priority - no immediate action required |
| NORMAL | Normal | Normal priority - standard handling |
| HIGH | High | High priority - requires prompt attention |
| URGENT | Urgent | Urgent - requires immediate attention |

---

## Endpoints

### 1. Create Customer Note

Creates a new note for a customer.

**Endpoint:** `POST /api/customer-notes`

**Permission:** `PERM_CREATE_CUSTOMER_NOTE`

**Request Body:**

```json
{
  "customerId": "abc123xyz",
  "noteType": "PHONE_CALL",
  "subject": "Discussed safari package options",
  "content": "Customer is interested in a 5-day Serengeti safari. Prefers luxury accommodations. Will follow up with a quote by Friday.",
  "followUpDate": "2024-01-20T10:00:00",
  "isPinned": false,
  "isPrivate": false,
  "priority": "HIGH"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customerId | String | Yes | Obfuscated customer ID |
| noteType | NoteType | No | Type of note (default: GENERAL) |
| subject | String | No | Brief subject/title (max 200 chars) |
| content | String | Yes | Note content (required) |
| followUpDate | DateTime | No | When to follow up |
| isPinned | Boolean | No | Pin to top of notes list (default: false) |
| isPrivate | Boolean | No | Only visible to creator (default: false) |
| priority | NotePriority | No | Note priority (default: NORMAL) |

**Success Response (201):**

```json
{
  "status": 201,
  "message": "Customer note created successfully",
  "data": {
    "id": "note123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "noteType": "PHONE_CALL",
    "noteTypeDisplayName": "Phone Call",
    "noteTypeDescription": "Notes from a phone conversation",
    "subject": "Discussed safari package options",
    "content": "Customer is interested in a 5-day Serengeti safari...",
    "followUpDate": "2024-01-20T10:00:00",
    "followUpCompleted": false,
    "followUpCompletedAt": null,
    "isFollowUpOverdue": false,
    "isPinned": false,
    "isPrivate": false,
    "priority": "HIGH",
    "priorityDisplayName": "High",
    "priorityDescription": "High priority - requires prompt attention",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Responses:**

| Code | Error Code | Description |
|------|------------|-------------|
| 400 | INVALID_CUSTOMER_ID | Invalid or malformed customer ID |
| 400 | CUSTOMER_NOT_FOUND | Customer does not exist |
| 400 | INVALID_CONTENT | Note content is empty |
| 400 | SUBJECT_TOO_LONG | Subject exceeds 200 characters |

---

### 2. Update Customer Note

Updates an existing customer note.

**Endpoint:** `PUT /api/customer-notes/{idObfuscated}`

**Permission:** `PERM_UPDATE_CUSTOMER_NOTE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated note ID |

**Request Body:**

All fields are optional. Only provided fields will be updated.

```json
{
  "noteType": "FOLLOW_UP",
  "subject": "Updated: Safari package follow-up",
  "content": "Customer confirmed interest. Sent quotation via email.",
  "isPinned": true,
  "isPrivate": false,
  "priority": "NORMAL"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noteType | NoteType | No | New note type |
| subject | String | No | New subject |
| content | String | No | New content (cannot be empty if provided) |
| isPinned | Boolean | No | Pin status |
| isPrivate | Boolean | No | Private status |
| priority | NotePriority | No | New priority |

**Notes:**
- At least one field must be provided for update
- Follow-up status cannot be updated through this endpoint - use the dedicated "Mark Follow-Up as Completed" endpoint

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer note updated successfully",
  "data": {
    "id": "note123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "noteType": "FOLLOW_UP",
    "noteTypeDisplayName": "Follow-up",
    "subject": "Updated: Safari package follow-up",
    "content": "Customer confirmed interest. Sent quotation via email.",
    "followUpDate": "2024-01-25T14:00:00",
    "followUpCompleted": false,
    "followUpCompletedAt": null,
    "isFollowUpOverdue": false,
    "isPinned": true,
    "isPrivate": false,
    "priority": "NORMAL",
    "priorityDisplayName": "Normal",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

**Error Responses:**

| Code | Error Code | Description |
|------|------------|-------------|
| 400 | INVALID_NOTE_ID | Invalid or malformed note ID |
| 400 | INVALID_CONTENT | Note content is empty |
| 400 | SUBJECT_TOO_LONG | Subject exceeds 200 characters |
| 400 | NO_FIELDS_TO_UPDATE | At least one field must be provided for update |
| 404 | CUSTOMER_NOTE_NOT_FOUND | Customer note not found |

---

### 3. Mark Follow-Up as Completed

Marks a note's follow-up as completed.

**Endpoint:** `PUT /api/customer-notes/{idObfuscated}/complete-follow-up`

**Permission:** `PERM_UPDATE_CUSTOMER_NOTE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated note ID |

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Follow-up marked as completed successfully",
  "data": {
    "id": "note123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "followUpDate": "2024-01-20T10:00:00",
    "followUpCompleted": true,
    "followUpCompletedAt": "2024-01-20T09:45:00",
    "isFollowUpOverdue": false,
    "updatedAt": "2024-01-20T09:45:00"
  }
}
```

**Error Responses:**

| Code | Error Code | Description |
|------|------------|-------------|
| 400 | INVALID_NOTE_ID | Invalid or malformed note ID |
| 400 | NO_FOLLOW_UP_DATE | Note does not have a follow-up date |
| 400 | FOLLOW_UP_ALREADY_COMPLETED | Follow-up is already completed |
| 404 | CUSTOMER_NOTE_NOT_FOUND | Customer note not found |

---

### 4. Delete Customer Notes

Deletes one or more customer notes.

**Endpoint:** `DELETE /api/customer-notes`

**Permission:** `PERM_DELETE_CUSTOMER_NOTE`

**Request Body:**

```json
["note123abc", "note456def", "note789ghi"]
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "3 customer note(s) deleted successfully",
  "data": null
}
```

**Error Responses:**

| Code | Error Code | Description |
|------|------------|-------------|
| 400 | INVALID_NOTE_ID | One or more invalid note IDs |
| 404 | CUSTOMER_NOTE_NOT_FOUND | One or more notes not found |

---

### 5. Get Customer Note by ID

Retrieves a single customer note by its obfuscated ID.

**Endpoint:** `GET /api/customer-notes/{idObfuscated}`

**Permission:** `PERM_READ_CUSTOMER_NOTE`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| idObfuscated | String | Obfuscated note ID |

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer note retrieved successfully",
  "data": {
    "id": "note123abc",
    "customerId": "abc123xyz",
    "customerDisplayName": "John Doe",
    "noteType": "PHONE_CALL",
    "noteTypeDisplayName": "Phone Call",
    "noteTypeDescription": "Notes from a phone conversation",
    "subject": "Discussed safari package options",
    "content": "Customer is interested in a 5-day Serengeti safari...",
    "followUpDate": "2024-01-20T10:00:00",
    "followUpCompleted": false,
    "followUpCompletedAt": null,
    "isFollowUpOverdue": true,
    "isPinned": false,
    "isPrivate": false,
    "priority": "HIGH",
    "priorityDisplayName": "High",
    "priorityDescription": "High priority - requires prompt attention",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Error Response (404):**

```json
{
  "status": 404,
  "message": "Customer note not found",
  "errorCode": "CUSTOMER_NOTE_NOT_FOUND"
}
```

---

### 6. Get All Customer Notes

Retrieves a paginated list of customer notes with optional filtering.

**Endpoint:** `GET /api/customer-notes`

**Permission:** `PERM_READ_CUSTOMER_NOTE`

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| customerId | String | null | Filter by customer ID (optional) |
| noteType | NoteType | null | Filter by note type |
| subject | String | null | Filter by subject (partial match) |
| isPinned | Boolean | null | Filter by pinned status |
| isPrivate | Boolean | null | Filter by private status |
| priority | NotePriority | null | Filter by priority |
| followUpCompleted | Boolean | null | Filter by follow-up completed status |
| pendingFollowUpsOnly | Boolean | null | Show only notes with pending follow-ups |
| overdueFollowUpsOnly | Boolean | null | Show only notes with overdue follow-ups |
| keyword | String | null | Search across subject and content |
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 10 | Page size |
| sortDirection | String | desc | Sort direction (asc/desc) |

**Filter Notes:**
- `pendingFollowUpsOnly`: When true, returns only notes with `followUpDate` set and `followUpCompleted` = false
- `overdueFollowUpsOnly`: When true, returns only notes with `followUpDate` in the past and `followUpCompleted` = false
- `customerId`: Use this parameter to filter notes for a specific customer

**Example Requests:**

```
GET /api/customer-notes?noteType=PHONE_CALL&priority=HIGH&followUpCompleted=false&page=0&size=20

GET /api/customer-notes?customerId=abc123xyz&isPinned=true

GET /api/customer-notes?customerId=abc123xyz&pendingFollowUpsOnly=true&sortDirection=asc

GET /api/customer-notes?customerId=abc123xyz&overdueFollowUpsOnly=true
```

**Success Response (200):**

```json
{
  "status": 200,
  "message": "Customer notes retrieved successfully",
  "data": {
    "notes": [
      {
        "id": "note123abc",
        "customerId": "abc123xyz",
        "customerDisplayName": "John Doe",
        "noteType": "PHONE_CALL",
        "noteTypeDisplayName": "Phone Call",
        "noteTypeDescription": "Notes from a phone conversation",
        "subject": "Discussed safari package options",
        "content": "Customer is interested in a 5-day Serengeti safari...",
        "followUpDate": "2024-01-20T10:00:00",
        "followUpCompleted": false,
        "followUpCompletedAt": null,
        "isFollowUpOverdue": true,
        "isPinned": false,
        "isPrivate": false,
        "priority": "HIGH",
        "priorityDisplayName": "High",
        "priorityDescription": "High priority - requires prompt attention",
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 50,
    "totalPages": 5,
    "pageSize": 10
  }
}
```

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request

```json
{
  "status": 400,
  "message": "Validation error message",
  "errorCode": "ERROR_CODE"
}
```

Common error codes:
- `INVALID_NOTE_ID` - Invalid obfuscated note ID
- `INVALID_CUSTOMER_ID` - Invalid customer ID
- `INVALID_CONTENT` - Note content is empty
- `SUBJECT_TOO_LONG` - Subject exceeds 200 characters
- `NO_FIELDS_TO_UPDATE` - No fields provided for update
- `NO_FOLLOW_UP_DATE` - Note has no follow-up date
- `FOLLOW_UP_ALREADY_COMPLETED` - Follow-up already marked complete

### 401 Unauthorized

```json
{
  "status": 401,
  "message": "Authentication required",
  "errorCode": "UNAUTHORIZED"
}
```

### 403 Forbidden

```json
{
  "status": 403,
  "message": "Access denied",
  "errorCode": "FORBIDDEN"
}
```

### 404 Not Found

```json
{
  "status": 404,
  "message": "Customer note not found",
  "errorCode": "CUSTOMER_NOTE_NOT_FOUND"
}
```

### 500 Internal Server Error

```json
{
  "status": 500,
  "message": "An unexpected error occurred",
  "errorCode": "INTERNAL_ERROR"
}
```

---

## Related APIs

- [Customer API](./CUSTOMER_API_DOCUMENTATION.md) - Manage customers
- [Customer Email API](./CUSTOMER_EMAIL_API_DOCUMENTATION.md) - Manage customer emails
- [Customer Phone API](./CUSTOMER_PHONE_API_DOCUMENTATION.md) - Manage customer phones
