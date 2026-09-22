-- When the tour operator licence lapses.
--
-- The number was already stored; the date it stops being true was not. So nothing in the system
-- knew the licence expires, and nothing could warn anybody. A licence renewed yearly, held in a
-- field with no expiry, is a number that quietly goes stale on every invoice, quote and voucher
-- that prints it.
--
-- This is also what keeps the licence OFF the rubber stamp. A stamp is permanent and the licence
-- is not, so the number belongs on the generated document, where one edit here corrects every
-- future document at once. Engraving it would mean re-cutting the stamp yearly, or worse,
-- stamping this year's paperwork with last year's licence.
--
-- Nullable on purpose: a company with no tour operator licence at all is complete without one,
-- and the completeness check only asks for a date when a number is present.

ALTER TABLE company_profile
    ADD COLUMN licence_expiry DATE NULL AFTER licence_number;
