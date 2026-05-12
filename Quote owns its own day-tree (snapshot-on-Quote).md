# Plan: Quote owns its own day-tree (snapshot-on-Quote)

## Context

Today the customer-booking flow has three layers, but only two of them are real "snapshots":

- **Itinerary** owns a deep editable day-tree: `Itinerary → ItineraryDay → {ItineraryDayAccommodation, ItineraryDayActivity, ItineraryDayPark → ItineraryDayParkTariff, ItineraryDayParkActivity}` + `ItineraryPax`. Cascades + `orphanRemoval=true` everywhere.
- **Safari** owns the *same* tree shape and is created by deep-copying from **Itinerary** in `SafariCreateService` (see lines 192–429 — `createSafariFromItinerary`, `cloneItineraryPax`, `cloneItineraryDays`, etc.). `SafariPax` already exists and is a copy of `ItineraryPax`.
- **Quote** has *no* day-tree and *no* pax. It only holds flat `QuoteItem` price lines derived from `ItineraryCostEstimationService`. It just references the Itinerary by FK.

That asymmetry is the bug. When a customer asks for a 5-day safari and the staff need to tweak accommodation/pax for that customer (2 → 4 → 14 pax across negotiation rounds), there is no place for those tweaks to live except on the Itinerary itself — which then gets bent for the *next* customer too. And the conversion path `QuoteStatusService.convertQuote()` (lines 579–699) explicitly calls `safariCreateService.createSafariFromItinerary(...)` using `quote.getItinerary().getId()` — so any per-customer customization at quote stage would be lost anyway, because Safari is snapshotted from Itinerary, not from Quote.

**Goal:** make Quote the per-customer working copy. Quote owns its own day-tree and pax (snapshotted from Itinerary at quote creation). Safari is then snapshotted from Quote (not from Itinerary). The Itinerary catalog stays clean and reusable.

### Locked design decisions (from clarifying questions)

1. **Rollout:** full end-to-end in one PR (backend + frontend).
2. **`QuoteItem` becomes a derived, read-only summary** auto-computed by walking `QuoteDay × QuotePax` (same way `ItineraryCostEstimationService` walks Itinerary today). Staff edits the day-tree, prices follow.
3. **Legacy quotes** (no day-tree, created before this ships) are left untouched. The new Days/Pax tabs show a "Created before snapshot mode — Days/Pax tab unavailable" banner. Conversion to Safari still works via the existing Itinerary-based fallback. No batch backfill.

## Architecture overview

Mirror the **Safari** day-tree structure exactly on the Quote side, then make Itinerary→Quote and Quote→Safari each do a deep clone.

| Money out (Itinerary, exists) | Negotiation (Quote, **new**) | Live booking (Safari, exists) |
|---|---|---|
| `Itinerary` | `Quote` (already exists, gets `days`/`paxList` collections) | `Safari` |
| `ItineraryDay` | `QuoteDay` (new) | `SafariDay` |
| `ItineraryDayAccommodation` | `QuoteDayAccommodation` (new) | `SafariDayAccommodation` |
| `ItineraryDayActivity` | `QuoteDayActivity` (new) | `SafariDayActivity` |
| `ItineraryDayPark` | `QuoteDayPark` (new) | `SafariDayPark` |
| `ItineraryDayParkTariff` | `QuoteDayParkTariff` (new) | `SafariDayParkTariff` |
| `ItineraryDayParkActivity` | `QuoteDayParkActivity` (new) | `SafariDayParkActivity` |
| `ItineraryPax` | `QuotePax` (new) | `SafariPax` |
| `ItineraryCostEstimationService` | `QuoteCostEstimationService` (new, derives `QuoteItem` list) | `SafariCostEstimationService` |

`Customer`, `Vehicle`, `User` (createdBy), `Hashids` `IdObfuscator`, `ApiResponse<T>`, JPA `Specification`, the PDF + email infrastructure, the multi-currency `Price` embeddable are all reused as-is.

## Domain model

The Quote-side entities mirror the Safari-side entities **structurally** but drop the *operational* fields (those only make sense on a live booking). The drops are listed inline below.

### `Quote` (existing entity, gets two new collections)

Add to [Quote.java](src/main/java/com/itineraryledger/kabengosafaris/Quote/Entity/Quote.java):

```java
@OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("dayNumber ASC")
private List<QuoteDay> days = new ArrayList<>();

@OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
private List<QuotePax> paxList = new ArrayList<>();
```

### `QuoteDay` (new, mirror of `SafariDay`)

Copy of [SafariDay.java](src/main/java/com/itineraryledger/kabengosafaris/Safari/SafariDay/Entity/SafariDay.java), drop the operational-only fields: `actualDate`, `actualStartTime`, `actualEndTime`, `weatherNotes`, `driverNotes`. Keep `dayNumber`, `dayTag`, `title`, all description fields, `startLocation`, `endLocation`, `distanceKm`, `isOvernight`, `mealsIncluded`, `internalNotes`. Children:

- `OneToMany QuoteDayAccommodation` (cascade ALL, orphanRemoval)
- `OneToMany QuoteDayActivity` (cascade ALL, orphanRemoval)
- `OneToMany QuoteDayPark` (cascade ALL, orphanRemoval)

### `QuoteDayAccommodation` (new)

Mirror of [SafariDayAccommodation.java](src/main/java/com/itineraryledger/kabengosafaris/Safari/SafariDay/SafariDayAccommodation/Entity/SafariDayAccommodation.java). Drop: `confirmationNumber`, `confirmedAt`, `checkInTime`, `checkOutTime`, `roomNumbers`, `guestFeedback`, `specialArrangements`, `bookingStatus`. Keep: `accommodation`, `roomType`, `roomStandard`, `boardType` FK refs; `roomCount`, `isAlternative`.

### `QuoteDayActivity` (new)

Mirror of `SafariDayActivity`. Drop: `isCompleted`, `completedAt`, `actualStartTime`, `actualEndTime`, `feedback`, `isSkipped`, `skipReason`. Keep: `activity` FK, `sortOrder`, `durationHours`, `startTime`, `endTime`, `notes`, `isIncludedInPrice`, `isOptional`.

### `QuoteDayPark` (new)

Mirror of `SafariDayPark`. Drop ops fields (`actualArrivalTime`, `actualDepartureTime`, `entryReceiptNumber`, wildlife sightings, visit notes, `feesPaid`). Keep `park` FK, `entryType`, `sortOrder`, `arrivalTime`, `departureTime`, `notes`. Children:

- `OneToMany QuoteDayParkActivity` (cascade ALL, orphanRemoval)
- `OneToMany QuoteDayParkTariff` (cascade ALL, orphanRemoval)

### `QuoteDayParkActivity` + `QuoteDayParkTariff` (new)

Direct mirrors of their Safari counterparts (these entities have almost no ops fields anyway).

### `QuotePax` (new)

Direct mirror of [SafariPax.java](src/main/java/com/itineraryledger/kabengosafaris/Safari/SafariPax/Entity/SafariPax.java), drop `specialRequirements` (that one really is operational). Keep `nationCategory`, `ageCategory`, `count`, `notes`. Unique constraint on `(quote_id, nation_category_id, age_category_id)`.

## File-by-file work

### A. Backend — entities & repos (new files)

Under `src/main/java/com/itineraryledger/kabengosafaris/Quote/`:

- `QuoteDay/Entity/QuoteDay.java`
- `QuoteDay/Repository/QuoteDayRepository.java`
- `QuoteDay/QuoteDayAccommodation/Entity/QuoteDayAccommodation.java`
- `QuoteDay/QuoteDayAccommodation/Repository/QuoteDayAccommodationRepository.java`
- `QuoteDay/QuoteDayActivity/Entity/QuoteDayActivity.java`
- `QuoteDay/QuoteDayActivity/Repository/QuoteDayActivityRepository.java`
- `QuoteDay/QuoteDayPark/Entity/QuoteDayPark.java`
- `QuoteDay/QuoteDayPark/Repository/QuoteDayParkRepository.java`
- `QuoteDay/QuoteDayPark/QuoteDayParkActivity/Entity/QuoteDayParkActivity.java`
- `QuoteDay/QuoteDayPark/QuoteDayParkActivity/Repository/QuoteDayParkActivityRepository.java`
- `QuoteDay/QuoteDayPark/QuoteDayParkTariff/Entity/QuoteDayParkTariff.java`
- `QuoteDay/QuoteDayPark/QuoteDayParkTariff/Repository/QuoteDayParkTariffRepository.java`
- `QuotePax/Entity/QuotePax.java`
- `QuotePax/Repository/QuotePaxRepository.java`

**Modified file:** [Quote.java](src/main/java/com/itineraryledger/kabengosafaris/Quote/Entity/Quote.java) — add `days` and `paxList` collections.

### B. Backend — deep-copy services

**Modified:** [QuoteFromItineraryGenerationService.java](src/main/java/com/itineraryledger/kabengosafaris/Quote/Services/QuoteServices/QuoteFromItineraryGenerationService.java)
- After creating the `Quote` entity (around line 162), deep-copy `ItineraryPax` → `QuotePax` and `ItineraryDay` tree → `QuoteDay` tree using the same pattern as `SafariCreateService` (lines 192–429).
- Replace the `createQuoteItemsFromEstimation(quoteId, costEstimation, condenseLineItems)` call with `quoteCostEstimationService.estimateAndPersistItems(quoteId)` (see C below).
- Extract the per-child clone loops as private helpers (`cloneAccommodations`, `cloneActivities`, `cloneParks`, `cloneParkTariffs`, `cloneParkActivities`) — mirror the names already used in `SafariCreateService` so the two files read the same way.

**New / modified:** [SafariCreateService.java](src/main/java/com/itineraryledger/kabengosafaris/Safari/Services/SafariCreateService.java)
- Add `createSafariFromQuote(Long quoteId, LocalDate startDate)` that deep-copies from `QuoteDay`/`QuotePax` (not Itinerary). Same clone loops as `createSafariFromItinerary` but reading the Quote tree.
- Keep `createSafariFromItinerary` for legacy quotes that have no day-tree (fallback).

**Modified:** [QuoteStatusService.java](src/main/java/com/itineraryledger/kabengosafaris/Quote/Services/QuoteServices/QuoteStatusService.java) `convertQuote()` (lines 579–699)
- Check `quote.getDays().isEmpty()`. If non-empty → `safariCreateService.createSafariFromQuote(quote.getId(), resolvedStartDate)`. Else (legacy) → fall back to existing `createSafariFromItinerary(...)` path.

### C. Backend — Quote cost estimation (replaces flat item generation)

**New:** `Quote/Services/QuoteServices/QuoteCostEstimationService.java`
- Adapts [ItineraryCostEstimationService.java](src/main/java/com/itineraryledger/kabengosafaris/Itinerary/Services/ItineraryCostEstimationService.java) to walk the Quote's own day-tree × `QuotePax`.
- Exposes `estimateAndPersistItems(Long quoteId)` which: (a) computes the cost breakdown, (b) deletes the Quote's existing `QuoteItem` rows, (c) writes the new derived rows, (d) calls `QuoteTotalsCalculationService` to refresh subtotals/taxes/discounts/grand totals.
- Wired in to fire from: `QuoteFromItineraryGenerationService` (initial creation), every Quote day-tree write (accommodation/activity/park add/update/delete), and every `QuotePax` write.

**Modified:** [QuoteItemRepository.java](src/main/java/com/itineraryledger/kabengosafaris/Quote/Repository/QuoteItemRepository.java) — add `deleteByQuoteId(Long quoteId)` if not already present.

**Modified:** Existing `QuoteItem` CRUD endpoints — gate writes (POST/PUT/DELETE) so they only succeed for legacy quotes (`days.isEmpty()`). For new quotes, items are read-only because they're derived.

### D. Backend — CRUD APIs (mirror Safari controllers)

For each new entity, mirror the existing Safari controller pattern. The Safari controllers to use as templates:

- `Safari/SafariDay/Controller/SafariDayController.java`
- `Safari/SafariDay/SafariDayAccommodation/Controller/...`
- `Safari/SafariDay/SafariDayPark/Controller/...`
- `Safari/SafariDay/SafariDayPark/SafariDayParkTariff/Controller/...`
- `Safari/SafariDay/SafariDayPark/SafariDayParkActivity/Controller/...`
- `Safari/SafariDay/SafariDayActivity/Controller/...`
- `Safari/SafariPax/Controller/SafariPaxController.java`

New Quote-side controllers + DTOs + GetService/UpsertService/DeleteService:

- `Quote/QuoteDay/Controller/QuoteDayController.java`  →  `/api/quotes/{id}/days`, `/api/quote-days/{dayId}`
- `Quote/QuoteDay/QuoteDayAccommodation/Controller/QuoteDayAccommodationController.java`
- `Quote/QuoteDay/QuoteDayActivity/Controller/QuoteDayActivityController.java`
- `Quote/QuoteDay/QuoteDayPark/Controller/QuoteDayParkController.java`
- `Quote/QuoteDay/QuoteDayPark/QuoteDayParkTariff/Controller/QuoteDayParkTariffController.java`
- `Quote/QuoteDay/QuoteDayPark/QuoteDayParkActivity/Controller/QuoteDayParkActivityController.java`
- `Quote/QuotePax/Controller/QuotePaxController.java`  →  `/api/quotes/{id}/pax`

Each write endpoint **must** trigger `quoteCostEstimationService.estimateAndPersistItems(quoteId)` on success (centralize via service helper / `@TransactionalEventListener` to avoid forgetting).

### E. Backend — permissions seed

**Modified:** `src/main/resources/permissions/entities.json` — append:

```
"QUOTE_DAY",
"QUOTE_DAY_ACCOMMODATION",
"QUOTE_DAY_ACTIVITY",
"QUOTE_DAY_PARK",
"QUOTE_DAY_PARK_ACTIVITY",
"QUOTE_DAY_PARK_TARIFF",
"QUOTE_PAX"
```

(Each generates the standard `PERM_CREATE_*` / `PERM_READ_*` / `PERM_UPDATE_*` / `PERM_DELETE_*` quartet via the existing initializer.)

### F. Frontend — Quote day pages (mirror Safari day pages)

The Safari day-page tree to mirror lives under [src/pages/Safaris/views/SafariDay/](src/pages/Safaris/views/SafariDay/). Clone it into `src/pages/Quotes/views/QuoteDay/`:

- `Quotes/views/QuoteDay/QuoteDayViewPage.jsx`  ← clone of [SafariDayViewPage.jsx](src/pages/Safaris/views/SafariDay/SafariDayViewPage.jsx). Strip Safari-only "Operational" tab and ops fields (`weatherNotes`, `actualStartTime`/`actualEndTime`, `driverNotes`).
- `Quotes/views/QuoteDay/QuoteDayAccommodation/QuoteDayAccommodationViewPage.jsx`  ← clone of [SafariDayAccommodationViewPage.jsx](src/pages/Safaris/views/SafariDay/SafariDayAccommodation/SafariDayAccommodationViewPage.jsx). Drop the "Booking" tab (`confirmationNumber`, `bookingStatus`, etc.).
- `Quotes/views/QuoteDay/QuoteDayPark/QuoteDayParkViewPage.jsx`
- `Quotes/views/QuoteDay/QuoteDayPark/QuoteDayParkTariff/QuoteDayParkTariffViewPage.jsx`
- `Quotes/views/QuoteDay/QuoteDayPark/QuoteDayParkActivity/QuoteDayParkActivityViewPage.jsx`
- `Quotes/views/QuoteDay/QuoteDayActivity/QuoteDayActivityViewPage.jsx`

Re-use the existing form-field components (`StringField`, `SelectField`, `BooleanField`, `DateField`, `TextAreaField`, `NumberField`) from [SettingsPages/components/FormFields.jsx](src/pages/SettingsPages/components/FormFields.jsx) — same canonical pattern we already use everywhere.

### G. Frontend — Quote pax page

`src/pages/Quotes/views/QuotePax/QuotePaxViewPage.jsx`  ← clone of [SafariPaxViewPage.jsx](src/pages/Safaris/views/SafariPax/SafariPaxViewPage.jsx). Drop the `specialRequirements` field.

### H. Frontend — QuoteViewPage updates

**Modified:** [QuoteViewPage.jsx](src/pages/Quotes/views/QuoteViewPage.jsx) — add two new tabs to the existing `VALID_TABS` list:

- **Days** — between "Items" and "Notes". Render a section mirroring [SafariDaysSection.jsx](src/pages/Safaris/views/components/SafariDaysSection.jsx) (clone into `Quotes/views/components/QuoteDaysSection.jsx`). If `quote.days?.length === 0`, show a single MUI `Alert` with the legacy banner: *"This quote was created before snapshot mode. Day-by-day editing is not available — the price list (Items tab) is the source of truth."* Hide the Add/Reorder controls in that case.
- **Pax** — between "Days" and "Notes". Same legacy-banner handling.

The legacy banner is the only place we treat old quotes specially; everything else (Items, Pricing, Workflow) already works for both.

### I. Frontend — routing

**Modified:** [App.jsx](src/App.jsx) — register the new routes (mirror the safari route shape):

- `/quotes/:id/days/:dayId`  →  `QuoteDayViewPage`
- `/quotes/:id/days/:dayId/accommodations/:accId`  →  `QuoteDayAccommodationViewPage`
- `/quotes/:id/days/:dayId/activities/:actId`  →  `QuoteDayActivityViewPage`
- `/quotes/:id/days/:dayId/parks/:parkId`  →  `QuoteDayParkViewPage`
- `/quotes/:id/days/:dayId/parks/:parkId/tariffs/:tariffId`  →  `QuoteDayParkTariffViewPage`
- `/quotes/:id/days/:dayId/parks/:parkId/activities/:actId`  →  `QuoteDayParkActivityViewPage`
- `/quotes/:id/pax/:paxId`  →  `QuotePaxViewPage`

### J. Frontend — flow message updates (small)

- [AddQuoteModal.jsx](src/pages/Quotes/components/AddQuoteModal.jsx) — append a one-line caption under the Itinerary picker: *"The quote will snapshot this itinerary's days, accommodations, parks, activities and pax. You can edit the snapshot freely without affecting the itinerary."*
- [ConvertQuoteModal.jsx](src/pages/Quotes/views/tabs/workflow/ConvertQuoteModal.jsx) — no UI change; backend automatically picks the Quote-tree path for new quotes and falls back to Itinerary for legacy quotes.
- [useDocumentTitle.js](src/hooks/useDocumentTitle.js) + [Topbar.jsx](src/pages/layouts/Topbar.jsx) — add titles + searchable entries for the new quote-day routes (cheap, like we did for vendors/expenses).

## Things to consciously NOT do (scope guard)

- **No itinerary-copy API.** We rejected the "clone the entire Itinerary into the catalog" approach because it pollutes the template shelf. Snapshot lives on the Quote.
- **No re-sync button** ("pull latest changes from Itinerary into this Quote"). Quotes go stale on purpose — a quote sent yesterday should not silently re-price.
- **No backfill** of existing quotes. Legacy ones show the banner forever. The Convert-to-Safari fallback uses the Itinerary path.
- **No QuoteItem hand-editing for new quotes.** It's derived. Staff edits the day-tree.
- **No "save quote-customization back as a template"** button yet. Easy follow-up if real demand surfaces — it's a single endpoint (`POST /api/itineraries/from-quote/{id}`) that mirrors `QuoteFromItineraryGenerationService` in reverse.

## Verification

End-to-end, against `sudo ./run-local.sh`:

1. **New-quote happy path**
   - `POST /api/quotes/generate-from-itinerary` with an Itinerary that has 5 days, 2 pax-rows, accommodation, parks, activities → response 201 + new Quote whose `GET /api/quotes/{id}/days` returns 5 days, `GET /api/quotes/{id}/pax` returns the 2 pax-rows, `GET /api/quotes/{id}/items` returns derived items.
2. **Edit the quote, not the itinerary**
   - `PUT /api/quote-days/{dayId}/accommodations/{accId}` change `roomCount` from 1 → 7. Reload Quote → totals reflect 7 rooms. Reload **Itinerary** → still 1 room (unchanged).
3. **Pax change cascades to totals**
   - `POST /api/quotes/{id}/pax` add a third pax-row (e.g. 12 more adults) → Quote totals recalc, Itinerary unchanged.
4. **Convert to Safari snapshots the Quote**
   - Approve the Quote (status → ACCEPTED), `POST /api/quotes/{id}/convert` → new Safari. `GET /api/safaris/{id}/days/{dayId}/accommodations` shows `roomCount=7`. `GET /api/safaris/{id}/pax` shows the third pax-row. Edit the original Itinerary → Safari unchanged.
5. **Legacy quote still works**
   - Open a Quote created before this deploy → Days tab shows the legacy banner; Pax tab too. `POST /api/quotes/{id}/convert` → Safari is created via the Itinerary fallback path; succeeds.
6. **Permissions**
   - A user without `PERM_UPDATE_QUOTE_DAY_ACCOMMODATION` cannot change room counts on a Quote.
7. **Build & test gates**
   - `mvn -DskipTests compile` clean (modulo the known root-owned `target/` quirk we work around with the alternate-target trick).
   - `npm run build` clean.

Frontend smoke run:

1. `/quotes` → Add Quote modal → caption explains snapshot behaviour. Create.
2. Land on `/quotes/{id}` → **Days** and **Pax** tabs present.
3. Click into a day → accommodation page → change roomCount → save → reload → persists.
4. Pax tab → change adult count → save → Items tab shows new totals.
5. Approve & convert → Safari detail page opens with the edited values, not the itinerary defaults.
6. Open a known legacy quote → banner shows on Days/Pax tabs; everything else still works.

## Risks & open follow-ups (not blocking)

- **Migration of legacy quotes**: if the user later wants the new flow on old quotes too, the one-time backfill is straightforward (call the deep-copy service for each non-CONVERTED quote). Out of scope for this PR.
- **Performance**: deep-copy on a 14-day itinerary with 3 parks/day and 4 pax-rows is ~80 inserts. Fine for one-off operations but worth a transactional batch insert if we ever support 30+ day itineraries.
- **Cost recalc storm**: every per-child write triggers `estimateAndPersistItems`. If staff does many edits in one screen, we'll recompute N times. Easy follow-up: collapse via a debounced/coalesced "Recalculate now" trigger, or do it on tab-blur. Out of scope for v1.
- **"Save customization as new template"**: the inverse direction (Quote → new Itinerary). Single endpoint, mirrors `QuoteFromItineraryGenerationService`. Add only when staff actually ask for it.
- **`QuoteDocument` reuse**: the new Quote-day tree doesn't need new document attachments — the existing `QuoteDocument` table covers PDFs/contracts at the Quote level. No change needed.
