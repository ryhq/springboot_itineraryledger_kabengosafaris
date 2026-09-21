-- FLIGHT joins the line-item type on a quote and on an invoice.
--
-- The Java enums got the constant in the commit that taught the engines to price a flight; these
-- columns did not, and they are MySQL ENUMs rather than VARCHARs. So every insert of a flight line
-- failed at the database, and the quote's recalculate answered 500 with nothing to say about why.
--
-- The comment on CostItemType already warns that adding a value there is only half the change. It
-- names the aggregator branch, which is the half that loses money quietly. This is the other half,
-- and it is louder: nothing is mispriced, the write simply does not happen. Found by calling
-- recalculate against a real quote with a flight on it — the 362-test suite is green either way,
-- because no test writes a QuoteItem to MySQL.
--
-- The value list is restated in full because MySQL's MODIFY COLUMN replaces the definition rather
-- than adding to it; dropping one of the existing names here would silently invalidate every row
-- already holding it.
--
-- credit_note_line_items is deliberately untouched: CreditNoteItemType has no FLIGHT, because a
-- credit note is issued against something that went wrong on a trip (OVERCHARGE, SERVICE_ISSUE),
-- and a refunded air ticket is an OVERCHARGE like any other. Widening a column no Java constant
-- can write would be dead schema.

ALTER TABLE quote_items
    MODIFY COLUMN item_type ENUM (
        'ACCOMMODATION','ACTIVITY','EQUIPMENT','FLIGHT','GUIDE','INSURANCE',
        'MEALS','OTHER','PARK_FEE','TRANSPORT','VISA'
    ) NOT NULL;

ALTER TABLE invoice_line_items
    MODIFY COLUMN item_type ENUM (
        'ACCOMMODATION','ACTIVITY','EQUIPMENT','FLIGHT','GUIDE','INSURANCE',
        'MEALS','OTHER','PARK_FEE','TRANSPORT','VISA'
    ) NOT NULL;
