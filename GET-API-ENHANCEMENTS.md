# GET API Enhancements: Sortable Fields & Circular Navigation

## Overview

Two features were added to **every GET API** across the entire backend system (~68 modules, ~183 files modified):

1. **Sortable Fields Metadata** — Every list/getAll endpoint now returns which fields can be sorted on
2. **Circular Navigation** — Every getById endpoint now returns `nextId` and `previousId` for record-to-record navigation

---

## Feature 1: Sortable Fields in List Responses

### What Changed (Backend)

Every `getAll` / list endpoint response now includes three new fields:

```json
{
  "customers": [...],
  "currentPage": 0,
  "totalItems": 42,
  "totalPages": 5,
  "validSortFields": ["code", "firstName", "lastName", "companyName", "customerType", "nationality", "country", "city", "source", "isVip", "isBlacklisted", "isActive", "totalBookings", "totalSpent", "lastBookingDate", "createdAt", "updatedAt"],
  "currentSortBy": "createdAt",
  "currentSortDir": "desc"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `validSortFields` | `string[]` | All field names the API accepts for sorting |
| `currentSortBy` | `string` | The field currently being sorted on |
| `currentSortDir` | `string` | Current sort direction (`"asc"` or `"desc"`) |

### Sort Validation

If an invalid `sortBy` value is sent, the API returns a **400 error**:

```json
{
  "status": 400,
  "message": "Invalid sort field: invalidField. Valid fields are: [code, firstName, ...]",
  "errorCode": "INVALID_SORT_FIELD"
}
```

### API Request Parameter

All list endpoints now accept `sortBy` as a query parameter:

```
GET /api/customers?page=0&size=10&sortBy=lastName&sortDirection=asc
GET /api/customers?sortBy=totalSpent&sortDirection=desc
GET /api/customers                          ← defaults to sortBy=createdAt, sortDirection=desc
```

---

## Feature 2: Circular Navigation in getById Responses

### What Changed (Backend)

Every `getById` / single-entity endpoint now wraps the DTO in a map with navigation IDs:

**Before:**
```json
{
  "status": 200,
  "message": "Customer retrieved successfully",
  "data": { "id": "abc123", "firstName": "John", ... }
}
```

**After:**
```json
{
  "status": 200,
  "message": "Customer retrieved successfully",
  "data": {
    "customer": { "id": "abc123", "firstName": "John", ... },
    "nextId": "def456",
    "previousId": "xyz789"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `nextId` | `string \| null` | Obfuscated ID of the next record (wraps to first if at end) |
| `previousId` | `string \| null` | Obfuscated ID of the previous record (wraps to last if at start) |

### Circular Behavior

- Viewing the **last** record → `nextId` points to the **first** record
- Viewing the **first** record → `previousId` points to the **last** record
- Only `null` when there are zero or one records total

### Tier 4 Child Entities (Parent-Scoped Navigation)

For nested entities like `ItineraryDay`, `SafariDayActivity`, etc., navigation is **scoped to the parent**:

- An ItineraryDay's `nextId` is the next day **within the same itinerary**, not globally
- A SafariDayParkActivity's `nextId` is the next activity **within the same park visit**

---

## Frontend Adoption Guide

### Step 1: Add `sortBy` State to List Pages

Currently, list pages only track `sortDirection`. Add `sortBy` state and URL persistence.

**In every list page (e.g., `CustomersPage.jsx`):**

```jsx
// Add state
const [sortBy, setSortBy] = useState("createdAt");

// Initialize from URL (in the URL init useEffect)
const sortByParam = searchParams.get("sortBy");
if (sortByParam) setSortBy(sortByParam);

// Sync to URL (in the URL sync useEffect)
if (sortBy && sortBy !== "createdAt") params.set("sortBy", sortBy);

// Pass to API call
if (sortBy) params.append("sortBy", sortBy);

// Pass to table component
<CustomersTable
  sortBy={sortBy}
  onSortByChange={setSortBy}
  // ...existing props
/>
```

### Step 2: Store `validSortFields` from API Response

```jsx
const [validSortFields, setValidSortFields] = useState([]);

// In fetchCustomers():
const { customers, currentPage, totalItems, totalPages, validSortFields } = response.data.data;
setValidSortFields(validSortFields || []);
```

### Step 3: Add Sort Field Selector to Table Components

Add a dropdown or clickable column headers that let users pick which field to sort by.

**Option A — Dropdown selector (simplest):**

```jsx
import { FormControl, Select, MenuItem } from "@mui/material";

// In table toolbar area, next to the existing sort direction button:
<FormControl size="small" sx={{ minWidth: 150 }}>
  <Select
    value={sortBy}
    onChange={(e) => onSortByChange(e.target.value)}
    displayEmpty
    sx={{ fontSize: "0.85rem" }}
  >
    {validSortFields.map((field) => (
      <MenuItem key={field} value={field} sx={{ fontSize: "0.85rem" }}>
        {formatFieldLabel(field)}
      </MenuItem>
    ))}
  </Select>
</FormControl>

// Helper to format camelCase to readable labels
function formatFieldLabel(field) {
  return field
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (s) => s.toUpperCase())
    .trim();
}
// "createdAt" → "Created At"
// "totalBookings" → "Total Bookings"
// "isVip" → "Is Vip"
```

**Option B — Clickable column headers (more polished):**

```jsx
import { TableSortLabel } from "@mui/material";

// In table header cell:
<TableCell>
  <TableSortLabel
    active={sortBy === "firstName"}
    direction={sortBy === "firstName" ? sortDirection : "asc"}
    onClick={() => handleSortClick("firstName")}
  >
    First Name
  </TableSortLabel>
</TableCell>

// Handler:
const handleSortClick = (field) => {
  if (sortBy === field) {
    // Toggle direction if same field
    onSortDirectionChange(sortDirection === "asc" ? "desc" : "asc");
  } else {
    // New field, default to ascending
    onSortByChange(field);
    onSortDirectionChange("asc");
  }
};
```

### Step 4: Adopt Circular Navigation in Detail Pages

**In every detail/view page (e.g., `CustomerViewPage.jsx`):**

```jsx
// Add state for navigation IDs
const [nextId, setNextId] = useState(null);
const [previousId, setPreviousId] = useState(null);

// Update fetch to extract navigation from the new response shape
const fetchCustomer = async () => {
  const response = await axiosInstance.get(`/customers/${id}`);
  const data = response.data.data;

  // NEW: Response is now { customer: {...}, nextId: "...", previousId: "..." }
  setCustomer(data.customer);    // was: setCustomer(data)
  setNextId(data.nextId);
  setPreviousId(data.previousId);
};
```

**Add navigation buttons:**

```jsx
import { ChevronLeft, ChevronRight } from "lucide-react";

// In the header/toolbar area of the detail page:
<Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
  <IconButton
    onClick={() => navigate(`/customers/${previousId}`)}
    disabled={!previousId}
    size="small"
    title="Previous record"
  >
    <ChevronLeft size={20} />
  </IconButton>

  <IconButton
    onClick={() => navigate(`/customers/${nextId}`)}
    disabled={!nextId}
    size="small"
    title="Next record"
  >
    <ChevronRight size={20} />
  </IconButton>
</Box>
```

**Re-fetch on ID change** (already handled if `useEffect` depends on `id`):

```jsx
useEffect(() => {
  fetchCustomer();
}, [id]); // Triggers when navigating to next/previous
```

### Step 5: Update `location.state` Data Access

Some detail pages use `location.state` to avoid re-fetching. Since the response shape changed, update accordingly:

```jsx
// Before:
const customer = location.state?.customer;

// After: location.state might still pass the old shape from list navigation
// The list page passes the item directly, so no change needed there.
// But if fetching from API, extract from the wrapper:
const fetchCustomer = async () => {
  const response = await axiosInstance.get(`/customers/${id}`);
  const data = response.data.data;
  // data is now { customer: {...}, nextId, previousId } — not the DTO directly
  setCustomer(data.customer);
};
```

---

## Entity-Specific Response Keys

Each entity uses its own key in the getById response. Here's the mapping:

| Entity | getById Response Key | getAll List Key |
|--------|---------------------|-----------------|
| Customer | `customer` | `customers` |
| Accommodation | `accommodation` | `accommodations` |
| Park | `park` | `parks` |
| Activity | `activity` | `activities` |
| Itinerary | `itinerary` | `itineraries` |
| Safari | `safari` | `safaris` |
| Quote | `quote` | `quotes` |
| Invoice | `invoice` | `invoices` |
| Role | `role` | `roles` |
| Hero | `hero` | `heroes` |
| BankAccount | `bankAccount` | `bankAccounts` |
| Season | `season` | `seasons` |
| Tariff | `tariff` | `tariffs` |
| Backup | `backup` | `backups` |

> Check each endpoint's actual response to confirm the key name. The pattern is the entity name in camelCase.

---

## Valid Sort Fields Per Module

### Top-Level Entities

| Module | Valid Sort Fields |
|--------|-----------------|
| Customer | `code, firstName, lastName, companyName, customerType, nationality, country, city, source, isVip, isBlacklisted, isActive, totalBookings, totalSpent, lastBookingDate, createdAt, updatedAt` |
| Accommodation | `name, slug, accommodationType, category, region, district, starRating, totalRooms, maxGuests, isActive, createdAt, updatedAt` |
| Park | `name, slug, parkType, region, district, isActive, createdAt, updatedAt` |
| Activity | `name, slug, minimumAge, maximumParticipants, isActive, createdAt, updatedAt` |
| Itinerary | `name, code, tripType, budgetCategory, totalDays, totalNights, status, createdAt, updatedAt` |
| Safari | `name, code, slug, startDate, endDate, totalDays, totalNights, state, isActive, createdAt, updatedAt` |
| Quote | `quoteCode, title, status, sentDate, validFrom, validTo, isActive, createdAt, updatedAt` |
| Invoice | `invoiceCode, title, status, issueDate, dueDate, sentDate, paidDate, isActive, createdAt, updatedAt` |
| Role | `name, displayName, active, isSystemRole, createdAt, updatedAt` |
| Hero | `title, page, createdAt, updatedAt` |
| BankAccount | `accountCode, accountName, bankName, currency, isActive, isDefault, createdAt, updatedAt` |
| EmailAccount | `email, name, providerType, enabled, isDefault, createdAt, updatedAt` |
| EmailEvent | `name, enabled, createdAt, updatedAt` |
| PdfDocument | `name, displayName, enabled, createdAt, updatedAt` |
| Season | `name, seasonType, createdAt, updatedAt` |
| SeasonPeriod | `year, createdAt, updatedAt` |
| Tariff | `name, slug, chargingBasis, isActive, createdAt, updatedAt` |
| PaxAgeCategory | `name, categoryType, minAge, maxAge, isActive, createdAt, updatedAt` |
| PaxNationCategory | `name, categoryType, priorityFactor, isActive, createdAt, updatedAt` |

### Rate Entities

| Module | Valid Sort Fields |
|--------|-----------------|
| ParkTariffRate | `rackRate, stoRate, currency, isActive, createdAt, updatedAt` |
| ActivityTariffRate | `rackRate, stoRate, currency, isActive, createdAt, updatedAt` |
| AccommodationRate | `rackRate, stoRate, currency, isActive, createdAt, updatedAt` |

### Sub-Entities

| Module | Valid Sort Fields |
|--------|-----------------|
| AccommodationBoardType | `name, createdAt, updatedAt` |
| AccommodationRoomStandard | `name, maxOccupancy, viewType, createdAt, updatedAt` |
| AccommodationRoomType | `name, maxOccupancy, createdAt, updatedAt` |
| AccommodationEmail | `email, emailType, label, createdAt, updatedAt` |
| AccommodationPhone | `phoneNumber, phoneType, label, createdAt, updatedAt` |
| AccommodationDocument | `title, documentType, fileName, fileSize, createdAt, updatedAt` |
| CustomerEmail | `email, emailType, label, isPrimary, isActive, createdAt, updatedAt` |
| CustomerPhone | `phoneNumber, phoneType, label, isPrimary, isActive, createdAt, updatedAt` |
| CustomerNote | `subject, noteType, priority, isPinned, isPrivate, createdAt, updatedAt` |
| CustomerDocument | `title, documentType, fileName, fileSize, createdAt, updatedAt` |

### Image Entities

| Module | Valid Sort Fields |
|--------|-----------------|
| AccommodationImage | `imageType, isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| ActivityImage | `isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| ParkImage | `imageType, isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| HeroImage | `isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| ParkActivityImage | `isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |

### Document Entities

All document entities share: `title, documentType, fileName, fileSize, createdAt, updatedAt`

### Line Items & Templates

| Module | Valid Sort Fields |
|--------|-----------------|
| InvoiceLineItem | `itemType, itemName, createdAt, updatedAt` |
| QuoteItem | `itemType, itemName, createdAt, updatedAt` |
| EmailTemplate | `name, isDefault, enabled, fileSize, createdAt, updatedAt` |
| EmailAccountSignature | `name, isDefault, enabled, fileSize, createdAt, updatedAt` |
| PdfTemplate | `name, version, createdAt, updatedAt` |

### Backup

| Module | Valid Sort Fields |
|--------|-----------------|
| Backup | `name, size, createdAt` |

---

## Breaking Change: getById Response Shape

The **getById** response shape changed for all entities. This is a **breaking change** for the frontend.

**Before:** `response.data.data` was the entity DTO directly.
**After:** `response.data.data` is a wrapper object with the entity + navigation IDs.

### Migration Checklist

For each detail/view page in the frontend:

- [ ] Update API response destructuring to extract entity from wrapper key
- [ ] Add `nextId`/`previousId` state variables
- [ ] Add next/previous navigation buttons
- [ ] Test that `location.state` passthrough still works (list pages pass the DTO directly, which is fine)

### Pages to Update

Every page that calls a `GET /api/{entity}/{id}` endpoint needs updating:

- `CustomerViewPage.jsx`
- `SafariViewPage.jsx` + all nested day/park/activity views
- `ItineraryViewPage.jsx` + all nested day/park/activity views
- `QuoteViewPage.jsx`
- `InvoiceViewPage.jsx`
- `AccommodationViewPage.jsx`
- `ParkViewPage.jsx`
- `ActivityViewPage.jsx`
- `RoleViewPage.jsx`
- All other entity detail pages

---

## Quick Test Checklist

### List Endpoints
- [ ] `GET /api/customers` → response includes `validSortFields`, `currentSortBy`, `currentSortDir`
- [ ] `GET /api/customers?sortBy=lastName&sortDirection=asc` → sorted by last name ascending
- [ ] `GET /api/customers?sortBy=invalidField` → returns 400 with valid fields list

### Detail Endpoints
- [ ] `GET /api/customers/{id}` → response includes `customer`, `nextId`, `previousId`
- [ ] Navigate to last customer → `nextId` wraps to first
- [ ] Navigate to first customer → `previousId` wraps to last

### Compilation
```bash
# The target/ directory may be root-owned from previous sudo mvn runs
sudo rm -rf target/
mvn compile
```
