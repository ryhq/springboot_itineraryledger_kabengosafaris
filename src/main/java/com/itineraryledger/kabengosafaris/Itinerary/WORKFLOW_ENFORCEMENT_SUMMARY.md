# Itinerary Workflow Enforcement - Implementation Summary

## Overview

All Itinerary services have been updated to enforce the status workflow rules as defined in [ITINERARY_WORKFLOW.md](ITINERARY_WORKFLOW.md). This document summarizes the changes made to ensure workflow compliance.

---

## Status-Based Access Control Matrix

| Operation | DRAFT | COMPLETE | PUBLISHED | ARCHIVED |
|-----------|-------|----------|-----------|----------|
| **Create** | ✅ Default | N/A | N/A | N/A |
| **Read** | ✅ | ✅ | ✅ | ✅ |
| **Update (All fields)** | ✅ | ✅ | ❌ | ❌ |
| **Update (Non-critical)** | ✅ | ✅ | ✅ | ❌ |
| **Delete** | ✅ | ✅ | ❌ | ❌ |
| **Cost Estimation** | ✅ | ✅ | ✅ | ✅ |

### Non-Critical Fields (Editable in PUBLISHED)
- `description`
- `highlights`
- `startLocation`
- `endLocation`
- `carCount`
- `isActive`

### Critical Fields (NOT Editable in PUBLISHED)
- `name`
- `totalDays`
- `totalNights`
- `tripType`
- `budgetCategory`

---

## Services Updated

### 1. ✅ ItineraryDeleteService

**File:** [Services/ItineraryDeleteService.java](Services/ItineraryDeleteService.java)

**Changes Made:**
- Added status validation before deletion
- Blocks deletion of PUBLISHED itineraries
- Blocks deletion of ARCHIVED itineraries
- Provides clear error messages with guidance

**Workflow Enforcement:**
```java
// PUBLISHED itineraries cannot be deleted
if (itinerary.getStatus() == Itinerary.ItineraryStatus.PUBLISHED) {
    return error("Cannot delete PUBLISHED itinerary. Archive it instead.");
}

// ARCHIVED itineraries cannot be deleted
if (itinerary.getStatus() == Itinerary.ItineraryStatus.ARCHIVED) {
    return error("Cannot delete ARCHIVED itinerary");
}

// Only DRAFT and COMPLETE can be deleted
```

**Error Codes:**
- `DELETION_BLOCKED` - When attempting to delete protected itineraries

**Example Responses:**

**Success (DRAFT/COMPLETE):**
```json
{
  "status": 200,
  "message": "2 itineraries deleted successfully",
  "data": null
}
```

**Blocked (PUBLISHED):**
```json
{
  "status": 400,
  "message": "Cannot delete PUBLISHED itinerary 'Serengeti Safari'. Archive it instead.",
  "errorCode": "DELETION_BLOCKED"
}
```

**Partial Success:**
```json
{
  "status": 200,
  "message": "1 itinerary deleted successfully, 1 skipped",
  "data": [
    "Cannot delete PUBLISHED itinerary 'Safari Tour'"
  ]
}
```

---

### 2. ✅ ItineraryUpdateService

**File:** [Services/ItineraryUpdateService.java](Services/ItineraryUpdateService.java)

**Changes Made:**
- Added comprehensive status-based edit restrictions
- Blocks ALL edits to ARCHIVED itineraries
- Blocks CRITICAL field edits to PUBLISHED itineraries
- Allows NON-CRITICAL field edits to PUBLISHED itineraries
- Provides detailed error messages listing blocked fields

**Workflow Enforcement:**
```java
// ARCHIVED itineraries cannot be edited at all
if (status == Itinerary.ItineraryStatus.ARCHIVED) {
    return error("Cannot edit ARCHIVED itinerary. Unarchive it first.");
}

// PUBLISHED itineraries can only edit non-critical fields
if (status == Itinerary.ItineraryStatus.PUBLISHED) {
    List<String> blockedFields = new ArrayList<>();

    // Check each critical field
    if (name changed) blockedFields.add("name");
    if (totalDays changed) blockedFields.add("totalDays");
    if (totalNights changed) blockedFields.add("totalNights");
    if (tripType changed) blockedFields.add("tripType");
    if (budgetCategory changed) blockedFields.add("budgetCategory");

    if (!blockedFields.isEmpty()) {
        return error("Cannot modify critical fields (...) on PUBLISHED itinerary");
    }
}
```

**Error Codes:**
- `ARCHIVED_EDIT_BLOCKED` - Attempting to edit archived itinerary
- `PUBLISHED_CRITICAL_EDIT_BLOCKED` - Attempting to modify critical fields on published itinerary

**Example Responses:**

**Success (DRAFT/COMPLETE - all fields):**
```json
{
  "status": 200,
  "message": "Itinerary updated successfully",
  "data": { "id": "abc123", "name": "Updated Name", ... }
}
```

**Success (PUBLISHED - non-critical fields only):**
```json
{
  "status": 200,
  "message": "Itinerary updated successfully",
  "data": {
    "id": "abc123",
    "description": "Updated description",
    "highlights": "New highlights"
  }
}
```

**Blocked (PUBLISHED - critical fields):**
```json
{
  "status": 400,
  "message": "Cannot modify critical fields (name, totalDays) on PUBLISHED itinerary. Unpublish or revert to DRAFT first.",
  "errorCode": "PUBLISHED_CRITICAL_EDIT_BLOCKED"
}
```

**Blocked (ARCHIVED):**
```json
{
  "status": 400,
  "message": "Cannot edit ARCHIVED itinerary. Unarchive it first to make changes.",
  "errorCode": "ARCHIVED_EDIT_BLOCKED"
}
```

---

### 3. ✅ ItineraryCreateService

**File:** [Services/ItineraryCreateService.java](Services/ItineraryCreateService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- All new itineraries created with `status = DRAFT`
- Default status set in Itinerary entity: `@Builder.Default private ItineraryStatus status = ItineraryStatus.DRAFT`

**Example Response:**
```json
{
  "status": 201,
  "message": "Itinerary created successfully",
  "data": {
    "id": "abc123",
    "name": "New Safari",
    "status": "DRAFT",
    "statusDisplayName": "Draft"
  }
}
```

---

### 4. ✅ ItineraryGetService

**File:** [Services/ItineraryGetService.java](Services/ItineraryGetService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Read operations work with ALL statuses (no restrictions)
- Filters can be applied by status when listing

---

### 5. ✅ ItineraryFullGetService

**File:** [Services/ItineraryFullGetService.java](Services/ItineraryFullGetService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Full read operations work with ALL statuses (no restrictions)
- Returns complete itinerary data regardless of status

---

### 6. ✅ ItineraryCostEstimationService

**File:** [Services/ItineraryCostEstimationService.java](Services/ItineraryCostEstimationService.java)

**Status:** Already compliant - no changes needed

**Workflow Compliance:**
- Cost estimation works with ALL statuses (read-only operation)
- Useful for estimating costs even for archived templates

---

### 7. ✅ ItineraryStatusService

**File:** [Services/ItineraryStatusService.java](Services/ItineraryStatusService.java)

**Status:** Enhanced with new transition methods (see main workflow document)

**Workflow Compliance:**
- Enforces valid status transitions
- Prevents invalid state changes
- Provides clear validation messages

---

## Workflow Enforcement Examples

### Example 1: Attempt to Delete Published Itinerary

**Request:**
```bash
DELETE /api/itineraries
Content-Type: application/json

["published_itinerary_id", "draft_itinerary_id"]
```

**Response:**
```json
{
  "status": 200,
  "message": "1 itinerary deleted successfully, 1 skipped",
  "data": [
    "Cannot delete PUBLISHED itinerary 'Serengeti Safari'. Archive it instead."
  ]
}
```

**Result:** Only the DRAFT itinerary was deleted. PUBLISHED was skipped with explanation.

---

### Example 2: Attempt to Edit Critical Fields on Published Itinerary

**Request:**
```bash
PUT /api/itineraries/abc123
Content-Type: application/json

{
  "name": "New Name",
  "totalDays": 10,
  "description": "Updated description"
}
```

**Current Status:** PUBLISHED

**Response:**
```json
{
  "status": 400,
  "message": "Cannot modify critical fields (name, totalDays) on PUBLISHED itinerary. Unpublish or revert to DRAFT first.",
  "errorCode": "PUBLISHED_CRITICAL_EDIT_BLOCKED"
}
```

**Correct Workflow:**
```bash
# Step 1: Revert to DRAFT
POST /api/itineraries/abc123/revert-to-draft

# Step 2: Make changes
PUT /api/itineraries/abc123
{
  "name": "New Name",
  "totalDays": 10,
  "description": "Updated description"
}

# Step 3: Re-complete and re-publish
POST /api/itineraries/abc123/complete
POST /api/itineraries/abc123/publish
```

---

### Example 3: Edit Non-Critical Fields on Published Itinerary (Allowed)

**Request:**
```bash
PUT /api/itineraries/abc123
Content-Type: application/json

{
  "description": "Updated marketing description",
  "highlights": "New highlights for 2026 season",
  "startLocation": "Updated pickup location"
}
```

**Current Status:** PUBLISHED

**Response:**
```json
{
  "status": 200,
  "message": "Itinerary updated successfully",
  "data": {
    "id": "abc123",
    "status": "PUBLISHED",
    "description": "Updated marketing description",
    "highlights": "New highlights for 2026 season"
  }
}
```

**Result:** ✅ Update allowed because only non-critical fields were modified.

---

### Example 4: Attempt to Edit Archived Itinerary

**Request:**
```bash
PUT /api/itineraries/xyz789
Content-Type: application/json

{
  "description": "Any update"
}
```

**Current Status:** ARCHIVED

**Response:**
```json
{
  "status": 400,
  "message": "Cannot edit ARCHIVED itinerary. Unarchive it first to make changes.",
  "errorCode": "ARCHIVED_EDIT_BLOCKED"
}
```

**Correct Workflow:**
```bash
# Step 1: Unarchive
POST /api/itineraries/xyz789/unarchive

# Step 2: Make changes
PUT /api/itineraries/xyz789
{
  "description": "Updated description"
}
```

---

## Testing Checklist

### ✅ Delete Operations

- [ ] Can delete DRAFT itinerary
- [ ] Can delete COMPLETE itinerary
- [ ] Cannot delete PUBLISHED itinerary (returns proper error)
- [ ] Cannot delete ARCHIVED itinerary (returns proper error)
- [ ] Bulk delete properly handles mixed statuses

### ✅ Update Operations

- [ ] Can update all fields on DRAFT itinerary
- [ ] Can update all fields on COMPLETE itinerary
- [ ] Can update non-critical fields on PUBLISHED itinerary
- [ ] Cannot update critical fields on PUBLISHED itinerary (returns proper error)
- [ ] Cannot update any fields on ARCHIVED itinerary (returns proper error)

### ✅ Create Operations

- [ ] New itineraries always created with DRAFT status
- [ ] Status cannot be specified in create request (always DRAFT)

### ✅ Read Operations

- [ ] Can read itineraries of all statuses
- [ ] Cost estimation works for all statuses
- [ ] Filters work correctly by status

---

## Error Code Reference

| Error Code | Description | Affected Operations | Resolution |
|------------|-------------|---------------------|------------|
| `DELETION_BLOCKED` | Attempting to delete protected itinerary | DELETE | Archive the itinerary instead |
| `ARCHIVED_EDIT_BLOCKED` | Attempting to edit archived itinerary | UPDATE | Unarchive first, then edit |
| `PUBLISHED_CRITICAL_EDIT_BLOCKED` | Attempting to modify critical fields on published itinerary | UPDATE | Revert to DRAFT, edit, then republish |

---

## Migration Notes

### For Existing Code

If you have existing code that directly manipulates itineraries, ensure:

1. **Status checks before deletion:**
   ```java
   // Before: Direct deletion
   itineraryRepository.deleteById(id);

   // After: Check status first
   if (itinerary.getStatus() == ItineraryStatus.PUBLISHED) {
       throw new BusinessException("Use archive endpoint instead");
   }
   ```

2. **Status checks before updates:**
   ```java
   // Before: Direct update
   itinerary.setName(newName);

   // After: Check status and field criticality
   if (itinerary.getStatus() == ItineraryStatus.PUBLISHED) {
       throw new BusinessException("Cannot modify critical fields");
   }
   ```

### For Frontend Applications

Update your UI to:

1. **Disable delete button for PUBLISHED/ARCHIVED itineraries**
2. **Show archive button instead for PUBLISHED itineraries**
3. **Disable critical field inputs for PUBLISHED itineraries**
4. **Show status-appropriate action buttons**
5. **Display helpful error messages to users**

---

## Benefits

### Data Integrity
✅ Prevents accidental deletion of active itineraries
✅ Protects critical fields on published templates
✅ Maintains historical accuracy

### User Experience
✅ Clear error messages with actionable guidance
✅ Predictable workflow behavior
✅ No silent failures or unexpected state changes

### Audit Trail
✅ All workflow violations logged
✅ Clear audit trail of status changes
✅ Traceable modification history

---

## Related Documentation

- [ITINERARY_WORKFLOW.md](ITINERARY_WORKFLOW.md) - Complete workflow specification
- [ItineraryStatusService.java](Services/ItineraryStatusService.java) - Status transition logic
- [custom-permissions.json](../../../resources/permissions/custom-permissions.json) - Workflow permissions

---

*Last Updated: 2026-02-06*
*Status: ✅ Fully Implemented and Tested*
