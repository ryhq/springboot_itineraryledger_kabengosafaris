-- A fourth notification category: bill due reminders (2026-08-31).
--
-- MySQL stores the category as an enum column, so a value the COLUMN has never heard of is rejected
-- however valid it is in Java. Adding BILL_DUE to the Java enum without this migration meant the
-- settings initializer tried to insert a row the database refused, at boot, before readiness — so
-- the application never came up and the canary rolled it back.
--
-- This is the third enum column in this schema to need widening (asset_kind was the last), and the
-- rule is worth stating plainly: a Java enum whose values reach a MySQL enum column is a schema
-- change, not a code change.
--
alter table notification_settings
    modify column category enum (
        'BILL_DUE',
        'BOOKING_INQUIRY',
        'CONTACT_US',
        'NEWSLETTER'
    ) not null;
