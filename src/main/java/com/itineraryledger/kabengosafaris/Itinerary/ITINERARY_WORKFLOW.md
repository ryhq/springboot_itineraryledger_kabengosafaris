# Itinerary Status Workflow

## Overview

The Itinerary module implements a comprehensive status workflow system that manages the lifecycle of safari itinerary templates from initial draft through publication and archival.

## Status Lifecycle

### Status States

```
DRAFT → COMPLETE → PUBLISHED → ARCHIVED
  ↑        ↓          ↓           |
  └────────┴──────────┘           │
           └──────────────────────┘
```

| Status | Description | Can Edit? | Can Delete? | Can Use for Safari? |
|--------|-------------|-----------|-------------|---------------------|
| **DRAFT** | Itinerary is being created or edited | ✅ Yes | ✅ Yes | ❌ No |
| **COMPLETE** | All required data is filled in, ready for review | ✅ Yes | ⚠️ With caution | ⚠️ Not recommended |
| **PUBLISHED** | Available for booking and creating safaris | ⚠️ Limited | ❌ No | ✅ Yes |
| **ARCHIVED** | No longer in active use | ❌ No | ❌ No | ❌ No |

---

## Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    ITINERARY LIFECYCLE                          │
└─────────────────────────────────────────────────────────────────┘

┌──────────┐
│  DRAFT   │ ◄─────────┐
└─────┬────┘           │
      │                │
      │ [Complete Requirements Met]
      │                │
      ├────────────────┘
      │
      │ POST /api/itineraries/{id}/complete
      │ Permission: PERM_COMPLETE_ITINERARY
      │
      ▼
┌──────────┐
│ COMPLETE │
└─────┬────┘
      │
      │ POST /api/itineraries/{id}/publish
      │ Permission: PERM_PUBLISH_ITINERARY
      │
      ▼
┌──────────┐
│PUBLISHED │
└─────┬────┘
      │
      │ POST /api/itineraries/{id}/unpublish
      │ Permission: PERM_UNPUBLISH_ITINERARY
      │
      ├──────► [Back to COMPLETE or DRAFT]
      │
      │ POST /api/itineraries/{id}/archive
      │ Permission: PERM_ARCHIVE_ITINERARY
      │
      ▼
┌──────────┐
│ ARCHIVED │
└─────┬────┘
      │
      │ POST /api/itineraries/{id}/unarchive
      │ Permission: PERM_UNARCHIVE_ITINERARY
      │
      └──────► [Back to COMPLETE or DRAFT]


ADDITIONAL TRANSITIONS:

┌──────────┐    POST /api/itineraries/{id}/revert-to-draft     ┌──────────┐
│ COMPLETE │ ───────────────────────────────────────────────► │  DRAFT   │
└──────────┘    Permission: PERM_REVERT_ITINERARY_TO_DRAFT     └──────────┘

┌──────────┐    POST /api/itineraries/{id}/revert-to-draft     ┌──────────┐
│PUBLISHED │ ───────────────────────────────────────────────► │  DRAFT   │
└──────────┘    Permission: PERM_REVERT_ITINERARY_TO_DRAFT     └──────────┘
```

---

## API Endpoints

### 1. Evaluate Status (Automatic Transition)
**Endpoint:** `POST /api/itineraries/{id}/evaluate-status`
**Permission:** `PERM_UPDATE_ITINERARY`
**Description:** Automatically evaluates completeness and transitions between DRAFT ↔ COMPLETE

**Rules:**
- If DRAFT and meets requirements → transitions to COMPLETE
- If COMPLETE and no longer meets requirements → reverts to DRAFT
- If PUBLISHED or ARCHIVED → no change

**Response:**
```json
{
  "status": 200,
  "message": "Status evaluated",
  "data": {
    "id": "abc123",
    "code": "ITI-7D6N-001",
    "status": "COMPLETE",
    "statusDisplayName": "Complete"
  }
}
```

---

### 2. Mark as Complete (Explicit Transition)
**Endpoint:** `POST /api/itineraries/{id}/complete`
**Permission:** `PERM_COMPLETE_ITINERARY`
**Description:** Explicitly mark an itinerary as COMPLETE

**Allowed Transitions:**
- DRAFT → COMPLETE ✅

**Validation:**
- Must be in DRAFT status
- Must meet all publishing requirements:
  - `totalDays > 0`
  - Days list not empty
  - Days count matches totalDays
  - At least one passenger category defined

**Error Responses:**
```json
{
  "status": 400,
  "message": "Only DRAFT itineraries can be marked as COMPLETE",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

```json
{
  "status": 400,
  "message": "Itinerary does not meet completion requirements...",
  "errorCode": "INCOMPLETE_ITINERARY"
}
```

---

### 3. Revert to Draft
**Endpoint:** `POST /api/itineraries/{id}/revert-to-draft`
**Permission:** `PERM_REVERT_ITINERARY_TO_DRAFT`
**Description:** Revert an itinerary back to DRAFT status

**Allowed Transitions:**
- COMPLETE → DRAFT ✅
- PUBLISHED → DRAFT ✅
- ARCHIVED → DRAFT ❌ (use unarchive instead)

**Use Cases:**
- Need to make significant changes to a complete itinerary
- Unpublish and send back for editing
- Reopen a published itinerary template

**Error Responses:**
```json
{
  "status": 400,
  "message": "Itinerary is already in DRAFT status",
  "errorCode": "ALREADY_DRAFT"
}
```

```json
{
  "status": 400,
  "message": "Cannot revert archived itinerary to DRAFT. Use unarchive endpoint instead.",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

---

### 4. Publish Itinerary
**Endpoint:** `POST /api/itineraries/{id}/publish`
**Permission:** `PERM_PUBLISH_ITINERARY`
**Description:** Publish an itinerary to make it available for booking

**Allowed Transitions:**
- DRAFT → PUBLISHED ✅ (if requirements met)
- COMPLETE → PUBLISHED ✅
- ARCHIVED → PUBLISHED ❌

**Validation:**
- Cannot be ARCHIVED
- Cannot be already PUBLISHED
- Must pass `canPublish()` check

**Error Responses:**
```json
{
  "status": 400,
  "message": "Cannot publish an archived itinerary",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

```json
{
  "status": 400,
  "message": "Itinerary does not meet publishing requirements...",
  "errorCode": "INCOMPLETE_ITINERARY"
}
```

---

### 5. Unpublish Itinerary
**Endpoint:** `POST /api/itineraries/{id}/unpublish`
**Permission:** `PERM_UNPUBLISH_ITINERARY`
**Description:** Revert from published status

**Allowed Transitions:**
- PUBLISHED → COMPLETE ✅ (if still meets requirements)
- PUBLISHED → DRAFT ✅ (if no longer meets requirements)

**Behavior:**
- Automatically determines target status based on `canPublish()`
- If requirements met → COMPLETE
- If requirements not met → DRAFT

**Error Responses:**
```json
{
  "status": 400,
  "message": "Itinerary is not published",
  "errorCode": "NOT_PUBLISHED"
}
```

---

### 6. Archive Itinerary
**Endpoint:** `POST /api/itineraries/{id}/archive`
**Permission:** `PERM_ARCHIVE_ITINERARY`
**Description:** Archive an itinerary (mark as no longer in use)

**Allowed Transitions:**
- ANY status → ARCHIVED ✅

**Side Effects:**
- Sets `isActive = false`
- Prevents deletion
- Prevents editing
- Hides from active lists

**Error Responses:**
```json
{
  "status": 400,
  "message": "Itinerary is already archived",
  "errorCode": "ALREADY_ARCHIVED"
}
```

---

### 7. Unarchive Itinerary
**Endpoint:** `POST /api/itineraries/{id}/unarchive`
**Permission:** `PERM_UNARCHIVE_ITINERARY`
**Description:** Restore an itinerary from archived status

**Allowed Transitions:**
- ARCHIVED → COMPLETE ✅ (if meets requirements)
- ARCHIVED → DRAFT ✅ (if doesn't meet requirements)

**Side Effects:**
- Sets `isActive = true`
- Restores editing capabilities

**Error Responses:**
```json
{
  "status": 400,
  "message": "Itinerary is not archived",
  "errorCode": "NOT_ARCHIVED"
}
```

---

## Publishing Requirements

An itinerary can be marked as COMPLETE or PUBLISHED only if it meets these requirements:

### Required Fields
✅ `totalDays > 0`
✅ `days` list is not empty
✅ Days count equals `totalDays`
✅ At least one passenger category in `paxList`

### Validation Logic
The `canPublish()` method in the Itinerary entity checks:
```java
public boolean canPublish() {
    return totalDays != null && totalDays > 0
        && !days.isEmpty()
        && days.size() == totalDays
        && !paxList.isEmpty();
}
```

---

## Permissions

### Standard CRUD Permissions
- `PERM_CREATE_ITINERARY` - Create new itineraries
- `PERM_READ_ITINERARY` - View itinerary data
- `PERM_UPDATE_ITINERARY` - Edit itinerary fields
- `PERM_DELETE_ITINERARY` - Delete itineraries

### Status Workflow Permissions
- `PERM_COMPLETE_ITINERARY` - Mark itinerary as complete
- `PERM_REVERT_ITINERARY_TO_DRAFT` - Revert to draft status
- `PERM_PUBLISH_ITINERARY` - Publish itinerary
- `PERM_UNPUBLISH_ITINERARY` - Unpublish itinerary
- `PERM_ARCHIVE_ITINERARY` - Archive itinerary
- `PERM_UNARCHIVE_ITINERARY` - Unarchive itinerary

### Recommended Role Assignments

| Role | Permissions |
|------|-------------|
| **Itinerary Creator** | CREATE, READ, UPDATE, COMPLETE, REVERT_TO_DRAFT |
| **Itinerary Manager** | All CREATE/READ/UPDATE + COMPLETE, REVERT_TO_DRAFT, PUBLISH, UNPUBLISH |
| **Operations Director** | All permissions including ARCHIVE, UNARCHIVE |
| **Admin** | All permissions |

---

## Business Rules

### 1. Draft Itineraries
- Can be freely edited
- Can be deleted
- Cannot be used to create safaris
- Automatically transitions to COMPLETE when requirements met (via evaluateStatus)

### 2. Complete Itineraries
- Can still be edited
- Should be reviewed before publishing
- Not recommended for creating safaris (use PUBLISHED instead)
- Can be reverted to DRAFT if changes needed

### 3. Published Itineraries
- Should have limited editing (only non-critical fields)
- Primary source for creating new safaris/quotations
- Cannot be deleted (archive instead)
- Can be unpublished if major changes needed

### 4. Archived Itineraries
- Read-only (cannot edit or delete)
- Not visible in active lists
- Can be unarchived if needed
- Historical reference only

---

## Integration Points

### Safari Module
When converting a quotation to a safari booking:
```java
// Only published itineraries should be used
if (itinerary.getStatus() != ItineraryStatus.PUBLISHED) {
    throw new BusinessException("Only published itineraries can be used for safari bookings");
}
```

### Quotation Module
When creating a quotation from an itinerary:
```java
// Prefer published, but allow complete itineraries
if (itinerary.getStatus() == ItineraryStatus.DRAFT) {
    throw new BusinessException("Cannot create quotation from draft itinerary");
}
if (itinerary.getStatus() == ItineraryStatus.ARCHIVED) {
    throw new BusinessException("Cannot create quotation from archived itinerary");
}
```

---

## Audit Trail

All status transitions are logged via `@AuditLogAnnotation`:
- **Action:** The permission/action performed (e.g., PUBLISH_ITINERARY)
- **Entity Type:** Itinerary
- **Entity ID:** Obfuscated itinerary ID
- **User:** Current authenticated user
- **Timestamp:** When the action occurred

---

## Testing Scenarios

### Scenario 1: Happy Path - Draft to Published
```bash
# 1. Create itinerary (status: DRAFT)
POST /api/itineraries
{
  "name": "7-Day Serengeti Safari",
  "totalDays": 7,
  "totalNights": 6
}

# 2. Add days and pax
POST /api/itineraries/{id}/days
POST /api/itineraries/{id}/pax

# 3. Evaluate status (should become COMPLETE)
POST /api/itineraries/{id}/evaluate-status
# Response: status = COMPLETE

# 4. Publish
POST /api/itineraries/{id}/publish
# Response: status = PUBLISHED
```

### Scenario 2: Revert for Editing
```bash
# Published itinerary needs changes
POST /api/itineraries/{id}/revert-to-draft
# Response: status = DRAFT

# Make changes
PUT /api/itineraries/{id}

# Re-complete
POST /api/itineraries/{id}/complete
# Response: status = COMPLETE

# Re-publish
POST /api/itineraries/{id}/publish
# Response: status = PUBLISHED
```

### Scenario 3: Archive Old Itinerary
```bash
# Archive unused itinerary
POST /api/itineraries/{id}/archive
# Response: status = ARCHIVED, isActive = false

# Later, restore if needed
POST /api/itineraries/{id}/unarchive
# Response: status = COMPLETE or DRAFT (depending on requirements)
```

---

## Error Handling

### Common Error Codes
- `ITINERARY_NOT_FOUND` (404) - Itinerary ID not found
- `INVALID_STATUS_TRANSITION` (400) - Cannot perform requested transition
- `INCOMPLETE_ITINERARY` (400) - Does not meet publishing requirements
- `ALREADY_PUBLISHED` (400) - Itinerary is already published
- `ALREADY_ARCHIVED` (400) - Itinerary is already archived
- `ALREADY_DRAFT` (400) - Itinerary is already in draft
- `NOT_PUBLISHED` (400) - Itinerary is not published
- `NOT_ARCHIVED` (400) - Itinerary is not archived

---

## Files

### Entity
- [Itinerary.java](Entity/Itinerary.java) - Main entity with status enum and `canPublish()` logic

### Service
- [ItineraryStatusService.java](Services/ItineraryStatusService.java) - All status transition logic

### Controller
- [ItineraryController.java](Controller/ItineraryController.java) - REST endpoints for status management

### Configuration
- [custom-permissions.json](../../../resources/permissions/custom-permissions.json) - Custom workflow permissions

---

## Future Enhancements

### Planned Features
1. **Version Control** - Track changes to published itineraries
2. **Approval Workflow** - Require manager approval before publishing
3. **Status History** - Track all status changes with timestamps
4. **Bulk Operations** - Archive/unarchive multiple itineraries
5. **Scheduled Publishing** - Auto-publish at specified date/time
6. **Status Notifications** - Email/notification on status changes

---

*Last Updated: 2026-02-06*
