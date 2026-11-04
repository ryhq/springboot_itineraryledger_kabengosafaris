-- A fifth notification category: the client portal (2026-11-04).
--
-- The portal's office alerts -- a proposal waiting for us, a customer accepting, a customer
-- declining -- were being sent to the BACKUP alert list. That list lives on a screen about backup
-- scheduling, scope, storage and retention, which is not where anybody decides who should hear
-- that a customer accepted a quotation. Twice, while writing the instructions for testing this
-- very feature, its own author looked for the switch on the wrong page. So the portal now has its
-- own entry beside the newsletter, booking-inquiry and contact-us lists, which are the other three
-- "somebody outside has done something and one of us needs to know".
--
-- MySQL stores the category as an enum column, so a value the COLUMN has never heard of is
-- rejected however valid it is in Java. The settings are seeded at boot, so without this migration
-- the initializer tries to insert a row the database refuses, before readiness, and the
-- application never comes up: the canary would roll it back.
--
-- This is the fourth enum column in this schema to need widening, and V9 said the rule plainly
-- when it widened this same column for BILL_DUE: a Java enum whose values reach a MySQL enum
-- column is a schema change, not a code change. EnumColumnsAcceptTheirValuesTest is what stops
-- anybody finding that out in production, and it is what stopped it this time.
--
alter table notification_settings
    modify column category enum (
        'BILL_DUE',
        'BOOKING_INQUIRY',
        'CLIENT_PORTAL',
        'CONTACT_US',
        'NEWSLETTER'
    ) not null;
