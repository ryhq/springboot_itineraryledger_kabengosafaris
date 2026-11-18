-- A quote day gets the calendar date it falls on.
--
-- Until now a quote day knew only its number, and the date was worked out wherever somebody
-- happened to need one - which meant nowhere, because the quotation document never printed a
-- date at all. A client reading "Day 9" cannot plan around a flight.
--
-- The column mirrors safari_days.actual_date: computed from the owner's start date plus the day
-- offset, never typed by hand, recalculated whenever days are added, removed or reordered.
--
-- Nullable on purpose. quotes.safari_start_date is itself nullable, and a quote that has not been
-- given a start date has no dates to state. Inventing one would put a figure on a document that
-- nothing in the system actually believes.

ALTER TABLE quote_days ADD COLUMN day_date DATE NULL AFTER day_number;

UPDATE quote_days qd
  JOIN quotes q ON q.id = qd.quote_id
   SET qd.day_date = DATE_ADD(q.safari_start_date, INTERVAL (qd.day_number - 1) DAY)
 WHERE q.safari_start_date IS NOT NULL;

-- Answers "who is in a lodge on the 8th" across every open quotation, which is the question
-- operations asks when a property calls about availability.
CREATE INDEX idx_quote_day_date ON quote_days (day_date);
