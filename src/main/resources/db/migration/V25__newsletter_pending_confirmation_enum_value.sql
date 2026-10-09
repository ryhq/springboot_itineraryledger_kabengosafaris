-- Nobody could subscribe to the newsletter (2026-10-09).
--
-- V12 introduced double opt-in: a new subscriber is written as PENDING_CONFIRMATION and only
-- becomes ACTIVE once they click the token. It added confirm_token and confirmed_at, and stopped
-- there. newsletter_subscriptions.status is a native MySQL enum of the three values that existed
-- when the schema was baselined, and PENDING_CONFIRMATION is not among them.
--
-- So NewsletterService.subscribe() has been setting a value the column refuses ever since. MySQL
-- rejects it as a truncation error at commit, which surfaces as a bare DATABASE_ERROR naming
-- nothing, on a public form on both company websites. Found by AnEnumValueExistsInTheColumnTest,
-- written for the identical fault in invoices.status one migration earlier.
--
-- Existing rows are untouched: V12 deliberately left people who subscribed under the old rules
-- ACTIVE, and widening the column does not disturb them.
--
alter table newsletter_subscriptions
    modify column status enum (
        'ACTIVE','BOUNCED','PENDING_CONFIRMATION','UNSUBSCRIBED'
    ) not null;
