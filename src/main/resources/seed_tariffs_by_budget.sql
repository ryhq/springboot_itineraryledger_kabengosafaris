-- =============================================
-- Fix tariffs: Add budget-dependent camping/accommodation fees
-- and correct Concession Fee to LUXURY/ULTRA_LUXURY only
-- =============================================

SET @now = NOW();

-- =============================================
-- STEP 1: Remove Concession Fee (10) from Serengeti for non-LUXURY budgets
-- (it was added to all Serengeti day-parks, but should only be LUXURY+)
-- =============================================
DELETE idpt FROM itinerary_day_park_tariffs idpt
JOIN itinerary_day_parks idp ON idp.id = idpt.itinerary_day_park_id
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idpt.tariff_id = 10
AND idp.park_id = 1
AND i.budget_category IN ('BACKPACKER', 'BUDGET', 'MID_RANGE');

-- =============================================
-- STEP 2: Add Concession Fee (10) for LUXURY/ULTRA_LUXURY
-- on ALL parks (not just Serengeti) for SLEEP_OVER days
-- =============================================

-- Ngorongoro LUXURY/ULTRA_LUXURY SLEEP_OVER
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 10, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 2
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type IN ('SLEEP_OVER', 'DAY_TRIP')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 10
);

-- Tarangire LUXURY/ULTRA_LUXURY
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 10, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 3
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type IN ('SLEEP_OVER', 'DAY_TRIP')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 10
);

-- Manyara LUXURY/ULTRA_LUXURY
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 10, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 4
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type IN ('SLEEP_OVER', 'DAY_TRIP')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 10
);

-- Arusha NP LUXURY/ULTRA_LUXURY
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 10, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 9
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type IN ('SLEEP_OVER', 'DAY_TRIP')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 10
);

-- Mkomazi LUXURY/ULTRA_LUXURY
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 10, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 14
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type IN ('SLEEP_OVER', 'DAY_TRIP')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 10
);

-- Kilimanjaro LUXURY/ULTRA_LUXURY (for lodges at gate area)
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 10, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 12
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 10
);

-- =============================================
-- STEP 3: Public Camping Fee (2) for BACKPACKER/BUDGET SLEEP_OVER days
-- =============================================
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 2, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND i.budget_category IN ('BACKPACKER', 'BUDGET')
AND idp.entry_type = 'SLEEP_OVER'
AND idp.park_id IN (1, 2, 3, 4, 9, 12, 14)
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 2
);

-- =============================================
-- STEP 4: Special Camping Fee (3) for MID_RANGE SLEEP_OVER days
-- =============================================
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 3, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND i.budget_category = 'MID_RANGE'
AND idp.entry_type = 'SLEEP_OVER'
AND idp.park_id IN (1, 2, 3, 4, 9, 12, 14)
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 3
);

-- =============================================
-- STEP 5: Kilimanjaro-specific camping fees
-- Upper Barafu Camping Fee (6) for Barafu camp days
-- Parks Accommodations Fee (8) for hut stays (Marangu route)
-- =============================================

-- Upper Barafu Fee for days at Barafu Camp (Kilimanjaro SLEEP_OVER days with "Barafu" in title)
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, 12, 6, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1
AND idp.park_id = 12
AND idp.entry_type = 'SLEEP_OVER'
AND (d.title LIKE '%Barafu%' OR d.end_location LIKE '%Barafu%')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 6
);

-- Parks Accommodations Fee (8) for Marangu route hut stays
-- Marangu route = "Kilimanjaro Marangu — Roof of Africa Trek"
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, 12, 8, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.name = 'Kilimanjaro Marangu — Roof of Africa Trek'
AND idp.park_id = 12
AND idp.entry_type = 'SLEEP_OVER'
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 8
);

-- =============================================
-- STEP 6: WMA (12) for Serengeti and Tarangire (Wildlife Management Area)
-- These parks border WMAs, applicable to all budgets
-- =============================================
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 12, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1
AND idp.park_id IN (1, 3)
AND idp.entry_type IN ('SLEEP_OVER', 'DAY_TRIP')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 12
);
