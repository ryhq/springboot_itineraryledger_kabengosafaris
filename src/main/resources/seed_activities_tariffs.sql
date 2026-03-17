-- =============================================
-- Seed: Activities, Tariffs & Standalone Activities
-- for all 46 itinerary packages
-- =============================================

SET @now = NOW();

-- =============================================
-- STEP 1: Setup missing park-activity & park-tariff links
-- =============================================

-- Jozani Chwaka Bay NP (27): Add Walking Safari(1), Others(8)
INSERT IGNORE INTO parks_activities (park_id, activity_id) VALUES (27, 1), (27, 8);

-- Jozani: Add Conservation Fee tariff
INSERT IGNORE INTO parks_tariffs (park_id, tariff_id) VALUES (27, 1);

-- Kilimanjaro (12): Add Walking Safari(1), Ranger(2) — essential for trekking
INSERT IGNORE INTO parks_activities (park_id, activity_id) VALUES (12, 1), (12, 2);

-- =============================================
-- STEP 2: Park Tariffs (itinerary_day_park_tariffs)
-- Every park day gets Conservation Fee (tariff_id=1)
-- Serengeti also gets Concession Fee (tariff_id=10)
-- Kilimanjaro also gets Rescue Fee (tariff_id=9)
-- =============================================

-- Conservation Fee for ALL day-parks (every park has this)
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 1
);

-- Concession Fee for Serengeti (park_id=1) day-parks
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 10, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1
AND idp.park_id = 1
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 10
);

-- Rescue Fee for Kilimanjaro (park_id=12) day-parks
INSERT INTO itinerary_day_park_tariffs (itinerary_day_park_id, park_id, tariff_id, is_included_in_price, created_at)
SELECT idp.id, idp.park_id, 9, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1
AND idp.park_id = 12
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_tariffs t
    WHERE t.itinerary_day_park_id = idp.id AND t.tariff_id = 9
);

-- =============================================
-- STEP 3: Park Activities (itinerary_day_park_activities)
-- Core activity per park for every day-park entry
-- =============================================

-- Serengeti (1): Walking Safari (activity_id=1)
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 1, 1, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 1
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 1
);

-- Ngorongoro (2): Crater Service (activity_id=14) — mandatory crater vehicle fee
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 14, 2, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 2
AND idp.entry_type IN ('DAY_TRIP', 'SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 14
);

-- Tarangire (3): Walking Safari (activity_id=1)
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 1, 3, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 3
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 1
);

-- Manyara (4): Walking Safari (activity_id=1)
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 1, 4, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 4
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 1
);

-- Arusha NP (9): Walking Safari (activity_id=1)
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 1, 9, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 9
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 1
);

-- Arusha NP (9): Canoeing (activity_id=6) — Momella Lakes canoe safari
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 6, 9, 1, 2, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 9
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 6
);

-- Kilimanjaro (12): Walking Safari (activity_id=1) — trekking
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 1, 12, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 12
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 1
);

-- Kilimanjaro (12): Ranger (activity_id=2) — mandatory for trekking
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 2, 12, 1, 2, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 12
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 2
);

-- Mkomazi (14): Rhino Viewing (activity_id=13)
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 13, 14, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 14
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 13
);

-- Jozani (27): Walking Safari (activity_id=1) — forest walk
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 1, 27, 1, 1, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
WHERE d.itinerary_id >= 1 AND idp.park_id = 27
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 1
);

-- =============================================
-- STEP 3b: Budget/Type-dependent EXTRA park activities
-- =============================================

-- LUXURY/ULTRA_LUXURY Ngorongoro: Hot Air Balloon (activity_id=16) — optional
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, notes, created_at)
SELECT idp.id, 16, 2, 0, 2, 'Optional — available at additional cost', @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 2
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type IN ('DAY_TRIP', 'SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 16
);

-- HONEYMOON Ngorongoro: Sun Down (activity_id=15) — included
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, notes, created_at)
SELECT idp.id, 15, 2, 1, 3, 'Romantic sunset experience on crater rim', @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 2
AND i.trip_type = 'HONEYMOON'
AND idp.entry_type IN ('DAY_TRIP', 'SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 15
);

-- LUXURY/ULTRA_LUXURY Serengeti: Bush Meals (activity_id=3) — included
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 3, 1, 1, 2, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 1
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type IN ('SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 3
);

-- Tarangire Night Game Drive (activity_id=5) for LUXURY/PRIVATE overnight stays
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 5, 3, 0, 2, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 3
AND i.budget_category IN ('LUXURY', 'ULTRA_LUXURY')
AND idp.entry_type = 'SLEEP_OVER'
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 5
);

-- Manyara Canopy Walk (activity_id=4) for LUXURY/ADVENTURE types
INSERT INTO itinerary_day_park_activities (itinerary_day_park_id, activity_id, park_id, is_included_in_price, sort_order, created_at)
SELECT idp.id, 4, 4, 0, 2, @now
FROM itinerary_day_parks idp
JOIN itinerary_days d ON d.id = idp.itinerary_day_id
JOIN itineraries i ON i.id = d.itinerary_id
WHERE i.id >= 1
AND idp.park_id = 4
AND i.trip_type IN ('ADVENTURE', 'PHOTOGRAPHY', 'PRIVATE')
AND idp.entry_type IN ('DAY_TRIP', 'SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_park_activities a
    WHERE a.itinerary_day_park_id = idp.id AND a.activity_id = 4
);

-- =============================================
-- STEP 4: Standalone Activities (itinerary_day_activities)
-- For days WITHOUT park visits + type-based extras
-- =============================================

-- Cultural Tours (activity_id=22) for days with no park (Zanzibar beach, Arusha rest, city tours)
INSERT INTO itinerary_day_activities (itinerary_day_id, activity_id, is_included_in_price, is_optional, sort_order, created_at)
SELECT d.id, 22, 1, 0, 1, @now
FROM itinerary_days d
LEFT JOIN itinerary_day_parks idp ON idp.itinerary_day_id = d.id
WHERE d.itinerary_id >= 1
AND idp.id IS NULL
AND d.title NOT LIKE '%Departure%'
AND d.title NOT LIKE '%Final Morning%'
AND d.title NOT LIKE '%Return%'
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_activities a
    WHERE a.itinerary_day_id = d.id AND a.activity_id = 22
);

-- Bird Watching (activity_id=21) for Manyara & Arusha park days (famous birding parks)
INSERT INTO itinerary_day_activities (itinerary_day_id, activity_id, is_included_in_price, is_optional, sort_order, created_at)
SELECT DISTINCT d.id, 21, 1, 1, 2, @now
FROM itinerary_days d
JOIN itinerary_day_parks idp ON idp.itinerary_day_id = d.id
WHERE d.itinerary_id >= 1
AND idp.park_id IN (4, 9)
AND idp.entry_type IN ('DAY_TRIP', 'SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_activities a
    WHERE a.itinerary_day_id = d.id AND a.activity_id = 21
);

-- Photography Safaris (activity_id=23) for PHOTOGRAPHY trip type itineraries
INSERT INTO itinerary_day_activities (itinerary_day_id, activity_id, is_included_in_price, is_optional, sort_order, created_at)
SELECT d.id, 23, 1, 0, 1, @now
FROM itinerary_days d
JOIN itineraries i ON i.id = d.itinerary_id
JOIN itinerary_day_parks idp ON idp.itinerary_day_id = d.id
WHERE i.id >= 1
AND i.trip_type = 'PHOTOGRAPHY'
AND idp.entry_type IN ('DAY_TRIP', 'SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_activities a
    WHERE a.itinerary_day_id = d.id AND a.activity_id = 23
);

-- Wildlife Tracking (activity_id=25) for ADVENTURE trip type on safari days
INSERT INTO itinerary_day_activities (itinerary_day_id, activity_id, is_included_in_price, is_optional, sort_order, created_at)
SELECT DISTINCT d.id, 25, 1, 1, 2, @now
FROM itinerary_days d
JOIN itineraries i ON i.id = d.itinerary_id
JOIN itinerary_day_parks idp ON idp.itinerary_day_id = d.id
WHERE i.id >= 1
AND i.trip_type = 'ADVENTURE'
AND idp.park_id IN (1, 3, 14)
AND idp.entry_type IN ('DAY_TRIP', 'SLEEP_OVER')
AND NOT EXISTS (
    SELECT 1 FROM itinerary_day_activities a
    WHERE a.itinerary_day_id = d.id AND a.activity_id = 25
);
