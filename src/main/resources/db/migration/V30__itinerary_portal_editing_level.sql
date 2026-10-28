-- The middle tier of the editing level (2026-10-27).
--
-- The level has always been documented as resolving company, then itinerary, then journey, most
-- specific winning. Only the two ends existed, so the sentence in EditingLevel was describing a
-- tier that was not there, and the question it answers had nowhere to be answered: a honeymoon
-- built around one lodge and a fixed group departure do not deserve the same freedom, and that
-- is a property of the product rather than of the company or of one customer.
--
-- Null means inherit, which is what every existing row gets and what nearly every row should
-- keep. Only a product with a reason to differ sets it.
--
-- Varchar rather than a native enum, for the reason V27, V28 and V29 all give: a native enum
-- here is frozen at the values it was created with and ddl-auto does not widen one.

alter table itineraries
    add column portal_editing_level varchar(30) null;
