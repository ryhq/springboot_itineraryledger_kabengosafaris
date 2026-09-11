-- Which line categories a discount comes off.
--
-- The twin of tax_applies_to (V11), and it exists for the same reason read the other way round:
-- a discount is ours to give on the parts of the trip we sell, and park, crater and conservation
-- fees are the authority's own charge, gazetted to the dollar. We cannot discount those, so a
-- single percentage over the whole subtotal either gives away money on a fee we merely collect,
-- or has to be fudged.
--
-- It was fudged. A customer promised 10% off accommodation, transport and activities had to be
-- written up as 7.92% of the whole quote, computed by hand, with the real rule typed into the
-- discount reason so the figure could be explained at all. The next person to edit a line has no
-- way of knowing that 7.92 has to be recomputed.
--
-- NULL means every category, so every quote and invoice written before this keeps its totals.
ALTER TABLE quotes
    ADD COLUMN discount_applies_to VARCHAR(200) NULL AFTER discount_reason;

-- And on invoices, because a scope that stops at the quote is not a scope: the invoice inherits
-- the quote's discount percentage, and without the scope beside it the discount would be taken
-- off the park fees at the moment the money is actually collected.
ALTER TABLE invoices
    ADD COLUMN discount_applies_to VARCHAR(200) NULL AFTER discount_reason;
