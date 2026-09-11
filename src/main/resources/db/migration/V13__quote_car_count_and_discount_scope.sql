-- A quote prices for a number of vehicles, and it has to remember which.
--
-- The figure was read through the source itinerary at pricing time. Editing that itinerary
-- afterwards silently repriced every quote built from it, and a quote with no itinerary fell back
-- to ONE vehicle -- so the crater service and every other per-vehicle charge billed a single car
-- for a party needing two. A flat whole-number error, invisible in a subtotal.
--
-- NULL means "fall back to the itinerary as before", so existing quotes keep the totals they have.
ALTER TABLE quotes
    ADD COLUMN car_count INT NULL AFTER is_sto_rate;
