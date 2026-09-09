-- Which line categories a quote's tax and margin uplift apply to.
--
-- A single percentage over the whole quote is wrong for a Tanzanian safari: park, crater and
-- conservation fees are government charges carrying no VAT of ours, so taxing them at the bed rate
-- invents a liability nobody owes. And a provision against suppliers who invoice beyond contract is
-- an accommodation risk, not a park-fee one.
--
-- NULL means every category, so every quote written before this keeps the totals it already has.
ALTER TABLE quotes
    ADD COLUMN tax_applies_to VARCHAR(200) NULL AFTER tax_percentage;

ALTER TABLE quotes
    ADD COLUMN margin_uplift_applies_to VARCHAR(200) NULL AFTER margin_uplift_reason;

-- The same on invoices, because a scope that stops at the quote is not a scope.
--
-- An invoice generated from a safari already inherits the quote's tax PERCENTAGE. Without the
-- scope beside it, a quote taxed 18% on the beds alone (1,281.60 on a 13,177.74 trip) becomes an
-- invoice taxed 18% on everything (2,371.99) -- the client is quoted one figure and billed one
-- 1,090 higher, at the moment the money is actually collected.
ALTER TABLE invoices
    ADD COLUMN tax_applies_to VARCHAR(200) NULL AFTER tax_percentage;
