-- Unlocking an invoice for correction (2026-10-09).
--
-- An invoice stopped being editable the moment it left DRAFT, so a figure found to be wrong after
-- the customer had paid a deposit could only be answered with a credit note: a second document
-- telling the customer the first one was wrong, for a balance they had not yet paid. The invoice
-- itself went on stating the wrong number, which is the number the customer reads.
--
-- ON_HOLD unlocks the invoice in place. The payments are never touched; the release recomputes the
-- status from what has actually been paid against the corrected total, so a correction cannot leave
-- the workflow claiming something the money does not support.
--
-- status_before_hold is not what the release restores. It records where the invoice came from, so a
-- held invoice can say what it was, and hold_reason puts the justification for unlocking a document
-- the customer already holds on the record instead of in somebody's memory.
--
alter table invoices
    add column status_before_hold varchar(50) null,
    add column hold_reason varchar(500) null;
