-- Our cost, printed on a customer's itinerary.
--
-- An import script wrote lines like
--     "Auto-linked alternative: MID_RANGE in Arusha, STO $90"
-- into the per-night `notes` field. That field is operational -- it is meant for the
-- accommodation plan the office works from -- but four client-facing PDF templates printed it
-- too, so the STO price and the internal budget grading reached customers.
--
-- The templates no longer print it (that is the fix that closes the leak). This clears the rows
-- that already carry cost, for the operational documents that still show the field and for
-- anyone reading the record.
--
-- Scoped to the script's own signature: only notes that both announce themselves as auto-linked
-- and quote a price are touched, so a note somebody typed by hand survives.

UPDATE itinerary_day_accommodations
SET notes = NULL
WHERE notes LIKE '%Auto-linked%'
  AND notes LIKE '%STO $%';

UPDATE safari_day_accommodations
SET notes = NULL
WHERE notes LIKE '%Auto-linked%'
  AND notes LIKE '%STO $%';
