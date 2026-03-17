-- =============================================
-- Seed default pax: 1 Non-Resident Adult per itinerary
-- nation_category_id=4 (Non-Resident), age_category_id=3 (Adult), count=1
-- =============================================

SET @now = NOW();

INSERT INTO itinerary_pax (itinerary_id, nation_category_id, age_category_id, count, created_at, updated_at)
SELECT i.id, 4, 3, 1, @now, @now
FROM itineraries i
WHERE i.id >= 1
AND NOT EXISTS (
    SELECT 1 FROM itinerary_pax p
    WHERE p.itinerary_id = i.id AND p.nation_category_id = 4 AND p.age_category_id = 3
);
