# Email Account Signature API Documentation

## Overview
The Email Account Signature API provides endpoints for managing email signatures associated with email accounts. Signatures support variable substitution for dynamic content, allowing senders to customize signature content with variables like sender name, account name, current date, and time. Each email account can have multiple signatures with only one set as default.

**System Default Signature:**
- When an email account is created, a system default signature is automatically generated
- System default signatures have `isSystemDefault=true` and cannot be deleted
- They can be modified like regular signatures
- Users can restore them to the original template using the restore endpoint
- System default signatures are only deleted when the email account itself is deleted

---

## Base URL
```
/api/email-accounts/{emailAccountId}/signatures
```

---

## Data Transfer Objects (DTOs)

### 1. CreateEmailAccountSignatureDTO (Request)
Used when creating a new email signature. Required fields: emailAccountId, name, and content.

```json
{
  "emailAccountId": "string (required - obfuscated email account ID)",
  "name": "string (required - unique signature name, NOT derived from email account)",
  "description": "string (optional - purpose of signature)",
  "content": "string (required - HTML/text content with variable placeholders)",
  "variables": [
    {
      "name": "string (variable name, e.g., senderName)",
      "defaultValue": "string (fallback value if not provided)",
      "description": "string (human-readable description)",
      "isRequired": "boolean (default: false)"
    }
  ],
  "isDefault": "boolean (default: false - makes this the default signature)",
  "enabled": "boolean (default: true - signature is active)"
}
```

**Example:**
```json
{
  "emailAccountId": "encoded_id_xyz",
  "name": "Sales_Team_Standard",
  "description": "Main sales team signature",
  "content": "<div style=\"font-family: Arial; color: #333;\"><p>Best regards,</p><p>{senderName}</p><p>{userAccountName}</p><p>Date: {currentDate}</p></div>",
  "variables": [
    {
      "name": "senderName",
      "defaultValue": "Sales Representative",
      "description": "Name of the person sending the email"
    },
    {
      "name": "userAccountName",
      "defaultValue": "Sales Team",
      "description": "Email account name"
    }
  ],
  "isDefault": true,
  "enabled": true
}
```

---

### 2. UpdateEmailAccountSignatureDTO (Request)
Used when updating an email signature. All fields are optional - only include fields you want to update.

```json
{
  "name": "string (optional - must be unique per account, renames file on disk)",
  "description": "string (optional)",
  "content": "string (optional - updates signature file)",
  "variables": [
    {
      "name": "string",
      "defaultValue": "string",
      "description": "string",
      "isRequired": "boolean"
    }
  ],
  "isDefault": "boolean (optional - sets as default or removes default)",
  "enabled": "boolean (optional - enable or disable signature)"
}
```

**Example - Update Name:**
```json
{
  "name": "Updated_Signature_Name"
}
```

**Example - Update Content:**
```json
{
  "content": "<div>Updated signature content with {senderName}</div>"
}
```

**Example - Update Name and Content:**
```json
{
  "name": "New_Sales_Signature",
  "content": "<div>New content with {senderName}</div>"
}
```

**Example - Set as Default:**
```json
{
  "isDefault": true
}
```

**Example - Toggle Enabled Status:**
```json
{
  "enabled": false
}
```

---

### 3. EmailAccountSignatureDTO (Response)
Returned in API responses. Contains all signature information with obfuscated IDs.

```json
{
  "id": "string (obfuscated signature ID)",
  "emailAccountId": "string (obfuscated email account ID)",
  "name": "string (unique signature name provided during creation)",
  "description": "string",
  "fileName": "string (filename stored on disk)",
  "isDefault": "boolean",
  "enabled": "boolean",
  "isSystemDefault": "boolean (true if system-generated, cannot be deleted)",
  "variables": [
    {
      "name": "string",
      "defaultValue": "string",
      "description": "string",
      "isRequired": "boolean"
    }
  ],
  "fileSize": "long (bytes)",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)",
}
```

**Example Response:**
```json
{
  "id": "encoded_sig_123",
  "emailAccountId": "encoded_id_xyz",
  "name": "Sales_Team_Standard",
  "description": "Main sales team signature",
  "fileName": "support_Sales_Team_Standard_1732800645000.html",
  "isDefault": true,
  "enabled": true,
  "variables": [
    {
      "name": "senderName",
      "defaultValue": "Sales Representative",
      "description": "Name of the person sending the email",
      "isRequired": false
    },
    {
      "name": "userAccountName",
      "defaultValue": "Sales Team",
      "description": "Email account name",
      "isRequired": false
    }
  ],
  "fileSize": 512,
  "createdAt": "2025-11-28T10:30:45",
  "updatedAt": "2025-11-28T10:30:45"
}
```

---

### 4. SignatureVariable (Variable Definition)
Represents a placeholder variable in a signature.

```json
{
  "name": "string (variable name without braces, e.g., 'senderName')",
  "defaultValue": "string (value used if not provided at runtime)",
  "description": "string (human-readable description)",
  "isRequired": "boolean (default: false)"
}
```

**Predefined Variables:**

The system automatically supports these predefined variables:

| Variable | Description | Default Value | Example |
|----------|-------------|----------------|---------|
| `{senderName}` | Name of the person sending the email | "Unknown Sender" | John Doe |
| `{userAccountName}` | Email account name/identifier | "Sales" | Support Team |
| `{currentDate}` | Current date in yyyy-MM-dd format | "N/A" | 2025-11-28 |
| `{currentTime}` | Current time in HH:mm:ss format | "N/A" | 10:30:45 |

Custom variables can be added with any name and default values.

---

## API Response Wrapper

All responses follow a standard format:

```json
{
  "success": "boolean",
  "statusCode": "integer (HTTP status)",
  "message": "string",
  "data": "object or array (optional)",
  "errorCode": "string (only on errors)",
  "timestamp": "datetime (ISO 8601)"
}
```

---

## Endpoints

### 1. Create Email Signature
**POST** `/api/email-accounts/{emailAccountId}/signatures`

Creates a new email signature for an email account. The signature name must be provided and must be unique per email account.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
```

**Request Body:**
```json
{
  "emailAccountId": "encoded_id_xyz",
  "name": "Sales_Team_Standard",
  "description": "Main sales team signature",
  "content": "<div>Best regards, {senderName}</div>",
  "variables": [
    {
      "name": "senderName",
      "defaultValue": "Sales Representative",
      "description": "Sender name"
    }
  ],
  "isDefault": true,
  "enabled": true
}
```

**Success Response (201):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Email signature created successfully",
  "data": {
    "id": "encoded_sig_123",
    "emailAccountId": "encoded_id_xyz",
    "name": "Sales_Team_Standard",
    "description": "Main sales team signature",
    "fileName": "support_Sales_Team_Standard_1732800645000.html",
    "isDefault": true,
    "enabled": true,
    "fileSize": 512,
    "createdAt": "2025-11-28T10:30:45",
    "updatedAt": "2025-11-28T10:30:45"
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

**Error Response (400 - Validation):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "name",
      "message": "Signature name is required"
    },
    {
      "field": "content",
      "message": "Signature content is required"
    }
  ],
  "timestamp": "2025-11-28T10:30:45"
}
```

**Error Response (400 - Duplicate Name):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Signature with this name already exists for this account",
  "errorCode": "SIGNATURE_ALREADY_EXISTS",
  "timestamp": "2025-11-28T10:30:45"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Email account not found",
  "errorCode": "EMAIL_ACCOUNT_NOT_FOUND",
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 2. Get All Signatures
**GET** `/api/email-accounts/{emailAccountId}/signatures`

Retrieves a paginated list of signatures for an email account with optional filtering and sorting.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
```

**Query Parameters:**
```
page=0 (default, 0-indexed)
size=10 (default)

Filters:
enabled=true/false (optional - filter by enabled status)
isDefault=true/false (optional - filter by default status)

Sorting:
sortDir=asc/desc (optional, default: desc - sort by creation date)
```

**Example Requests:**
```
# Get all signatures for an account
GET /api/email-accounts/encoded_id_xyz/signatures?page=0&size=10

# Get only enabled signatures
GET /api/email-accounts/encoded_id_xyz/signatures?enabled=true&sortDir=desc

# Get default signature
GET /api/email-accounts/encoded_id_xyz/signatures?isDefault=true

# Get enabled default signatures with custom page size
GET /api/email-accounts/encoded_id_xyz/signatures?enabled=true&isDefault=true&page=0&size=5&sortDir=asc
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Signatures retrieved successfully",
  "data": {
    "signatures": [
      {
        "id": "encoded_sig_1",
        "emailAccountId": "encoded_id_xyz",
        "name": "sales_email_signature",
        "description": "Main sales team signature",
        "fileName": "sales_email_signature_1732800645000.html",
        "isDefault": true,
        "enabled": true,
        "variables": [
          {
            "name": "senderName",
            "defaultValue": "Sales Representative",
            "description": "Sender name"
          }
        ],
        "fileSize": 512,
        "createdAt": "2025-11-28T10:30:45",
        "updatedAt": "2025-11-28T10:30:45"
      },
      {
        "id": "encoded_sig_2",
        "emailAccountId": "encoded_id_xyz",
        "name": "support_email_signature",
        "description": "Support team signature",
        "fileName": "support_email_signature_1732800700000.html",
        "isDefault": false,
        "enabled": true,
        "variables": [],
        "fileSize": 256,
        "createdAt": "2025-11-28T10:31:00",
        "updatedAt": "2025-11-28T10:31:00"
      }
    ],
    "currentPage": 0,
    "totalItems": 2,
    "totalPages": 1
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 3. Get Single Signature
**GET** `/api/email-accounts/{emailAccountId}/signatures/{signatureId}`

Retrieves details of a single signature by ID.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
signatureId = obfuscated signature ID (string)
```

**Example Request:**
```
GET /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Signature retrieved successfully",
  "data": {
    "id": "encoded_sig_123",
    "emailAccountId": "encoded_id_xyz",
    "name": "sales_email_signature",
    "description": "Main sales team signature",
    "fileName": "sales_email_signature_1732800645000.html",
    "isDefault": true,
    "enabled": true,
    "variables": [
      {
        "name": "senderName",
        "defaultValue": "Sales Representative",
        "description": "Sender name"
      }
    ],
    "fileSize": 512,
    "createdAt": "2025-11-28T10:30:45",
    "updatedAt": "2025-11-28T10:30:45",
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Signature not found",
  "errorCode": "SIGNATURE_NOT_FOUND",
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 4. Get Signature Preview
**GET** `/api/email-accounts/{emailAccountId}/signatures/{signatureId}/preview`

Retrieves signature content with variable substitution. Query parameters provide variable values for replacement.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
signatureId = obfuscated signature ID (string)
```

**Query Parameters:**
```
Variable names (all optional):
senderName=string (replaces {senderName})
userAccountName=string (replaces {userAccountName})
currentDate=string (replaces {currentDate})
currentTime=string (replaces {currentTime})
[customVariable]=string (replaces {customVariable})
```

**Example Requests:**
```
# Get preview with sender name
GET /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/preview?senderName=John%20Doe

# Get preview with multiple variables
GET /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/preview?senderName=John%20Doe&userAccountName=Sales%20Team&currentDate=2025-11-28

# Get preview with all available variables
GET /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/preview?senderName=John%20Doe&userAccountName=Sales&currentDate=2025-11-28&currentTime=10:30:45
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Signature preview retrieved successfully",
  "data": {
    "id": "encoded_sig_123",
    "name": "sales_email_signature",
    "signature": "<div style=\"font-family: Arial; color: #333;\"><p>Best regards,</p><p>John Doe</p><p>Sales Team</p><p>Date: 2025-11-28</p></div>",
    "variables": [
      {
        "name": "senderName",
        "defaultValue": "Sales Representative",
        "description": "Sender name"
      },
      {
        "name": "userAccountName",
        "defaultValue": "Sales Team",
        "description": "Email account name"
      }
    ]
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 5. Get Signature Content (for Editor)
**GET** `/api/email-accounts/{emailAccountId}/signatures/{signatureId}/content`

Retrieves full signature details including raw HTML content for WYSIWYG editor usage.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
signatureId = obfuscated signature ID (string)
```

**Example Request:**
```
GET /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/content
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Signature content retrieved successfully",
  "data": {
    "id": "encoded_sig_123",
    "emailAccountId": "encoded_id_xyz",
    "name": "sales_email_signature",
    "description": "Main sales team signature",
    "fileName": "sales_email_signature_1732800645000.html",
    "content": "<div style=\"font-family: Arial; color: #333;\"><p>Best regards,</p><p>{senderName}</p><p>{userAccountName}</p><p>Date: {currentDate}</p></div>",
    "isDefault": true,
    "enabled": true,
    "variables": [
      {
        "name": "senderName",
        "defaultValue": "Sales Representative",
        "description": "Sender name"
      },
      {
        "name": "userAccountName",
        "defaultValue": "Sales Team",
        "description": "Email account name"
      }
    ],
    "fileSize": 512,
    "createdAt": "2025-11-28T10:30:45",
    "updatedAt": "2025-11-28T10:30:45",
  },
  "timestamp": "2025-11-28T10:30:45"
}
```

---

### 6. Download/Preview Signature HTML File by ID
**GET** `/api/email-accounts/{emailAccountId}/signatures/{signatureId}/download`

Downloads or displays the raw HTML signature file in the browser.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
signatureId = obfuscated signature ID (string)
```

**Query Parameters:**
```
download = boolean (optional, default: false)
  - false: Display inline in browser
  - true: Force download as file
```

**Example Requests:**
```
# Preview in browser
GET /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/download

# Force download
GET /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/download?download=true
```

**Success Response (200):**
```
Content-Type: text/html; charset=UTF-8
Content-Disposition: inline; filename="sales_email_signature.html"
  OR
Content-Disposition: attachment; filename="sales_email_signature.html"

<div style="font-family: Arial; color: #333;">
  <p>Best regards,</p>
  <p>{senderName}</p>
  <p>{userAccountName}</p>
  <p>Date: {currentDate}</p>
</div>
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Signature not found",
  "errorCode": "SIGNATURE_NOT_FOUND",
  "timestamp": "2025-11-28T10:35:00"
}
```

---

### 7. Get Signature HTML File by Filename
**GET** `/api/email-accounts/{emailAccountId}/signatures/files/{fileName}`

Retrieves the raw HTML signature file using the filename directly. This is useful when you have the `fileName` property from a signature DTO and want to access the HTML file directly.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
fileName = the signature filename (string)
  Example: "developerks_developerks_1764855656760.html"
```

**Query Parameters:**
```
download = boolean (optional, default: false)
  - false: Display inline in browser
  - true: Force download as file
```

**Example Requests:**
```
# Preview in browser
GET /api/email-accounts/encoded_id_xyz/signatures/files/developerks_developerks_1764855656760.html

# Force download
GET /api/email-accounts/encoded_id_xyz/signatures/files/developerks_developerks_1764855656760.html?download=true
```

**Use Case:**
1. Get signature content: `GET /api/email-accounts/{id}/signatures/{signatureId}/content`
2. Response includes: `{"fileName": "developerks_developerks_1764855656760.html", ...}`
3. Use fileName to access HTML: `GET /api/email-accounts/{id}/signatures/files/developerks_developerks_1764855656760.html`

**Success Response (200):**
```
Content-Type: text/html; charset=UTF-8
Content-Disposition: inline; filename="developerks_developerks_1764855656760.html"
  OR
Content-Disposition: attachment; filename="developerks_developerks_1764855656760.html"

<div style="font-family: Arial; color: #333;">
  <p>Best regards,</p>
  <p>{senderName}</p>
  <p>{userAccountName}</p>
  <p>Date: {currentDate}</p>
</div>
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Signature file not found",
  "errorCode": "SIGNATURE_FILE_NOT_FOUND",
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (500):**
```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to read signature file",
  "errorCode": "SIGNATURE_FILE_READ_FAILED",
  "timestamp": "2025-11-28T10:35:00"
}
```

---

### 8. Update Signature
**PUT** `/api/email-accounts/{emailAccountId}/signatures/{signatureId}`

Updates specific fields of a signature. Only include fields you want to change. If name or content is updated, the signature file is updated on disk.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
signatureId = obfuscated signature ID (string)
```

**Request Body (Example - Update Name):**
```json
{
  "name": "Updated_Signature_Name"
}
```

**Request Body (Example - Update Content):**
```json
{
  "content": "<div>Updated signature content with {senderName}</div>"
}
```

**Request Body (Example - Update Name and Content):**
```json
{
  "name": "New_Sales_Signature",
  "content": "<div>New content with {senderName}</div>"
}
```

**Request Body (Example - Set as Default):**
```json
{
  "isDefault": true
}
```

**Request Body (Example - Toggle Enabled):**
```json
{
  "enabled": false
}
```

**Request Body (Example - Update Multiple Fields):**
```json
{
  "name": "Professional_Signature",
  "description": "Updated description",
  "content": "<div>Updated content</div>",
  "variables": [
    {
      "name": "senderName",
      "defaultValue": "John",
      "description": "Sender name"
    }
  ],
  "isDefault": true,
  "enabled": true
}
```

**Important Notes:**
- When `name` is provided, it must be unique per email account, and the signature file will be renamed on disk
- When `isDefault=true` is set, all other signatures for this account are automatically set to `isDefault=false`
- Only one signature per account can be set as default
- If content is provided, the signature file content is updated on disk
- Partial updates are supported - only provide fields you want to change

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Signature updated successfully",
  "data": {
    "id": "encoded_sig_123",
    "emailAccountId": "encoded_id_xyz",
    "name": "sales_email_signature",
    "description": "Updated description",
    "fileName": "sales_email_signature_1732800700000.html",
    "isDefault": true,
    "enabled": true,
    "variables": [],
    "fileSize": 512,
    "createdAt": "2025-11-28T10:30:45",
    "updatedAt": "2025-11-28T10:35:00",
  },
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (400 - Duplicate Name):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Signature with this name already exists for this account",
  "errorCode": "SIGNATURE_ALREADY_EXISTS",
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Signature not found",
  "errorCode": "SIGNATURE_NOT_FOUND",
  "timestamp": "2025-11-28T10:35:00"
}
```

---

### 9. Restore System Default Signature
**POST** `/api/email-accounts/{emailAccountId}/signatures/{signatureId}/restore`

Restores a system default signature to its original template. This resets the content, description, and variables back to the default values.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
signatureId = obfuscated signature ID (string)
```

**Validation:**
- Can only restore signatures where isSystemDefault=true
- Returns 400 error if signature is not a system default
- Returns 404 if signature not found
- Signature must belong to the specified email account

**Example Request:**
```
POST /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/restore
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "System default signature restored successfully",
  "data": {
    "id": "encoded_sig_123",
    "emailAccountId": "encoded_id_xyz",
    "name": "Default Signature",
    "description": "System default signature - created automatically",
    "content": "<!DOCTYPE html>...",
    "fileName": "account_name_Default_Signature_1234567890.html",
    "isDefault": true,
    "enabled": true,
    "isSystemDefault": true,
    "variables": [],
    "fileSize": 1024,
    "createdAt": "2025-11-28T10:30:45",
    "updatedAt": "2025-11-28T10:40:00"
  },
  "timestamp": "2025-11-28T10:40:00"
}
```

**Error Response (400 - Not System Default):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Signature is not a system default signature",
  "errorCode": "NOT_SYSTEM_DEFAULT_SIGNATURE",
  "timestamp": "2025-11-28T10:40:00"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Signature not found",
  "errorCode": "SIGNATURE_NOT_FOUND",
  "timestamp": "2025-11-28T10:40:00"
}
```

---

### 10. Delete Single Signature
**DELETE** `/api/email-accounts/{emailAccountId}/signatures/{signatureId}`

Deletes a single signature by ID. The associated signature file is deleted from disk.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
signatureId = obfuscated signature ID (string)
```

**Validation:**
- Cannot delete if the signature is set as default
- Cannot delete if the signature is a system default (isSystemDefault=true)
- Returns 400 error if signature is default or system default
- Returns 404 if signature not found
- Signature must belong to the specified email account

**Example Request:**
```
DELETE /api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Signatures deleted successfully",
  "data": {
    "deletedCount": 1,
    "deletedFileNames": ["sales_team_standard_1732800645000.html"]
  },
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (400 - Is System Default):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete signatures: some are system default",
  "errorCode": "CANNOT_DELETE_SYSTEM_DEFAULT_SIGNATURES",
  "data": {
    "deletedIds": [],
    "message": "Cannot delete any signatures: 1 signature(s) in the list are system default signatures. System default signatures can only be modified, not deleted.",
    "systemDefaultSignatureIds": [3]
  },
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (400 - Is Default):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete signatures: some are set as default",
  "errorCode": "CANNOT_DELETE_DEFAULT_SIGNATURES",
  "data": {
    "deletedIds": [],
    "message": "Cannot delete any signatures: 1 signature(s) in the list are set as default. Please change the default signature first.",
    "defaultSignatureIds": [5]
  },
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Signature not found",
  "errorCode": "SIGNATURE_NOT_FOUND",
  "timestamp": "2025-11-28T10:35:00"
}
```

---

### 11. Delete Multiple Signatures (Batch Delete)
**DELETE** `/api/email-accounts/{emailAccountId}/signatures`

Deletes multiple signatures by batch request. All associated signature files are deleted from disk.

**Path Parameters:**
```
emailAccountId = obfuscated email account ID (string)
```

**Request Body:**
```json
[
  "encoded_sig_1",
  "encoded_sig_2",
  "encoded_sig_3"
]
```

**Validation:**
- If ANY signature in the list is a system default, NO signatures will be deleted
- If ANY signature in the list is set as default, NO signatures will be deleted
- Returns 400 error if any signature is system default or default
- All signatures must belong to the specified email account
- This is an atomic operation - either all valid signatures are deleted, or none are

**Example Request:**
```
DELETE /api/email-accounts/encoded_id_xyz/signatures

Body:
[
  "encoded_sig_1",
  "encoded_sig_2",
  "encoded_sig_3"
]
```

**Success Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Signatures deleted successfully",
  "data": {
    "deletedCount": 3,
    "deletedFileNames": [
      "sales_team_standard_1732800645000.html",
      "support_formal_1732800700000.html",
      "marketing_casual_1732800750000.html"
    ]
  },
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (400 - Has Default Signatures):**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Cannot delete signatures: some are set as default",
  "errorCode": "CANNOT_DELETE_DEFAULT_SIGNATURES",
  "data": {
    "deletedIds": [],
    "message": "Cannot delete any signatures: 1 signature(s) in the list are set as default. Please change the default signature first.",
    "defaultSignatureIds": [5]
  },
  "timestamp": "2025-11-28T10:35:00"
}
```

**Error Response (404):**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Email account not found",
  "errorCode": "EMAIL_ACCOUNT_NOT_FOUND",
  "timestamp": "2025-11-28T10:35:00"
}
```

---

## Error Codes

| Error Code | Status | Description |
|-----------|--------|-------------|
| `VALIDATION_ERROR` | 400 | Input validation failed (missing required fields) |
| `SIGNATURE_ALREADY_EXISTS` | 400 | Signature with this name already exists for the account |
| `CANNOT_DELETE_DEFAULT_SIGNATURES` | 400 | Cannot delete signatures that are set as default |
| `CANNOT_DELETE_SYSTEM_DEFAULT_SIGNATURES` | 400 | Cannot delete system default signatures (can only be modified) |
| `NOT_SYSTEM_DEFAULT_SIGNATURE` | 400 | Operation requires a system default signature |
| `EMAIL_ACCOUNT_NOT_FOUND` | 404 | Email account not found |
| `SIGNATURE_NOT_FOUND` | 404 | Signature not found |
| `SIGNATURE_FILE_NOT_FOUND` | 404 | Signature file not found or doesn't belong to account |
| `FILE_WRITE_ERROR` | 500 | Error writing signature file to disk |
| `FILE_READ_ERROR` | 500 | Error reading signature file from disk |
| `SIGNATURE_FILE_READ_FAILED` | 500 | Failed to read signature file |
| `FILE_DELETE_ERROR` | 500 | Error deleting signature file from disk |
| `SIGNATURES_DELETE_FAILED` | 500 | Failed to delete signatures |
| `SIGNATURE_FILE_RETRIEVE_FAILED` | 500 | Failed to retrieve signature file |
| `SIGNATURE_DOWNLOAD_FAILED` | 500 | Failed to download signature file |
| `SIGNATURE_RESTORE_FAILED` | 500 | Failed to restore system default signature |
| `DATABASE_ERROR` | 500 | Database operation failed |
| `INVALID_PAGE` | 400 | Invalid page number (must be >= 0) |
| `INVALID_SIZE` | 400 | Invalid page size (must be > 0) |

---

## Important Notes

1. **File-Based Storage**: Signatures are stored as HTML/text files in `/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/email-signatures/` directory. The database only stores metadata and file references.

2. **Signature Filename Format**: `{accountName}_{signatureName}_{timestamp}.html`
   - Example: `support_email_Sales_Team_Standard_1732800645000.html`
   - The signature name is the value provided in the `name` field, not derived from the account

3. **Obfuscated IDs**: All IDs (`emailAccountId`, `signatureId`) are obfuscated for security. Use the returned IDs from API responses for subsequent requests.

4. **Default Signature**: Only one signature per email account can be set as default. Setting a signature as default automatically unsets all others.

5. **System Default Signature**:
   - Automatically created when a new email account is created
   - Has `isSystemDefault=true` and `isDefault=true` by default
   - Cannot be deleted - only modified
   - Can be restored to original template using the restore endpoint
   - Template loaded from `src/main/resources/templates/email-signatures/default_signature_template.html`
   - Only deleted when the email account itself is deleted

6. **Delete Protection**:
   - System default signatures cannot be deleted (returns `CANNOT_DELETE_SYSTEM_DEFAULT_SIGNATURES` error)
   - Default signatures cannot be deleted (returns `CANNOT_DELETE_DEFAULT_SIGNATURES` error)
   - If you try to delete a signature that is system default or set as default, or include such signatures in a batch delete request, the entire operation will be rejected with a 400 error
   - To delete a default signature, first set another signature as default

7. **Batch Delete**: The delete endpoint supports both single and batch deletion:
   - Single: `DELETE /api/email-accounts/{emailAccountId}/signatures/{signatureId}`
   - Batch: `DELETE /api/email-accounts/{emailAccountId}/signatures` with a list of signature IDs in the request body
   - If ANY signature in the batch is system default or default, NO signatures will be deleted (atomic operation)

8. **Variable Substitution**:
   - Predefined variables like `{currentDate}` and `{currentTime}` are automatically generated when preview is requested
   - Custom variables use their default values if not provided in the preview request
   - Variables not found in the signature content are ignored

9. **Cascade Deletion**: When an email account is deleted, all associated signatures and their files are automatically deleted from disk.

10. **Pagination**: Default page size is 10 items. Use `page` and `size` parameters to control pagination.

11. **Sorting**: Signatures are sorted by `createdAt` timestamp. Use `sortDir=asc` or `sortDir=desc` to control sort direction.

12. **Partial Updates**: PUT endpoint supports partial updates - only provide fields you want to change. Other fields retain their current values.

13. **Restore System Default**: Use `POST /api/email-accounts/{emailAccountId}/signatures/{signatureId}/restore` to reset a modified system default signature back to its original template.

---

## Example Workflow

### 1. Create a Signature with Variables
```bash
curl -X POST http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures \
  -H "Content-Type: application/json" \
  -d '{
    "emailAccountId": "encoded_id_xyz",
    "name": "Sales_Team_Standard",
    "description": "Sales team signature",
    "content": "<div><p>Best regards,</p><p>{senderName}</p><p>{userAccountName}</p></div>",
    "variables": [
      {
        "name": "senderName",
        "defaultValue": "Sales Representative",
        "description": "Name of the sender"
      },
      {
        "name": "userAccountName",
        "defaultValue": "Sales Team",
        "description": "Email account name"
      }
    ],
    "isDefault": true,
    "enabled": true
  }'
```

### 2. Get All Signatures
```bash
curl -X GET "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures?page=0&size=10&sortDir=desc"
```

### 3. Preview Signature with Variables
```bash
curl -X GET "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/preview?senderName=John%20Doe&userAccountName=Sales"
```

### 4. Update Signature Name
```bash
curl -X PUT http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated_Sales_Signature"
  }'
```

### 5. Update Signature Content
```bash
curl -X PUT http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123 \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<div><p>Updated signature with {senderName}</p></div>"
  }'
```

### 6. Set Another Signature as Default
```bash
curl -X PUT http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_456 \
  -H "Content-Type: application/json" \
  -d '{
    "isDefault": true
  }'
```

### 7. Get Signature Content for Editor
```bash
curl -X GET "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/content"
```

### 8. Download Signature HTML File by ID
```bash
# Preview in browser
curl -X GET "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/download"

# Force download
curl -X GET "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123/download?download=true"
```

### 9. Get Signature HTML File by Filename
```bash
# Preview in browser (after getting fileName from content API)
curl -X GET "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/files/developerks_developerks_1764855656760.html"

# Force download
curl -X GET "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/files/developerks_developerks_1764855656760.html?download=true"
```

### 10. Delete a Single Signature (Non-Default)
```bash
curl -X DELETE "http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures/encoded_sig_123"
```

### 11. Delete Multiple Signatures (Batch)
```bash
curl -X DELETE http://localhost:8080/api/email-accounts/encoded_id_xyz/signatures \
  -H "Content-Type: application/json" \
  -d '[
    "encoded_sig_1",
    "encoded_sig_2",
    "encoded_sig_3"
  ]'
```

---

## Version
API Version: 1.0

Last Updated: 2025-12-21
