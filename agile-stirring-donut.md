# Plan: Add Sortable Fields & Circular Navigation to All GET APIs

## Context
The kabengosafaris GET APIs currently return paginated lists without advertising which fields are sortable, and single-entity GETs return only the entity DTO with no way to navigate to next/previous records. The reference project (nguserosdachurch) implements both patterns. This plan replicates those patterns across the entire system (~68 GetServices, ~49 repositories).

**User decisions:**
- Tier 4 child entities (ItineraryDay, SafariDay children): Add parent-scoped navigation
- Unpaginated endpoints: Add sortable fields to ALL list endpoints
- FullGetService files: Add navigation to both regular and Full get endpoints

---

## Feature 1: Sortable Fields in All List Responses

### Pattern (per GetService)

```java
// 1. Constants at class level
private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
    "name", "slug", "isActive", "createdAt", "updatedAt"
);
private static final String DEFAULT_SORT_FIELD = "createdAt";

// 2. Validation method
private String validateSortField(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
    for (String field : VALID_SORT_FIELDS) {
        if (field.equalsIgnoreCase(sortBy)) return field;
    }
    return null;
}

// 3. In getAll method - validate and return error if invalid
String validatedSortBy = validateSortField(sortBy);
if (validatedSortBy == null) {
    return ResponseEntity.badRequest().body(
        ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
    );
}

// 4. Add to response map
response.put("validSortFields", VALID_SORT_FIELDS);
response.put("currentSortBy", validatedSortBy);
response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");
```

### For unpaginated endpoints
Same pattern but without pagination metadata — just add `validSortFields`, `currentSortBy`, `currentSortDir` to the response.

---

## Feature 2: Next/Previous ID for Circular Navigation

### Repository Pattern (for standard entities with Long ID)

```java
// Add to each repository
@Query("SELECT r.id FROM EntityName r WHERE r.id > :currentId ORDER BY r.id ASC LIMIT 1")
Optional<Long> findNextId(@Param("currentId") Long currentId);

@Query("SELECT r.id FROM EntityName r WHERE r.id < :currentId ORDER BY r.id DESC LIMIT 1")
Optional<Long> findPreviousId(@Param("currentId") Long currentId);

@Query("SELECT r.id FROM EntityName r ORDER BY r.id ASC LIMIT 1")
Optional<Long> findFirstId();

@Query("SELECT r.id FROM EntityName r ORDER BY r.id DESC LIMIT 1")
Optional<Long> findLastId();
```

### Repository Pattern (for parent-scoped Tier 4 entities)

```java
@Query("SELECT d.id FROM ItineraryDay d WHERE d.itinerary.id = :parentId AND d.id > :currentId ORDER BY d.id ASC LIMIT 1")
Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);
// ... similar for previous/first/last with parent scope
```

### GetService Pattern (in getById / getFullXxx)

```java
// After fetching entity, before building response:
Long nextId = repository.findNextId(entityId).orElse(null);
Long previousId = repository.findPreviousId(entityId).orElse(null);

// Circular wrap-around
if (nextId == null) nextId = repository.findFirstId().orElse(null);
if (previousId == null) previousId = repository.findLastId().orElse(null);

// Build response with navigation
Map<String, Object> response = new HashMap<>();
response.put("entityKey", entityDTO);  // e.g., "customer", "park"
response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
```

**Note:** This changes getById return from `ApiResponse.success(200, msg, dto)` to `ApiResponse.success(200, msg, mapWithNavigation)`.

---

## Implementation Phases

### Phase 1: Top-Level Entities (19 modules)
Each module: modify GetService + Repository. FullGetServices share the parent repository.

| # | Module | GetService(s) | Repository | VALID_SORT_FIELDS | DEFAULT_SORT |
|---|--------|--------------|------------|-------------------|-------------|
| 1 | Customer | CustomerGetService | CustomerRepository | `code, firstName, lastName, companyName, customerType, nationality, country, city, source, isVip, isBlacklisted, isActive, totalBookings, totalSpent, lastBookingDate, createdAt, updatedAt` | createdAt |
| 2 | Accommodation | AccommodationGetService | AccommodationRepository | `name, slug, accommodationType, category, region, district, starRating, totalRooms, maxGuests, isActive, createdAt, updatedAt` | createdAt |
| 3 | Park | ParkGetService | ParkRepository | `name, slug, parkType, region, district, isActive, createdAt, updatedAt` | createdAt |
| 4 | Activity | ActivityGetService | ActivityRepository | `name, slug, minimumAge, maximumParticipants, isActive, createdAt, updatedAt` | createdAt |
| 5 | Itinerary | ItineraryGetService, ItineraryFullGetService | ItineraryRepository | `name, code, tripType, budgetCategory, totalDays, totalNights, status, createdAt, updatedAt` | createdAt |
| 6 | Safari | SafariGetService, SafariFullGetService | SafariRepository | `name, code, slug, startDate, endDate, totalDays, totalNights, state, isActive, createdAt, updatedAt` | createdAt |
| 7 | Quote | QuoteGetService, QuoteFullGetService | QuoteRepository | `quoteCode, title, status, sentDate, validFrom, validTo, isActive, createdAt, updatedAt` | createdAt |
| 8 | Invoice | InvoiceGetService, InvoiceFullGetService | InvoiceRepository | `invoiceCode, title, status, issueDate, dueDate, sentDate, paidDate, isActive, createdAt, updatedAt` | createdAt |
| 9 | Role | RoleGetService | RoleRepository | `name, displayName, active, isSystemRole, createdAt, updatedAt` | createdAt |
| 10 | Hero | HeroGetService | HeroRepository | `title, page, createdAt, updatedAt` | createdAt |
| 11 | BankAccount | BankAccountGetService | BankAccountRepository | `accountCode, accountName, bankName, currency, isActive, isDefault, createdAt, updatedAt` | createdAt |
| 12 | EmailAccount | EmailAccountGetService | EmailAccountRepository | `email, name, providerType, enabled, isDefault, createdAt, updatedAt` | createdAt |
| 13 | EmailEvent | EmailEventGetService | EmailEventRepository | `name, enabled, createdAt, updatedAt` | createdAt |
| 14 | PdfDocument | PdfDocumentGetService | PdfDocumentRepository | `name, displayName, enabled, createdAt, updatedAt` | createdAt |
| 15 | Season | GetSeasonService | SeasonRepository | `name, seasonType, createdAt, updatedAt` | createdAt |
| 16 | SeasonPeriod | GetSeasonPeriodService | SeasonPeriodRepository | `year, createdAt, updatedAt` | createdAt |
| 17 | Tariff | GetTariffService | TariffRepository | `name, slug, chargingBasis, isActive, createdAt, updatedAt` | createdAt |
| 18 | PaxAgeCategory | GetPaxAgeCategoryService | PaxAgeCategoryRepository | `name, categoryType, minAge, maxAge, isActive, createdAt, updatedAt` | createdAt |
| 19 | PaxNationCategory | GetPaxNationCategoryService | PaxNationCategoryRepository | `name, categoryType, priorityFactor, isActive, createdAt, updatedAt` | createdAt |

### Phase 2: Rate Entities (3 modules)

| # | Module | GetService | Repository | VALID_SORT_FIELDS | DEFAULT_SORT |
|---|--------|-----------|------------|-------------------|-------------|
| 1 | ParkTariffRate | ParkTariffRateGetService | ParkTariffRateRepository | `rackRate, stoRate, currency, isActive, createdAt, updatedAt` | createdAt |
| 2 | ActivityTariffRate | ActivityTariffRateGetService | ActivityTariffRateRepository | `rackRate, stoRate, currency, isActive, createdAt, updatedAt` | createdAt |
| 3 | AccommodationRate | AccommodationRateGetService | AccommodationRateRepository | `rackRate, stoRate, currency, isActive, createdAt, updatedAt` | createdAt |

### Phase 3: Accommodation Sub-entities (6 modules)

| # | GetService | Repository | VALID_SORT_FIELDS |
|---|-----------|------------|-------------------|
| 1 | AccommodationBoardTypeGetService | AccommodationBoardTypeRepository | `name, createdAt, updatedAt` |
| 2 | AccommodationRoomStandardGetService | AccommodationRoomStandardRepository | `name, maxOccupancy, viewType, createdAt, updatedAt` |
| 3 | AccommodationRoomTypeGetService | AccommodationRoomTypeRepository | `name, maxOccupancy, createdAt, updatedAt` |
| 4 | AccommodationEmailGetService | AccommodationEmailRepository | `email, emailType, label, createdAt, updatedAt` |
| 5 | AccommodationPhoneGetService | AccommodationPhoneRepository | `phoneNumber, phoneType, label, createdAt, updatedAt` |
| 6 | AccommodationDocumentGetService | AccommodationDocumentRepository | `title, documentType, fileName, fileSize, createdAt, updatedAt` |

### Phase 4: Customer Sub-entities (4 modules)

| # | GetService | Repository | VALID_SORT_FIELDS |
|---|-----------|------------|-------------------|
| 1 | CustomerEmailGetService | CustomerEmailRepository | `email, emailType, label, isPrimary, isActive, createdAt, updatedAt` |
| 2 | CustomerPhoneGetService | CustomerPhoneRepository | `phoneNumber, phoneType, label, isPrimary, isActive, createdAt, updatedAt` |
| 3 | CustomerNoteGetService | CustomerNoteRepository | `subject, noteType, priority, isPinned, isPrivate, createdAt, updatedAt` |
| 4 | CustomerDocumentGetService | CustomerDocumentRepository | `title, documentType, fileName, fileSize, createdAt, updatedAt` |

### Phase 5: Image Sub-entities (5 modules)

| # | GetService | Repository | VALID_SORT_FIELDS |
|---|-----------|------------|-------------------|
| 1 | AccommodationImageGetService | AccommodationImageRepository | `imageType, isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| 2 | ActivityImageGetService | ActivityImageRepository | `isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| 3 | ParkImageGetService | ParkImageRepository | `imageType, isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| 4 | HeroImageGetService | HeroImageRepository | `isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |
| 5 | ParkActivityImageGetService | ParkActivityImageRepository | `isPrimary, isActive, displayOrder, fileSize, createdAt, updatedAt` |

### Phase 6: Document Sub-entities (7 modules)

| # | GetService | Repository | VALID_SORT_FIELDS |
|---|-----------|------------|-------------------|
| 1 | InvoiceDocumentGetService | InvoiceDocumentRepository | `title, documentType, fileName, fileSize, createdAt, updatedAt` |
| 2 | QuoteDocumentGetService | QuoteDocumentRepository | same |
| 3 | SafariDocumentGetService | SafariDocumentRepository | same |
| 4 | ItineraryDocumentGetService | ItineraryDocumentRepository | same |
| 5 | ParkDocumentGetService | ParkDocumentRepository | same |
| 6 | ActivityDocumentGetService | ActivityDocumentRepository | same |
| 7 | ParkActivityDocumentGetService | ParkActivityDocumentRepository | same |

### Phase 7: Line Items + Templates (5 modules)

| # | GetService | Repository | VALID_SORT_FIELDS |
|---|-----------|------------|-------------------|
| 1 | InvoiceLineItemGetService | InvoiceLineItemRepository | `itemType, itemName, createdAt, updatedAt` |
| 2 | QuoteItemGetService | QuoteItemRepository | `itemType, itemName, createdAt, updatedAt` |
| 3 | EmailTemplateGetService | EmailTemplateRepository | `name, isDefault, enabled, fileSize, createdAt, updatedAt` |
| 4 | EmailAccountSignatureGetService | EmailAccountSignatureRepository | `name, isDefault, enabled, fileSize, createdAt, updatedAt` |
| 5 | PdfTemplateGetService | PdfTemplateRepository | `name, version, createdAt, updatedAt` |

### Phase 8: Tier 4 Child Entities - Parent-Scoped Navigation (14 modules)

These use parent-scoped queries (e.g., `WHERE itinerary.id = :parentId AND id > :currentId`).

**Itinerary children (7):**
| # | GetService | Repository | Has getById? | Navigation scope |
|---|-----------|------------|-------------|-----------------|
| 1 | ItineraryDayGetService | ItineraryDayRepository | Yes | itineraryId |
| 2 | ItineraryDayAccommodationGetService | ItineraryDayAccommodationRepository | No (list only) | Skip nav |
| 3 | ItineraryDayActivityGetService | ItineraryDayActivityRepository | Yes | dayId |
| 4 | ItineraryDayParkGetService | ItineraryDayParkRepository | Yes | dayId |
| 5 | ItineraryDayParkActivityGetService | ItineraryDayParkActivityRepository | Yes | parkVisitId |
| 6 | ItineraryDayParkTariffGetService | ItineraryDayParkTariffRepository | Yes | parkVisitId |
| 7 | ItineraryPaxGetService | ItineraryPaxRepository | No (list only) | Skip nav |

**Safari children (7):** Mirror of Itinerary structure with same patterns.

### Phase 9: Backup Module (special case)
- BackupGetService: Add `VALID_SORT_FIELDS` constant, validate existing sort logic, add to response. No navigation (file-based, not JPA).

---

## Files Modified Summary

| Category | GetServices | Repositories | Total files |
|----------|-----------|-------------|-------------|
| Phase 1: Top-level | 19 + 4 Full = 23 | 19 | 42 |
| Phase 2: Rates | 3 | 3 | 6 |
| Phase 3: Accommodation sub | 6 | 6 | 12 |
| Phase 4: Customer sub | 4 | 4 | 8 |
| Phase 5: Images | 5 | 5 | 10 |
| Phase 6: Documents | 7 | 7 | 14 |
| Phase 7: Line items + templates | 5 | 5 | 10 |
| Phase 8: Tier 4 children | 14 | ~10 | ~24 |
| Phase 9: Backup | 1 | 0 | 1 |
| **TOTAL** | **~68** | **~59** | **~127** |

---

## Edge Cases

1. **Composite key entities** (ParkTariff, ParkActivity): No GetService of their own — no changes needed
2. **Backup module**: File-based, not JPA — sortable fields only, no navigation queries
3. **FullGetServices**: Share parent repository — reuse same navigation queries, no new repo methods
4. **Multiple getAll variants** (e.g., RoleGetService has getAllRoles, getRolesForUser, getRolesForPermission): All variants get sort validation + validSortFields in response
5. **Dropdown/helper endpoints** (getAccommodationsList, getUniqueSeasons, etc.): Add validSortFields to response
6. **Entities without getById** (ItineraryPax, SafariPax, ItineraryDayAccommodation, SafariDayAccommodation): Sortable fields only, skip navigation

---

## Verification

1. **Compile check**: `mvn compile` after each phase to catch errors early
2. **API test**: Hit a representative endpoint for each pattern:
   - `GET /api/customers` → verify `validSortFields`, `currentSortBy`, `currentSortDir` in response
   - `GET /api/customers?sortBy=invalidField` → verify 400 error with valid fields list
   - `GET /api/customers/{id}` → verify `nextId`, `previousId` in response alongside customer DTO
   - Navigate to last entity → verify `nextId` wraps to first (circular)
   - Navigate to first entity → verify `previousId` wraps to last (circular)
3. **Edge case test**: Hit unpaginated endpoints, FullGetService endpoints, Tier 4 parent-scoped navigation
