# Notification Settings API Documentation

## Overview

The Notification Settings API provides endpoints for managing runtime-configurable notification settings. These settings control when email notifications are sent and to whom for events like newsletter subscriptions and booking inquiries, without requiring an application restart.

**Base URL:** `/api/notification-settings`

---

## Authentication & Authorization

All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

### Required Permissions

| Endpoint | Permission Required |
|----------|---------------------|
| GET /api/notification-settings | `PERM_READ_NOTIFICATION_SETTING` |
| GET /api/notification-settings/category/{category} | `PERM_READ_NOTIFICATION_SETTING` |
| GET /api/notification-settings/active | `PERM_READ_NOTIFICATION_SETTING` |
| GET /api/notification-settings/{settingKey} | `PERM_READ_NOTIFICATION_SETTING` |
| PUT /api/notification-settings/{settingKey} | `PERM_UPDATE_NOTIFICATION_SETTING` |
| POST /api/notification-settings/{settingKey}/reset | `PERM_UPDATE_NOTIFICATION_SETTING` |
| GET /api/notification-settings/health | None (public) |

---

## Available Settings

### NEWSLETTER Category

Controls email notifications sent to admin when users subscribe to the newsletter.

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `notification.newsletter.enabled` | BOOLEAN | `true` | Enable or disable email notifications for new newsletter subscriptions |
| `notification.newsletter.emails` | STRING | `admin@kabengosafaris.com` | Email addresses to receive newsletter subscription notifications (comma-separated) |

### BOOKING_INQUIRY Category

Controls email notifications sent to admin when visitors submit booking inquiries.

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `notification.booking_inquiry.enabled` | BOOLEAN | `true` | Enable or disable email notifications for new booking inquiries |
| `notification.booking_inquiry.emails` | STRING | `admin@kabengosafaris.com` | Email addresses to receive booking inquiry notifications (comma-separated) |

### CONTACT_US Category

Controls email notifications sent to admin when visitors submit messages through the Contact Us form.

| Setting Key | Data Type | Default Value | Description |
|-------------|-----------|---------------|-------------|
| `notification.contact_us.enabled` | BOOLEAN | `true` | Enable or disable email notifications for new contact form messages |
| `notification.contact_us.emails` | STRING | `admin@kabengosafaris.com` | Email addresses to receive contact form notifications (comma-separated) |

---

## Notification Flow

### Newsletter Subscription Notification

When a user subscribes (or re-subscribes) to the newsletter:

1. `NewsletterService` saves the subscription
2. Checks `notification.newsletter.enabled` — if `false`, stops
3. Gets recipient emails from `notification.newsletter.emails`
4. Renders the `NEWSLETTER_SUBSCRIPTION` email template with variables:
   - `subscriberEmail`, `subscriberName`, `subscriptionDate`
   - `preferredLocale`, `source`, `isResubscription`
   - `linkedCustomerName`, `totalActiveSubscribers`
5. Sends the rendered email to each recipient
6. Notification failures are silently logged — they never break the subscribe flow

### Booking Inquiry Notification

When a visitor submits a booking inquiry:

1. `BookingInquiryService` saves the inquiry with auto-generated code (e.g., `INQ-0001-03-26`)
2. Checks `notification.booking_inquiry.enabled` — if `false`, stops
3. Gets recipient emails from `notification.booking_inquiry.emails`
4. Renders the `BOOKING_INQUIRY` email template with variables:
   - `inquiryCode`, `firstName`, `lastName`, `email`, `phone`, `country`
   - `adults`, `children`, `totalTravelers`
   - `preferredStartDate`, `preferredEndDate`, `budgetCategory`, `tripType`
   - `specialRequests`, `message`, `source`, `preferredLocale`, `inquiryDate`
   - Itinerary details (if linked): `itineraryName`, `itineraryCode`, `itineraryTotalDays`, `itineraryTotalNights`, `itineraryStartLocation`, `itineraryEndLocation`, `itineraryDescription`
5. Sends the rendered email to each recipient
6. Notification failures are silently logged — they never break the inquiry submission flow

### Contact Us Notification

When a visitor submits a message through the Contact Us form:

1. `ContactMessageService` saves the message with auto-generated code (e.g., `MSG-0001-03-26`)
2. Checks `notification.contact_us.enabled` — if `false`, stops
3. Gets recipient emails from `notification.contact_us.emails`
4. Renders the `CONTACT_US` email template with variables:
   - `contactCode`, `name`, `email`, `phone`, `subject`
   - `message`, `source`, `preferredLocale`, `contactDate`
5. Sends the rendered email to each recipient
6. Notification failures are silently logged — they never break the contact form submission flow

---

## Endpoints

### 1. Get All Notification Settings

Retrieves all notification configuration settings.

**Endpoint:** `GET /api/notification-settings`

**Permission:** `PERM_READ_NOTIFICATION_SETTING`

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Notification settings retrieved successfully",
  "data": [
    {
      "id": 1,
      "settingKey": "notification.newsletter.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable email notifications for new newsletter subscriptions",
      "active": true,
      "isSystemDefault": true,
      "category": "NEWSLETTER",
      "requiresRestart": false,
      "createdAt": "2026-03-12T10:00:00",
      "updatedAt": "2026-03-12T10:00:00"
    },
    {
      "id": 2,
      "settingKey": "notification.newsletter.emails",
      "settingValue": "admin@kabengosafaris.com",
      "dataType": "STRING",
      "description": "Email addresses to receive newsletter subscription notifications (comma-separated)",
      "active": true,
      "isSystemDefault": true,
      "category": "NEWSLETTER",
      "requiresRestart": false,
      "createdAt": "2026-03-12T10:00:00",
      "updatedAt": "2026-03-12T10:00:00"
    },
    {
      "id": 3,
      "settingKey": "notification.booking_inquiry.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable email notifications for new booking inquiries",
      "active": true,
      "isSystemDefault": true,
      "category": "BOOKING_INQUIRY",
      "requiresRestart": false,
      "createdAt": "2026-03-12T10:00:00",
      "updatedAt": "2026-03-12T10:00:00"
    },
    {
      "id": 4,
      "settingKey": "notification.booking_inquiry.emails",
      "settingValue": "admin@kabengosafaris.com",
      "dataType": "STRING",
      "description": "Email addresses to receive booking inquiry notifications (comma-separated)",
      "active": true,
      "isSystemDefault": true,
      "category": "BOOKING_INQUIRY",
      "requiresRestart": false,
      "createdAt": "2026-03-12T10:00:00",
      "updatedAt": "2026-03-12T10:00:00"
    }
  ],
  "timestamp": "2026-03-12T10:35:00"
}
```

---

### 2. Get Settings by Category

Retrieves notification settings for a specific category.

**Endpoint:** `GET /api/notification-settings/category/{category}`

**Permission:** `PERM_READ_NOTIFICATION_SETTING`

#### Path Parameters

| Parameter | Type | Required | Valid Values |
|-----------|------|----------|--------------|
| `category` | Enum | Yes | `NEWSLETTER`, `BOOKING_INQUIRY`, `CONTACT_US` |

#### Example

```
GET /api/notification-settings/category/NEWSLETTER
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Notification settings for category NEWSLETTER retrieved successfully",
  "data": [
    {
      "id": 1,
      "settingKey": "notification.newsletter.enabled",
      "settingValue": "true",
      "dataType": "BOOLEAN",
      "description": "Enable or disable email notifications for new newsletter subscriptions",
      "active": true,
      "isSystemDefault": true,
      "category": "NEWSLETTER",
      "requiresRestart": false
    },
    {
      "id": 2,
      "settingKey": "notification.newsletter.emails",
      "settingValue": "admin@kabengosafaris.com",
      "dataType": "STRING",
      "description": "Email addresses to receive newsletter subscription notifications (comma-separated)",
      "active": true,
      "isSystemDefault": true,
      "category": "NEWSLETTER",
      "requiresRestart": false
    }
  ],
  "timestamp": "2026-03-12T10:35:00"
}
```

---

### 3. Get Active Settings Only

Retrieves only active notification settings (where active=true).

**Endpoint:** `GET /api/notification-settings/active`

**Permission:** `PERM_READ_NOTIFICATION_SETTING`

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Active notification settings retrieved successfully",
  "data": [
    // Array of active settings only
  ],
  "timestamp": "2026-03-12T10:35:00"
}
```

---

### 4. Get Setting by Key

Retrieves a specific notification setting by its setting key.

**Endpoint:** `GET /api/notification-settings/{settingKey}`

**Permission:** `PERM_READ_NOTIFICATION_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `settingKey` | String | Yes | The setting key (e.g., `notification.newsletter.enabled`) |

#### Example

```
GET /api/notification-settings/notification.newsletter.enabled
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Notification setting retrieved successfully",
  "data": {
    "id": 1,
    "settingKey": "notification.newsletter.enabled",
    "settingValue": "true",
    "dataType": "BOOLEAN",
    "description": "Enable or disable email notifications for new newsletter subscriptions",
    "active": true,
    "isSystemDefault": true,
    "category": "NEWSLETTER",
    "requiresRestart": false,
    "createdAt": "2026-03-12T10:00:00",
    "updatedAt": "2026-03-12T10:00:00"
  },
  "timestamp": "2026-03-12T10:35:00"
}
```

#### Response - Not Found

```json
{
  "success": false,
  "status": 404,
  "message": "Notification setting not found: notification.invalid.key",
  "errorCode": "NOT_FOUND",
  "timestamp": "2026-03-12T10:35:00"
}
```

---

### 5. Update Notification Setting

Updates a specific notification setting value by its setting key.

**Endpoint:** `PUT /api/notification-settings/{settingKey}`

**Permission:** `PERM_UPDATE_NOTIFICATION_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `settingKey` | String | Yes | The setting key to update |

#### Request Body

```json
{
  "settingValue": "false",
  "active": true
}
```

#### Field Validation

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `settingValue` | String | Yes | New value for the setting (validated against dataType) |
| `active` | Boolean | No | Whether the setting is active |

#### Example - Disable Newsletter Notifications

```
PUT /api/notification-settings/notification.newsletter.enabled
Content-Type: application/json

{
  "settingValue": "false",
  "active": true
}
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Notification setting updated successfully",
  "data": {
    "id": 1,
    "settingKey": "notification.newsletter.enabled",
    "settingValue": "false",
    "dataType": "BOOLEAN",
    "description": "Enable or disable email notifications for new newsletter subscriptions",
    "active": true,
    "isSystemDefault": true,
    "category": "NEWSLETTER",
    "requiresRestart": false,
    "updatedAt": "2026-03-12T11:20:00"
  },
  "timestamp": "2026-03-12T11:20:00"
}
```

#### Response - Validation Error

```json
{
  "success": false,
  "status": 404,
  "message": "Boolean value must be 'true' or 'false'",
  "errorCode": "NOT_FOUND",
  "timestamp": "2026-03-12T11:20:00"
}
```

---

### 6. Reset Setting to Default

Resets a system default notification setting back to active state.

**Endpoint:** `POST /api/notification-settings/{settingKey}/reset`

**Permission:** `PERM_UPDATE_NOTIFICATION_SETTING`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `settingKey` | String | Yes | The setting key to reset |

#### Example

```
POST /api/notification-settings/notification.newsletter.enabled/reset
```

#### Response - Success

```json
{
  "success": true,
  "status": 200,
  "message": "Notification setting reset successfully",
  "data": {
    "id": 1,
    "settingKey": "notification.newsletter.enabled",
    "settingValue": "true",
    "dataType": "BOOLEAN",
    "description": "Enable or disable email notifications for new newsletter subscriptions",
    "active": true,
    "isSystemDefault": true,
    "category": "NEWSLETTER",
    "requiresRestart": false,
    "updatedAt": "2026-03-12T11:25:00"
  },
  "timestamp": "2026-03-12T11:25:00"
}
```

#### Response - Bad Request (Non-System Default)

```json
{
  "success": false,
  "status": 400,
  "message": "Cannot reset non-system default setting",
  "errorCode": "BAD_REQUEST",
  "timestamp": "2026-03-12T11:25:00"
}
```

---

### 7. Health Check

Health check endpoint to verify the Notification Settings API is operational.

**Endpoint:** `GET /api/notification-settings/health`

**Permission:** None (public endpoint)

#### Response

```json
{
  "success": true,
  "status": 200,
  "message": "Notification Settings API is healthy",
  "data": null,
  "timestamp": "2026-03-12T10:35:00"
}
```

---

## Using Settings in Code

The `NotificationSettingGetterServices` class provides typed getter methods to read settings with automatic fallback to `application.properties` values:

```java
@Autowired
private NotificationSettingGetterServices notificationSettings;

// ========================================
// Newsletter Notification Settings
// ========================================

// Check if newsletter notifications are enabled
if (notificationSettings.isNewsletterNotificationEnabled()) {
    // Get recipient email addresses
    List<String> emails = notificationSettings.getNewsletterNotificationEmails();
    // emails = ["admin@kabengosafaris.com", "marketing@kabengosafaris.com"]

    for (String email : emails) {
        // Send newsletter subscription notification
    }
}

// ========================================
// Booking Inquiry Notification Settings
// ========================================

// Check if booking inquiry notifications are enabled
if (notificationSettings.isBookingInquiryNotificationEnabled()) {
    // Get recipient email addresses
    List<String> emails = notificationSettings.getBookingInquiryNotificationEmails();

    for (String email : emails) {
        // Send booking inquiry notification
    }
}

// ========================================
// Contact Us Notification Settings
// ========================================

// Check if contact message notifications are enabled
if (notificationSettings.isContactMessageNotificationEnabled()) {
    // Get recipient email addresses
    List<String> emails = notificationSettings.getContactMessageNotificationEmails();

    for (String email : emails) {
        // Send contact message notification
    }
}
```

### Available Helper Methods

#### Newsletter Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isNewsletterNotificationEnabled()` | Boolean | Check if newsletter notifications are enabled |
| `getNewsletterNotificationEmails()` | List&lt;String&gt; | Get list of newsletter notification recipient emails |

#### Booking Inquiry Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isBookingInquiryNotificationEnabled()` | Boolean | Check if booking inquiry notifications are enabled |
| `getBookingInquiryNotificationEmails()` | List&lt;String&gt; | Get list of booking inquiry notification recipient emails |

#### Contact Us Settings
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isContactMessageNotificationEnabled()` | Boolean | Check if contact message notifications are enabled |
| `getContactMessageNotificationEmails()` | List&lt;String&gt; | Get list of contact message notification recipient emails |

### Fallback Behavior

Each getter method follows this resolution order:
1. Look up the setting key in the `notification_settings` database table
2. If the setting exists and is active, return its value
3. If the setting does not exist or is inactive, fall back to the `@Value` from `application.properties`

This ensures the application always has a working configuration, even if the database settings haven't been initialized yet.

---

## cURL Examples

### Get All Notification Settings
```bash
curl -X GET "http://localhost:4450/api/notification-settings" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Newsletter Settings Only
```bash
curl -X GET "http://localhost:4450/api/notification-settings/category/NEWSLETTER" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Booking Inquiry Settings Only
```bash
curl -X GET "http://localhost:4450/api/notification-settings/category/BOOKING_INQUIRY" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Contact Us Settings Only
```bash
curl -X GET "http://localhost:4450/api/notification-settings/category/CONTACT_US" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Active Settings Only
```bash
curl -X GET "http://localhost:4450/api/notification-settings/active" \
  -H "Authorization: Bearer <jwt_token>"
```

### Get Specific Setting by Key
```bash
curl -X GET "http://localhost:4450/api/notification-settings/notification.newsletter.enabled" \
  -H "Authorization: Bearer <jwt_token>"
```

### Disable Newsletter Notifications
```bash
curl -X PUT "http://localhost:4450/api/notification-settings/notification.newsletter.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "false",
    "active": true
  }'
```

### Enable Booking Inquiry Notifications
```bash
curl -X PUT "http://localhost:4450/api/notification-settings/notification.booking_inquiry.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "true",
    "active": true
  }'
```

### Add Multiple Notification Recipients
```bash
curl -X PUT "http://localhost:4450/api/notification-settings/notification.newsletter.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "admin@kabengosafaris.com,marketing@kabengosafaris.com,info@kabengosafaris.com",
    "active": true
  }'
```

### Set Booking Inquiry Notification Recipients
```bash
curl -X PUT "http://localhost:4450/api/notification-settings/notification.booking_inquiry.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "sales@kabengosafaris.com,bookings@kabengosafaris.com",
    "active": true
  }'
```

### Set Contact Us Notification Emails
```bash
curl -X PUT "http://localhost:4450/api/notification-settings/notification.contact_us.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "settingValue": "info@kabengosafaris.com,support@kabengosafaris.com",
    "active": true
  }'
```

### Submit Contact Form (Public)
```bash
curl -X POST "http://localhost:4450/api/public/contact" \
  -H "Content-Type: application/json" \
  -H "Accept-Language: en" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+255 700 000 000",
    "subject": "Safari Inquiry",
    "message": "I would like to know more about your 5-day Serengeti safari package."
  }'
```

### Reset Setting to Default
```bash
curl -X POST "http://localhost:4450/api/notification-settings/notification.newsletter.enabled/reset" \
  -H "Authorization: Bearer <jwt_token>"
```

### Health Check
```bash
curl -X GET "http://localhost:4450/api/notification-settings/health"
```

---

## Common Use Cases

### 1. Disable All Newsletter Notifications
```bash
curl -X PUT "http://localhost:4450/api/notification-settings/notification.newsletter.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'
```

### 2. Route Booking Inquiries to Sales Team
```bash
curl -X PUT "http://localhost:4450/api/notification-settings/notification.booking_inquiry.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "sales@kabengosafaris.com,reservations@kabengosafaris.com", "active": true}'
```

### 3. Temporarily Disable All Notifications (Maintenance)
```bash
# Disable newsletter notifications
curl -X PUT "http://localhost:4450/api/notification-settings/notification.newsletter.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'

# Disable booking inquiry notifications
curl -X PUT "http://localhost:4450/api/notification-settings/notification.booking_inquiry.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'

# Disable contact us notifications
curl -X PUT "http://localhost:4450/api/notification-settings/notification.contact_us.enabled" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "false", "active": true}'
```

### 4. Re-enable All Notifications After Maintenance
```bash
# Reset all notification enabled settings
for key in notification.newsletter.enabled notification.booking_inquiry.enabled notification.contact_us.enabled; do
  curl -X POST "http://localhost:4450/api/notification-settings/$key/reset" \
    -H "Authorization: Bearer <jwt_token>"
done
```

### 5. Route Different Events to Different Teams
```bash
# Newsletter to marketing
curl -X PUT "http://localhost:4450/api/notification-settings/notification.newsletter.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "marketing@kabengosafaris.com", "active": true}'

# Booking inquiries to sales
curl -X PUT "http://localhost:4450/api/notification-settings/notification.booking_inquiry.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "sales@kabengosafaris.com", "active": true}'

# Contact messages to support
curl -X PUT "http://localhost:4450/api/notification-settings/notification.contact_us.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "support@kabengosafaris.com", "active": true}'
```

### 6. Add a New Recipient Without Replacing Existing Ones
```bash
# First, get current emails
curl -X GET "http://localhost:4450/api/notification-settings/notification.booking_inquiry.emails" \
  -H "Authorization: Bearer <jwt_token>"

# Then update with the full comma-separated list (old + new)
curl -X PUT "http://localhost:4450/api/notification-settings/notification.booking_inquiry.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "admin@kabengosafaris.com,new-member@kabengosafaris.com", "active": true}'
```

---

## Error Codes

| HTTP Status | Error Type | Description |
|-------------|------------|-------------|
| 400 | `BAD_REQUEST` | Cannot reset non-system default setting, or invalid value for data type |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | Insufficient permissions for this operation |
| 404 | `NOT_FOUND` | Setting not found with the specified key |
| 500 | `INTERNAL_SERVER_ERROR` | Server error or database connection issue |

### Example Error Responses

#### Setting Not Found
```json
{
  "success": false,
  "status": 404,
  "message": "Notification setting not found: notification.invalid.key",
  "errorCode": "NOT_FOUND",
  "timestamp": "2026-03-12T11:20:00"
}
```

#### Invalid Boolean Value
```json
{
  "success": false,
  "status": 404,
  "message": "Boolean value must be 'true' or 'false'",
  "errorCode": "NOT_FOUND",
  "timestamp": "2026-03-12T11:20:00"
}
```

#### Insufficient Permissions
```json
{
  "success": false,
  "status": 403,
  "message": "Access Denied: Insufficient permissions",
  "timestamp": "2026-03-12T11:20:00"
}
```

---

## Data Type Validation

When updating settings, the `settingValue` must be compatible with the setting's `dataType`:

| Data Type | Valid Examples | Invalid Examples |
|-----------|----------------|------------------|
| BOOLEAN | `"true"`, `"false"` | `"yes"`, `"1"`, `"enabled"` |
| STRING | `"admin@kabengosafaris.com"`, `"a@b.com,c@d.com"` | Any string is valid |

---

## Email Template Variables

### NEWSLETTER_SUBSCRIPTION Event

| Variable | Required | Description |
|----------|----------|-------------|
| `subscriberEmail` | Yes | Email address of the subscriber |
| `subscriberName` | No | Name of the subscriber (if provided) |
| `subscriptionDate` | Yes | Formatted date/time of subscription |
| `preferredLocale` | No | Subscriber's preferred language |
| `source` | No | Subscription source (e.g., "WEBSITE") |
| `isResubscription` | No | Whether this is a re-subscription ("true"/"false") |
| `linkedCustomerName` | No | Name of linked customer (if matched) |
| `totalActiveSubscribers` | No | Total number of active subscribers |

### BOOKING_INQUIRY Event

| Variable | Required | Description |
|----------|----------|-------------|
| `inquiryCode` | Yes | Auto-generated inquiry code (e.g., "INQ-0001-03-26") |
| `firstName` | Yes | Inquirer's first name |
| `lastName` | Yes | Inquirer's last name |
| `email` | Yes | Inquirer's email address |
| `phone` | No | Inquirer's phone number |
| `country` | No | Inquirer's country |
| `adults` | No | Number of adults |
| `children` | No | Number of children |
| `totalTravelers` | No | Total number of travelers |
| `preferredStartDate` | No | Preferred start date |
| `preferredEndDate` | No | Preferred end date |
| `budgetCategory` | No | Budget category (e.g., "LUXURY", "MID_RANGE") |
| `tripType` | No | Trip type (e.g., "FAMILY", "HONEYMOON") |
| `specialRequests` | No | Special requests text |
| `message` | No | Additional message |
| `source` | No | Inquiry source (e.g., "WEBSITE") |
| `preferredLocale` | No | Preferred language |
| `inquiryDate` | No | Formatted inquiry date/time |
| `itineraryName` | No | Linked itinerary name |
| `itineraryCode` | No | Linked itinerary code |
| `itineraryTotalDays` | No | Total days in linked itinerary |
| `itineraryTotalNights` | No | Total nights in linked itinerary |
| `itineraryStartLocation` | No | Start location of linked itinerary |
| `itineraryEndLocation` | No | End location of linked itinerary |
| `itineraryDescription` | No | Description of linked itinerary |

### CONTACT_US Event

| Variable | Required | Description |
|----------|----------|-------------|
| `contactCode` | Yes | Auto-generated contact message code (e.g., "MSG-0001-03-26") |
| `name` | Yes | Full name of the sender |
| `email` | Yes | Sender's email address |
| `phone` | No | Sender's phone number |
| `subject` | No | Subject line of the message |
| `message` | Yes | Full message content |
| `source` | No | Message source (e.g., "WEBSITE") |
| `preferredLocale` | No | Sender's preferred language |
| `contactDate` | Yes | Formatted date/time of submission |

---

## Architecture

### Module Structure

```
NotificationSetting/
├── API_DOCUMENTATION.md           # This file
├── NotificationSetting.java       # Entity (notification_settings table)
├── NotificationSettingRepository.java  # JPA Repository
├── NotificationSettingDTO.java     # Response DTO
├── UpdateNotificationSettingDTO.java   # Update request DTO
├── NotificationSettingServices.java    # CRUD service
├── NotificationSettingGetterServices.java  # Typed getters with @Value fallback
└── NotificationSettingController.java  # REST controller
```

### Related Files

| File | Purpose |
|------|---------|
| `Initializers/NotificationSettingsInitializer.java` | Seeds 6 default settings on startup |
| `Newsletter/Services/NewsletterService.java` | Dispatches newsletter subscription notifications |
| `BookingInquiry/Services/BookingInquiryService.java` | Dispatches booking inquiry notifications |
| `ContactMessage/Services/ContactMessageService.java` | Dispatches contact message notifications |
| `application.properties` | Fallback default values |
| `permissions/entities.json` | `NOTIFICATION_SETTING` entity for CRUD permissions |

### Database Table

**Table:** `notification_settings`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `setting_key` | VARCHAR(255) | UNIQUE, NOT NULL |
| `setting_value` | VARCHAR(1000) | NOT NULL |
| `data_type` | VARCHAR(20) | NOT NULL (BOOLEAN, STRING, INTEGER, LONG, DOUBLE) |
| `description` | VARCHAR(500) | |
| `active` | BOOLEAN | DEFAULT true |
| `is_system_default` | BOOLEAN | DEFAULT true |
| `category` | VARCHAR(30) | NOT NULL (NEWSLETTER, BOOKING_INQUIRY, CONTACT_US) |
| `requires_restart` | BOOLEAN | DEFAULT false |
| `created_at` | DATETIME | NOT NULL, auto-set |
| `updated_at` | DATETIME | auto-updated |

---

## Troubleshooting

### Problem: Notification emails not being sent

**Check:**
1. Verify notification is enabled: `GET /api/notification-settings/notification.newsletter.enabled`
2. Verify recipient emails are configured: `GET /api/notification-settings/notification.newsletter.emails`
3. Verify the email event (`NEWSLETTER_SUBSCRIPTION`, `BOOKING_INQUIRY`, or `CONTACT_US`) is enabled in the Email Events module
4. Verify a default email template exists and is enabled for the event
5. Verify a default email account is configured and enabled
6. Check application logs for notification dispatch errors

### Problem: Setting update doesn't take effect

**Solution:** Notification settings take effect immediately (no restart required). Check:
- The setting's `active` field is `true`
- The `settingValue` is valid for the data type

### Problem: Cannot update setting (403 Forbidden)

**Solution:** Verify user has `PERM_UPDATE_NOTIFICATION_SETTING` permission. Contact administrator to grant the permission via the Role management API.

### Problem: Duplicate notification emails

**Solution:** The emails field is comma-separated. Ensure no duplicate addresses in the list:
```bash
# Check current emails
curl -X GET "http://localhost:4450/api/notification-settings/notification.newsletter.emails" \
  -H "Authorization: Bearer <jwt_token>"

# Update with deduplicated list
curl -X PUT "http://localhost:4450/api/notification-settings/notification.newsletter.emails" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"settingValue": "admin@kabengosafaris.com,sales@kabengosafaris.com", "active": true}'
```

---

## Related APIs

- **[Email Events API](../EmailEvent/)** - Manage email event definitions and templates
- **[Backup Settings API](../Backup/BackupSettings/API_DOCUMENTATION.md)** - Similar database-driven settings pattern for backups
- **[Newsletter API](../Newsletter/)** - Newsletter subscription management (public endpoints)
- **[Booking Inquiry API](../BookingInquiry/)** - Booking inquiry submission (public endpoints)
- **[Contact Message API](../ContactMessage/)** - Contact form message handling (public endpoints)
