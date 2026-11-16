-- A sixth notification category: safari operations (2026-11-16).
--
-- Readiness warnings, payment gaps, and the started / completed / post-trip notices were addressed
-- from the BACKUP settings list. That is the same mistake V32 fixed for the client portal, and the
-- comment on Category.CLIENT_PORTAL describes it exactly: nobody deciding who should hear that a
-- safari leaves on Friday with no vehicle assigned would look on a page about database dumps and
-- retention. Worse, the only way to stop the alerts was to empty that list, which also switched off
-- the backup failure warning.
--
-- The category also carries the cadences, because the address was never the whole defect. The
-- payment gap query was startDate <= today with no lower bound and nothing recording that it had
-- already spoken, and a safari in those states cannot leave them on its own: auto-start runs from
-- FULLY_PAID, auto-complete from IN_PROGRESS, auto-close from COMPLETED. One booking nobody marked
-- paid produced a mail subject-lined CRITICAL every morning, indefinitely.
--
-- MySQL stores the category as an enum column, so a value the COLUMN has never heard of is rejected
-- however valid it is in Java, and the settings initializer runs at boot, before readiness. Without
-- this migration the application does not come up and the canary rolls it back. The rule V9 wrote
-- down still holds: a Java enum whose values reach a MySQL enum column is a schema change, not a
-- code change.
--
alter table notification_settings
    modify column category enum (
        'BILL_DUE',
        'BOOKING_INQUIRY',
        'CLIENT_PORTAL',
        'CONTACT_US',
        'NEWSLETTER',
        'SAFARI_OPERATIONS'
    ) not null;
