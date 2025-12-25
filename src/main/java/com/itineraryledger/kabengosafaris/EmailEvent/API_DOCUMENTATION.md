# Email Event & Template API Documentation

## Overview
The Email Event & Template API provides endpoints for managing system-wide email events and their associated templates. Email events represent notification triggers (e.g., USER_REGISTRATION, PASSWORD_RESET) that send emails to users based on specific actions or events in the system.

**Key Features:**
- **Email Events**: System-defined notification events that cannot be created or deleted via API
- **System-Defined Variables**: Each email event has predefined, immutable variables (not user-editable)
- **Email Templates**: Customizable HTML templates using event-defined variable placeholders
- **Template Rendering**: Variables replaced with actual data at email send time
- **Test Email Sending**: Send test emails to authenticated users for template preview and verification
- **System Default Templates**: Auto-created templates that cannot be deleted but can be restored
- **Template Management**: Create custom templates, update content, manage defaults
- **File Storage**: Templates stored as HTML files on disk with database metadata
- **Pagination & Filtering**: Advanced querying with JPA Specification

**System Default Templates:**
- Each email event automatically gets a system default template on initialization
- System default templates have `isSystemDefault=true` and cannot be deleted
- They can be modified like regular templates
- Users can restore them to the original template using the restore endpoint
- Original templates are stored in `src/main/resources/templates/email-templates/`

**Template Protection:**
- System default templates cannot be deleted
- Default enabled templates cannot be deleted (must disable or change default first)
- Deletion operations are atomic: if any template in a batch is protected, none are deleted

---

## Base URLs

### Email Events
```
/api/email-events
```

### Email Templates
```
/api/email-events/{eventId}/templates
```

---

## Data Transfer Objects (DTOs)

### 1. EmailEventDTO (Response)
Returned when retrieving email events.

```json
{
  "id": "string (obfuscated event ID)",
  "name": "string (unique event name, e.g., USER_REGISTRATION)",
  "description": "string (event description)",
  "enabled": "boolean (whether event is active)",
  "variablesJson": "string (JSON array of system-defined variables - IMMUTABLE via API)",
  "templateCount": "number (total templates for this event)",
  "hasSystemDefaultTemplate": "boolean (whether system default exists)",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

**Example:**
```json
{
  "id": "encoded_event_abc123",
  "name": "USER_REGISTRATION",
  "description": "Sent when a new user registers in the system",
  "enabled": true,
  "variablesJson": "[{\"name\":\"username\",\"description\":\"User's username\",\"isRequired\":true},{\"name\":\"activationToken\",\"description\":\"Account activation token (JWT)\",\"isRequired\":true},{\"name\":\"activationLink\",\"description\":\"Full account activation URL with token\",\"isRequired\":true},{\"name\":\"expirationHours\",\"description\":\"Number of hours until activation link expires\",\"isRequired\":true},{\"name\":\"expirationDateTime\",\"description\":\"Exact date and time when activation link expires (ISO format)\",\"isRequired\":true}]",
  "templateCount": 3,
  "hasSystemDefaultTemplate": true,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

---

### 2. UpdateEmailEventDTO (Request)
Used when updating email event settings. All fields are optional.

```json
{
  "description": "string (optional - updated description)",
  "enabled": "boolean (optional - enable/disable event)"
}
```

**Example:**
```json
{
  "description": "Updated: Sent to new users with welcome message",
  "enabled": false
}
```

**Notes:**
- The event `name` cannot be changed as it's used as a system identifier
- The `variablesJson` field is system-defined and CANNOT be modified via API
- Only `description` and `enabled` status can be updated

---

### 3. EmailTemplateDTO (Response)
Returned when retrieving email templates.

```json
{
  "id": "string (obfuscated template ID)",
  "emailEventId": "string (obfuscated event ID)",
  "emailEventName": "string (event name)",
  "name": "string (template name)",
  "description": "string (template description)",
  "fileName": "string (HTML file name on disk)",
  "isDefault": "boolean (whether this is the default template)",
  "isSystemDefault": "boolean (whether this is system-generated)",
  "enabled": "boolean (whether template is active)",
  "fileSize": "number (file size in bytes)",
  "fileSizeFormatted": "string (human-readable size, e.g., '5.2 KB')",
  "content": "string (HTML content - only included when explicitly requested)",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

**Example:**
```json
{
  "id": "encoded_template_xyz789",
  "emailEventId": "encoded_event_abc123",
  "emailEventName": "USER_REGISTRATION",
  "name": "Welcome_Premium_Users",
  "description": "Premium user welcome email with special benefits",
  "fileName": "USER_REGISTRATION_Welcome_Premium_Users_20250115_143000.html",
  "isDefault": false,
  "isSystemDefault": false,
  "enabled": true,
  "fileSize": 2048,
  "fileSizeFormatted": "2.0 KB",
  "createdAt": "2025-01-15T14:30:00",
  "updatedAt": "2025-01-15T14:30:00"
}
```

**Note:** Variables are now defined at the EmailEvent level, not the template level. To see what variables are available for a template, check the parent EmailEvent's `variablesJson` field.

---

### 4. CreateEmailTemplateDTO (Request)
Used when creating a new email template. Required fields: name and content.

```json
{
  "name": "string (required - unique template name per event)",
  "description": "string (optional - template purpose)",
  "content": "string (required - HTML content with {{variableName}} placeholders)",
  "isDefault": "boolean (optional, default: false - set as default)",
  "enabled": "boolean (optional, default: true - template is active)"
}
```

**Example:**
```json
{
  "name": "Welcome_Email_Premium",
  "description": "Premium user welcome email template",
  "content": "<!DOCTYPE html><html><head>...</head><body><h1>Welcome {{username}}!</h1><p>Click here to activate: <a href='{{activationLink}}'>Activate</a></p><p>Expires in {{expirationHours}} hours ({{expirationDateTime}})</p></body></html>",
  "isDefault": false,
  "enabled": true
}
```

**Important:**
- Templates must use variable names defined in the parent EmailEvent's `variablesJson`
- Use `{{variableName}}` syntax for placeholders (e.g., `{{username}}`, `{{activationLink}}`)
- Template creators control placement and styling; the system provides the actual data
- At email send time, placeholders are replaced with actual values

---

### 5. UpdateEmailTemplateDTO (Request)
Used when updating an email template. All fields are optional.

```json
{
  "name": "string (optional - renames template and file)",
  "description": "string (optional)",
  "content": "string (optional - updates HTML content with {{variableName}} placeholders)",
  "isDefault": "boolean (optional - set/unset as default)",
  "enabled": "boolean (optional - enable/disable)"
}
```

**Example - Update Content:**
```json
{
  "content": "<!DOCTYPE html><html><body><h1>Hello {{username}}!</h1><p>Activate here: {{activationLink}}</p></body></html>"
}
```

**Example - Set as Default:**
```json
{
  "isDefault": true
}
```

**Note:** Remember to use the variable names defined in the parent EmailEvent's `variablesJson` when updating content.

---

## Email Event Endpoints

### 1. Get All Email Events
**GET** `/api/email-events`

Retrieves all email events in the system.

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Email events retrieved successfully",
  "data": [
    {
      "id": "encoded_id",
      "name": "USER_REGISTRATION",
      "description": "Sent when a new user registers",
      "enabled": true,
      "templateCount": 2,
      "hasSystemDefaultTemplate": true,
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ]
}
```

---

### 2. Get Email Event by ID
**GET** `/api/email-events/{eventId}`

Retrieves a specific email event by its obfuscated ID.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Email event retrieved successfully",
  "data": {
    "id": "encoded_id",
    "name": "USER_REGISTRATION",
    ...
  }
}
```

**Error Responses:**
- `400` INVALID_EVENT_ID: Invalid event ID format
- `404` EMAIL_EVENT_NOT_FOUND: Event not found

---

### 3. Update Email Event
**PUT** `/api/email-events/{eventId}`

Updates email event settings (description and enabled status only).

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID

**Request Body:**
```json
{
  "description": "Updated description",
  "enabled": false
}
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Email event updated successfully",
  "data": {
    "id": "encoded_id",
    "name": "USER_REGISTRATION",
    "description": "Updated description",
    "enabled": false,
    ...
  }
}
```

**Error Responses:**
- `400` INVALID_EVENT_ID: Invalid event ID format
- `404` EMAIL_EVENT_NOT_FOUND: Event not found

---

## Email Template Endpoints

### 4. Create Email Template
**POST** `/api/email-events/{eventId}/templates`

Creates a new email template for an event.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID

**Request Body:**
```json
{
  "name": "Welcome_Email_Premium",
  "description": "Premium user welcome email",
  "content": "<!DOCTYPE html><html><body><h1>Welcome {{username}}!</h1><a href='{{activationLink}}'>Activate Account</a></body></html>",
  "isDefault": false,
  "enabled": true
}
```

**Response (201 Created):**
```json
{
  "status": 201,
  "message": "Template created successfully",
  "data": {
    "id": "encoded_template_id",
    "emailEventId": "encoded_event_id",
    "name": "Welcome_Email_Premium",
    "fileName": "USER_REGISTRATION_Welcome_Email_Premium_20250115_143000.html",
    ...
  }
}
```

**Validation:**
- Template name must be unique within the event
- Content is required and must not be blank
- If `isDefault=true`, other defaults for the event are automatically cleared

**Error Responses:**
- `400` INVALID_EVENT_ID: Invalid event ID format
- `400` TEMPLATE_ALREADY_EXISTS: Template name already exists for this event
- `404` EMAIL_EVENT_NOT_FOUND: Event not found
- `500` TEMPLATE_FILE_SAVE_FAILED: Failed to save template file

---

### 5. Get All Templates for Event
**GET** `/api/email-events/{eventId}/templates`

Retrieves all templates for an event with pagination, filtering, and sorting.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID

**Query Parameters:**
- `enabled` (boolean, optional): Filter by enabled status
- `isDefault` (boolean, optional): Filter by default status
- `isSystemDefault` (boolean, optional): Filter by system default status
- `name` (string, optional): Filter by name (partial match)
- `page` (number, optional, default: 0): Page number (0-based)
- `size` (number, optional, default: 10): Page size
- `sortDir` (string, optional, default: "desc"): Sort direction ("asc" or "desc")

**Example Request:**
```
GET /api/email-events/encoded_id/templates?enabled=true&page=0&size=10&sortDir=desc
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Templates retrieved successfully",
  "data": {
    "templates": [
      {
        "id": "encoded_id",
        "name": "Welcome_Email_Premium",
        ...
      }
    ],
    "currentPage": 0,
    "totalItems": 15,
    "totalPages": 2
  }
}
```

**Error Responses:**
- `400` INVALID_EVENT_ID: Invalid event ID format
- `400` INVALID_PAGE: Page number cannot be negative
- `400` INVALID_SIZE: Page size must be greater than 0

---

### 6. Get Template by ID
**GET** `/api/email-events/{eventId}/templates/{templateId}`

Retrieves a specific template (without content).

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID
- `templateId` (string, required): Obfuscated template ID

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Template retrieved successfully",
  "data": {
    "id": "encoded_id",
    "name": "Welcome_Email_Premium",
    ...
  }
}
```

**Error Responses:**
- `400` INVALID_ID: Invalid ID format
- `404` TEMPLATE_NOT_FOUND: Template not found

---

### 7. Get Template with Content
**GET** `/api/email-events/{eventId}/templates/{templateId}/content`

Retrieves a template with HTML content included.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID
- `templateId` (string, required): Obfuscated template ID

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Template content retrieved successfully",
  "data": {
    "id": "encoded_id",
    "name": "Welcome_Email_Premium",
    "content": "<!DOCTYPE html>...",
    ...
  }
}
```

**Error Responses:**
- `400` INVALID_ID: Invalid ID format
- `404` TEMPLATE_NOT_FOUND: Template not found
- `500` TEMPLATE_CONTENT_READ_FAILED: Failed to read template file

---

### 8. Get Template as HTML File
**GET** `/api/email-events/{eventId}/templates/{templateId}/file`

Retrieves template content as an HTML file (for download or inline display).

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID
- `templateId` (string, required): Obfuscated template ID

**Query Parameters:**
- `download` (boolean, optional, default: false): If true, downloads file; if false, displays inline

**Example Requests:**
```
GET /api/email-events/encoded_id/templates/encoded_template_id/file
GET /api/email-events/encoded_id/templates/encoded_template_id/file?download=true
```

**Response (200 OK):**
- **Content-Type**: `text/html`
- **Content-Disposition**: `inline; filename="..."` or `attachment; filename="..."`
- **Body**: HTML content

**Error Responses:**
- `400` INVALID_ID: Invalid ID format
- `404` TEMPLATE_NOT_FOUND: Template not found
- `500` TEMPLATE_CONTENT_READ_FAILED: Failed to read template file

---

### 9. Get Template File by Name
**GET** `/api/email-events/{eventId}/templates/file/{fileName}`

Retrieves template content by file name.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID
- `fileName` (string, required): Template file name

**Query Parameters:**
- `download` (boolean, optional, default: false): If true, downloads file

**Example Request:**
```
GET /api/email-events/encoded_id/templates/file/USER_REGISTRATION_Welcome_20250115_143000.html
```

**Response (200 OK):**
- **Content-Type**: `text/html`
- **Body**: HTML content

**Error Responses:**
- `400` INVALID_EVENT_ID: Invalid event ID format
- `404` TEMPLATE_NOT_FOUND: Template not found

---

### 10. Update Template
**PUT** `/api/email-events/{eventId}/templates/{templateId}`

Updates an email template. All fields are optional.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID
- `templateId` (string, required): Obfuscated template ID

**Request Body:**
```json
{
  "name": "Updated_Template_Name",
  "description": "Updated description",
  "content": "<!DOCTYPE html><html><body>Hello {{username}}!</body></html>",
  "isDefault": true,
  "enabled": false
}
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Template updated successfully",
  "data": {
    "id": "encoded_id",
    "name": "Updated_Template_Name",
    ...
  }
}
```

**Validation:**
- If updating name, new name must be unique within the event
- Updating name renames the file on disk
- If setting `isDefault=true`, other defaults are automatically cleared

**Error Responses:**
- `400` INVALID_ID: Invalid ID format
- `400` TEMPLATE_ALREADY_EXISTS: Template name already exists
- `404` TEMPLATE_NOT_FOUND: Template not found
- `500` TEMPLATE_FILE_READ_FAILED: Failed to read existing file
- `500` TEMPLATE_FILE_SAVE_FAILED: Failed to save updated file

---

### 11. Restore System Default Template
**POST** `/api/email-events/{eventId}/templates/{templateId}/restore`

Restores a system default template to its original state from resources.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID
- `templateId` (string, required): Obfuscated template ID

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "System default template restored successfully",
  "data": {
    "id": "encoded_id",
    "name": "System_Default",
    "description": "System default template - created automatically",
    "isSystemDefault": true,
    ...
  }
}
```

**Validation:**
- Only works for templates with `isSystemDefault=true`
- Restores content, description, and variables to original values
- Template file is updated on disk

**Error Responses:**
- `400` INVALID_ID: Invalid ID format
- `400` NOT_SYSTEM_DEFAULT_TEMPLATE: Template is not a system default
- `404` TEMPLATE_NOT_FOUND: Template not found
- `500` TEMPLATE_FILE_UPDATE_FAILED: Failed to update template file

---

### 12. Delete Templates (Batch)
**DELETE** `/api/email-events/{eventId}/templates`

Deletes multiple templates by ID. Atomic operation: if any template is protected, none are deleted.

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID

**Request Body:**
```json
["encoded_template_id_1", "encoded_template_id_2", "encoded_template_id_3"]
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Templates deleted successfully",
  "data": {
    "deletedCount": 3,
    "deletedFileNames": ["file1.html", "file2.html", "file3.html"]
  }
}
```

**Validation:**
- System default templates (`isSystemDefault=true`) cannot be deleted
- Default enabled templates (`isDefault=true` AND `enabled=true`) cannot be deleted
- If any template in the batch is protected, **no templates are deleted** (atomic operation)

**Error Responses:**
- `400` INVALID_EVENT_ID: Invalid event ID format
- `400` NO_TEMPLATE_IDS: No template IDs provided
- `400` CANNOT_DELETE_SYSTEM_DEFAULT_TEMPLATES: Some templates are system defaults
- `400` CANNOT_DELETE_DEFAULT_ENABLED_TEMPLATES: Some templates are default and enabled

---

### 13. Send Test Email
**POST** `/api/email-events/{eventId}/templates/{templateId}/test`

Sends a test email using a specific template to the authenticated user's email address. This endpoint is useful for testing specific email templates during development, previewing how a template will look before setting it as default, verifying template rendering with real variable data, and testing template changes without affecting production emails.

**Important Notes:**
- The email will be sent to the authenticated user's email address (from JWT token)
- Uses the specified template (not the default template)
- Test data appropriate for the event type will be auto-generated
- Each email event type must have its own test implementation
- If a test is not implemented for an event, a 501 error will be returned
- The subject line will be prefixed with `[TEST]` to distinguish test emails
- Test emails use real tokens and links, but are marked as test emails
- Both the event and template must be enabled
- Template must belong to the specified event

**Path Parameters:**
- `eventId` (string, required): Obfuscated email event ID
- `templateId` (string, required): Obfuscated template ID to test

**Authentication:**
- Requires valid JWT token in Authorization header
- Email will be sent to the authenticated user's email

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Test email sent successfully to user@example.com",
  "data": {
    "eventName": "USER_REGISTRATION",
    "templateName": "Welcome_Email_Premium",
    "recipientEmail": "user@example.com",
    "subject": "[TEST] Welcome to Kabengosafaris - Activate Your Account",
    "sentAt": "2025-01-15T14:30:00"
  }
}
```

**Error Responses:**
- `400` EVENT_DISABLED: Email event is currently disabled
- `400` TEMPLATE_DISABLED: Template is currently disabled
- `400` TEMPLATE_EVENT_MISMATCH: Template does not belong to the specified event
- `400` INVALID_REQUEST: Invalid event ID, template ID, or authentication issue
- `404` EMAIL_EVENT_NOT_FOUND: Event not found
- `404` TEMPLATE_NOT_FOUND: Template not found
- `500` CONFIGURATION_ERROR: Email account not configured or file read error
- `501` TEST_NOT_IMPLEMENTED: Test email not implemented for this event type

**Example Request:**
```bash
curl -X POST "http://localhost:4450/api/email-events/{eventId}/templates/{templateId}/test" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Supported Event Types:**
Currently implemented test cases:
- `USER_REGISTRATION`: Sends welcome email with activation link
  - Generates real JWT activation token
  - Calculates expiration time from security settings
  - Builds full activation URL with app base URL

Not yet implemented:
- Other event types will return 501 error until test implementation is added

**Test Data Generation:**
Each event type has specific test data:

**USER_REGISTRATION Test Data:**
```json
{
  "username": "authenticated_user",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "enabled": "false",
  "accountLocked": "false",
  "createdAt": "2025-01-15 14:30:00",
  "activationToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "activationLink": "http://localhost:4450/api/auth/activate?token=eyJ...",
  "expirationHours": "24",
  "expirationDateTime": "2025-01-16 14:30:00"
}
```

**Variable Details:**
- **username**: User's unique username (required)
- **email**: User's email address (optional)
- **firstName**: User's first name (optional)
- **lastName**: User's last name (optional)
- **phoneNumber**: User's phone number, empty string if not provided (optional)
- **enabled**: Account enabled status as string ("true" or "false") (optional)
- **accountLocked**: Account locked status as string ("true" or "false") (optional)
- **createdAt**: Account creation timestamp in "yyyy-MM-dd HH:mm:ss" format (optional)
- **activationToken**: JWT token for account activation (required)
- **activationLink**: Full URL with activation token (required)
- **expirationHours**: Hours until link expires (required)
- **expirationDateTime**: Exact expiration time in "yyyy-MM-dd HH:mm:ss" format (required)

**Use Cases:**
1. **Template Development**: Test new templates before activating them
2. **Template Comparison**: Send multiple test emails with different templates to compare designs
3. **Quality Assurance**: Verify template changes work correctly with real data
4. **Email Client Testing**: See how templates render in different email clients

---

## Error Codes Reference

| Code | Status | Description |
|------|--------|-------------|
| EMAIL_EVENT_NOT_FOUND | 404 | Email event not found |
| INVALID_EVENT_ID | 400 | Invalid event ID format |
| EMAIL_EVENTS_RETRIEVAL_FAILED | 500 | Failed to retrieve email events |
| EMAIL_EVENT_RETRIEVAL_FAILED | 500 | Failed to retrieve email event |
| EMAIL_EVENT_UPDATE_FAILED | 500 | Failed to update email event |
| TEMPLATE_NOT_FOUND | 404 | Template not found |
| INVALID_ID | 400 | Invalid ID format |
| TEMPLATE_ALREADY_EXISTS | 400 | Template name already exists for event |
| TEMPLATE_FILE_SAVE_FAILED | 500 | Failed to save template file |
| TEMPLATE_FILE_READ_FAILED | 500 | Failed to read template file |
| TEMPLATE_FILE_UPDATE_FAILED | 500 | Failed to update template file |
| TEMPLATE_CREATE_FAILED | 500 | Failed to create template |
| TEMPLATES_FETCH_FAILED | 500 | Failed to fetch templates |
| TEMPLATE_FETCH_FAILED | 500 | Failed to fetch template |
| TEMPLATE_CONTENT_READ_FAILED | 500 | Failed to read template content |
| TEMPLATE_CONTENT_FETCH_FAILED | 500 | Failed to fetch template content |
| TEMPLATE_CONTENT_FILE_FETCH_FAILED | 500 | Failed to fetch template content file |
| TEMPLATE_UPDATE_FAILED | 500 | Failed to update template |
| TEMPLATE_RESTORE_FAILED | 500 | Failed to restore template |
| NOT_SYSTEM_DEFAULT_TEMPLATE | 400 | Template is not a system default |
| TEMPLATES_DELETE_FAILED | 500 | Failed to delete templates |
| NO_TEMPLATE_IDS | 400 | No template IDs provided |
| CANNOT_DELETE_SYSTEM_DEFAULT_TEMPLATES | 400 | Cannot delete system default templates |
| CANNOT_DELETE_DEFAULT_ENABLED_TEMPLATES | 400 | Cannot delete default enabled templates |
| INVALID_PAGE | 400 | Page number cannot be negative |
| INVALID_SIZE | 400 | Page size must be greater than 0 |

---

## Important Notes

### Email Events
1. **System-Defined**: Email events are predefined and initialized on application startup
2. **Cannot Create/Delete**: Email events cannot be created or deleted via API
3. **System-Defined Variables**: Each event has predefined variables in `variablesJson` that cannot be modified via API
4. **Only Update Settings**: Only `description` and `enabled` status can be modified
5. **Event Names**: Event names are immutable system identifiers (e.g., USER_REGISTRATION)

### Templates
1. **System Default Protection**: Templates with `isSystemDefault=true` cannot be deleted
2. **Default Enabled Protection**: Templates with `isDefault=true` AND `enabled=true` cannot be deleted
3. **One Default Per Event**: Only one template per event can be marked as default
4. **Atomic Deletion**: Batch delete is all-or-nothing; if any template is protected, none are deleted
5. **File Naming**: Files are named as `{eventName}_{templateName}_{timestamp}.html`
6. **File Storage**: Templates are stored on disk at the path configured in `email.template.storage.path`
7. **Variable Placeholders**: Templates must use `{{variableName}}` syntax for variables defined in the parent EmailEvent
8. **Template Control**: Template creators control placement, styling, and layout; the system provides the actual data
9. **Runtime Rendering**: Variable placeholders are replaced with actual values when emails are sent

### System Initialization
1. **Auto-Creation**: When the application starts, predefined events are created in the database
2. **System-Defined Variables**: Each event is initialized with predefined variables from `EmailEventVariables`
3. **Default Templates**: Each event automatically gets a system default template
4. **Template Source**: Original templates are loaded from `src/main/resources/templates/email-templates/`
5. **Fallback Handling**: If a template file is missing, a basic fallback template is generated

### Predefined Email Events
Currently, the system initializes the following email event on startup:

- **USER_REGISTRATION**: New user registration emails
  - **Required Variables**: `username`, `activationToken`, `activationLink`, `expirationHours`, `expirationDateTime`
  - **Optional Variables**: `email`, `firstName`, `lastName`, `phoneNumber`, `enabled`, `accountLocked`, `createdAt`
  - **Description**: Sent when a new user registers in the system. Contains welcome message and account activation instructions.
  - **System-Defined**: Variables are immutable and cannot be modified via API

**Future Events** (to be implemented):
- PASSWORD_RESET
- EMAIL_VERIFICATION
- ACCOUNT_ACTIVATED
- ACCOUNT_DEACTIVATED
- PASSWORD_CHANGED

### System-Defined Variables Architecture

**How It Works:**
1. **Event-Level Variables**: Each `EmailEvent` has a `variablesJson` field that defines what variables are available for templates
2. **Immutable via API**: The `variablesJson` field cannot be modified through API endpoints - it's system-controlled
3. **Template Placeholders**: Templates use `{{variableName}}` syntax to reference these variables
4. **Runtime Rendering**: When sending emails, the system replaces placeholders with actual data using `EmailTemplateRenderer`

**Example - USER_REGISTRATION Variables:**
```json
[
  {
    "name": "username",
    "description": "User's username",
    "isRequired": true
  },
  {
    "name": "activationToken",
    "description": "Account activation token (JWT)",
    "isRequired": true
  },
  {
    "name": "activationLink",
    "description": "Full account activation URL with token",
    "isRequired": true
  },
  {
    "name": "expirationHours",
    "description": "Number of hours until activation link expires",
    "isRequired": true
  },
  {
    "name": "expirationDateTime",
    "description": "Exact date and time when activation link expires (ISO format)",
    "isRequired": true
  }
]
```

**Using Variables in Templates:**
```html
<!DOCTYPE html>
<html>
<body>
  <h1>Welcome {{username}}!</h1>
  <p>Click here to activate your account:</p>
  <a href="{{activationLink}}">Activate Account</a>
  <p>This link expires in {{expirationHours}} hours ({{expirationDateTime}})</p>
</body>
</html>
```

**Validation Rules:**
1. **Lifecycle Hooks**: `variablesJson` is validated automatically via `@PrePersist` and `@PreUpdate` JPA hooks in `EmailEvent` entity
2. **Allowed Keys**: Only `name`, `defaultValue`, `description`, `isRequired` are permitted
3. **Strict Checking**: Any other key will cause an `IllegalArgumentException`
4. **JSON Format**: Must be a valid JSON array of objects
5. **Empty Values**: `null`, empty string, or `"[]"` are valid (no variables)

**Benefits:**
- **Consistency**: All templates for an event use the same variable names
- **Type Safety**: System code knows exactly what variables to provide
- **User Freedom**: Template creators control layout/styling, system provides data
- **Immutability**: Variables can't be accidentally modified, ensuring system stability

**Note:** Variables are defined in `EmailEventVariables.java` and initialized during application startup.

---

## Template Rendering Service

### EmailTemplateRenderer

The `EmailTemplateRenderer` service is used to render email templates with actual variable values at runtime. This service is designed for internal system use when sending emails.

**Location:** `com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer`

**Method Signature:**
```java
public String renderTemplate(String eventName, Map<String, String> variables)
```

**Parameters:**
- `eventName`: The name of the email event (e.g., "USER_REGISTRATION")
- `variables`: Map of variable names to their actual values

**Returns:** Rendered HTML content with placeholders replaced by actual values

**Example Usage in RegistrationServices:**
```java
@Autowired
private EmailTemplateRenderer emailTemplateRenderer;

@Autowired
private SecuritySettingsGetterServices securitySettings;

public void sendRegistrationEmail(User user, String activationToken) {
    // Calculate expiration
    Long expirationMinutes = securitySettings.getRegistrationJwtExpirationMinutes();
    LocalDateTime expirationDateTime = LocalDateTime.now().plusMinutes(expirationMinutes);
    Long expirationHours = expirationMinutes / 60;

    // Build activation link
    String activationLink = "https://yourapp.com/activate?token=" + activationToken;

    // Prepare variables
    Map<String, String> variables = new HashMap<>();
    variables.put("username", user.getUsername());
    variables.put("activationToken", activationToken);
    variables.put("activationLink", activationLink);
    variables.put("expirationHours", String.valueOf(expirationHours));
    variables.put("expirationDateTime", expirationDateTime.format(DateTimeFormatter.ISO_DATE_TIME));

    // Render template
    String htmlContent = emailTemplateRenderer.renderTemplate("USER_REGISTRATION", variables);

    // Send email using your email service
    emailService.sendEmail(user.getEmail(), "Welcome to Kabengosafaris", htmlContent);
}
```

**How It Works:**
1. **Fetches Event**: Retrieves the `EmailEvent` by name from the database
2. **Gets Default Template**: Finds the default enabled template for the event
3. **Loads Content**: Reads the HTML template file from disk
4. **Validates Variables**: Ensures all required variables are provided
5. **Replaces Placeholders**: Replaces all `{{variableName}}` with actual values
6. **Returns HTML**: Returns fully rendered HTML ready to send

**Exception Handling:**
- `IllegalArgumentException`: Event not found or missing required variables
- `IllegalStateException`: Event is disabled
- `RuntimeException`: Template rendering failed

**Validation:**
- All variables marked as `isRequired: true` in the event's `variablesJson` must be provided
- Missing required variables will throw an exception with a clear error message
- Optional variables (where `isRequired: false` or not specified) can be omitted
- If a variable has a `defaultValue` in `variablesJson`, it will be used if not provided

---

## Configuration

### Application Properties
Add the following to `application.properties`:

```properties
# Email Template Configuration
email.template.storage.path=/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/email-templates/
email.template.max.file.size=2097152
```

### Storage Path
- Templates are stored as HTML files on disk
- Path must be writable by the application
- Files are automatically created/deleted as templates are managed

---

## Best Practices

1. **Template Variables**: Use meaningful variable names and provide clear descriptions
2. **Default Templates**: Always have at least one enabled template per event
3. **System Defaults**: Keep system default templates for fallback scenarios
4. **Content Updates**: Test template content thoroughly before setting as default
5. **Pagination**: Use pagination for large result sets to improve performance
6. **Filtering**: Use filters to narrow down results efficiently
7. **File Management**: Monitor disk space as templates are stored as files
8. **Backup**: Regular backups of template storage path recommended

---

## Example Workflows

### Workflow 1: Creating a Custom Welcome Email Template
```
1. GET /api/email-events → Find USER_REGISTRATION event ID and view variablesJson
2. Note available variables: username, activationToken, activationLink, expirationHours, expirationDateTime
3. POST /api/email-events/{eventId}/templates
   Body: {
     "name": "Premium_Welcome",
     "content": "<!DOCTYPE html><html><body><h1>Welcome {{username}}!</h1><a href='{{activationLink}}'>Activate</a><p>Expires: {{expirationDateTime}}</p></body></html>",
     "isDefault": false,
     "enabled": true
   }
4. Test template
5. PUT /api/email-events/{eventId}/templates/{templateId}
   Body: { "isDefault": true }
```

### Workflow 2: Modifying and Restoring System Default
```
1. GET /api/email-events/{eventId}/templates?isSystemDefault=true
2. PUT /api/email-events/{eventId}/templates/{templateId}
   Body: { "content": "...updated content..." }
3. Test modified template
4. If needed, restore:
   POST /api/email-events/{eventId}/templates/{templateId}/restore
```

### Workflow 3: Disabling Email Notifications
```
1. PUT /api/email-events/{eventId}
   Body: { "enabled": false }
→ All emails for this event will be suppressed
```

---

**End of Documentation**
